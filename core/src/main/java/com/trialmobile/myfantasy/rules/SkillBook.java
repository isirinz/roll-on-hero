package com.trialmobile.myfantasy.rules;

/**
 * 영웅이 가진 스킬의 레벨과 전투 중 타이머(쿨타임, 버프 남은 시간).
 * 레벨 0은 아직 해금되지 않은 상태다. 타이머는 저장하지 않는다.
 * <p>
 * 시간은 밀리초 정수로 다룬다. 실수로 계산하면 프레임을 어떻게 나누느냐에 따라 사건 순서가 미세하게 달라져
 * 실시간 진행과 오프라인 빨리 감기의 결과가 어긋나기 때문이다.
 */
public final class SkillBook {
    private final int[] levels = new int[Skill.values().length];
    private final long[] cooldowns = new long[Skill.values().length];
    private long vorpalRemaining;

    public int getLevel(Skill skill) {
        return levels[skill.ordinal()];
    }

    public void setLevel(Skill skill, int level) {
        levels[skill.ordinal()] = Math.max(0, level);
    }

    public boolean isUnlocked(Skill skill) {
        return getLevel(skill) > 0;
    }

    /**
     * 영웅 레벨에 맞춰 새 스킬을 해금한다.
     * @return 새로 해금된 스킬 수
     */
    public int syncUnlocks(int heroLevel) {
        int unlocked = 0;
        for (Skill s : Skill.values()) {
            if (heroLevel >= s.unlockLevel && levels[s.ordinal()] == 0) {
                levels[s.ordinal()] = 1;
                unlocked++;
            }
        }
        return unlocked;
    }

    // --- 타이머 ---

    /** 시간이 흐른다(밀리초): 쿨타임과 버프가 줄어든다. */
    public void tickMillis(long millis) {
        if (millis <= 0) return;
        for (int i = 0; i < cooldowns.length; i++) {
            cooldowns[i] = Math.max(0L, cooldowns[i] - millis);
        }
        vorpalRemaining = Math.max(0L, vorpalRemaining - millis);
    }

    /** 시간이 흐른다(초). 테스트·도구용 편의 메서드. */
    public void tick(double seconds) {
        tickMillis(Math.round(seconds * 1000.0));
    }

    /** 남은 쿨타임(밀리초). */
    public long getCooldownMillis(Skill skill) {
        return cooldowns[skill.ordinal()];
    }

    public boolean isReady(Skill skill) {
        return isUnlocked(skill) && cooldowns[skill.ordinal()] <= 0L;
    }

    /** 쿨타임 진행률: 0(방금 사용) ~ 1(사용 가능). */
    public float readiness(Skill skill) {
        return 1f - (float) cooldowns[skill.ordinal()] / skill.cooldownMillis;
    }

    void startCooldown(Skill skill) {
        cooldowns[skill.ordinal()] = skill.cooldownMillis;
    }

    public boolean isVorpalActive() {
        return vorpalRemaining > 0L;
    }

    public long getVorpalRemainingMillis() {
        return vorpalRemaining;
    }

    void startVorpal(long millis) {
        vorpalRemaining = millis;
    }

    /** 쿨타임과 버프를 모두 초기화한다. */
    public void resetTimers() {
        for (int i = 0; i < cooldowns.length; i++) cooldowns[i] = 0L;
        vorpalRemaining = 0L;
    }
}
