package com.trialmobile.myfantasy.rules;

/** 터널스 앤 트롤스의 8가지 능력치. */
public enum Attribute {
    STR("힘"),
    CON("체력"),
    DEX("민첩"),
    SPD("속도"),
    LK("행운"),
    IQ("지능"),
    WIZ("마력"),
    CHR("매력");

    public final String label;

    Attribute(String label) {
        this.label = label;
    }
}
