package com.trialmobile.myfantasy.rules;

/**
 * 영웅의 주문(스킬). 터널스 앤 트롤스의 대표 주문을 바탕으로 했다. 이름도 원작처럼 시전자가 외치는 우스꽝스러운 한마디다.
 * 여기의 label/shortLabel은 기본값이고, 화면에는 i18n의 {@code skill.<id>.name/short}를 쓴다.
 * 해금 레벨에 도달하면 자동으로 1레벨이 되고, 이후 골드로 강화한다.
 * 전투 중 쿨타임이 끝나면 자동으로 발동한다(효과 계산은 {@link Spells}).
 * <p>
 * 저장 데이터는 이 enum의 순서로 스킬 레벨을 기록하므로, 새 스킬은 반드시 맨 뒤에 추가할 것.
 */
public enum Skill {
    /** IQ만큼 피해. 원작의 "Take That You Fiend!". */
    TAKE_THAT("take_that", "받아라 이 악마야!", "악마야", Kind.DAMAGE, 1, 6.0, 30),
    /** 체력이 60% 이하일 때 WIZ에 비례해 회복. 원작의 "Poor Baby". */
    POOR_BABY("poor_baby", "아이고 가엾어라", "치유", Kind.HEAL, 3, 12.0, 40),
    /** 일정 시간 무기 주사위 2배. 원작의 "Vorpal Blade"를 원작풍 외침으로. */
    VORPAL_BLADE("vorpal_blade", "썰고 다지고 썰어라!", "썰기", Kind.BUFF, 5, 15.0, 60),
    /** 주사위 + WIZ 피해. 원작의 "Blasting Power"를 원작풍 외침으로. */
    BLASTING_POWER("blasting_power", "펑! 하고 터져라", "펑", Kind.DAMAGE, 8, 10.0, 90),
    /** (IQ + WIZ) × 2 피해. 원작의 "Hellbomb Bursts"를 원작풍 외침으로. */
    HELLBOMB("hellbomb", "다들 엎드려!!", "엎드려", Kind.DAMAGE, 15, 30.0, 200);

    public enum Kind { DAMAGE, HEAL, BUFF }

    public final String id;
    public final String label;
    /** 전투 화면 쿨타임 바에 쓰는 짧은 이름. */
    public final String shortLabel;
    public final Kind kind;
    public final int unlockLevel;
    /** 쿨타임(초). */
    public final double cooldown;
    /** 쿨타임(밀리초). 전투 시간은 밀리초 정수로 계산한다. */
    public final long cooldownMillis;
    /** 1→2레벨 강화 비용. */
    public final long upgradeBaseCost;

    Skill(String id, String label, String shortLabel, Kind kind, int unlockLevel, double cooldown, long upgradeBaseCost) {
        this.id = id;
        this.label = label;
        this.shortLabel = shortLabel;
        this.kind = kind;
        this.unlockLevel = unlockLevel;
        this.cooldown = cooldown;
        this.cooldownMillis = Math.round(cooldown * 1000.0);
        this.upgradeBaseCost = upgradeBaseCost;
    }
}
