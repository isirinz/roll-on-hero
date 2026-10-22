package com.trialmobile.myfantasy.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.trialmobile.myfantasy.rules.Item;
import com.trialmobile.myfantasy.rules.ItemSlot;
import com.trialmobile.myfantasy.rules.ItemCatalog;

/**
 * 장비 아이콘 칸: 슬롯 틀({@code slot}) 위에 아이템 그림({@code item-<id>}, 32px를 2배)과
 * 오른쪽 아래 강화 단계(+3) 배지. 빈 칸이면 그 부위의 첫 아이템 그림을 어둡게 실루엣으로 보여 준다.
 */
public class ItemIcon extends Stack {
    public static final float SIZE = 84f;
    private static final Color SILHOUETTE = new Color(0.12f, 0.08f, 0.1f, 0.45f);
    private static final Color LOCKED = new Color(0.45f, 0.42f, 0.45f, 0.75f);

    private final Skin skin;
    private final Image picture = new Image();
    private final Label badge;
    private final Container<Label> badgeBox;

    public ItemIcon(Skin skin) {
        this.skin = skin;
        setTouchable(Touchable.disabled);
        if (skin.has("slot", TextureRegion.class)) {
            Image frame = new Image(skin.getDrawable("slot"));
            frame.setScaling(Scaling.stretch);
            add(frame);
        }
        picture.setScaling(Scaling.fit);
        Container<Image> pic = new Container<Image>(picture);
        pic.size(64f);
        add(pic);
        badge = new Label("", skin, "field-small");
        badge.setColor(Palette.GOLD);
        badgeBox = new Container<Label>(badge);
        badgeBox.align(Align.bottomRight).pad(0f, 0f, 4f, 8f);
        add(badgeBox);
    }

    /** 장착 칸 표시. item이 null이면 빈 칸(부위 실루엣). */
    public void setItem(ItemSlot slot, Item item, int enhance) {
        if (item == null) {
            Item sample = ItemCatalog.bySlot(slot).isEmpty() ? null : ItemCatalog.bySlot(slot).get(0);
            show(sample);
            picture.setColor(SILHOUETTE);
        } else {
            show(item);
            picture.setColor(Color.WHITE);
        }
        badge.setText(item != null && enhance > 0 ? "+" + enhance : "");
    }

    /** 상점 목록 표시. {@code dim}이면(능력치 부족 등) 흐리게. */
    public void setItem(Item item, boolean dim) {
        show(item);
        picture.setColor(dim ? LOCKED : Color.WHITE);
        badge.setText("");
    }

    private void show(Item item) {
        String name = item == null ? null : "item-" + item.id;
        picture.setDrawable(name != null && skin.has(name, TextureRegion.class) ? skin.getDrawable(name) : null);
    }
}
