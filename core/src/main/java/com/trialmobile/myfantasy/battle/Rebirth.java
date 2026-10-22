package com.trialmobile.myfantasy.battle;

import com.trialmobile.myfantasy.rules.Attribute;
import com.trialmobile.myfantasy.rules.Hero;

/**
 * 환생: 지금까지의 모험(레벨·장비·골드·스킬·스테이지)을 내려놓고 처음부터 다시 시작하는 대신,
 * 이번 생에 도달한 가장 깊은 스테이지만큼 영혼석을 얻는다. 영혼석으로 산 혼(영구 특성)은 환생해도 남는다.
 * <p>
 * 순수 Java. {@link PlayerState#rebirth}에 들어 있고 저장된다(세이브 버전 4).
 */
public final class Rebirth {
    /** 환생할 수 있는 최소 기록: 스테이지 번호(0부터) 10 = 챕터 2-1. */
    public static final int UNLOCK_STAGE_INDEX = StageProgress.STAGES_PER_CHAPTER;

    /** 영구 특성(혼). 레벨당 효과, 첫 레벨 값(영혼석). 값은 레벨마다 base씩 늘어난다. */
    public enum Perk {
        /** 전사의 혼: 타격 총합 +5%/레벨. */
        MIGHT("might", 5, 1),
        /** 황금의 혼: 골드 +10%/레벨. */
        FORTUNE("fortune", 10, 1),
        /** 현자의 혼: 모험 점수(경험치) +10%/레벨. */
        WISDOM("wisdom", 10, 1),
        /** 영웅의 혼: 모든 능력치 +1/레벨(지금 영웅에게도 바로, 환생 뒤 시작 능력치에도). */
        HEROIC("heroic", 1, 3);

        public final String id;
        /** 레벨당 효과(퍼센트 또는 능력치 점수). */
        public final int perLevel;
        /** 1레벨 값. n레벨로 올리는 값은 baseCost * n. */
        public final int baseCost;

        Perk(String id, int perLevel, int baseCost) {
            this.id = id;
            this.perLevel = perLevel;
            this.baseCost = baseCost;
        }
    }

    /** 쓰지 않은 영혼석. */
    private long souls;
    /** 지금까지 얻은 영혼석 합계(기록용). */
    private long totalSouls;
    private int rebirths;
    /** 이번 생에 도달한 가장 깊은 스테이지 번호(0부터). */
    private int bestStage;
    /** 모든 생을 통틀어 도달한 가장 깊은 스테이지 번호(0부터). 환생해도 줄지 않는다(오프라인 보상 최대 시간 기준). */
    private int bestStageEver;
    private final int[] perkLevels = new int[Perk.values().length];

    // ------------------------------------------------------------------
    // 환생

    /** 이번 생에 도달한 스테이지를 기록한다(몬스터가 나올 때마다 호출). */
    public void recordStage(int stageIndex) {
        if (stageIndex > bestStage) bestStage = stageIndex;
        if (stageIndex > bestStageEver) bestStageEver = stageIndex;
    }

    public boolean canRebirth() {
        return bestStage >= UNLOCK_STAGE_INDEX;
    }

    /** 지금 환생하면 받는 영혼석: (최고 스테이지 번호 / 5)^1.6, 내림. 2-1 → 3, 5-1 → 27, 10-1 → 101. */
    public long soulsForRebirth() {
        return soulsFor(bestStage);
    }

    public static long soulsFor(int stageIndex) {
        if (stageIndex < UNLOCK_STAGE_INDEX) return 0L;
        return (long) Math.floor(Math.pow(stageIndex / 5.0, 1.6));
    }

    /**
     * 환생한 새 상태를 만든다. 혼·영혼석·환생 횟수·2배 버프는 이어 가고, 나머지는 새 게임처럼 시작한다.
     * 영웅 이름은 그대로, 시작 능력치에는 영웅의 혼 보너스를 더한다.
     * @return 환생할 수 없으면 null
     */
    public static PlayerState reborn(PlayerState old) {
        Rebirth r = old.rebirth;
        if (!r.canRebirth()) return null;
        PlayerState next = new PlayerState(Hero.createStarter(old.hero.getName()));
        Rebirth n = next.rebirth;
        long earned = r.soulsForRebirth();
        n.souls = r.souls + earned;
        n.totalSouls = r.totalSouls + earned;
        n.rebirths = r.rebirths + 1;
        n.bestStage = 0;
        n.bestStageEver = Math.max(r.bestStageEver, r.bestStage);
        System.arraycopy(r.perkLevels, 0, n.perkLevels, 0, n.perkLevels.length);
        n.applyAttributeBonus(next.hero, n.attributeBonus());
        next.boost.restore(old.boost.getActiveUntil(), old.boost.getReadyAt(), Long.MAX_VALUE / 4);
        return next;
    }

