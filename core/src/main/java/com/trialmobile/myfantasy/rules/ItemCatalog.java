package com.trialmobile.myfantasy.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 게임에 등장하는 모든 장비. 저장 데이터는 아이템을 {@link Item#id}로 기록하므로 id는 바꾸지 말 것.
 * 수치는 원작의 무기·갑옷 표를 참고해 방치형 진행에 맞게 늘렸다. 가격·요구치 조정은 여기서 한다.
 */
public final class ItemCatalog {
    // --- 무기 ---
    public static final Item SHORT_SWORD = Item.weapon("short_sword", "숏소드", 3, 0, 7, 0, 0);
    public static final Item BROADSWORD = Item.weapon("broadsword", "브로드소드", 3, 4, 15, 10, 400);
    public static final Item BASTARD_SWORD = Item.weapon("bastard_sword", "바스타드 소드", 4, 4, 17, 12, 2500);
    public static final Item GREAT_SWORD = Item.weapon("great_sword", "그레이트소드", 6, 0, 21, 16, 12000);
    public static final Item CLAYMORE = Item.weapon("claymore", "클레이모어", 7, 3, 25, 18, 50000);
    public static final Item RUNE_BLADE = Item.weapon("rune_blade", "룬 블레이드", 9, 4, 30, 22, 200000);
    public static final Item DRAGON_SLAYER = Item.weapon("dragon_slayer", "드래곤 슬레이어", 12, 6, 38, 26, 900000);

    // --- 방어구 ---
    public static final Item QUILTED = Item.armor("quilted", "누비 갑옷", 3, 0, 0);
    public static final Item LEATHER = Item.armor("leather", "가죽 갑옷", 6, 0, 300);
    public static final Item RING_MAIL = Item.armor("ring_mail", "링 메일", 8, 14, 1800);
    public static final Item MAIL = Item.armor("mail", "사슬 갑옷", 11, 18, 9000);
    public static final Item PLATE = Item.armor("plate", "판금 갑옷", 14, 24, 40000);
    public static final Item MITHRIL_PLATE = Item.armor("mithril_plate", "미스릴 판금", 18, 28, 180000);
    public static final Item DRAGON_SCALE = Item.armor("dragon_scale", "용비늘 갑옷", 24, 34, 800000);

    // --- 방패 ---
    public static final Item BUCKLER = Item.shield("buckler", "버클러", 3, 0, 150);
    public static final Item TARGET = Item.shield("target", "타지 방패", 4, 12, 1000);
    public static final Item KITE = Item.shield("kite", "카이트 실드", 5, 16, 6000);
    public static final Item TOWER = Item.shield("tower", "타워 실드", 7, 22, 30000);
    public static final Item RUNE_SHIELD = Item.shield("rune_shield", "룬 방패", 10, 30, 250000);

    private static final List<Item> ALL;
    private static final Map<String, Item> BY_ID = new LinkedHashMap<String, Item>();

    static {
        List<Item> list = new ArrayList<Item>();
        Collections.addAll(list,
            SHORT_SWORD, BROADSWORD, BASTARD_SWORD, GREAT_SWORD, CLAYMORE, RUNE_BLADE, DRAGON_SLAYER,
            QUILTED, LEATHER, RING_MAIL, MAIL, PLATE, MITHRIL_PLATE, DRAGON_SCALE,
            BUCKLER, TARGET, KITE, TOWER, RUNE_SHIELD);
        ALL = Collections.unmodifiableList(list);
        for (Item item : ALL) {
            if (BY_ID.put(item.id, item) != null) throw new IllegalStateException("중복 아이템 id: " + item.id);
        }
    }

    private ItemCatalog() {
    }

    /** 칸별·가격순으로 정렬된 전체 목록. */
    public static List<Item> all() {
        return ALL;
    }

    public static List<Item> bySlot(ItemSlot slot) {
        List<Item> result = new ArrayList<Item>();
        for (Item item : ALL) if (item.slot == slot) result.add(item);
        return result;
    }

    /** id로 찾는다. 없으면 null. */
    public static Item byId(String id) {
        return id == null ? null : BY_ID.get(id);
    }
}
