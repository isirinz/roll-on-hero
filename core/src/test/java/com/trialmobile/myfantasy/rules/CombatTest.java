package com.trialmobile.myfantasy.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CombatTest {

    private static DiceRoll roll(int count, int adds, int sum, int sixes) {
        return new DiceRoll(count, adds, sum, sixes);
    }

    private static Hero hero(Item armor) {
        Hero h = new Hero("테스트", new Attributes(12, 30, 12, 12, 12, 10, 10, 10));
        h.getEquipment().equip(ItemCatalog.SHORT_SWORD);
        if (armor != null) h.getEquipment().equip(armor);
        return h;
    }

    @Test
    public void winnerDealsDifferenceToLoser() {
        Hero h = hero(null);
        Monster m = new Monster("고블린", 30);
        RoundResult r = Combat.apply(h, m, roll(3, 0, 15, 0), roll(4, 15, 0, 0));
        // 15 vs 15 → 동점
        assertTrue(r.tie());
        assertEquals(0, r.damageToA);
        assertEquals(0, r.damageToB);

        r = Combat.apply(h, m, roll(3, 0, 18, 0), roll(4, 0, 10, 0));
        assertTrue(r.aWon());
        assertEquals(8, r.damageToB);
        assertEquals(22, m.getMr());
        assertEquals(30, h.getHp());
    }

    @Test
    public void armorAbsorbsLoserDamage() {
        Hero h = hero(ItemCatalog.LEATHER); // 6 흡수
        Monster m = new Monster("오크", 40);
        RoundResult r = Combat.apply(h, m, roll(3, 0, 10, 0), roll(5, 0, 20, 0));
        assertFalse(r.aWon());
        assertEquals(4, r.damageToA); // 10 - 6
        assertEquals(26, h.getHp());
    }

    @Test
    public void spiteDamageIgnoresArmor() {
        Hero h = hero(ItemCatalog.PLATE); // 14 흡수
        Monster m = new Monster("트롤", 60);
        // 몬스터가 5 차이로 이겼지만 판금이 다 막는다. 대신 몬스터의 6 두 개가 스파이트로 들어온다.
        RoundResult r = Combat.apply(h, m, roll(3, 0, 15, 0), roll(7, 0, 20, 2));
        assertEquals(2, r.damageToA);
        // 진 영웅의 6도 스파이트로 몬스터에게 들어간다.
        h.healFull();
        r = Combat.apply(h, m, roll(3, 0, 12, 1), roll(7, 0, 20, 0));
        assertEquals(1, r.damageToB);
    }

    @Test
    public void tieExchangesOnlySpite() {
        Hero h = hero(null);
        Monster m = new Monster("코볼트", 20);
        RoundResult r = Combat.apply(h, m, roll(3, 0, 14, 1), roll(3, 0, 14, 2));
        assertTrue(r.tie());
        assertEquals(2, r.damageToA);
        assertEquals(1, r.damageToB);
    }

    @Test
    public void monsterWeakensAsMrDrops() {
        Monster m = new Monster("오우거", 45);
        assertEquals(5, m.combatDice());
        assertEquals(23, m.combatAdds());
        m.takeDamage(25);
        assertEquals(20, m.getMr());
        assertEquals(5, m.combatDice()); // 주사위 수는 처음 MR 기준
        assertEquals(10, m.combatAdds());
        m.takeDamage(100);
        assertTrue(m.isDead());
        assertEquals(0, m.getMr());
    }

    @Test
    public void personalAddsFollowFifthEdition() {
        Attributes a = new Attributes(15, 10, 8, 12, 13, 10, 10, 10);
        // 힘 +3, 민첩 -1, 속도 0, 행운 +1
        assertEquals(3, a.personalAdds());
        assertEquals(0, Attributes.addsFor(9));
        assertEquals(0, Attributes.addsFor(12));
        assertEquals(-3, Attributes.addsFor(6));
    }

    @Test
    public void heroLevelsUpAndHeals() {
        Hero h = Hero.createStarter("모험가");
        int con = h.maxCon();
        h.takeDamage(5);
        int need = h.apToNextLevel();
        assertEquals(0, h.gainAp(need - 1));
        assertEquals(1, h.getLevel());
        assertEquals(1, h.gainAp(1));
        assertEquals(2, h.getLevel());
        assertEquals(0, h.getAp());
        assertEquals(con + 2, h.maxCon());
        assertEquals(h.maxCon(), h.getHp());
    }

    @Test
    public void diceRollsStayInRange() {
        Dice dice = new Dice(42L);
        for (int i = 0; i < 1000; i++) {
            DiceRoll r = dice.roll(3, 0);
            assertTrue(r.sum >= 3 && r.sum <= 18);
            assertTrue(r.sixes >= 0 && r.sixes <= 3);
            assertTrue(dice.rollTaro() >= 3);
        }
        assertEquals("3D6+2", DiceRoll.notation(3, 2));
        assertEquals("2D6-1", DiceRoll.notation(2, -1));
        assertEquals("4D6", DiceRoll.notation(4, 0));
    }
}
