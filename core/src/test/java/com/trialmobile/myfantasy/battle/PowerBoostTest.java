package com.trialmobile.myfantasy.battle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.trialmobile.myfantasy.rules.Dice;
import com.trialmobile.myfantasy.rules.DiceRoll;
import com.trialmobile.myfantasy.rules.Hero;
import com.trialmobile.myfantasy.rules.Monster;
import com.trialmobile.myfantasy.rules.RoundResult;
import com.trialmobile.myfantasy.save.SaveData;
import com.trialmobile.myfantasy.save.SaveMapper;

import org.junit.Test;

public class PowerBoostTest {
    private static final long T0 = 1_700_000_000_000L;

    @Test
    public void activatesForFifteenMinutesThenCoolsDownForTwoHours() {
        PowerBoost b = new PowerBoost();
        assertTrue(b.isReady(T0));
        assertEquals(1, b.multiplier(T0));
        assertTrue(b.activate(T0));
        assertEquals(2, b.multiplier(T0));
        assertFalse(b.activate(T0 + 1000L)); // 이미 켜짐
        assertEquals(PowerBoost.DURATION_MS - 1000L, b.activeRemaining(T0 + 1000L));

        long end = T0 + PowerBoost.DURATION_MS;
        assertTrue(b.isActive(end - 1L));
        assertFalse(b.isActive(end));
        assertFalse(b.isReady(end)); // 아직 쿨타임
        assertEquals(PowerBoost.COOLDOWN_MS - PowerBoost.DURATION_MS, b.cooldownRemaining(end));

        long ready = T0 + PowerBoost.COOLDOWN_MS;
        assertFalse(b.isReady(ready - 1L));
        assertTrue(b.isReady(ready));
        assertTrue(b.activate(ready));
    }

    @Test
    public void notActiveBeforeItWasTurnedOn() {
        PowerBoost b = new PowerBoost();
        b.activate(T0);
        assertFalse(b.isActive(T0 - 1L)); // 오프라인 시뮬레이션에서 켜기 전 시각
    }

    @Test
    public void restoreClampsFarFutureTimes() {
        PowerBoost b = new PowerBoost();
        b.restore(T0 + 99L * PowerBoost.COOLDOWN_MS, T0 + 99L * PowerBoost.COOLDOWN_MS, T0);
        assertEquals(T0 + PowerBoost.DURATION_MS, b.getActiveUntil());
        assertEquals(T0 + PowerBoost.COOLDOWN_MS, b.getReadyAt());
    }

    @Test
    public void saveKeepsBoost() {
        PlayerState s = PlayerState.newGame();
        s.boost.activate(T0);
        SaveData d = SaveMapper.toData(s, T0 + 1000L);
        PlayerState back = SaveMapper.fromData(d);
        assertTrue(back.boost.isActive(T0 + 2000L));
        assertFalse(back.boost.isReady(T0 + PowerBoost.DURATION_MS));
    }

    @Test
    public void multipliedRollDoublesTotalButNotSixes() {
        DiceRoll r = DiceRoll.ofFaces(2, 6, 3, 1);
        assertEquals(10, r.sum);
        assertEquals(12, r.total());
        assertEquals(1, r.sixes);
        assertEquals(3, r.face(1));
        DiceRoll d = r.multiplied(2);
        assertEquals(24, d.total());
        assertEquals(1, d.sixes);
        assertEquals(6, d.face(0));
    }

    @Test
    public void boostDoublesAttackRollsAndGold() {
        final long[] now = {T0};
        PlayerState state = PlayerState.newGame();
        state.boost.activate(T0);
        BattleSystem battle = new BattleSystem(state, new Dice(3L));
        battle.setClock(() -> now[0]);
        final int[] counts = new int[3]; // 배율 2 라운드, 배율 1 라운드, 홀수 골드
        battle.setListener(new BattleSystem.ListenerAdapter() {
            @Override
            public void onRound(RoundResult result, Hero hero, Monster monster) {
                counts[result.rollA.multiplier == 2 ? 0 : 1]++;
                assertEquals(1, result.rollB.multiplier);
            }

            @Override
            public void onMonsterDefeated(Monster monster, int ap, int gold) {
                if (now[0] < T0 + PowerBoost.DURATION_MS && gold % 2 != 0) counts[2]++;
            }
        });
        for (int i = 0; i < 2000; i++) battle.update(0.1f);
        assertTrue(counts[0] > 0);
        assertEquals(0, counts[1]);
        assertEquals(0, counts[2]);

        now[0] = T0 + PowerBoost.DURATION_MS; // 버프 끝
        counts[0] = 0;
        for (int i = 0; i < 500; i++) battle.update(0.1f);
        assertEquals(0, counts[0]);
        assertTrue(counts[1] > 0);
    }

    @Test
    public void offlineUsesTheTimeItWasAway() {
        PlayerState state = PlayerState.newGame();
        state.boost.activate(T0);
        BattleSystem battle = new BattleSystem(state, new Dice(4L));
        battle.setClock(() -> T0 + 60L * 60L * 1000L); // 1시간 뒤 복귀, 버프는 이미 끝남
        assertEquals(1, battle.boostMultiplier());
        battle.fastForward(3600); // 떠난 동안(앞 15분)은 버프가 켜져 있었다
        assertEquals(1, battle.boostMultiplier());
    }
}
