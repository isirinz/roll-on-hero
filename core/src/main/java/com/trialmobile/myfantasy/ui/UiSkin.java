package com.trialmobile.myfantasy.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar.ProgressBarStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.trialmobile.myfantasy.assets.Fonts;

/**
 * 코드로 만드는 Scene2D Skin. 픽셀아트 에셋은 assets/ui/ 에 있다.
 * 패널·버튼·탭·슬롯은 PixelLab Pro로 만들고 {@code tools/ui-assets/build_ui2.py}로 나인패치용으로 정리했다.
 * <p>
 * 픽셀아트는 Nearest 필터로 {@link #SCALE}배(게이지는 {@link #BAR_SCALE}배) 확대해 그린다.
 * 나인패치 여백 값은 build_ui2.py의 값과 짝이다.
 * <p>
 * 드로어블: {@code white}(1x1), {@code round}(색 입히는 둥근 사각형, 캐릭터 자리 표시용),
 * {@code panel}(나무 액자 + 양피지), {@code panel-dark}(어두운 나무 + 보라 바탕), {@code frame}(액자, 가운데 투명),
 * {@code ribbon}(제목 리본), {@code pill}(골드 표시 캡슐), {@code tab}/{@code tab-on},
 * {@code btn-<색>}/{@code btn-<색>-down} (blue green gold rose purple off),
 * {@code gauge}(금테 게이지 틀, 배율 1 — {@link Gauge}가 확대), 색 띠 {@code gauge-hp/hero/exp/trail}, 속도 화살표 {@code arrow},
 * {@code bar-back/hp/skill/gold/green}, 통그림 {@code slot}(스킬 슬롯 고리), {@code badge}(방패),
 * 아이콘 {@code icon-coin/hero/equipment/skill/shop/flag}, 스킬 아이콘 {@code skill-<Skill.id>}, 장비 아이콘 {@code item-<Item.id>}(ui/items/), 배경 {@code battle-bg}, 챕터 배경 {@code bg-1..10}.
 * <p>
 * Label: {@code default/small/ink/dim/large/gold/title} (ink는 흰색 기준이라 setColor로 색을 정한다) (크림 패널 위, 진한 글자),
 * {@code field-small/field/field-large} (어두운 바탕·전투 배경 위, 외곽선 밝은 글자).
 * TextButton: {@code default}(금색), {@code small}(금색·작은 글자), {@code accent}(초록·작은 글자, 구매/강화),
 * {@code blue}, {@code blue-small}, {@code rose}, {@code purple}, {@code purple-small}(환생), {@code tab}(하단 탭). ProgressBar {@code hp/loading/exp/cooldown}; ScrollPane {@code default}.
 * <p>
 * 폰트는 {@link Fonts}가 소유하므로 Skin에 등록하지 않는다(중복 dispose 방지).
 */
public final class UiSkin {
    /** 패널·버튼·탭·슬롯 픽셀 배율. 캐릭터(3배)와 맞춘다. */
    public static final float SCALE = 3f;
    public static final float BAR_SCALE = 2f;
    /** 챕터 배경 그림 수(assets/ui/bg/chapter_1..N.png). */
    public static final int CHAPTER_BACKGROUNDS = 10;

    private static final String[] BUTTON_COLORS = {"blue", "green", "gold", "rose", "purple", "off"};
    private static final String[] BAR_COLORS = {"back", "hp", "skill", "gold", "green"};
    private static final String[] ICONS = {"coin", "hero", "equipment", "skill", "shop", "flag", "boost", "rebirth", "soul"};
    private static final String[] SKILL_IDS = {"take_that", "poor_baby", "vorpal_blade", "blasting_power", "hellbomb"};

    private UiSkin() {
    }

