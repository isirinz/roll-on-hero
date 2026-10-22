package com.trialmobile.myfantasy.save;

import com.trialmobile.myfantasy.battle.PlayerState;
import com.trialmobile.myfantasy.rules.Attribute;
import com.trialmobile.myfantasy.rules.Attributes;
import com.trialmobile.myfantasy.rules.Equipment;
import com.trialmobile.myfantasy.rules.Hero;
import com.trialmobile.myfantasy.rules.Item;
import com.trialmobile.myfantasy.rules.ItemCatalog;
import com.trialmobile.myfantasy.rules.ItemSlot;
import com.trialmobile.myfantasy.rules.Skill;
import com.trialmobile.myfantasy.rules.SkillBook;

/**
 * {@link PlayerState} ↔ {@link SaveData} 변환 (순수 Java, 단위 테스트 가능).
 * 손상되거나 알 수 없는 값은 안전한 기본값으로 바꿔서 불러온다.
 * <p>
 * 버전 1 → 2: 스킬 레벨이 없으므로 영웅 레벨 기준으로 해금된 스킬만 1레벨로 시작한다.
 */
public final class SaveMapper {
    private SaveMapper() {
    }

    public static SaveData toData(PlayerState state, long nowMillis) {
        Hero hero = state.hero;
        Equipment eq = hero.getEquipment();
        SaveData d = new SaveData();
        d.version = SaveData.CURRENT_VERSION;
        d.savedAt = nowMillis;

        d.heroName = hero.getName();
        d.level = hero.getLevel();
        d.ap = hero.getAp();
        d.con = hero.getHp();
        Attribute[] attrs = Attribute.values();
        d.attributes = new int[attrs.length];
        for (int i = 0; i < attrs.length; i++) d.attributes[i] = hero.getAttributes().get(attrs[i]);

        d.weapon = idOf(eq.get(ItemSlot.WEAPON));
        d.armor = idOf(eq.get(ItemSlot.ARMOR));
        d.shield = idOf(eq.get(ItemSlot.SHIELD));
        d.weaponEnhance = eq.getEnhance(ItemSlot.WEAPON);
        d.armorEnhance = eq.getEnhance(ItemSlot.ARMOR);
        d.shieldEnhance = eq.getEnhance(ItemSlot.SHIELD);
        d.ownedItems = state.getOwnedItemIds().toArray(new String[0]);

        Skill[] skills = Skill.values();
        d.skillLevels = new int[skills.length];
        for (int i = 0; i < skills.length; i++) d.skillLevels[i] = hero.getSkills().getLevel(skills[i]);

        d.gold = state.getGold();
        d.kills = state.getKills();
        d.chapter = state.progress.getChapter();
        d.stage = state.progress.getStage();
        d.wave = state.progress.getWave();
        d.farming = state.progress.isFarming();
        d.farmLimit = state.progress.getFarmLimit();
        d.boostActiveUntil = state.boost.getActiveUntil();
        d.boostReadyAt = state.boost.getReadyAt();
        d.souls = state.rebirth.getSouls();
        d.totalSouls = state.rebirth.getTotalSouls();
        d.rebirths = state.rebirth.getRebirths();
        d.bestStage = state.rebirth.getBestStage();
        d.bestStageEver = state.rebirth.getBestStageEver();
        d.perkLevels = state.rebirth.getPerkLevels();
        return d;
    }

    public static PlayerState fromData(SaveData d) {
        PlayerState fresh = PlayerState.newGame();
        if (d == null) return fresh;

        Attributes attributes = fresh.hero.getAttributes().copy();
        Attribute[] attrs = Attribute.values();
        if (d.attributes != null) {
            for (int i = 0; i < attrs.length && i < d.attributes.length; i++) attributes.set(attrs[i], d.attributes[i]);
        }
        String name = d.heroName == null || d.heroName.isEmpty() ? fresh.hero.getName() : d.heroName;
        Hero hero = new Hero(name, attributes);

        Equipment eq = hero.getEquipment();
        equipIfValid(eq, ItemSlot.WEAPON, d.weapon);
        equipIfValid(eq, ItemSlot.ARMOR, d.armor);
        equipIfValid(eq, ItemSlot.SHIELD, d.shield);
        eq.setEnhance(ItemSlot.WEAPON, d.weaponEnhance);
        eq.setEnhance(ItemSlot.ARMOR, d.armorEnhance);
        eq.setEnhance(ItemSlot.SHIELD, d.shieldEnhance);
        hero.restore(d.level, d.ap, d.con); // 레벨에 맞는 스킬 해금도 여기서

        SkillBook book = hero.getSkills();
        Skill[] skills = Skill.values();
        if (d.skillLevels != null) {
            for (int i = 0; i < skills.length && i < d.skillLevels.length; i++) {
                // 해금된 스킬만, 저장된 레벨이 더 높을 때만 반영한다
                if (book.isUnlocked(skills[i]) && d.skillLevels[i] > book.getLevel(skills[i])) {
                    book.setLevel(skills[i], d.skillLevels[i]);
                }
            }
        }

        PlayerState state = new PlayerState(hero); // 장착 아이템은 자동으로 보유 처리
        if (d.ownedItems != null) {
            for (String id : d.ownedItems) {
                Item item = ItemCatalog.byId(id);
                if (item != null) state.addOwned(item);
            }
        }
        state.restoreCounters(d.gold, d.kills);
        state.progress.set(d.chapter, d.stage, d.wave);
        if (d.farmLimit > 0) state.progress.setFarmLimit(d.farmLimit);
        else state.progress.setFarming(d.farming); // 버전 1
        state.boost.restore(d.boostActiveUntil, d.boostReadyAt, d.savedAt); // 버전 2 이하는 0 = 바로 사용 가능
        // 버전 3 이하는 환생 기록이 없다: 지금 스테이지를 이번 생 최고 기록으로 본다.
        // 버전 4 이하는 모든 생 최고 기록이 없다: 이번 생 최고 기록으로 본다.
        int best = Math.max(d.bestStage, state.progress.stageIndex());
        state.rebirth.restore(d.souls, d.totalSouls, d.rebirths, best, Math.max(d.bestStageEver, best), d.perkLevels);
        return state;
    }

    private static String idOf(Item item) {
        return item == null ? null : item.id;
    }

    private static void equipIfValid(Equipment eq, ItemSlot slot, String id) {
        Item item = ItemCatalog.byId(id);
        eq.set(slot, item != null && item.slot == slot ? item : null);
    }
}
