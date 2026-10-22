package com.trialmobile.myfantasy.rules;

/**
 * 몬스터 등급(Monster Rating, MR) 방식의 몬스터.
 * <ul>
 *   <li>MR이 곧 생명력이다.</li>
 *   <li>주사위 수 = 처음 MR / 10 + 1 (피해를 입어도 줄지 않는다).</li>
 *   <li>보정치 = 현재 MR / 2 (올림). 다칠수록 약해진다.</li>
 * </ul>
 */
public final class Monster implements Combatant {
    private final String kindId;
    private final String name;
    private final int maxMr;
    private final int armor;
    private final boolean boss;
    private int mr;

    /**
     * @param kindId 몬스터 종류 id (그림·연출을 고르는 데 쓴다, 예: "goblin"). 없으면 null.
     */
    public Monster(String kindId, String name, int mr, int armor, boolean boss) {
        this.kindId = kindId;
        this.name = name;
        this.maxMr = Math.max(1, mr);
        this.mr = this.maxMr;
        this.armor = Math.max(0, armor);
        this.boss = boss;
    }

    public Monster(String name, int mr, int armor, boolean boss) {
        this(null, name, mr, armor, boss);
    }

    public Monster(String name, int mr) {
        this(null, name, mr, 0, false);
    }

    /** 몬스터 종류 id (예: "goblin"). 종류 없이 만든 몬스터는 null. */
    public String getKindId() {
        return kindId;
    }

    @Override
    public String getName() {
        return name;
    }

    public boolean isBoss() {
        return boss;
    }

    public int getMaxMr() {
        return maxMr;
    }

    public int getMr() {
        return mr;
    }

    @Override
    public int combatDice() {
        return maxMr / 10 + 1;
    }

    @Override
    public int combatAdds() {
        return (mr + 1) / 2;
    }

    @Override
    public int armorHits() {
        return armor;
    }

    @Override
    public int getHp() {
        return mr;
    }

    @Override
    public int getMaxHp() {
        return maxMr;
    }

    @Override
    public void takeDamage(int amount) {
        if (amount > 0) mr = Math.max(0, mr - amount);
    }

    @Override
    public boolean isDead() {
        return mr <= 0;
    }
}
