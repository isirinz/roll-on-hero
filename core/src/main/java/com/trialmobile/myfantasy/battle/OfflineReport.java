package com.trialmobile.myfantasy.battle;

import com.trialmobile.myfantasy.rules.Hero;
import com.trialmobile.myfantasy.rules.Monster;
import com.trialmobile.myfantasy.rules.SkillCast;

/**
 * 오프라인 보상 요약. {@link BattleSystem#fastForward(double)}가 진행 중 이벤트를 모아 만든다.
 */
public final class OfflineReport extends BattleSystem.ListenerAdapter {
    /** 이 시간보다 짧게 비웠으면 보상 화면을 띄우지 않는다(초). */
    public static final double MIN_SECONDS = 60.0;
    /** 처음(챕터 1)의 최대 인정 시간(시간). 새 챕터에 처음 도달할 때마다 1시간씩 늘어난다. */
    public static final int BASE_HOURS = 2;
    /** 최대 인정 시간의 상한(시간). 챕터 11에 도달하면 이 값이 된다. */
    public static final int MAX_HOURS = 12;
    /** 오프라인 보상 최대 인정 시간의 절대 상한(초). */
    public static final double MAX_SECONDS = MAX_HOURS * 60 * 60;

    public final double seconds;
    public final String startStage;
    public final int startLevel;
    private final long startGold;

    /** 이 보상에 적용된 최대 인정 시간(초). 0이면 모름. 화면이 채워 넣는다. */
    public double capSeconds;

    public String endStage;
    public int endLevel;
    public long goldGained;
    public long apGained;
    public int kills;
    public int stagesCleared;
    public int defeats;
    public int skillCasts;

    OfflineReport(PlayerState state, double seconds) {
        this.seconds = seconds;
        this.startStage = state.progress.label();
        this.startLevel = state.hero.getLevel();
        this.startGold = state.getGold();
    }

    void finish(PlayerState state) {
        endStage = state.progress.label();
        endLevel = state.hero.getLevel();
        goldGained = state.getGold() - startGold;
    }

    /** 최대 인정 시간(시간): 2시간 + 모든 생 최고 기록의 챕터마다 1시간, 최대 12시간. 1장 2h, 5장 6h, 10장 11h, 11장~ 12h. */
    public static int maxHoursFor(int bestStageEver) {
        int chapterIndex = Math.max(0, bestStageEver) / StageProgress.STAGES_PER_CHAPTER;
        return Math.min(MAX_HOURS, BASE_HOURS + chapterIndex);
    }

    /** {@link #maxHoursFor}를 초로. */
    public static double maxSecondsFor(int bestStageEver) {
        return maxHoursFor(bestStageEver) * 3600.0;
    }

    /** 실제로 비운 시간을 보상 인정 시간으로 바꾼다(절대 상한 12시간). 너무 짧으면 0. */
    public static double clampSeconds(double awaySeconds) {
        return clampSeconds(awaySeconds, MAX_SECONDS);
    }

    /** 실제로 비운 시간을 최대 capSeconds까지 인정한다. 너무 짧으면 0. */
    public static double clampSeconds(double awaySeconds, double capSeconds) {
        if (Double.isNaN(awaySeconds) || awaySeconds < MIN_SECONDS) return 0.0;
        return Math.min(awaySeconds, Math.min(capSeconds, MAX_SECONDS));
    }

    /** 최대 인정 시간까지 꽉 채웠는지(더 오래 비웠어도 잘렸을 수 있다). */
    public boolean reachedCap() {
        return capSeconds > 0 && seconds >= capSeconds - 1.0;
    }

    /** "2시간 13분" 같은 표기. */
    public static String formatDuration(double seconds) {
        long total = Math.round(seconds);
        long h = total / 3600;
        long m = (total % 3600) / 60;
        if (h > 0) return m > 0 ? h + "시간 " + m + "분" : h + "시간";
        if (m > 0) return m + "분";
        return total + "초";
    }

    @Override
    public void onMonsterDefeated(Monster monster, int ap, int gold) {
        kills++;
        apGained += ap;
    }

    @Override
    public void onStageCleared(String clearedLabel) {
        stagesCleared++;
    }

    @Override
    public void onHeroDefeated(Monster monster) {
        defeats++;
    }

    @Override
    public void onSkillCast(SkillCast cast, Hero hero, Monster monster) {
        skillCasts++;
    }
}
