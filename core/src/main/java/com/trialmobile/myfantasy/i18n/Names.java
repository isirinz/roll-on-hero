package com.trialmobile.myfantasy.i18n;

import com.trialmobile.myfantasy.battle.PlayerState;
import com.trialmobile.myfantasy.rules.Attribute;
import com.trialmobile.myfantasy.rules.Attributes;
import com.trialmobile.myfantasy.rules.DiceRoll;
import com.trialmobile.myfantasy.rules.Hero;
import com.trialmobile.myfantasy.rules.Item;
import com.trialmobile.myfantasy.rules.ItemSlot;
import com.trialmobile.myfantasy.rules.Monster;
import com.trialmobile.myfantasy.rules.Skill;
import com.trialmobile.myfantasy.rules.Spells;

/**
 * 게임 규칙 객체(스킬·아이템·몬스터·능력치 …)의 화면용 이름과 설명.
 * <p>
 * 규칙 패키지는 libGDX 없이 테스트되는 순수 Java라 번역을 모른다. 규칙 객체의 한국어 {@code label}/{@code name}은
 * 기본값(키가 없을 때 대신 쓰는 값)일 뿐이고, 화면에는 항상 이 클래스를 거쳐 id로 찾은 번역을 쓴다.
 * 키: {@code skill.<id>.name/short}, {@code item.<id>}, {@code monster.<kindId>}, {@code slot.<SLOT>}, {@code attr.<ATTR>}.
 */
public final class Names {
    private Names() {
    }

    public static String skill(Skill s) {
        return Lang.getOr("skill." + s.id + ".name", s.label);
    }

    public static String skillShort(Skill s) {
        return Lang.getOr("skill." + s.id + ".short", s.shortLabel);
    }

    public static String item(Item item) {
        return Lang.getOr("item." + item.id, item.name);
    }

    public static String slot(ItemSlot slot) {
        return Lang.getOr("slot." + slot.name(), slot.label);
    }

    public static String attr(Attribute a) {
        return Lang.getOr("attr." + a.name(), a.label);
    }

    public static String monster(Monster m) {
        return m.getKindId() == null ? m.getName() : Lang.getOr("monster." + m.getKindId(), m.getName());
    }

    /** 처음 이름(“모험가”) 그대로면 현재 언어의 기본 이름으로, 바꾼 이름이면 그대로. */
    public static String hero(Hero h) {
        return PlayerState.DEFAULT_HERO_NAME.equals(h.getName()) ? Lang.get("hero.defaultName") : h.getName();
    }

    /** "3D6+4" 또는 "방어 6". */
    public static String itemStat(Item item) {
        return item.slot == ItemSlot.WEAPON
            ? DiceRoll.notation(item.dice, item.adds)
            : Lang.format("stat.armor", item.hits);
    }

    /** "힘 15 · 민첩 10" (요구치 없으면 빈 문자열). */
    public static String itemRequirement(Item item) {
        StringBuilder sb = new StringBuilder();
        if (item.reqStr > 0) sb.append(Lang.format("req.str", item.reqStr));
        if (item.reqDex > 0) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(Lang.format("req.dex", item.reqDex));
        }
        return sb.toString();
    }

    /** 스킬 탭의 현재 효과 설명. 수치는 {@link Spells}의 공식과 같다. */
    public static String skillEffect(Skill skill, Hero hero, int level) {
        Attributes a = hero.getAttributes();
        double mult = Spells.multiplier(level);
        String text;
        switch (skill) {
            case TAKE_THAT:
                text = Lang.format("skill.desc.take_that", Math.round(a.get(Attribute.IQ) * mult), decimal(mult));
                break;
            case POOR_BABY:
                text = Lang.format("skill.desc.poor_baby", Math.round(a.get(Attribute.WIZ) * 1.5 * mult));
                break;
            case VORPAL_BLADE:
                text = Lang.format("skill.desc.vorpal_blade", decimal(Spells.vorpalSeconds(level)));
                break;
            case BLASTING_POWER:
                text = Lang.format("skill.desc.blasting_power", Spells.blastDice(level), Math.round(a.get(Attribute.WIZ) * mult));
                break;
            case HELLBOMB:
                text = Lang.format("skill.desc.hellbomb", Math.round((a.get(Attribute.IQ) + a.get(Attribute.WIZ)) * 2 * mult));
                break;
            default:
                text = "";
        }
        return text + " · " + Lang.format("skill.cooldown", Math.round(skill.cooldown));
    }

    /** "2시간 13분" / "2h 13m". */
    public static String duration(double seconds) {
        long total = Math.round(seconds);
        long h = total / 3600;
        long m = (total % 3600) / 60;
        if (h > 0) return m > 0 ? Lang.format("duration.hm", h, m) : Lang.format("duration.h", h);
        if (m > 0) return Lang.format("duration.m", m);
        return Lang.format("duration.s", total);
    }

    /** 1.0 → "1", 1.3 → "1.3". */
    private static String decimal(double v) {
        long r = Math.round(v * 10);
        return r % 10 == 0 ? Long.toString(r / 10) : (r / 10) + "." + (r % 10);
    }
}
