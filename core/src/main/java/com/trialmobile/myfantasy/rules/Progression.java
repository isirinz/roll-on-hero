package com.trialmobile.myfantasy.rules;

/**
 * 영웅 성장 수치. 밸런스 조정은 이 클래스에서 한다.
 * 원작의 레벨 기준(1000, 3000, 7000 AP…)은 방치형에 너무 느려서 곡선으로 대체했다.
 */
public final class Progression {
    /** 레벨 1→2에 필요한 AP. */
    public static final int BASE_AP = 30;
    /** 레벨이 오를수록 필요 AP가 늘어나는 정도. */
    public static final double AP_EXPONENT = 1.6;

    private Progression() {
    }

    /** 현재 레벨에서 다음 레벨까지 필요한 AP. */
    public static int apForNextLevel(int level) {
        return (int) Math.round(BASE_AP * Math.pow(Math.max(1, level), AP_EXPONENT));
    }

    /**
     * 레벨업 시 능력치 상승. 힘·민첩·행운은 전투 보정치, 체력은 최대 생명력,
     * 지능·마력은 스킬 위력이 된다({@link Spells}).
     */
    public static void applyLevelUp(Attributes a) {
        a.add(Attribute.STR, 1);
        a.add(Attribute.CON, 2);
        a.add(Attribute.DEX, 1);
        a.add(Attribute.LK, 1);
        a.add(Attribute.IQ, 1);
        a.add(Attribute.WIZ, 1);
    }
}
