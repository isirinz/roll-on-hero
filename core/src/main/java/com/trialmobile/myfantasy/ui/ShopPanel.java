package com.trialmobile.myfantasy.ui;

import com.trialmobile.myfantasy.audio.Sfx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.trialmobile.myfantasy.i18n.Lang;
import com.trialmobile.myfantasy.i18n.Names;
import com.trialmobile.myfantasy.battle.PlayerState;
import com.trialmobile.myfantasy.rules.Attribute;
import com.trialmobile.myfantasy.rules.Attributes;
import com.trialmobile.myfantasy.rules.Item;
import com.trialmobile.myfantasy.rules.ItemCatalog;
import com.trialmobile.myfantasy.rules.ItemSlot;
import com.trialmobile.myfantasy.shop.Shop;

/** 상점 탭: 칸별 장비 목록. 구매하면 바로 장착하고, 보유한 장비는 다시 장착할 수 있다. */
public class ShopPanel extends TabPanel {
    private static final Color NAME_EQUIPPED = Palette.GOOD;
    private static final Color NAME_LOCKED = new Color(0.62f, 0.52f, 0.5f, 1f);

    private final PlayerState state;
    private final Shop shop;
    private final Runnable onChange;
    private final Array<ItemRow> rows = new Array<ItemRow>();
    private final TextButtonStyle smallStyle;
    private final TextButtonStyle accentStyle;

    public ShopPanel(Skin skin, PlayerState state, Shop shop, Runnable onChange) {
        this.state = state;
        this.shop = shop;
        this.onChange = onChange;
        this.smallStyle = skin.get("small", TextButtonStyle.class);
        this.accentStyle = skin.get("accent", TextButtonStyle.class);
        top();
        for (ItemSlot slot : ItemSlot.values()) {
            add(header(skin, Names.slot(slot))).padTop(slot.ordinal() == 0 ? 0f : 14f).padBottom(8f).row();
            for (Item item : ItemCatalog.bySlot(slot)) {
                ItemRow row = new ItemRow(skin, item);
                rows.add(row);
                add(row).growX().minWidth(0f).padBottom(8f).row();
            }
        }
        refresh();
    }

    @Override
    public void refresh() {
        for (ItemRow row : rows) row.refresh();
    }

    private final class ItemRow extends Table {
        final Item item;
        final Label name;
        final Label detail;
        final PriceButton button;
        final ItemIcon icon;

        ItemRow(Skin skin, Item item) {
            this.item = item;
            icon = new ItemIcon(skin);
            name = line(skin, "ink");
            name.setText(Names.item(item));
            detail = wrapped(skin, "dim");
            String req = Names.itemRequirement(item);
            detail.setText(req.isEmpty() ? Names.itemStat(item) : Lang.format("shop.requires", Names.itemStat(item), req));
            button = new PriceButton(skin, "small");
            button.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    onClick();
                }
            });
            Table text = new Table();
            text.add(name).growX().minWidth(0f).row();
            text.add(detail).growX().minWidth(0f).padTop(4f);
            Table card = card(skin);
            card.add(icon).size(72f).padRight(10f);
            card.add(text).growX().minWidth(0f).padRight(10f);
            card.add(button).size(BUTTON_WIDTH, BUTTON_HEIGHT);
            add(card).growX().minWidth(0f);
        }

        void onClick() {
            Shop.Status status = shop.status(item);
            boolean changed = false;
            if (status == Shop.Status.AFFORDABLE) {
                changed = shop.buy(item);
                if (changed) Sfx.play(Sfx.UI_BUY);
            } else if (status == Shop.Status.OWNED) {
                changed = shop.equip(item);
                if (changed) Sfx.play(Sfx.UI_CLICK);
            }
            if (changed) onChange.run();
        }

        void refresh() {
            Shop.Status status = shop.status(item);
            Color nameColor = Palette.TEXT;
            TextButtonStyle style = smallStyle;
            boolean enabled = false;
            switch (status) {
                case EQUIPPED:
                    button.setPlain(Lang.get("btn.equipped"));
                    nameColor = NAME_EQUIPPED;
                    break;
                case OWNED:
                    button.setPlain(Lang.get("btn.equip"));
                    enabled = true;
                    break;
                case OWNED_LOCKED:
                    button.setPlain(Lang.get("btn.statsLow"));
                    break;
                case LOCKED:
                    button.setPlain(missingRequirement());
                    nameColor = NAME_LOCKED;
                    break;
                case AFFORDABLE:
                    button.setPrice(Lang.get("btn.buy"), item.price);
                    style = accentStyle;
                    enabled = true;
                    break;
                default: // TOO_EXPENSIVE
                    button.setPrice("", item.price);
                    break;
            }
            if (button.getStyle() != style) button.setStyle(style);
            button.setDisabled(!enabled);
            name.setColor(nameColor);
            icon.setItem(item, status == Shop.Status.LOCKED || status == Shop.Status.OWNED_LOCKED);
        }

        /** "힘 15 필요" — 모자란 능력치만. */
        private String missingRequirement() {
            Attributes a = state.hero.getAttributes();
            if (a.get(Attribute.STR) < item.reqStr) return Lang.format("btn.needStr", item.reqStr);
            if (a.get(Attribute.DEX) < item.reqDex) return Lang.format("btn.needDex", item.reqDex);
            return Lang.get("btn.cannotEquip");
        }
    }
}
