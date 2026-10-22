package com.trialmobile.myfantasy.ui;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Scaling;

/**
 * 가격이 붙은 버튼: "강화 [동전] 1.23A". 골드 표기에서 G를 뺐으므로(단위 글자와 헷갈림) 동전 그림으로 골드임을 보여 준다.
 * 가격 없이 글자만 쓸 때(“Lv 3 필요” 등)는 {@link #setPlain}.
 */
public class PriceButton extends TextButton {
    private static final float COIN_SIZE = 30f;

    private final Image coin;
    private final Label amount;

    public PriceButton(Skin skin, String style) {
        this(skin, style, "icon-coin");
    }

    /** @param icon 가격 앞 그림(골드 {@code icon-coin}, 영혼석 {@code icon-soul}) */
    public PriceButton(Skin skin, String style, String icon) {
        super("", skin, style);
        coin = skin.has(icon, TextureRegion.class) ? new Image(skin.getDrawable(icon)) : new Image();
        coin.setScaling(Scaling.fit);
        amount = new Label("", getLabel().getStyle());
        Label verb = getLabel();
        clearChildren();
        add(verb);
        add(coin).size(COIN_SIZE).padLeft(6f).padRight(2f);
        add(amount);
    }

    /** @param verb "구매", "강화" 등. 비우면 가격만 표시한다. */
    public void setPrice(String verb, long gold) {
        setText(verb);
        getLabel().setVisible(verb.length() > 0);
        getCell(getLabel()).width(verb.length() > 0 ? getLabel().getPrefWidth() : 0f);
        coin.setVisible(true);
        getCell(coin).size(COIN_SIZE).padLeft(verb.length() > 0 ? 6f : 0f);
        amount.setText(Format.gold(gold));
        getCell(amount).width(amount.getPrefWidth());
        invalidateHierarchy();
    }

    public void setPlain(String text) {
        setText(text);
        getLabel().setVisible(true);
        getCell(getLabel()).width(getLabel().getPrefWidth());
        coin.setVisible(false);
        getCell(coin).size(0f).padLeft(0f);
        amount.setText("");
        getCell(amount).width(0f);
        invalidateHierarchy();
    }

    @Override
    public void setStyle(com.badlogic.gdx.scenes.scene2d.ui.Button.ButtonStyle style) {
        super.setStyle(style);
        if (amount != null) amount.setStyle(getLabel().getStyle());
    }
}
