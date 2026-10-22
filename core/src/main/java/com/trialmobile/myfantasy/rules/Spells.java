package com.trialmobile.myfantasy.rules;

/**
 * 스킬 효과 계산과 발동. 스킬 밸런스 조정은 이 클래스와 {@link Skill}에서 한다.
 * <ul>
 *   <li>스킬 레벨 배율 = 1 + 0.3 × (스킬 레벨 − 1)</li>
 *   <li>받아라 이 악마야!: IQ × 배율 피해</li>
 *   <li>아이고 가엾어라: 체력 60% 이하일 때만, WIZ × 1.5 × 배율 회복</li>
 *   <li>보팔 블레이드: 4 + 0.5 × (레벨 − 1)초 동안 무기 주사위 2배</li>
 *   <li>폭발의 힘: (3 + 레벨/2)D6 + WIZ × 배율 피해</li>
 *   <li>헬봄 버스트: (IQ + WIZ) × 2 × 배율 피해</li>
 * </ul>
 * IQ와 WIZ는 레벨업마다 1씩 오른다({@link Progression}).
 */
public final class Spells {
    public static final double LEVEL_SCALING = 0.3;
    public static final double HEAL_THRESHOLD = 0.6;

    private Spells() {
    }

    public static double multiplier(int skillLevel) {
        return 1.0 + LEVEL_SCALING * (Math.max(1, skillLevel) - 1);
    }

    /** 지금 효과가 있는 상황인지 (쿨타임과 별개). */
    public static boolean canCast(Skill skill, Hero hero, Monster monster) {
        if (!hero.getSkills().isUnlocked(skill) || hero.isDead()) return false;
        switch (skill.kind) {
            case HEAL:
                return hero.getHp() <= hero.maxCon() * HEAL_THRESHOLD;
            case BUFF:
                return monster != null && !monster.isDead() && !hero.getSkills().isVorpalActive();
            default:
                return monster != null && !monster.isDead();
        }
    }

    /**
     * 다음 스킬 발동까지 남은 시간. 지금 바로 쓸 수 있으면 0,
     * 쿨타임이 끝났지만 조건이 안 맞는 스킬(예: 체력이 높을 때의 회복)은 제외한다.
     * @return 밀리초, 쓸 스킬이 없으면 {@link Long#MAX_VALUE}
     */
    public static long millisUntilNextCast(Hero hero, Monster monster) {
        SkillBook book = hero.getSkills();
        long best = Long.MAX_VALUE;
        for (Skill s : Skill.values()) {
            if (!book.isUnlocked(s)) continue;
            long cd = book.getCooldownMillis(s);
            if (cd > 0L) best = Math.min(best, cd);
            else if (canCast(s, hero, monster)) return 0L;
        }
        return best;
    }

    /** 스킬을 발동한다: 효과를 적용하고 쿨타임을 시작한다. */
    public static SkillCast cast(Skill skill, Hero hero, Monster monster, Dice dice) {
        return cast(skill, hero, monster, dice, 1);
    }

    /** {@code damageMultiplier}: 공격 스킬 피해 배율(2배 버프). 회복·버프에는 걸리지 않는다. */
    public static SkillCast cast(Skill skill, Hero hero, Monster monster, Dice dice, int damageMultiplier) {
        SkillBook book = hero.getSkills();
        Attributes a = hero.getAttributes();
        double mult = multiplier(book.getLevel(skill));
        int damage = 0;
        int heal = 0;
        double buff = 0.0;
        switch (skill) {
            case TAKE_THAT:
                damage = (int) Math.round(a.get(Attribute.IQ) * mult);
                break;
            case POOR_BABY:
                int before = hero.getHp();
                hero.heal((int) Math.round(a.get(Attribute.WIZ) * 1.5 * mult));
                heal = hero.getHp() - before;
                break;
            case VORPAL_BLADE:
                buff = vorpalSeconds(book.getLevel(skill));
                book.startVorpal(Math.round(buff * 1000.0));
                break;
            case BLASTING_POWER:
                damage = dice.roll(blastDice(book.getLevel(skill)), 0).total()
                    + (int) Math.round(a.get(Attribute.WIZ) * mult);
                break;
            case HELLBOMB:
                damage = (int) Math.round((a.get(Attribute.IQ) + a.get(Attribute.WIZ)) * 2 * mult);
                break;
        }
        damage *= Math.max(1, damageMultiplier);
        if (damage > 0 && monster != null) monster.takeDamage(damage);
        book.startCooldown(skill);
        return new SkillCast(skill, damage, heal, buff);
    }

    public static double vorpalSeconds(int skillLevel) {
        return 4.0 + 0.5 * (Math.max(1, skillLevel) - 1);
    }

    public static int blastDice(int skillLevel) {
        return 3 + Math.max(1, skillLevel) / 2;
    }

    /** 스킬 탭에 보여 줄 현재 효과 설명. */
    public static String describe(Skill skill, Hero hero, int skillLevel) {
        Attributes a = hero.getAttributes();
        double mult = multiplier(skillLevel);
        String cd = " · " + Math.round(skill.cooldown) + "초마다";
        switch (skill) {
            case TAKE_THAT:
                return "피해 " + Math.round(a.get(Attribute.IQ) * mult) + " (지능 × " + fmt(mult) + ")" + cd;
            case POOR_BABY:
                return "체력 60% 이하일 때 " + Math.round(a.get(Attribute.WIZ) * 1.5 * mult) + " 회복" + cd;
            case VORPAL_BLADE:
                return fmt(vorpalSeconds(skillLevel)) + "초간 무기 주사위 2배" + cd;
            case BLASTING_POWER:
                return "피해 " + blastDice(skillLevel) + "D6+" + Math.round(a.get(Attribute.WIZ) * mult) + cd;
            case HELLBOMB:
                return "피해 " + Math.round((a.get(Attribute.IQ) + a.get(Attribute.WIZ)) * 2 * mult) + cd;
            default:
                return "";
        }
    }

    private static String fmt(double v) {
        long r = Math.round(v * 10);
        return r % 10 == 0 ? String.valueOf(r / 10) : (r / 10) + "." + (r % 10);
    }
}