    public static Skin create(Fonts fonts) {
        Skin skin = new Skin();

        // --- 기본 ---
        Pixmap white = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        white.setColor(Color.WHITE);
        white.fill();
        skin.add("white", new Texture(white));
        white.dispose();
        Texture roundTex = roundedRect(32, 10);
        skin.add("round-tex", roundTex); // dispose 대상으로 등록
        skin.add("round", new NinePatch(new TextureRegion(roundTex), 10, 10, 10, 10));

        // --- 픽셀아트 나인패치 ---
        ninePatch(skin, "panel", "ui/panel.png", 12, 12, 12, 12, SCALE);
        ninePatch(skin, "panel-dark", "ui/panel_dark.png", 11, 11, 11, 11, SCALE);
        ninePatch(skin, "frame", "ui/frame.png", 10, 10, 10, 10, SCALE);
        ninePatch(skin, "ribbon", "ui/ribbon.png", 12, 12, 4, 5, SCALE);
        ninePatch(skin, "pill", "ui/pill.png", 8, 8, 4, 4, SCALE);
        ninePatch(skin, "tab", "ui/tab.png", 8, 8, 8, 9, SCALE);
        ninePatch(skin, "tab-on", "ui/tab_on.png", 8, 8, 8, 9, SCALE);
        for (String c : BUTTON_COLORS) {
            // 눌린 그림은 윗면이 2px 내려가 있으므로 글자 영역도 2px 내린다(T+2, B-2)
            ninePatch(skin, "btn-" + c, "ui/btn_" + c + ".png", 6, 6, 5, 6, SCALE);
            ninePatch(skin, "btn-" + c + "-down", "ui/btn_" + c + "_down.png", 6, 6, 7, 4, SCALE);
        }
        for (String c : BAR_COLORS) {
            ninePatch(skin, "bar-" + c, "ui/bar_" + c + ".png", 3, 3, 3, 3, BAR_SCALE);
        }

        ninePatch(skin, "gauge", "ui/gauge.png", 6, 6, 5, 5, 1f); // Gauge가 자기 배율로 복사해 쓴다
        // 게이지 채움 색 띠(위→아래 7줄: 광택, 밝음, 기본×2, 그늘, 진한 그늘, 바닥선)
        strip(skin, "gauge-hp", "ffc0b8", "ff7a78", "e8445a", "e8445a", "c02a48", "8e1a3a", "5e1030");
        strip(skin, "gauge-hero", "d8ffb0", "9ef07a", "5cc85a", "5cc85a", "3a9e4a", "267838", "18502a");
        strip(skin, "gauge-exp", "fff6c8", "ffe27a", "ffc844", "ffc844", "f0a020", "c07418", "8a4e10");
        strip(skin, "gauge-trail", "fff4e8", "fff0d8", "ffe6c4", "ffe6c4", "f5d4aa", "e0b88a", "b8906a");
        arrow(skin);
        DiceArt.register(skin);

        // --- 통그림·아이콘·배경 ---
        region(skin, "slot", "ui/slot.png");
        region(skin, "badge", "ui/badge.png");
        for (String icon : ICONS) region(skin, "icon-" + icon, "ui/icon_" + icon + ".png");
        for (String id : SKILL_IDS) region(skin, "skill-" + id, "ui/skill_" + id + ".png");
        for (com.trialmobile.myfantasy.rules.Item item : com.trialmobile.myfantasy.rules.ItemCatalog.all()) {
            region(skin, "item-" + item.id, "ui/items/" + item.id + ".png");
        }
        region(skin, "battle-bg", "ui/battle_bg.png");
        // 챕터 테마별 전투 배경(없으면 battle-bg를 쓴다). 번호는 MonsterTable의 테마 순서(1부터).
        for (int i = 1; i <= CHAPTER_BACKGROUNDS; i++) region(skin, "bg-" + i, "ui/bg/chapter_" + i + ".png");

        // --- Label ---
        skin.add("default", new LabelStyle(fonts.normal, Palette.TEXT));
        skin.add("small", new LabelStyle(fonts.small, Palette.TEXT));
        // 글자색을 setColor로 직접 정하는 용도(스타일 색이 흰색이라 곱해져도 그대로 나온다)
        skin.add("ink", new LabelStyle(fonts.normal, Color.WHITE));
        skin.add("ink-small", new LabelStyle(fonts.small, Color.WHITE));
        skin.add("gold-small", new LabelStyle(fonts.normal, Palette.HEADER));
        skin.add("dim", new LabelStyle(fonts.small, Palette.TEXT_DIM));
        skin.add("large", new LabelStyle(fonts.large, Palette.TEXT));
        skin.add("gold", new LabelStyle(fonts.large, Palette.HEADER));
        skin.add("title", new LabelStyle(fonts.title, Palette.GOLD));
        skin.add("field-small", new LabelStyle(fonts.smallOutline, Palette.TEXT_LIGHT));
        skin.add("field", new LabelStyle(fonts.normalOutline, Palette.TEXT_LIGHT));
        skin.add("field-large", new LabelStyle(fonts.largeOutline, Palette.TEXT_LIGHT));

        // --- TextButton --- (밝은 색 버튼 위에는 외곽선 있는 크림색 글자)
        skin.add("default", buttonStyle(skin, fonts.normalOutline, "gold"));
        skin.add("small", buttonStyle(skin, fonts.smallOutline, "gold"));
        skin.add("accent", buttonStyle(skin, fonts.smallOutline, "green"));
        skin.add("blue", buttonStyle(skin, fonts.normalOutline, "blue"));
        skin.add("blue-small", buttonStyle(skin, fonts.smallOutline, "blue"));
        skin.add("rose", buttonStyle(skin, fonts.normalOutline, "rose"));
        skin.add("purple", buttonStyle(skin, fonts.normalOutline, "purple"));
        skin.add("purple-small", buttonStyle(skin, fonts.smallOutline, "purple"));
        TextButtonStyle tab = new TextButtonStyle();
        tab.up = skin.getDrawable("tab");
        tab.down = skin.getDrawable("tab-on");
        tab.checked = skin.getDrawable("tab-on");
        tab.font = fonts.smallOutline;
        tab.fontColor = Palette.PARCHMENT;
        tab.checkedFontColor = Palette.CREAM;
        skin.add("tab", tab);

        // --- ScrollPane ---
        ScrollPaneStyle scroll = new ScrollPaneStyle();
        Drawable knob = skin.newDrawable("bar-back");
        ((BaseDrawable) knob).setMinWidth(8f);
        scroll.vScrollKnob = knob;
        skin.add("default", scroll);

        // --- ProgressBar ---
        skin.add("hp", barStyle(skin, "hp", 20f));
        skin.add("loading", barStyle(skin, "gold", 24f));
        skin.add("exp", barStyle(skin, "gold", 16f));
        skin.add("cooldown", barStyle(skin, "skill", 12f));

        return skin;
    }

