package com.trialmobile.myfantasy.rules;

/**
 * 터널스 앤 트롤스 전투 라운드 판정.
 * <ol>
 *   <li>양쪽이 공격 굴림(주사위 + 보정치)을 해서 타격 총합(Hit Point Total)을 낸다.</li>
 *   <li>총합이 낮은 쪽이 그 차이만큼 피해를 입는다. 방어구가 일부를 흡수한다.</li>
 *   <li>스파이트 데미지(7판): 주사위에서 나온 6 하나당 1점은 방어구를 무시하고 상대에게 들어간다.
 *       진 쪽의 6은 이긴 쪽에게 피해를 주고, 이긴 쪽의 6은 방어구로 막아도 최소 피해로 남는다.</li>
 *   <li>동점이면 차이 피해는 없고, 양쪽의 스파이트 데미지만 서로에게 들어간다.</li>
 * </ol>
 */
public final class Combat {
    private Combat() {
    }

    /** 한 라운드를 굴리고 피해를 적용한다. */
    public static RoundResult resolveRound(Combatant a, Combatant b, Dice dice) {
        return resolveRound(a, b, dice, 1);
    }

    /** {@code aMultiplier}: a의 타격 총합 배율(2배 버프). 6의 개수는 배율과 무관하다. */
    public static RoundResult resolveRound(Combatant a, Combatant b, Dice dice, int aMultiplier) {
        return resolveRound(a, b, dice, aMultiplier, 0);
    }

    /** {@code aBonusPercent}: a의 타격 총합 퍼센트 보너스(환생 특성). */
    public static RoundResult resolveRound(Combatant a, Combatant b, Dice dice, int aMultiplier, int aBonusPercent) {
        DiceRoll rollA = dice.roll(a.combatDice(), a.combatAdds()).multiplied(aMultiplier).withBonus(aBonusPercent);
        DiceRoll rollB = dice.roll(b.combatDice(), b.combatAdds());
        return apply(a, b, rollA, rollB);
    }

    /** 이미 굴린 결과로 피해를 계산해 적용한다(테스트용으로도 쓴다). */
    public static RoundResult apply(Combatant a, Combatant b, DiceRoll rollA, DiceRoll rollB) {
        int diff = rollA.total() - rollB.total();
        int damageToA;
        int damageToB;
        if (diff > 0) {
            damageToB = Math.max(diff - b.armorHits(), rollA.sixes);
            damageToA = rollB.sixes;
        } else if (diff < 0) {
            damageToA = Math.max(-diff - a.armorHits(), rollB.sixes);
            damageToB = rollA.sixes;
        } else {
            damageToA = rollB.sixes;
            damageToB = rollA.sixes;
        }
        a.takeDamage(damageToA);
        b.takeDamage(damageToB);
        return new RoundResult(rollA, rollB, damageToA, damageToB);
    }
}
