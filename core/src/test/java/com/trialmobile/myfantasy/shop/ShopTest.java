package com.trialmobile.myfantasy.shop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.trialmobile.myfantasy.battle.PlayerState;
import com.trialmobile.myfantasy.rules.Attribute;
import com.trialmobile.myfantasy.rules.Equipment;
import com.trialmobile.myfantasy.rules.Item;
import com.trialmobile.myfantasy.rules.ItemCatalog;
import com.trialmobile.myfantasy.rules.ItemSlot;

import org.junit.Test;

public class ShopTest {

    @Test
    public void starterGearIsOwnedAndEquipped() {
        PlayerState state = PlayerState.newGame();
        Shop shop = new Shop(state);
        assertEquals(Shop.Status.EQUIPPED, shop.status(ItemCatalog.SHORT_SWORD));
        assertEquals(Shop.Status.EQUIPPED, shop.status(ItemCatalog.QUILTED));
        assertEquals(null, state.hero.getEquipment().get(ItemSlot.SHIELD));
    }

    @Test
    public void buyingSpendsGoldAndEquips() {
        PlayerState state = PlayerState.newGame();
        Shop shop = new Shop(state);
        Item leather = ItemCatalog.LEATHER;

        assertEquals(Shop.Status.TOO_EXPENSIVE, shop.status(leather));
        assertFalse(shop.buy(leather));

        state.addGold(leather.price + 10);
        assertEquals(Shop.Status.AFFORDABLE, shop.status(leather));
        assertTrue(shop.buy(leather));
        assertEquals(10, state.getGold());
        assertEquals(Shop.Status.EQUIPPED, shop.status(leather));
        assertEquals(Shop.Status.OWNED, shop.status(ItemCatalog.QUILTED));
        assertEquals(6, state.hero.armorHits());

        // 보유한 장비로 되돌리기는 무료
        assertTrue(shop.equip(ItemCatalog.QUILTED));
        assertEquals(3, state.hero.armorHits());
        assertEquals(10, state.getGold());
    }

    @Test
    public void requirementsBlockPurchase() {
        PlayerState state = PlayerState.newGame();
        Shop shop = new Shop(state);
        state.addGold(1000000);
        Item broadsword = ItemCatalog.BROADSWORD; // 힘 15 필요, 시작 힘 13
        assertEquals(Shop.Status.LOCKED, shop.status(broadsword));
        assertFalse(shop.buy(broadsword));

        state.hero.getAttributes().set(Attribute.STR, 15);
        assertEquals(Shop.Status.AFFORDABLE, shop.status(broadsword));
        assertTrue(shop.buy(broadsword));
        assertEquals(3, state.hero.combatDice());
    }

    @Test
    public void enhancementCostGrowsAndKeepsOnSlot() {
        PlayerState state = PlayerState.newGame();
        Shop shop = new Shop(state);
        long first = shop.enhanceCost(ItemSlot.WEAPON);
        assertEquals(Shop.WEAPON_ENHANCE_BASE, first);
        assertFalse(shop.canEnhance(ItemSlot.WEAPON));

        state.addGold(100000);
        int addsBefore = state.hero.combatAdds();
        assertTrue(shop.enhance(ItemSlot.WEAPON));
        assertEquals(addsBefore + ItemSlot.WEAPON.addsPerEnhance, state.hero.combatAdds());
        assertTrue(shop.enhanceCost(ItemSlot.WEAPON) > first);

        // 빈 방패 칸은 강화할 수 없다
        assertFalse(shop.canEnhance(ItemSlot.SHIELD));

        // 더 좋은 무기로 바꿔도 강화 단계는 유지된다
        state.hero.getAttributes().set(Attribute.STR, 20);
        state.hero.getAttributes().set(Attribute.DEX, 20);
        assertTrue(shop.buy(ItemCatalog.BASTARD_SWORD));
        Equipment eq = state.hero.getEquipment();
        assertEquals(1, eq.getEnhance(ItemSlot.WEAPON));
        assertEquals(ItemCatalog.BASTARD_SWORD.adds + ItemSlot.WEAPON.addsPerEnhance, eq.weaponAdds());
    }

    @Test
    public void catalogIdsAreUniqueAndSorted() {
        for (ItemSlot slot : ItemSlot.values()) {
            long last = -1;
            for (Item item : ItemCatalog.bySlot(slot)) {
                assertTrue(item.name + " 가격이 정렬되어야 한다", item.price > last);
                last = item.price;
                assertEquals(item, ItemCatalog.byId(item.id));
            }
        }
    }
}
