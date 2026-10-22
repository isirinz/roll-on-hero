package com.trialmobile.myfantasy.rules;

/**
 * 장비 아이템 (불변).
 * 무기는 {@link #dice}D6+{@link #adds}로 공격하고, 방어구·방패는 매 라운드 {@link #hits}만큼 피해를 흡수한다.
 * 원작처럼 힘·민첩 요구치가 있어서 능력치가 모자라면 장착할 수 없다.
 */
public final class Item {
    public final String id;
    public final String name;
    public final ItemSlot slot;
    public final int dice;
    public final int adds;
    public final int hits;
    public final int reqStr;
    public final int reqDex;
    public final long price;

    public Item(String id, String name, ItemSlot slot, int dice, int adds, int hits,
                int reqStr, int reqDex, long price) {
        this.id = id;
        this.name = name;
        this.slot = slot;
        this.dice = dice;
        this.adds = adds;
        this.hits = hits;
        this.reqStr = reqStr;
        this.reqDex = reqDex;
        this.price = price;
    }

    public static Item weapon(String id, String name, int dice, int adds, int reqStr, int reqDex, long price) {
        return new Item(id, name, ItemSlot.WEAPON, dice, adds, 0, reqStr, reqDex, price);
    }

    public static Item armor(String id, String name, int hits, int reqStr, long price) {
        return new Item(id, name, ItemSlot.ARMOR, 0, 0, hits, reqStr, 0, price);
    }

    public static Item shield(String id, String name, int hits, int reqStr, long price) {
        return new Item(id, name, ItemSlot.SHIELD, 0, 0, hits, reqStr, 0, price);
    }

    public boolean meetsRequirements(Attributes a) {
        return a.get(Attribute.STR) >= reqStr && a.get(Attribute.DEX) >= reqDex;
    }

    /** "3D6+4" 또는 "방어 6". */
    public String statText() {
        return slot == ItemSlot.WEAPON ? DiceRoll.notation(dice, adds) : "방어 " + hits;
    }

    /** "힘 15 · 민첩 10" (요구치 없으면 빈 문자열). */
    public String requirementText() {
        StringBuilder sb = new StringBuilder();
        if (reqStr > 0) sb.append("힘 ").append(reqStr);
        if (reqDex > 0) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append("민첩 ").append(reqDex);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return name + " (" + statText() + ")";
    }
}
