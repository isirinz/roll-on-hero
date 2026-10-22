package com.trialmobile.myfantasy.rules;

/** 스킬 한 번 발동한 결과 (불변). */
public final class SkillCast {
    public final Skill skill;
    /** 몬스터에게 준 피해 (방어구·스파이트와 무관하게 MR을 바로 깎는다). */
    public final int damage;
    /** 영웅이 회복한 체력. */
    public final int heal;
    /** 걸린 버프 지속 시간(초). */
    public final double buffSeconds;

    public SkillCast(Skill skill, int damage, int heal, double buffSeconds) {
        this.skill = skill;
        this.damage = damage;
        this.heal = heal;
        this.buffSeconds = buffSeconds;
    }
}
