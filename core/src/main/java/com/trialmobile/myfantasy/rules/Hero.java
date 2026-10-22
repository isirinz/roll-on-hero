package com.trialmobile.myfantasy.rules;

/**
 * 플레이어 영웅.
 * 터널스 앤 트롤스처럼 체력(CON) 수치가 곧 최대 생명력이다.
 */
public final class Hero implements Combatant {
    private final String name;
    private final Attributes attributes;
    private final Equipment equipment = new Equipment();
    private final SkillBook skills = new SkillBook();
    private int level = 1;
    /** 현재 레벨에서 모은 모험 점수(Adventure Points). */
    private int ap;
    private int con;

    public Hero(String name, Attributes attributes) {
        this.name = name;
        this.attributes = attributes;
        this.con = maxCon();
        skills.syncUnlocks(level);
    }

    /** 새 게임 시작용 전사. 운에 좌우되지 않도록 고정 능력치를 쓴다. */
    public static Hero createStarter(String name) {
        Hero hero = new Hero(name, new Attributes(13, 14, 12, 12, 12, 10, 10, 10));
        hero.equipment.equip(ItemCatalog.SHORT_SWORD);
        hero.equipment.equip(ItemCatalog.QUILTED);
        return hero;
    }

    /** 저장 데이터에서 성장 상태를 되살린다. */
    public void restore(int level, int ap, int con) {
        this.level = Math.max(1, level);
        this.ap = Math.max(0, ap);
        skills.syncUnlocks(this.level);
        // 쓰러진 상태로 저장됐다면 부활한 것으로 본다.
        this.con = con <= 0 ? maxCon() : Math.min(maxCon(), con);
    }

    @Override
    public String getName() {
        return name;
    }

    public Attributes getAttributes() {
        return attributes;
    }

    public Equipment getEquipment() {
        return equipment;
    }

    public SkillBook getSkills() {
        return skills;
    }

    public int getLevel() {
        return level;
    }

    public int getAp() {
        return ap;
    }

    public int apToNextLevel() {
        return Progression.apForNextLevel(level);
    }

    // --- 전투 ---

    @Override
    public int combatDice() {
        int dice = equipment.weaponDice();
        return skills.isVorpalActive() ? dice * 2 : dice; // 보팔 블레이드
    }

    @Override
    public int combatAdds() {
        return equipment.weaponAdds() + attributes.personalAdds();
    }

    @Override
    public int armorHits() {
        return equipment.armorHits();
    }

    @Override
    public int getHp() {
        return con;
    }

    @Override
    public int getMaxHp() {
        return maxCon();
    }

    public int maxCon() {
        return attributes.get(Attribute.CON);
    }

    @Override
    public void takeDamage(int amount) {
        if (amount > 0) con = Math.max(0, con - amount);
    }

    public void heal(int amount) {
        if (amount > 0) con = Math.min(maxCon(), con + amount);
    }

    public void healFull() {
        con = maxCon();
    }

    @Override
    public boolean isDead() {
        return con <= 0;
    }

    // --- 성장 ---

    /**
     * 모험 점수를 얻는다. 레벨업이 일어나면 능력치가 오르고 체력이 회복된다.
     * @return 이번에 오른 레벨 수
     */
    public int gainAp(int amount) {
        ap += Math.max(0, amount);
        int gained = 0;
        while (ap >= apToNextLevel()) {
            ap -= apToNextLevel();
            level++;
            gained++;
            Progression.applyLevelUp(attributes);
        }
        if (gained > 0) {
            healFull();
            skills.syncUnlocks(level);
        }
        return gained;
    }
}
