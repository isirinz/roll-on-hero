package com.trialmobile.myfantasy.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.utils.Align;

/**
 * 전투 화면의 스킬 슬롯 하나: 금 고리 안에 스킬 아이콘, 쿨타임 동안은 위에서부터 어둡게 가려진다.
 * 고리 아래에 작은 글씨로 레벨을 쓴다(해금 전이면 흐린 색으로 해금 레벨). 스킬은 자동 발동이라 누를 필요는 없다(표시용).
 */
public class SkillSlot extends Widget {
    private static final Color SHADE = new Color(0.10f, 0.05f, 0.12f, 0.72f);
    private static final Color LOCKED = new Color(0.45f, 0.42f, 0.48f, 1f);
    /** 고리 그림(26x29) 안쪽 둥근 칸의 위치·크기(원본 픽셀). */
    private static final float INNER_X = 4f, INNER_Y = 6f, INNER_SIZE = 18f;
    /** 고리 아래 글자 줄 높이. */
    private static final float CAPTION_HEIGHT = 24f;
    private static final Color CAPTION_LOCKED = new Color(0.78f, 0.72f, 0.70f, 1f);

    private final TextureRegion ring;
    private final TextureRegion icon;
    private final TextureRegion white;
    private final BitmapFont font;
    private final GlyphLayout layout = new GlyphLayout();
    private final float scale;

    private float readiness = 1f;
    private boolean unlocked = true;
    private boolean glowing;
    private String caption = "";
    private float flash;

    public SkillSlot(Skin skin, String skillId, float scale) {
        this.scale = scale;
        ring = skin.has("slot", TextureRegion.class) ? skin.getRegion("slot") : null;
        icon = skin.has("skill-" + skillId, TextureRegion.class) ? skin.getRegion("skill-" + skillId) : null;
        white = new TextureRegion(skin.get("white", com.badlogic.gdx.graphics.Texture.class));
        font = skin.get("field-small", LabelStyle.class).font;
    }

    /** @param readiness 0(방금 씀) ~ 1(준비됨) */
    public void setState(boolean unlocked, float readiness, String caption, boolean glowing) {
        if (unlocked && this.readiness < 1f && readiness >= 1f) flash = 0.35f;
        this.unlocked = unlocked;
        this.readiness = readiness;
        this.caption = caption;
        this.glowing = glowing;
    }

    /** 스킬이 발동했을 때 고리를 잠깐 번쩍인다. */
    public void pulse() {
        flash = 0.35f;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (flash > 0f) flash = Math.max(0f, flash - delta);
    }

    @Override
    public float getPrefWidth() {
        return (ring != null ? ring.getRegionWidth() : 26) * scale;
    }

    @Override
    public float getPrefHeight() {
        return ringHeight() + CAPTION_HEIGHT;
    }

    private float ringHeight() {
        return (ring != null ? ring.getRegionHeight() : 29) * scale;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        validate();
        float a = getColor().a * parentAlpha;
        float x = getX() + (getWidth() - getPrefWidth()) / 2f;
        float y = getY() + (getHeight() - getPrefHeight()) / 2f + CAPTION_HEIGHT; // 고리 아래쪽 좌표
        float ix = x + INNER_X * scale, iy = y + INNER_Y * scale, is = INNER_SIZE * scale;

        // 고리 안쪽은 투명하게 뚫려 있다: 바탕 → 아이콘 → 쿨타임 그늘 → 빛 → 고리 순으로 그리면
        // 네모난 것들의 모서리는 고리가 덮어 둥글게 보인다.
        batch.setColor(Palette.OUTLINE.r, Palette.OUTLINE.g, Palette.OUTLINE.b, a);
        batch.draw(white, ix, iy, is, is);
        if (icon != null) {
            if (unlocked) batch.setColor(1f, 1f, 1f, a);
            else batch.setColor(LOCKED.r, LOCKED.g, LOCKED.b, a);
            batch.draw(icon, ix, iy, is, is);
        }
        // 쿨타임: 아직 안 찬 만큼 위에서부터 가린다
        float shade = unlocked ? 1f - readiness : 0.5f;
        if (shade > 0.001f) {
            batch.setColor(SHADE.r, SHADE.g, SHADE.b, SHADE.a * a);
            float h = (float) Math.ceil(is * shade / scale) * scale; // 픽셀 단위로 끊어 내린다
            batch.draw(white, ix, iy + is - Math.min(h, is), is, Math.min(h, is));
        }
        if (glowing || flash > 0f) {
            float k = glowing ? 0.25f : flash / 0.35f * 0.6f;
            batch.setColor(1f, 0.9f, 0.5f, k * a);
            batch.draw(white, ix, iy, is, is);
        }
        if (ring != null) {
            batch.setColor(1f, 1f, 1f, a);
            batch.draw(ring, x, y, getPrefWidth(), ringHeight());
        }
        batch.setColor(Color.WHITE);

        if (caption != null && caption.length() > 0) {
            Color c = unlocked ? Palette.CREAM : CAPTION_LOCKED;
            font.setColor(c.r, c.g, c.b, a);
            float w = getPrefWidth() + 40f; // 고리보다 조금 넓게 써도 된다
            layout.setText(font, caption, font.getColor(), w, Align.center, false);
            font.draw(batch, layout, x - 20f, y - (CAPTION_HEIGHT - layout.height) / 2f + 1f);
            font.setColor(Color.WHITE);
        }
    }
}
