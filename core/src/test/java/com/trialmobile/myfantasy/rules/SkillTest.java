package com.trialmobile.myfantasy.rules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.trialmobile.myfantasy.battle.BattleSystem;
import com.trialmobile.myfantasy.battle.PlayerState;
import com.trialmobile.myfantasy.shop.Shop;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class SkillTest {

    @Test
    public void skillsUnlockWithHeroLevel() {
        Hero h = Hero.createStarter("모험가");
        SkillBook book = h.getSkills();
        assertTrue(book.isUnlocked(Skill.TAKE_THAT));
        assertFalse(book.isUnlocked(Skill.POOR_BABY));
        assertEquals(1, book.getLevel(Skill.TAKE_THAT));

        while (h.getLevel() < Skill.POOR_BABY.unlockLevel) h.gainAp(h.apToNextLevel());
        assertTrue(book.isUnlocked(Skill.POOR_BABY));
        assertFalse(book.isUnlocked(Skill.HELLBOMB));
        // 레벨업 때 지능·마력도 오른다
        assertEquals(10 + h.getLevel() - 1, h.getAttributes().get(Attribute.IQ));
    }

    @Test
    public void takeThatDealsIqDamageAndStartsCooldown() {
        Hero h = Hero.createStarter("모험가");
        Monster m = new Monster("오크", 50);
        SkillCast cast = Spells.cast(Skill.TAKE_THAT, h, m, new Dice(1L));
        assertEquals(10, cast.damage); // IQ 10, 스킬 1레벨
        assertEquals(40, m.getMr());
        assertFalse(h.getSkills().isReady(Skill.TAKE_THAT));
        assertEquals(0f, h.getSkills().readiness(Skill.TAKE_THAT), 0.0001);

        h.getSkills().tick(Skill.TAKE_THAT.cooldown / 2);
        assertEquals(0.5f, h.getSkills().readiness(Skill.TAKE_THAT), 0.0001);
        h.getSkills().tick(Skill.TAKE_THAT.cooldown / 2);
        assertTrue(h.getSkills().isReady(Skill.TAKE_THAT));

        h.getSkills().setLevel(Skill.TAKE_THAT, 3); // 배율 1.6
        assertEquals(16, Spells.cast(Skill.TAKE_THAT, h, m, new Dice(1L)).damage);
    }

    @Test
    public void healOnlyWhenHurt() {
        Hero h = Hero.createStarter("모험가");
        h.getSkills().setLevel(Skill.POOR_BABY, 1);
        Monster m = new Monster("고블린", 10);
        assertFalse(Spells.canCast(Skill.POOR_BABY, h, m));
        h.takeDamage(h.maxCon() / 2 + 1);
        assertTrue(Spells.canCast(Skill.POOR_BABY, h, m));
        int before = h.getHp();
        SkillCast cast = Spells.cast(Skill.POOR_BABY, h, m, new Dice(1L));
        assertEquals(h.getHp() - before, cast.heal);
        assertTrue(cast.heal > 0);
        assertTrue(h.getHp() <= h.maxCon());
    }

    @Test
    public void vorpalDoublesWeaponDiceForAWhile() {
        Hero h = Hero.createStarter("모험가");
        h.getSkills().setLevel(Skill.VORPAL_BLADE, 1);
        Monster m = new Monster("트롤", 80);
        assertEquals(3, h.combatDice());
        SkillCast cast = Spells.cast(Skill.VORPAL_BLADE, h, m, new Dice(1L));
        assertEquals(4.0, cast.buffSeconds, 1e-9);
        assertEquals(6, h.combatDice());
        assertFalse(Spells.canCast(Skill.VORPAL_BLADE, h, m)); // 이미 걸려 있음
        h.getSkills().tick(4.0);
        assertEquals(3, h.combatDice());
    }

    @Test
    public void skillsFireAutomaticallyOnCooldown() {
        PlayerState state = PlayerState.newGame();
        BattleSystem battle = new BattleSystem(state, new Dice(11L));
        final List<SkillCast> casts = new ArrayList<SkillCast>();
        battle.setListener(new BattleSystem.ListenerAdapter() {
            @Override
            public void onSkillCast(SkillCast cast, Hero hero, Monster monster) {
                casts.add(cast);
            }
        });
        // 60초 동안 6초 쿨타임 스킬은 최대 10번 + 첫 발동 1번
        for (int i = 0; i < 60 * 20; i++) battle.update(0.05f);
        int takeThat = 0;
        for (SkillCast c : casts) if (c.skill == Skill.TAKE_THAT) takeThat++;
        assertTrue("스킬이 자동으로 발동해야 한다", takeThat >= 5);
        assertTrue("쿨타임보다 자주 발동하면 안 된다", takeThat <= 11);
    }

    @Test
    public void skillUpgradeCostsGold() {
        PlayerState state = PlayerState.newGame();
        Shop shop = new Shop(state);
        assertEquals(Skill.TAKE_THAT.upgradeBaseCost, shop.skillUpgradeCost(Skill.TAKE_THAT));
        assertFalse(shop.canUpgradeSkill(Skill.TAKE_THAT));
        state.addGold(10000);
        assertFalse(shop.canUpgradeSkill(Skill.HELLBOMB)); // 미해금
        assertTrue(shop.upgradeSkill(Skill.TAKE_THAT));
        assertEquals(2, state.hero.getSkills().getLevel(Skill.TAKE_THAT));
        assertEquals(10000 - Skill.TAKE_THAT.upgradeBaseCost, state.getGold());
        assertTrue(shop.skillUpgradeCost(Skill.TAKE_THAT) > Skill.TAKE_THAT.upgradeBaseCost);
    }
}
