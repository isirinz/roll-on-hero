package com.trialmobile.myfantasy.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.utils.Align;

/**
 * 금테 게이지(체력·경험치). 그리는 순서: 어두운 홈 → 잔상 막대 → 채움 막대(위아래 음영 띠) → 눈금 → 지나가는 광택 → 금테 → 글자.
 * <p>
 * 값이 줄면 채움 막대는 곧바로 줄고, 밝은 잔상 막대가 잠깐 머물다 따라 줄어 "얼마나 깎였는지"가 보인다.
 * 값이 늘면 채움 막대가 부드럽게 차오른다. 틀 그림은 {@code gauge}(45x11, 안쪽 홈 투명), 채움 색 띠는
 * {@code gauge-<종류>} (1x7 세로 띠)로 {@link UiSkin}이 만든다.
 */
public class Gauge extends Widget {
    /** 틀 그림 안쪽 홈의 위치(원본 픽셀). */
    private static final int TROUGH_SIDE = 4, TROUGH_BOTTOM = 2, TROUGH_HEIGHT = 7;
    private static final int FRAME_HEIGHT = 11;
    private static final float TRAIL_DELAY = 0.35f;
    private static final float SHINE_PERIOD = 3.2f;

    private final NinePatch frame;
    private final TextureRegion fill;
    private final TextureRegion trail;
    private final TextureRegion white;
    private final BitmapFont font;
    private final GlyphLayout layout = new GlyphLayout();
    private final float scale;
    private final int ticks;

    private float target = 1f;
    private float shown = 1f;
    private float trailValue = 1f;
    private float trailWait;
    private float shineTime;
    private String text = "";

    /**
     * @param kind  채움 색: {@code hp}(빨강), {@code hero}(초록), {@code exp}(금색)
     * @param ticks 눈금 칸 수(0이면 눈금 없음)
     */
    public Gauge(Skin skin, String kind, float scale, int ticks) {
        this.scale = scale;
        this.ticks = ticks;
        frame = new NinePatch(skin.getPatch("gauge"));
        frame.scale(scale, scale);
        fill = skin.getRegion("gauge-" + kind);
        trail = skin.getRegion("gauge-trail");
        white = new TextureRegion(skin.get("white", Texture.class));
        font = skin.get("field-small", LabelStyle.class).font;
    }

    /** @param instant true면 애니메이션 없이 바로(몬스터 교체 등) */
    public void setValue(float value, boolean instant) {
        value = Math.max(0f, Math.min(1f, value));
        if (instant) {
            target = shown = trailValue = value;
            trailWait = 0f;
            return;
        }
        if (value < target) {
            trailValue = Math.max(trailValue, shown);
            trailWait = TRAIL_DELAY;
        }
        target = value;
    }

    public void setValue(float value) {
        setValue(value, false);
    }

    public void setText(String text) {
        this.text = text == null ? "" : text;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        // 줄어들 땐 바로, 늘어날 땐 부드럽게
        if (target < shown) shown = target;
        else shown = Math.min(target, shown + delta * 1.5f);
        if (trailWait > 0f) trailWait -= delta;
        else if (trailValue > shown) trailValue = Math.max(shown, trailValue - delta * 0.9f);
        if (trailValue < shown) trailValue = shown;
        shineTime = (shineTime + delta) % SHINE_PERIOD;
    }

    @Override
    public float getPrefWidth() {
        return 160f;
    }

    @Override
    public float getPrefHeight() {
        return FRAME_HEIGHT * scale;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        validate();
        float a = getColor().a * parentAlpha;
        float h = getPrefHeight();
        float x = getX(), y = getY() + (getHeight() - h) / 2f, w = getWidth();
        float ix = x + TROUGH_SIDE * scale, iy = y + TROUGH_BOTTOM * scale;
        float iw = w - TROUGH_SIDE * 2 * scale, ih = TROUGH_HEIGHT * scale;

        // 홈
        batch.setColor(0.13f, 0.07f, 0.13f, a);
        batch.draw(white, ix, iy, iw, ih);
        // 잔상 → 채움 (픽셀 단위로 끊는다)
        float tw = snap(iw * trailValue), fw = snap(iw * shown);
        if (tw > fw) {
            batch.setColor(1f, 1f, 1f, a);
            batch.draw(trail, ix + fw, iy, tw - fw, ih);
        }
        if (fw > 0f) {
            batch.setColor(1f, 1f, 1f, a);
            batch.draw(fill, ix, iy, fw, ih);
            // 지나가는 광택: 기울어진 밝은 띠
            float t = shineTime / SHINE_PERIOD * 1.6f - 0.3f; // -0.3 ~ 1.3
            float sx = ix + iw * t;
            batch.setColor(1f, 1f, 1f, 0.35f * a);
            for (int row = 0; row < TROUGH_HEIGHT; row++) {
                float px = snap(sx + row * scale - ix) + ix;
                float left = Math.max(ix, px), right = Math.min(ix + fw, px + 2 * scale);
                if (right > left) batch.draw(white, left, iy + row * scale, right - left, scale);
            }
        }
        // 눈금
        if (ticks > 1) {
            batch.setColor(0f, 0f, 0f, 0.28f * a);
            for (int i = 1; i < ticks; i++) {
                float tx = ix + snap(iw * i / ticks);
                batch.draw(white, tx, iy + scale, scale, ih - 2 * scale);
            }
        }
        // 금테
        batch.setColor(1f, 1f, 1f, a);
        frame.draw(batch, x, y, w, h);
        batch.setColor(Color.WHITE);

        if (text.length() > 0) {
            font.setColor(Palette.CREAM.r, Palette.CREAM.g, Palette.CREAM.b, a);
            layout.setText(font, text, font.getColor(), w, Align.center, false);
            font.draw(batch, layout, x, iy + ih / 2f + layout.height / 2f + scale * 0.5f);
            font.setColor(Color.WHITE);
        }
    }

    private float snap(float v) {
        return (float) Math.floor(v / scale) * scale;
    }
}
