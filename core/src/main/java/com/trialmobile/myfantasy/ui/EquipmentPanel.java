package com.trialmobile.myfantasy.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.trialmobile.myfantasy.audio.Sfx;
import com.trialmobile.myfantasy.i18n.Lang;
import com.trialmobile.myfantasy.i18n.Names;
import com.trialmobile.myfantasy.battle.PlayerState;
import com.trialmobile.myfantasy.rules.DiceRoll;
import com.trialmobile.myfantasy.rules.Equipment;
import com.trialmobile.myfantasy.rules.Hero;
import com.trialmobile.myfantasy.rules.Item;
import com.trialmobile.myfantasy.rules.ItemSlot;
import com.trialmobile.myfantasy.shop.Shop;

/** 장비 탭: 장착 중인 장비와 칸별 강화. */
public class EquipmentPanel extends TabPanel {
    private final PlayerState state;
    private final Shop shop;
    private final Runnable onChange;
    private final SlotRow[] rows = new SlotRow[ItemSlot.values().length];
    /** 맨 위 요약: 공격 굴림·방어 숫자를 크게. */
    private final Label attackValue;
    private final Label armorValue;

    public EquipmentPanel(Skin skin, PlayerState state, Shop shop, Runnable onChange) {
        this.state = state;
        this.shop = shop;
        this.onChange = onChange;
        top();
        attackValue = line(skin, "large");
        armorValue = line(skin, "large");
        Table summary = card(skin);
        summary.add(statBlock(skin, Lang.get("equip.statAttack"), attackValue)).growX().uniformX().minWidth(0f);
        summary.add(statBlock(skin, Lang.get("equip.statArmor"), armorValue)).growX().uniformX().minWidth(0f);
        add(summary).growX().minWidth(0f).padBottom(12f).row();
        for (ItemSlot slot : ItemSlot.values()) {
            SlotRow row = new SlotRow(skin, slot);
            rows[slot.ordinal()] = row;
            add(row).growX().minWidth(0f).padBottom(10f).row();
        }
        refresh();
    }

    @Override
    public void refresh() {
        for (SlotRow row : rows) row.refresh();
        Hero h = state.hero;
        attackValue.setText(DiceRoll.notation(h.combatDice(), h.combatAdds()));
        armorValue.setText(String.valueOf(h.armorHits()));
    }

    /** 작은 이름표 아래 큰 숫자. */
    private static Table statBlock(Skin skin, String name, Label value) {
        Table t = new Table();
        Label label = line(skin, "ink");
        label.setColor(Palette.TEXT_DIM);
        label.setText(name);
        label.setAlignment(Align.center);
        value.setAlignment(Align.center);
        t.add(label).growX().minWidth(0f).row();
        t.add(value).growX().minWidth(0f).padTop(2f);
        return t;
    }

    private final class SlotRow extends Table {
        final ItemSlot slot;
        final Label title;
        /** 이 칸의 수치(3D6+4 / 방어 6): 진하게. */
        final Label stat;
        /** 강화하면 오르는 양: 초록. */
        final Label detail;
        final PriceButton button;
        final ItemIcon icon;

        SlotRow(Skin skin, ItemSlot slot) {
            this.slot = slot;
            icon = new ItemIcon(skin);
            title = line(skin, "default");
            stat = line(skin, "ink"); // 흰 글자에 색을 입힌다(기본 글자색에 곱하면 검게 나온다)
            stat.setColor(Palette.HEADER);
            detail = line(skin, "ink");
            button = new PriceButton(skin, "accent");
            button.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (shop.enhance(SlotRow.this.slot)) {
                        Sfx.play(Sfx.UI_UPGRADE);
                        onChange.run();
                    }
                }
            });
            Table card = card(skin);
            card.add(icon).size(ItemIcon.SIZE).padRight(12f);
            Table text = new Table();
            text.add(title).growX().minWidth(0f).row();
            // 수치와 강화 효과를 한 줄에: "3D6   강화 +4"
            Table statLine = new Table();
            statLine.add(stat).left().padRight(14f);
            statLine.add(detail).growX().minWidth(0f).left();
            text.add(statLine).growX().minWidth(0f).padTop(6f);
            card.add(text).growX().minWidth(0f).padRight(10f);
            card.add(button).size(BUTTON_WIDTH, BUTTON_HEIGHT);
            add(card).growX().minWidth(0f);
        }

        void refresh() {
            Equipment eq = state.hero.getEquipment();
            Item item = eq.get(slot);
            int level = eq.getEnhance(slot);
            icon.setItem(slot, item, level);
            if (item == null) {
                title.setText(Lang.format("equip.empty", Names.slot(slot)));
                stat.setText("-");
                detail.setText(Lang.get("equip.buyHint"));
                detail.setColor(Palette.TEXT_DIM);
                button.setPlain(Lang.get("btn.upgrade"));
                button.setDisabled(true);
                return;
            }
            title.setText(Names.item(item) + (level > 0 ? " +" + level : "")); // 부위는 아이콘으로 보이니 이름만
            String stat = slot == ItemSlot.WEAPON
                ? DiceRoll.notation(eq.weaponDice(), eq.weaponAdds())
                : Lang.format("stat.armor", eq.slotHits(slot));
            String next = slot == ItemSlot.WEAPON
                ? Lang.format("equip.nextAttack", slot.addsPerEnhance)
                : Lang.format("equip.nextArmor", slot.hitsPerEnhance);
            this.stat.setText(stat);
            detail.setText(Lang.format("equip.next", next));
            detail.setColor(Palette.GOOD);
            button.setPrice(Lang.get("btn.upgrade"), shop.enhanceCost(slot));
            button.setDisabled(!shop.canEnhance(slot));
        }
    }
}
