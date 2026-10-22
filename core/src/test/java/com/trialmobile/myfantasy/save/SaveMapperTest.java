package com.trialmobile.myfantasy.save;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.trialmobile.myfantasy.battle.BattleSystem;
import com.trialmobile.myfantasy.battle.PlayerState;
import com.trialmobile.myfantasy.battle.StageProgress;
import com.trialmobile.myfantasy.rules.Attribute;
import com.trialmobile.myfantasy.rules.Dice;
import com.trialmobile.myfantasy.rules.Equipment;
import com.trialmobile.myfantasy.rules.ItemCatalog;
import com.trialmobile.myfantasy.rules.ItemSlot;
import com.trialmobile.myfantasy.shop.Shop;

import org.junit.Test;

public class SaveMapperTest {

    @Test
    public void roundTripKeepsEverything() {
        PlayerState state = PlayerState.newGame();
        new BattleSystem(state, new Dice(5L)).fastForward(1800);
        Shop shop = new Shop(state);
        state.addGold(5000);
        shop.buy(ItemCatalog.BUCKLER);
        shop.buy(ItemCatalog.LEATHER);
        shop.enhance(ItemSlot.WEAPON);
        shop.enhance(ItemSlot.WEAPON);

        SaveData data = SaveMapper.toData(state, 123456789L);
        assertEquals(123456789L, data.savedAt);
        PlayerState loaded = SaveMapper.fromData(data);

        assertEquals(state.hero.getName(), loaded.hero.getName());
        assertEquals(state.hero.getLevel(), loaded.hero.getLevel());
        assertEquals(state.hero.getAp(), loaded.hero.getAp());
        assertEquals(state.hero.getHp(), loaded.hero.getHp());
        for (Attribute a : Attribute.values()) {
            assertEquals(state.hero.getAttributes().get(a), loaded.hero.getAttributes().get(a));
        }
        Equipment e1 = state.hero.getEquipment();
        Equipment e2 = loaded.hero.getEquipment();
        for (ItemSlot slot : ItemSlot.values()) {
            assertEquals(e1.get(slot), e2.get(slot));
            assertEquals(e1.getEnhance(slot), e2.getEnhance(slot));
        }
        assertEquals(state.getOwnedItemIds(), loaded.getOwnedItemIds());
        assertEquals(state.getGold(), loaded.getGold());
        assertEquals(state.getKills(), loaded.getKills());
        assertEquals(state.progress.label(), loaded.progress.label());
        assertEquals(state.progress.getWave(), loaded.progress.getWave());
        assertEquals(state.progress.isFarming(), loaded.progress.isFarming());
        assertEquals(state.hero.combatAdds(), loaded.hero.combatAdds());
        assertEquals(state.hero.armorHits(), loaded.hero.armorHits());
        for (com.trialmobile.myfantasy.rules.Skill k : com.trialmobile.myfantasy.rules.Skill.values()) {
            assertEquals(state.hero.getSkills().getLevel(k), loaded.hero.getSkills().getLevel(k));
        }
        assertEquals(state.progress.getFarmLimit(), loaded.progress.getFarmLimit());
    }

    @Test
    public void version1SaveMigrates() {
        SaveData d = new SaveData();
        d.version = 1;
        d.level = 6;
        d.attributes = new int[] {18, 24, 17, 12, 17, 15, 15, 10};
        d.weapon = ItemCatalog.SHORT_SWORD.id;
        d.chapter = 1;
        d.stage = 7;
        d.wave = 2;
        d.farming = true; // 버전 1의 사냥 표시
        d.skillLevels = null;

        PlayerState s = SaveMapper.fromData(d);
        com.trialmobile.myfantasy.rules.SkillBook book = s.hero.getSkills();
        assertEquals(1, book.getLevel(com.trialmobile.myfantasy.rules.Skill.TAKE_THAT));
        assertEquals(1, book.getLevel(com.trialmobile.myfantasy.rules.Skill.POOR_BABY));
        assertEquals(1, book.getLevel(com.trialmobile.myfantasy.rules.Skill.VORPAL_BLADE));
        assertEquals(0, book.getLevel(com.trialmobile.myfantasy.rules.Skill.BLASTING_POWER));
        assertEquals(StageProgress.WAVES_PER_STAGE - 1, s.progress.getFarmLimit());
    }

    @Test
    public void brokenDataFallsBackSafely() {
        SaveData d = new SaveData();
        d.weapon = "no_such_item";
        d.armor = ItemCatalog.BROADSWORD.id; // 잘못된 칸
        d.ownedItems = new String[] {"ghost", ItemCatalog.TOWER.id};
        d.attributes = new int[] {20, 25};
        d.level = 0;
        d.con = 0;
        d.chapter = -3;
        d.stage = 99;
        d.wave = 0;
        d.gold = -50;

        PlayerState s = SaveMapper.fromData(d);
        assertEquals(null, s.hero.getEquipment().get(ItemSlot.WEAPON));
        assertEquals(null, s.hero.getEquipment().get(ItemSlot.ARMOR));
        assertEquals(1, s.hero.combatDice()); // 맨주먹
        assertTrue(s.owns(ItemCatalog.TOWER));
        assertEquals(20, s.hero.getAttributes().get(Attribute.STR));
        assertEquals(25, s.hero.getAttributes().get(Attribute.CON));
        assertEquals(12, s.hero.getAttributes().get(Attribute.DEX)); // 없는 값은 기본값
        assertEquals(1, s.hero.getLevel());
        assertEquals(25, s.hero.getHp()); // 쓰러진 채 저장됐으면 회복
        assertEquals("1-10", s.progress.label());
        assertEquals(1, s.progress.getWave());
        assertEquals(0, s.getGold());
    }

    @Test
    public void nullDataStartsNewGame() {
        PlayerState s = SaveMapper.fromData(null);
        assertEquals(1, s.hero.getLevel());
        assertEquals(ItemCatalog.SHORT_SWORD, s.hero.getEquipment().get(ItemSlot.WEAPON));
    }
}
