package com.trialmobile.myfantasy.rules;

/** 장비 칸. 강화 효과도 칸별로 정해진다. */
public enum ItemSlot {
    /** 강화 1단계당 공격 보정치 +4. */
    WEAPON("무기", 4, 0),
    /** 강화 1단계당 방어 +1. */
    ARMOR("방어구", 0, 1),
    /** 강화 1단계당 방어 +1. */
    SHIELD("방패", 0, 1);

    public final String label;
    public final int addsPerEnhance;
    public final int hitsPerEnhance;

    ItemSlot(String label, int addsPerEnhance, int hitsPerEnhance) {
        this.label = label;
        this.addsPerEnhance = addsPerEnhance;
        this.hitsPerEnhance = hitsPerEnhance;
    }
}