    // ------------------------------------------------------------------
    // 혼(영구 특성)

    public int getLevel(Perk perk) {
        return perkLevels[perk.ordinal()];
    }

    /** 다음 레벨로 올리는 값(영혼석). */
    public long cost(Perk perk) {
        return (long) perk.baseCost * (getLevel(perk) + 1);
    }

    public boolean canBuy(Perk perk) {
        return souls >= cost(perk);
    }

    /** 영혼석을 내고 한 레벨 올린다. 영웅의 혼은 지금 영웅의 능력치도 바로 올린다. */
    public boolean buy(Perk perk, Hero hero) {
        long c = cost(perk);
        if (souls < c) return false;
        souls -= c;
        perkLevels[perk.ordinal()]++;
        if (perk == Perk.HEROIC && hero != null) applyAttributeBonus(hero, perk.perLevel);
        return true;
    }

    private void applyAttributeBonus(Hero hero, int amount) {
        if (amount <= 0) return;
        for (Attribute a : Attribute.values()) hero.getAttributes().add(a, amount);
        hero.healFull();
    }

    /** 타격 총합 보너스(퍼센트). */
    public int hitBonusPercent() {
        return getLevel(Perk.MIGHT) * Perk.MIGHT.perLevel;
    }

    public int goldBonusPercent() {
        return getLevel(Perk.FORTUNE) * Perk.FORTUNE.perLevel;
    }

    public int apBonusPercent() {
        return getLevel(Perk.WISDOM) * Perk.WISDOM.perLevel;
    }

    /** 모든 능력치 보너스(점수). */
    public int attributeBonus() {
        return getLevel(Perk.HEROIC) * Perk.HEROIC.perLevel;
    }

    /** 값에 퍼센트 보너스를 더한다(내림). */
    public static int withBonus(int value, int percent) {
        if (percent <= 0) return value;
        return (int) Math.min(Integer.MAX_VALUE, (long) value * (100 + percent) / 100);
    }

    // ------------------------------------------------------------------

    public long getSouls() {
        return souls;
    }

    public long getTotalSouls() {
        return totalSouls;
    }

    public int getRebirths() {
        return rebirths;
    }

    public int getBestStage() {
        return bestStage;
    }

    public int getBestStageEver() {
        return Math.max(bestStageEver, bestStage);
    }

    /** "3-4" 같은 표기(스테이지 번호 0부터). */
    public static String stageLabel(int stageIndex) {
        int chapter = stageIndex / StageProgress.STAGES_PER_CHAPTER + 1;
        int stage = stageIndex % StageProgress.STAGES_PER_CHAPTER + 1;
        return chapter + "-" + stage;
    }

    public int[] getPerkLevels() {
        return perkLevels.clone();
    }

    /** 저장 데이터에서 되살린다(모든 생 최고 기록 = 이번 생 기록). */
    public void restore(long souls, long totalSouls, int rebirths, int bestStage, int[] levels) {
        restore(souls, totalSouls, rebirths, bestStage, bestStage, levels);
    }

    /** 저장 데이터에서 되살린다. 음수나 이상한 값은 0으로. */
    public void restore(long souls, long totalSouls, int rebirths, int bestStage, int bestStageEver, int[] levels) {
        this.souls = Math.max(0L, souls);
        this.totalSouls = Math.max(this.souls, totalSouls);
        this.rebirths = Math.max(0, rebirths);
        this.bestStage = Math.max(0, bestStage);
        this.bestStageEver = Math.max(this.bestStage, bestStageEver);
        for (int i = 0; i < perkLevels.length; i++) {
            perkLevels[i] = levels != null && i < levels.length ? Math.max(0, levels[i]) : 0;
        }
    }
}
