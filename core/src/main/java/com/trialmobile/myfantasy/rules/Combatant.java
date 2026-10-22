package com.trialmobile.myfantasy.rules;

/** 전투에 참여할 수 있는 대상(영웅, 몬스터). */
public interface Combatant {
    String getName();

    /** 공격 시 굴릴 D6 개수. */
    int combatDice();

    /** 공격 굴림에 더하는 보정치. */
    int combatAdds();

    /** 방어구가 매 라운드 흡수하는 피해량. */
    int armorHits();

    int getHp();

    int getMaxHp();

    void takeDamage(int amount);

    boolean isDead();
}
