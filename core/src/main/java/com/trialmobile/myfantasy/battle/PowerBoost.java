package com.trialmobile.myfantasy.battle;

/**
 * 2배 버프: 켜면 {@link #DURATION_MS} 동안 공격(타격 총합·공격 스킬 피해)과 골드 획득이 2배.
 * 켠 시각부터 {@link #COOLDOWN_MS}가 지나야 다시 켤 수 있다. 실제 시각(epoch 밀리초) 기준이라
 * 앱을 꺼 두어도 시간이 흐른다. 나중에 보상형 광고를 본 뒤 {@link #activate}를 부르면 된다.
 * <p>
 * 순수 Java. 시각은 항상 호출자가 넘긴다(테스트·오프라인 시뮬레이션용).
 */
public final class PowerBoost {
    public static final long DURATION_MS = 15L * 60L * 1000L;
    public static final long COOLDOWN_MS = 2L * 60L * 60L * 1000L;
    public static final int MULTIPLIER = 2;

    /** 버프가 끝나는 시각. 0 = 켠 적 없음. */
    private long activeUntil;
    /** 다시 켤 수 있게 되는 시각. */
    private long readyAt;

    /** 켜진 구간 [activeUntil - DURATION, activeUntil) 안인지. 오프라인 시뮬레이션은 켜기 전 시각도 지나가므로 시작도 본다. */
    public boolean isActive(long now) {
        return now < activeUntil && now >= activeUntil - DURATION_MS;
    }

    /** 지금 켤 수 있는지(켜져 있지 않고 쿨타임도 끝남). */
    public boolean isReady(long now) {
        return !isActive(now) && now >= readyAt;
    }

    /** 켤 수 있으면 켜고 true. */
    public boolean activate(long now) {
        if (!isReady(now)) return false;
        activeUntil = now + DURATION_MS;
        readyAt = now + COOLDOWN_MS;
        return true;
    }

    public long activeRemaining(long now) {
        return isActive(now) ? activeUntil - now : 0L;
    }

    public long cooldownRemaining(long now) {
        return Math.max(0L, readyAt - now);
    }

    /** 지금 적용되는 배율(1 또는 {@link #MULTIPLIER}). */
    public int multiplier(long now) {
        return isActive(now) ? MULTIPLIER : 1;
    }

    public long getActiveUntil() {
        return activeUntil;
    }

    public long getReadyAt() {
        return readyAt;
    }

    /**
     * 저장 데이터에서 되살린다. 저장 시각보다 지나치게 먼 미래 값(기기 시계 조작 등)은 정상 범위로 줄인다.
     */
    public void restore(long activeUntil, long readyAt, long savedAt) {
        this.activeUntil = Math.max(0L, Math.min(activeUntil, savedAt + DURATION_MS));
        this.readyAt = Math.max(0L, Math.min(readyAt, savedAt + COOLDOWN_MS));
    }
}
