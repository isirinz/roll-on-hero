package com.trialmobile.myfantasy.battle;

import com.trialmobile.myfantasy.rules.Hero;
import com.trialmobile.myfantasy.rules.Item;
import com.trialmobile.myfantasy.rules.ItemSlot;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** 저장 대상이 되는 플레이어 진행 상태. 화면이 바뀌어도 유지되도록 MainGame이 들고 있다. */
public final class PlayerState {
    public final Hero hero;
    public final StageProgress progress = new StageProgress();
    /** 2배 버프(공격·골드). */
    public final PowerBoost boost = new PowerBoost();
    /** 환생 기록과 영구 특성(혼). 환생해도 이어진다. */
    public final Rebirth rebirth = new Rebirth();
    private final Set<String> ownedItems = new LinkedHashSet<String>();
    private long gold;
    private int kills;

    public PlayerState(Hero hero) {
        this.hero = hero;
        for (ItemSlot slot : ItemSlot.values()) {
            Item item = hero.getEquipment().get(slot);
            if (item != null) ownedItems.add(item.id);
        }
    }

    /** 처음 영웅 이름. 화면에서는 이 이름이면 현재 언어의 기본 이름으로 바꿔 보여 준다. */
    public static final String DEFAULT_HERO_NAME = "모험가";

    public static PlayerState newGame() {
        return new PlayerState(Hero.createStarter(DEFAULT_HERO_NAME));
    }

    /** 오프라인 보상 최대 인정 시간(초). 모든 생을 통틀어 가장 깊이 간 챕터가 기준. */
    public double offlineCapSeconds() {
        return OfflineReport.maxSecondsFor(rebirth.getBestStageEver());
    }

    public long getGold() {
        return gold;
    }

    public void addGold(long amount) {
        gold += Math.max(0, amount);
    }

    /** 골드가 충분하면 차감하고 true. */
    public boolean spendGold(long amount) {
        if (amount < 0 || gold < amount) return false;
        gold -= amount;
        return true;
    }

    public int getKills() {
        return kills;
    }

    void addKill() {
        kills++;
    }

    public boolean owns(Item item) {
        return ownedItems.contains(item.id);
    }

    public void addOwned(Item item) {
        ownedItems.add(item.id);
    }

    public Set<String> getOwnedItemIds() {
        return Collections.unmodifiableSet(ownedItems);
    }

    /** 저장 데이터에서 수치를 되살린다. */
    public void restoreCounters(long gold, int kills) {
        this.gold = Math.max(0, gold);
        this.kills = Math.max(0, kills);
    }
}
