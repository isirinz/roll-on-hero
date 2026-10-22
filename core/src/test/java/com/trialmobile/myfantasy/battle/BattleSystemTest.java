package com.trialmobile.myfantasy.battle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.trialmobile.myfantasy.rules.Dice;
import com.trialmobile.myfantasy.rules.Hero;
import com.trialmobile.myfantasy.rules.Monster;

import org.junit.Test;

public class BattleSystemTest {

    @Test
    public void stageAdvancesThroughWavesAndBoss() {
        StageProgress p = new StageProgress();
        for (int i = 1; i < StageProgress.WAVES_PER_STAGE; i++) {
            assertFalse(p.isBossWave());
            assertFalse(p.advance());
        }
        assertTrue(p.isBossWave());
        assertTrue(p.advance());
        assertEquals("1-2", p.label());
        assertEquals(1, p.getWave());

        p.set(1, StageProgress.STAGES_PER_CHAPTER, StageProgress.WAVES_PER_STAGE);
        assertTrue(p.advance());
        assertEquals("2-1", p.label());
    }

    @Test
    public void farmingLoopsBeforeBoss() {
        StageProgress p = new StageProgress();
        p.setFarming(true);
        for (int i = 0; i < 20; i++) {
            assertFalse(p.isBossWave());
            assertFalse(p.advance());
        }
        p.setFarming(false);
        while (!p.isBossWave()) p.advance();
        assertTrue(p.advance());
        assertFalse(p.isFarming());
    }

    @Test
    public void defeatRetreatsToSaferWaves() {
        StageProgress p = new StageProgress();
        p.set(1, 3, 3);
        p.retreatAfterDefeat(); // 3웨이브에서 쓰러짐 → 1~2웨이브 반복
        assertTrue(p.isFarming());
        assertEquals(2, p.getFarmLimit());
        assertEquals(1, p.getWave());
        assertFalse(p.advance());
        assertEquals(2, p.getWave());
        assertFalse(p.advance());
        assertEquals(1, p.getWave());

        p.set(2, 1, 1);
        p.retreatAfterDefeat(); // 첫 웨이브에서 쓰러짐 → 이전 스테이지(1-10)로 후퇴
        assertEquals("1-10", p.label());
        assertEquals(StageProgress.WAVES_PER_STAGE - 1, p.getFarmLimit());

        p.set(1, 1, 1);
        p.setFarmLimit(0);
        p.retreatAfterDefeat(); // 1-1 첫 웨이브는 물러날 곳이 없다
        assertEquals("1-1", p.label());
        assertFalse(p.isFarming());
    }

    @Test
    public void everyMonsterHasAKindId() {
        Dice dice = new Dice(3L);
        StageProgress p = new StageProgress();
        for (int chapter = 1; chapter <= 4; chapter++) {
            for (int wave = 1; wave <= StageProgress.WAVES_PER_STAGE; wave++) {
                p.set(chapter, 1, wave);
                for (int i = 0; i < 10; i++) {
                    Monster m = MonsterTable.create(p, dice);
                    assertTrue(m.getName(), m.getKindId() != null && !m.getKindId().isEmpty());
                    assertTrue(m.getKindId(), m.getKindId().matches("[a-z_]+"));
                }
            }
        }
    }

    @Test
    public void bossMonstersAreStronger() {
        Dice dice = new Dice(7L);
        StageProgress p = new StageProgress();
        p.set(1, 5, 1);
        int normal = MonsterTable.create(p, dice).getMaxMr();
        p.set(1, 5, StageProgress.WAVES_PER_STAGE);
        Monster boss = MonsterTable.create(p, dice);
        assertTrue(boss.isBoss());
        assertTrue(boss.getMaxMr() > normal);
    }

    @Test
    public void idleBattleMakesProgress() {
        PlayerState state = PlayerState.newGame();
        BattleSystem battle = new BattleSystem(state, new Dice(1234L));
        final int[] bossDefeats = {0};
        battle.setListener(new BattleSystem.ListenerAdapter() {
            @Override
            public void onHeroDefeated(Monster monster) {
                if (monster.isBoss()) bossDefeats[0]++;
            }
        });

        // 게임 시간 30분을 0.05초 프레임으로 진행
        for (int i = 0; i < 30 * 60 * 20; i++) battle.update(0.05f);

        Hero hero = state.hero;
        assertTrue("레벨이 올라야 한다", hero.getLevel() > 5);
        assertTrue("1장을 넘어가야 한다", state.progress.stageIndex() >= 10);
        assertTrue(state.getGold() > 0);
        assertTrue(hero.getHp() >= 0 && hero.getHp() <= hero.maxCon());
        assertTrue("보스 패배는 가끔만 일어나야 한다", bossDefeats[0] < state.getKills() / 10);
    }

    @Test
    public void speedMultiplierRunsFaster() {
        PlayerState slow = PlayerState.newGame();
        PlayerState fast = PlayerState.newGame();
        BattleSystem a = new BattleSystem(slow, new Dice(99L));
        BattleSystem b = new BattleSystem(fast, new Dice(99L));
        b.setSpeed(3f);
        for (int i = 0; i < 600; i++) {
            a.update(0.05f);
            b.update(0.05f);
        }
        assertTrue(fast.getKills() > slow.getKills());
    }
}
