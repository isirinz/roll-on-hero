package com.trialmobile.myfantasy.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;

/**
 * 전투 화면 하단 탭에 들어가는 패널. 게임 상태가 바뀌면 {@link #refresh()}가 호출된다.
 * <p>
 * 패널은 전투 화면이 감싼 스크롤 안에 들어가므로 직접 스크롤을 만들지 않는다. 대신 가로 폭은 절대
 * 넘치면 안 되므로, 글자는 {@link #line}(한 줄, 넘치면 …)이나 {@link #wrapped}(줄바꿈)로 만들고
 * 글자 칸에는 {@code minWidth(0)}을 준다.
 */
public abstract class TabPanel extends Table {
    /** 목록 한 줄의 오른쪽 버튼 크기. */
    protected static final float BUTTON_WIDTH = 204f;
    protected static final float BUTTON_HEIGHT = 68f;
    private static final Color CARD = new Color(0.66f, 0.44f, 0.29f, 0.16f);

    public abstract void refresh();

    /** 한 줄 글자. 칸보다 길면 작은 글꼴로, 그래도 길면 끝을 …로 줄인다. */
    protected static Label line(Skin skin, String style) {
        // 길면(영어 등) 한 단계 작은 글꼴로 줄이고, 그래도 넘치면 …
        return new FitLabel("", skin, style);
    }

    /** 칸 너비에 맞춰 줄바꿈되는 글자. */
    protected static Label wrapped(Skin skin, String style) {
        Label label = new Label("", skin, style);
        label.setWrap(true);
        label.setAlignment(Align.topLeft);
        return label;
    }

    /** 목록 한 줄의 옅은 바탕 카드. */
    protected static Table card(Skin skin) {
        Table t = new Table();
        t.setBackground(skin.newDrawable("white", CARD));
        t.pad(12f, 14f, 12f, 12f);
        return t;
    }

    /** 리본 모양 소제목. */
    protected static Container<Label> header(Skin skin, String text) {
        Label label = new Label(text, skin, "field");
        label.setAlignment(Align.center);
        Container<Label> c = new Container<Label>(label);
        if (skin.has("ribbon", NinePatch.class)) c.setBackground(skin.getDrawable("ribbon"));
        c.pad(4f, 44f, 14f, 44f);
        return c;
    }

    /** 픽셀아트 그림. 없으면 빈 칸. */
    protected static Image picture(Skin skin, String name) {
        Image image = skin.has(name, TextureRegion.class) ? new Image(skin.getDrawable(name)) : new Image();
        image.setScaling(Scaling.fit);
        return image;
    }
}
