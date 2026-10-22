package com.trialmobile.myfantasy.battle;

import com.trialmobile.myfantasy.rules.Combat;
import com.trialmobile.myfantasy.rules.Dice;
import com.trialmobile.myfantasy.rules.Hero;
import com.trialmobile.myfantasy.rules.Monster;
import com.trialmobile.myfantasy.rules.RoundResult;
import com.trialmobile.myfantasy.rules.Skill;
import com.trialmobile.myfantasy.rules.SkillBook;
import com.trialmobile.myfantasy.rules.SkillCast;
import com.trialmobile.myfantasy.rules.Spells;

/**
 * 방치형 자동 전투 진행기. libGDX에 의존하지 않으므로 화면 없이도 돌릴 수 있다(시뮬레이션, 테스트, 오프라인 보상).
 * <p>
 * 흐름: 몬스터 등장 → 일정 간격으로 전투 라운드 → 처치 시 보상 후 다음 웨이브.
 * 마지막 웨이브(보스)를 잡으면 다음 스테이지로 간다. 영웅이 쓰러지면 잠시 후 부활해 현재 스테이지 첫 웨이브부터 다시 한다.
 * <p>
 * 영웅이 쓰러지면 사냥 모드가 된다: 쓰러진 웨이브 앞까지만 반복하며 경험치를 모으고(첫 웨이브였다면 이전 스테이지로 후퇴),
 * 레벨이 오르면 다시 앞으로 나아간다. 플레이어가 직접 재도전하게 하려면 {@link #challengeBoss()}를 호출한다.
 * <p>
 * 스킬: 라운드와 별개로 스킬마다 쿨타임이 흐르고, 전투 중 쿨타임이 끝나면 그 순간 자동 발동한다.
 * 시간은 밀리초 정수로 다루고, "다음 라운드"와 "다음 스킬 발동" 중 빠른 사건까지 건너뛰는 방식으로 진행한다.
 * 그래서 프레임 단위 진행({@link #update})과 오프라인 빨리 감기({@link #fastForward})가 정확히 같은 결과를 낸다.
 */
public final class BattleSystem {
    /** 전투 라운드 간격(밀리초). */
    public static final long ROUND_INTERVAL_MS = 900;
    /** 몬스터 처치 후 다음 몬스터 등장까지(밀리초). 주사위 착지 → 공격 동작 → 처치 연출(쓰러짐·사라짐)이 다 보이도록. */
    public static final long SPAWN_DELAY_MS = 1300;
    /** 영웅이 쓰러진 뒤 부활까지(밀리초). */
    public static final long REVIVE_DELAY_MS = 2000;
    /** 몬스터를 잡을 때마다 회복하는 최대 체력 비율. */
    public static final double HEAL_ON_KILL = 0.2;
    /** 한 번의 update에서 처리할 최대 사건 수(프레임이 크게 밀렸을 때 보호). */
    private static final int MAX_EVENTS_PER_UPDATE = 50;

    public enum Phase { SPAWNING, FIGHTING, HERO_DOWN }

    /** 현재 시각(epoch 밀리초). 2배 버프 판정에 쓴다. 테스트에서 바꿔 끼운다. */
    public interface Clock {
        long nowMillis();
    }

    public static final Clock SYSTEM_CLOCK = new Clock() {
        @Override
        public long nowMillis() {
            return System.currentTimeMillis();
        }
    };

    /** 전투 이벤트 수신자. 화면은 이걸로 UI를 갱신한다. */
    public interface Listener {
        void onMonsterAppeared(Monster monster);

        void onRound(RoundResult result, Hero hero, Monster monster);

        void onSkillCast(SkillCast cast, Hero hero, Monster monster);

        void onMonsterDefeated(Monster monster, int ap, int gold);

        void onLevelUp(Hero hero);

        void onStageCleared(String clearedLabel);

        void onHeroDefeated(Monster monster);

        void onHeroRevived(Hero hero);
    }

