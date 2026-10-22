package com.trialmobile.myfantasy.battle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.trialmobile.myfantasy.rules.Attribute;
import com.trialmobile.myfantasy.rules.DiceRoll;
import com.trialmobile.myfantasy.rules.ItemCatalog;
import com.trialmobile.myfantasy.rules.ItemSlot;
import com.trialmobile.myfantasy.save.SaveData;
import com.trialmobile.myfantasy.save.SaveMapper;

import org.junit.Test;

public class RebirthTest {

    @Test
    public void soulsGrowWithDepth() {
        assertEquals(0L, Rebirth.soulsFor(9));
        assertEquals(3L, Rebirth.soulsFor(10));
        assertEquals(27L, Rebirth.soulsFor(40));
        assertEquals(101L, Rebirth.soulsFor(90));
        assertEquals("2-1", Rebirth.stageLabel(10));
        assertEquals("10-10", Rebirth.stageLabel(99));
    }

    @Test
    public void lockedUntilChapterTwo() {
        PlayerState s = PlayerState.newGame();
        s.rebirth.recordStage(9);
        assertFalse(s.rebirth.canRebirth());
        assertEquals(null, Rebirth.reborn(s));
        s.rebirth.recordStage(10);
        assertTrue(s.rebirth.canRebirth());
    }

    @Test
    public void rebirthResetsRunButKeepsSoulsAndPerks() {
        PlayerState s = PlayerState.newGame();
        s.addGold(99999);
        s.hero.restore(20, 5, 1);
        s.hero.getEquipment().equip(ItemCatalog.CLAYMORE);
        s.progress.set(5, 1, 1);
        s.rebirth.recordStage(s.progress.stageIndex());
        s.boost.activate(1000L);

        PlayerState n = Rebirth.reborn(s);
        assertEquals(27L, n.rebirth.getSouls());
        assertEquals(1, n.rebirth.getRebirths());
        assertEquals(0, n.rebirth.getBestStage());
        assertEquals(0L, n.getGold());
        assertEquals(1, n.hero.getLevel());
        assertEquals("1-1", n.progress.label());
        assertEquals(ItemCatalog.SHORT_SWORD, n.hero.getEquipment().get(ItemSlot.WEAPON));
        assertEquals(s.hero.getName(), n.hero.getName());
        assertEquals(s.boost.getReadyAt(), n.boost.getReadyAt());

        // 혼 구매: 영웅의 혼은 능력치를 바로 올리고, 다음 환생에도 남는다
        int str = n.hero.getAttributes().get(Attribute.STR);
        assertEquals(3L, n.rebirth.cost(Rebirth.Perk.HEROIC));
        assertTrue(n.rebirth.buy(Rebirth.Perk.HEROIC, n.hero));
        assertEquals(str + 1, n.hero.getAttributes().get(Attribute.STR));
        assertEquals(24L, n.rebirth.getSouls());
        assertTrue(n.rebirth.buy(Rebirth.Perk.MIGHT, n.hero));
        assertTrue(n.rebirth.buy(Rebirth.Perk.MIGHT, n.hero)); // 1 + 2
        assertEquals(21L, n.rebirth.getSouls());
        assertEquals(10, n.rebirth.hitBonusPercent());

        n.rebirth.recordStage(10);
        PlayerState again = Rebirth.reborn(n);
        assertEquals(24L, again.rebirth.getSouls());
        assertEquals(2, again.rebirth.getRebirths());
        assertEquals(str + 1, again.hero.getAttributes().get(Attribute.STR)); // 시작 능력치에 보너스
        assertEquals(10, again.rebirth.hitBonusPercent());
    }

    @Test
    public void bonusesApply() {
        assertEquals(110, Rebirth.withBonus(100, 10));
        assertEquals(100, Rebirth.withBonus(100, 0));
        DiceRoll r = DiceRoll.ofFaces(0, 5, 5).withBonus(10).multiplied(2);
        assertEquals(22, r.total());
    }

    @Test
    public void saveKeepsRebirth() {
        PlayerState s = PlayerState.newGame();
        s.progress.set(3, 1, 1);
        s.rebirth.recordStage(s.progress.stageIndex());
        PlayerState n = Rebirth.reborn(s);
        n.rebirth.buy(Rebirth.Perk.FORTUNE, n.hero);
        SaveData d = SaveMapper.toData(n, 1000L);
        PlayerState back = SaveMapper.fromData(d);
        assertEquals(n.rebirth.getSouls(), back.rebirth.getSouls());
        assertEquals(1, back.rebirth.getRebirths());
        assertEquals(1, back.rebirth.getLevel(Rebirth.Perk.FORTUNE));
        assertEquals(10, back.rebirth.goldBonusPercent());
    }

    @Test
    public void bestStageEverSurvivesRebirthAndSaves() {
        PlayerState s = PlayerState.newGame();
        s.rebirth.recordStage(44); // 5-5
        assertEquals(6 * 3600.0, s.offlineCapSeconds(), 0.0);
        PlayerState n = Rebirth.reborn(s);
        assertEquals(0, n.rebirth.getBestStage());          // 이번 생 기록은 처음부터
        assertEquals(44, n.rebirth.getBestStageEver());     // 모든 생 기록은 유지
        assertEquals(6 * 3600.0, n.offlineCapSeconds(), 0.0); // 오프라인 한도도 유지
        PlayerState back = SaveMapper.fromData(SaveMapper.toData(n, 1000L));
        assertEquals(44, back.rebirth.getBestStageEver());
    }

    @Test
    public void version4SaveUsesThisLifeBestAsEver() {
        PlayerState s = PlayerState.newGame();
        s.progress.set(3, 2, 1);
        SaveData d = SaveMapper.toData(s, 1000L);
        d.version = 4;
        d.bestStage = 25;
        d.bestStageEver = 0; // 버전 4에는 없는 필드
        PlayerState back = SaveMapper.fromData(d);
        assertEquals(25, back.rebirth.getBestStageEver());
    }
}
