package com.trialmobile.myfantasy.rules;

/**
 * 장착 중인 장비와 칸별 강화 단계.
 * 강화 단계는 아이템이 아니라 칸에 붙어 있어서, 더 좋은 장비로 바꿔도 강화가 유지된다.
 * 칸이 비어 있으면 그 칸의 강화는 효과가 없다. 무기가 없으면 맨주먹(1D6)으로 싸운다.
 */
public final class Equipment {
    private final Item[] items = new Item[ItemSlot.values().length];
    private final int[] enhance = new int[ItemSlot.values().length];

    public Item get(ItemSlot slot) {
        return items[slot.ordinal()];
    }

    public void set(ItemSlot slot, Item item) {
        if (item != null && item.slot != slot) {
            throw new IllegalArgumentException(item.name + "은(는) " + slot.label + " 칸에 장착할 수 없습니다");
        }
        items[slot.ordinal()] = item;
    }

    public void equip(Item item) {
        set(item.slot, item);
    }

    public int getEnhance(ItemSlot slot) {
        return enhance[slot.ordinal()];
    }

    public void setEnhance(ItemSlot slot, int level) {
        enhance[slot.ordinal()] = Math.max(0, level);
    }

    public int weaponDice() {
        Item w = get(ItemSlot.WEAPON);
        return w == null ? 1 : w.dice;
    }

    public int weaponAdds() {
        Item w = get(ItemSlot.WEAPON);
        if (w == null) return 0;
        return w.adds + getEnhance(ItemSlot.WEAPON) * ItemSlot.WEAPON.addsPerEnhance;
    }

    /** 방어구 + 방패 + 강화의 합. */
    public int armorHits() {
        return slotHits(ItemSlot.ARMOR) + slotHits(ItemSlot.SHIELD);
    }

    public int slotHits(ItemSlot slot) {
        Item item = get(slot);
        if (item == null) return 0;
        return item.hits + getEnhance(slot) * slot.hitsPerEnhance;
    }
}