    /** 필요한 이벤트만 골라 받을 때 쓰는 빈 구현. */
    public static class ListenerAdapter implements Listener {
        @Override public void onMonsterAppeared(Monster monster) { }
        @Override public void onRound(RoundResult result, Hero hero, Monster monster) { }
        @Override public void onSkillCast(SkillCast cast, Hero hero, Monster monster) { }
        @Override public void onMonsterDefeated(Monster monster, int ap, int gold) { }
        @Override public void onLevelUp(Hero hero) { }
        @Override public void onStageCleared(String clearedLabel) { }
        @Override public void onHeroDefeated(Monster monster) { }
        @Override public void onHeroRevived(Hero hero) { }
    }

    private final PlayerState state;
    private final Dice dice;
    private Listener listener = new ListenerAdapter();

    private Phase phase = Phase.SPAWNING;
    private Monster monster;
    /** 다음 단계(등장/라운드/부활)까지 남은 시간(밀리초). */
    private long timer = SPAWN_DELAY_MS;
    /** 프레임 시간 중 아직 1밀리초가 안 돼서 이월된 부분. */
    private double carryMillis;
    private float speed = 1f;
    private Clock clock = SYSTEM_CLOCK;
    /** 오프라인 빨리 감기 중의 가상 시각(그 외에는 -1). 비운 시간 중 버프가 켜져 있던 부분만 2배가 된다. */
    private long simulatedNow = -1L;

    public BattleSystem(PlayerState state, Dice dice) {
        this.state = state;
        this.dice = dice;
    }

    public void setListener(Listener listener) {
        this.listener = listener == null ? new ListenerAdapter() : listener;
    }

    public void setClock(Clock clock) {
        this.clock = clock == null ? SYSTEM_CLOCK : clock;
    }

    /** 버프 판정 기준 시각. 오프라인 빨리 감기 중이면 그 시점의 가상 시각. */
    public long now() {
        return simulatedNow >= 0L ? simulatedNow : clock.nowMillis();
    }

    /** 지금 공격·골드에 걸리는 배율. */
    public int boostMultiplier() {
        return state.boost.multiplier(now());
    }

    /** 전투 속도 배율(1 = 기본). */
    public void setSpeed(float speed) {
        this.speed = Math.max(0.1f, speed);
    }

    public float getSpeed() {
        return speed;
    }

    public PlayerState getState() {
        return state;
    }

    public Monster getMonster() {
        return monster;
    }

    public Phase getPhase() {
        return phase;
    }

    /** 사냥 모드를 끄고, 다음 웨이브부터 다시 앞으로 나아가게 한다. */
    public void challengeBoss() {
        state.progress.setFarming(false);
    }

    /** 매 프레임 호출. */
    public void update(float delta) {
        carryMillis += (double) delta * speed * 1000.0;
        long millis = (long) carryMillis;
        carryMillis -= millis;
        advance(millis, MAX_EVENTS_PER_UPDATE);
    }

    /**
     * 게임 시간을 {@code seconds}초만큼 한 번에 진행한다(오프라인 보상).
     * 실제 전투 규칙을 그대로 돌리므로 보상이 실제 플레이와 같은 기준으로 계산된다.
     * 그동안의 이벤트는 화면에 보내지 않고 요약으로 돌려준다. 속도 배율은 적용하지 않는다.
     */
    public OfflineReport fastForward(double seconds) {
        OfflineReport report = new OfflineReport(state, seconds);
        Listener saved = listener;
        listener = report;
        long millis = Math.round(Math.max(0.0, seconds) * 1000.0);
        simulatedNow = Math.max(0L, clock.nowMillis() - millis); // 떠났던 시각부터 다시 흘려 보낸다
        try {
            advance(millis, Integer.MAX_VALUE);
        } finally {
            listener = saved;
            simulatedNow = -1L;
        }
        report.finish(state);
        return report;
    }

