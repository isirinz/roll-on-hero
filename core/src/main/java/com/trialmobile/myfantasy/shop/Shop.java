package com.trialmobile.myfantasy.shop;

import com.trialmobile.myfantasy.battle.PlayerState;
import com.trialmobile.myfantasy.rules.Equipment;
import com.trialmobile.myfantasy.rules.Item;
import com.trialmobile.myfantasy.rules.ItemSlot;
import com.trialmobile.myfantasy.rules.Skill;
import com.trialmobile.myfantasy.rules.SkillBook;

/**
 * 상점과 강화 규칙. UI는 {@link #status(Item)}로 버튼 상태를 정하고 {@link #buy}/{@link #equip}/{@link #enhance}를 부른다.
 * 스킬 강화({@link #upgradeSkill})도 같은 골드를 쓴다.
 */
public final class Shop {
    /** 강화 비용 = 기본값 × 증가율^현재단계. 칸별 기본값. */
    public static final long WEAPON_ENHANCE_BASE = 40;
    public static final long ARMOR_ENHANCE_BASE = 30;
    public static final long SHIELD_ENHANCE_BASE = 25;
    public static final double ENHANCE_GROWTH = 1.13;
    /** 스킬 강화 비용 = 스킬별 기본값 × 증가율^(현재 레벨 − 1). */
    public static final double SKILL_GROWTH = 1.15;

    public enum Status {
        /** 장착 중. */
        EQUIPPED,
        /** 보유 중이며 장착 가능. */
        OWNED,
        /** 보유 중이지만 능력치가 모자라 장착 불가. */
        OWNED_LOCKED,
        /** 능력치가 모자라 구매해도 장착 불가(구매는 허용하지 않음). */
        LOCKED,
        /** 살 수 있음. */
        AFFORDABLE,
        /** 골드 부족. */
        TOO_EXPENSIVE
    }

    private final PlayerState state;

    public Shop(PlayerState state) {
        this.state = state;
    }

    public Status status(Item item) {
        Equipment eq = state.hero.getEquipment();
        boolean meets = item.meetsRequirements(state.hero.getAttributes());
        if (eq.get(item.slot) == item) return Status.EQUIPPED;
        if (state.owns(item)) return meets ? Status.OWNED : Status.OWNED_LOCKED;
        if (!meets) return Status.LOCKED;
        return state.getGold() >= item.price ? Status.AFFORDABLE : Status.TOO_EXPENSIVE;
    }

    /** 구매 후 바로 장착한다. 성공하면 true. */
    public boolean buy(Item item) {
        if (status(item) != Status.AFFORDABLE) return false;
        if (!state.spendGold(item.price)) return false;
        state.addOwned(item);
        state.hero.getEquipment().equip(item);
        return true;
    }

    /** 보유한 아이템을 장착한다. 성공하면 true. */
    public boolean equip(Item item) {
        if (status(item) != Status.OWNED) return false;
        state.hero.getEquipment().equip(item);
        return true;
    }

    public long enhanceCost(ItemSlot slot) {
        int level = state.hero.getEquipment().getEnhance(slot);
        return Math.round(enhanceBase(slot) * Math.pow(ENHANCE_GROWTH, level));
    }

    /** 장비가 있고 골드가 충분하면 강화할 수 있다. */
    public boolean canEnhance(ItemSlot slot) {
        return state.hero.getEquipment().get(slot) != null && state.getGold() >= enhanceCost(slot);
    }

    public boolean enhance(ItemSlot slot) {
        if (!canEnhance(slot)) return false;
        if (!state.spendGold(enhanceCost(slot))) return false;
        Equipment eq = state.hero.getEquipment();
        eq.setEnhance(slot, eq.getEnhance(slot) + 1);
        return true;
    }

    public long skillUpgradeCost(Skill skill) {
        int level = Math.max(1, state.hero.getSkills().getLevel(skill));
        return Math.round(skill.upgradeBaseCost * Math.pow(SKILL_GROWTH, level - 1));
    }

    /** 해금된 스킬이고 골드가 충분하면 강화할 수 있다. */
    public boolean canUpgradeSkill(Skill skill) {
        return state.hero.getSkills().isUnlocked(skill) && state.getGold() >= skillUpgradeCost(skill);
    }

    public boolean upgradeSkill(Skill skill) {
        if (!canUpgradeSkill(skill)) return false;
        if (!state.spendGold(skillUpgradeCost(skill))) return false;
        SkillBook book = state.hero.getSkills();
        book.setLevel(skill, book.getLevel(skill) + 1);
        return true;
    }

    private static long enhanceBase(ItemSlot slot) {
        switch (slot) {
            case WEAPON:
                return WEAPON_ENHANCE_BASE;
            case ARMOR:
                return ARMOR_ENHANCE_BASE;
            default:
                return SHIELD_ENHANCE_BASE;
        }
    }
}
