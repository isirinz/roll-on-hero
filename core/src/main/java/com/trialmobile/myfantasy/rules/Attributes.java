package com.trialmobile.myfantasy.rules;

/** 능력치 묶음. */
public final class Attributes {
    private final int[] values = new int[Attribute.values().length];

    public Attributes() {
    }

    public Attributes(int str, int con, int dex, int spd, int lk, int iq, int wiz, int chr) {
        set(Attribute.STR, str);
        set(Attribute.CON, con);
        set(Attribute.DEX, dex);
        set(Attribute.SPD, spd);
        set(Attribute.LK, lk);
        set(Attribute.IQ, iq);
        set(Attribute.WIZ, wiz);
        set(Attribute.CHR, chr);
    }

    /** 모든 능력치를 3D6 TARO로 굴려 만든다. */
    public static Attributes roll(Dice dice) {
        Attributes a = new Attributes();
        for (Attribute attribute : Attribute.values()) a.set(attribute, dice.rollTaro());
        return a;
    }

    public int get(Attribute attribute) {
        return values[attribute.ordinal()];
    }

    public void set(Attribute attribute, int value) {
        values[attribute.ordinal()] = Math.max(1, value);
    }

    public void add(Attribute attribute, int amount) {
        set(attribute, get(attribute) + amount);
    }

    /**
     * 개인 전투 보정치(Personal Adds, 5판 방식).
     * 힘·행운·민첩·속도 각각 12를 넘는 1점당 +1, 9 미만인 1점당 -1.
     */
    public int personalAdds() {
        return addsFor(get(Attribute.STR)) + addsFor(get(Attribute.LK))
            + addsFor(get(Attribute.DEX)) + addsFor(get(Attribute.SPD));
    }

    static int addsFor(int value) {
        if (value > 12) return value - 12;
        if (value < 9) return value - 9;
        return 0;
    }

    public Attributes copy() {
        Attributes c = new Attributes();
        System.arraycopy(values, 0, c.values, 0, values.length);
        return c;
    }
}