    /**
     * 시간을 진행하면서 그 사이에 일어나는 사건(라운드, 스킬 발동 등)을 순서대로 처리한다.
     * 같은 시각이면 스킬이 라운드보다 먼저 발동한다.
     */
    private void advance(long millis, int maxEvents) {
        long remaining = Math.max(0L, millis);
        Hero hero = state.hero;
        for (int events = 0; events < maxEvents; events++) {
            long untilSkill = phase == Phase.FIGHTING
                ? Spells.millisUntilNextCast(hero, monster) : Long.MAX_VALUE;
            long untilStep = Math.max(0L, timer);
            boolean skillFirst = untilSkill <= untilStep;
            long next = skillFirst ? untilSkill : untilStep;
            if (next > remaining) {
                passTime(remaining);
                return;
            }
            passTime(next);
            remaining -= next;
            if (skillFirst) castReadySkills();
            else step();
        }
        // 사건 한도를 넘었다: 처리하지 못한 밀린 시간은 버린다(실시간 진행에서만 발생).
        if (timer < 0L) timer = 0L;
    }

    private void passTime(long millis) {
        if (millis <= 0L) return;
        if (simulatedNow >= 0L) simulatedNow += millis;
        timer -= millis;
        state.hero.getSkills().tickMillis(millis);
    }

    /** 다음 단계 하나를 즉시 실행하고, 그 다음 단계까지의 대기 시간을 예약한다. */
    public void step() {
        switch (phase) {
            case SPAWNING:
                spawn();
                break;
            case FIGHTING:
                fight();
                break;
            case HERO_DOWN:
                state.hero.healFull();
                listener.onHeroRevived(state.hero);
                phase = Phase.SPAWNING;
                timer = SPAWN_DELAY_MS;
                break;
        }
    }

    private void spawn() {
        monster = MonsterTable.create(state.progress, dice);
        state.rebirth.recordStage(state.progress.stageIndex());
        phase = Phase.FIGHTING;
        listener.onMonsterAppeared(monster);
        timer = ROUND_INTERVAL_MS;
    }

    private void fight() {
        Hero hero = state.hero;
        RoundResult result = Combat.resolveRound(hero, monster, dice, boostMultiplier(),
            state.rebirth.hitBonusPercent());
        listener.onRound(result, hero, monster);
        if (!resolveOutcome()) timer = ROUND_INTERVAL_MS;
    }

    /** 쿨타임이 끝났고 지금 효과가 있는 스킬을 모두 발동한다(Skill 순서대로). */
    private void castReadySkills() {
        Hero hero = state.hero;
        SkillBook book = hero.getSkills();
        for (Skill skill : Skill.values()) {
            if (phase != Phase.FIGHTING) return;
            if (!book.isReady(skill) || !Spells.canCast(skill, hero, monster)) continue;
            SkillCast cast = Spells.cast(skill, hero, monster, dice, boostMultiplier());
            listener.onSkillCast(cast, hero, monster);
            resolveOutcome();
        }
    }

    /**
     * 피해가 적용된 뒤 처치/패배를 처리한다.
     * @return 이번 몬스터와의 전투가 끝났으면 true (다음 단계 대기 시간도 예약됨)
     */
    private boolean resolveOutcome() {
        Hero hero = state.hero;
        if (monster.isDead()) {
            int ap = Rebirth.withBonus(MonsterTable.apReward(monster), state.rebirth.apBonusPercent());
            int gold = Rebirth.withBonus(MonsterTable.goldReward(monster, dice), state.rebirth.goldBonusPercent())
                * boostMultiplier();
            state.addGold(gold);
            state.addKill();
            int levels = hero.gainAp(ap);
            listener.onMonsterDefeated(monster, ap, gold);
            if (levels > 0) {
                state.progress.setFarming(false); // 강해졌으니 보스 재도전
                listener.onLevelUp(hero);
            }
        }

        if (hero.isDead()) {
            Monster killer = monster;
            monster = null;
            state.progress.retreatAfterDefeat(); // 쓰러진 웨이브 앞까지만 사냥
            phase = Phase.HERO_DOWN;
            timer = REVIVE_DELAY_MS;
            listener.onHeroDefeated(killer);
            return true;
        }

        if (monster.isDead()) {
            monster = null;
            hero.heal((int) Math.ceil(hero.maxCon() * HEAL_ON_KILL));
            String label = state.progress.label();
            if (state.progress.advance()) {
                hero.healFull();
                listener.onStageCleared(label);
            }
            phase = Phase.SPAWNING;
            timer = SPAWN_DELAY_MS;
            return true;
        }
        return false;
    }
}
