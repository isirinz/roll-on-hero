package com.trialmobile.myfantasy.battle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.trialmobile.myfantasy.rules.Dice;

import org.junit.Test;

public class OfflineReportTest {

    @Test
    public void clampIgnoresShortAndCapsLongAbsence() {
        assertEquals(0.0, OfflineReport.clampSeconds(30), 0.0);
        assertEquals(0.0, OfflineReport.clampSeconds(-100), 0.0);
        assertEquals(120.0, OfflineReport.clampSeconds(120), 0.0);
        assertEquals(OfflineReport.MAX_SECONDS, OfflineReport.clampSeconds(3 * 24 * 3600), 0.0);
        assertEquals(2 * 3600.0, OfflineReport.clampSeconds(5 * 3600, 2 * 3600.0), 0.0);
        assertEquals(0.0, OfflineReport.clampSeconds(30, 2 * 3600.0), 0.0);
    }

    @Test
    public void capGrowsOneHourPerChapter() {
        assertEquals(2, OfflineReport.maxHoursFor(0));    // 1-1
        assertEquals(2, OfflineReport.maxHoursFor(9));    // 1-10
        assertEquals(3, OfflineReport.maxHoursFor(10));   // 2-1
        assertEquals(6, OfflineReport.maxHoursFor(44));   // 5-5
        assertEquals(11, OfflineReport.maxHoursFor(99));  // 10-10
        assertEquals(12, OfflineReport.maxHoursFor(100)); // 11-1
        assertEquals(12, OfflineReport.maxHoursFor(500));
        assertEquals(2 * 3600.0, PlayerState.newGame().offlineCapSeconds(), 0.0);
    }

    @Test
    public void reportKnowsWhenCapWasReached() {
        PlayerState s = PlayerState.newGame();
        double cap = s.offlineCapSeconds(); // 떠날 때 기준(빨리 감기로 챕터가 올라가도 이번 한도는 그대로)
        OfflineReport r = new BattleSystem(s, new Dice(5L)).fastForward(cap);
        r.capSeconds = cap;
        assertTrue(r.reachedCap());
        OfflineReport shorter = new BattleSystem(PlayerState.newGame(), new Dice(5L)).fastForward(600);
        shorter.capSeconds = 2 * 3600;
        assertTrue(!shorter.reachedCap());
    }

    @Test
    public void formatDuration() {
        assertEquals("45초", OfflineReport.formatDuration(45));
        assertEquals("5분", OfflineReport.formatDuration(300));
        assertEquals("2시간", OfflineReport.formatDuration(7200));
        assertEquals("2시간 13분", OfflineReport.formatDuration(7980));
    }

    @Test
    public void fastForwardMatchesRealtimePlay() {
        // 같은 시드라면 빨리 감기와 프레임 단위 진행의 결과가 같아야 한다.
        PlayerState a = PlayerState.newGame();
        PlayerState b = PlayerState.newGame();
        BattleSystem realtime = new BattleSystem(a, new Dice(77L));
        BattleSystem offline = new BattleSystem(b, new Dice(77L));

        for (int i = 0; i < 600 * 20; i++) realtime.update(0.05f); // 10분
        OfflineReport report = offline.fastForward(600);

        assertEquals(a.getKills(), b.getKills());
        assertEquals(a.getGold(), b.getGold());
        assertEquals(a.hero.getLevel(), b.hero.getLevel());
        assertEquals(a.progress.label(), b.progress.label());

        assertEquals(b.getKills(), report.kills);
        assertEquals(b.getGold(), report.goldGained);
        assertEquals("1-1", report.startStage);
        assertEquals(b.progress.label(), report.endStage);
        assertTrue(report.kills > 0);
    }

    @Test
    public void fastForwardKeepsScreenListener() {
        PlayerState s = PlayerState.newGame();
        BattleSystem battle = new BattleSystem(s, new Dice(3L));
        final int[] events = {0};
        battle.setListener(new BattleSystem.ListenerAdapter() {
            @Override
            public void onRound(com.trialmobile.myfantasy.rules.RoundResult r,
                                com.trialmobile.myfantasy.rules.Hero h,
                                com.trialmobile.myfantasy.rules.Monster m) {
                events[0]++;
            }
        });
        battle.fastForward(300);
        assertEquals(0, events[0]); // 빨리 감기 중에는 화면에 이벤트를 보내지 않는다
        for (int i = 0; i < 100; i++) battle.update(0.05f);
        assertTrue(events[0] > 0); // 끝나면 원래 리스너로 복귀
    }
}