    private static Texture texture(Skin skin, String path) {
        Texture t = new Texture(Gdx.files.internal(path));
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        skin.add(path, t); // dispose 대상으로 등록 (이름이 경로라 드로어블 이름과 겹치지 않는다)
        return t;
    }

    private static void ninePatch(Skin skin, String name, String path, int l, int r, int t, int b, float scale) {
        NinePatch patch = new NinePatch(texture(skin, path), l, r, t, b);
        patch.scale(scale, scale);
        skin.add(name, patch);
    }

    /** 파일이 없으면 건너뛴다(에셋을 하나씩 추가해 가는 중이라도 실행되게). */
    private static void region(Skin skin, String name, String path) {
        FileHandle file = Gdx.files.internal(path);
        if (!file.exists()) {
            Gdx.app.log("UiSkin", "에셋 없음: " + path);
            return;
        }
        skin.add(name, new TextureRegion(texture(skin, path)));
    }

    /** 세로 7줄 1px 색 띠(게이지 채움). 가로로 늘려 쓴다. */
    private static void strip(Skin skin, String name, String... colors) {
        Pixmap p = new Pixmap(1, colors.length, Pixmap.Format.RGBA8888);
        for (int i = 0; i < colors.length; i++) {
            p.setColor(Color.valueOf(colors[i]));
            p.drawPixel(0, i);
        }
        Texture t = new Texture(p);
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        p.dispose();
        skin.add(name + "-tex", t);
        skin.add(name, new TextureRegion(t));
    }

    /** 오른쪽을 가리키는 작은 삼각형(6x9, 진한 외곽선 + 크림색). 속도 버튼에 개수로 배속을 표시한다. */
    private static void arrow(Skin skin) {
        int w = 6, h = 9;
        Pixmap p = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        for (int r = 0; r < h; r++) {
            int len = w - Math.abs(r - h / 2);
            for (int c = 0; c < len; c++) {
                boolean edge = c == 0 || c == len - 1 || r == 0 || r == h - 1;
                p.setColor(edge ? Palette.OUTLINE : Palette.CREAM);
                p.drawPixel(c, r);
            }
        }
        Texture t = new Texture(p);
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        p.dispose();
        skin.add("arrow-tex", t);
        skin.add("arrow", new TextureRegion(t));
    }

    private static TextButtonStyle buttonStyle(Skin skin, BitmapFont font, String color) {
        TextButtonStyle s = new TextButtonStyle();
        s.up = skin.getDrawable("btn-" + color);
        s.down = skin.getDrawable("btn-" + color + "-down");
        s.disabled = skin.getDrawable("btn-off");
        s.font = font;
        s.fontColor = Palette.CREAM;
        s.disabledFontColor = Palette.PARCHMENT;
        return s;
    }

    private static ProgressBarStyle barStyle(Skin skin, String fill, float height) {
        ProgressBarStyle s = new ProgressBarStyle();
        NinePatchDrawable back = (NinePatchDrawable) skin.newDrawable("bar-back");
        NinePatchDrawable before = (NinePatchDrawable) skin.newDrawable("bar-" + fill);
        back.setMinHeight(height);
        before.setMinHeight(height);
        s.background = back;
        s.knobBefore = before;
        return s;
    }

    /** 흰색 둥근 사각형 텍스처. 나인패치로 늘려 쓰고 색은 tint로 입힌다. */
    private static Texture roundedRect(int size, int radius) {
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        p.setColor(Color.WHITE);
        p.fillRectangle(radius, 0, size - radius * 2, size);
        p.fillRectangle(0, radius, size, size - radius * 2);
        p.fillCircle(radius, radius, radius);
        p.fillCircle(size - radius - 1, radius, radius);
        p.fillCircle(radius, size - radius - 1, radius);
        p.fillCircle(size - radius - 1, size - radius - 1, radius);
        Texture t = new Texture(p);
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        p.dispose();
        return t;
    }
}
