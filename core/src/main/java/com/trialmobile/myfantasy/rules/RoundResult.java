package com.trialmobile.myfantasy.rules;

/** 전투 한 라운드의 결과 (a = 공격 측/영웅, b = 상대/몬스터). */
public final class RoundResult {
    public final DiceRoll rollA;
    public final DiceRoll rollB;
    public final int damageToA;
    public final int damageToB;

    public RoundResult(DiceRoll rollA, DiceRoll rollB, int damageToA, int damageToB) {
        this.rollA = rollA;
        this.rollB = rollB;
        this.damageToA = damageToA;
        this.damageToB = damageToB;
    }

    /** a가 타격 총합에서 이겼는지. */
    public boolean aWon() {
        return rollA.total() > rollB.total();
    }

    public boolean tie() {
        return rollA.total() == rollB.total();
    }
}
