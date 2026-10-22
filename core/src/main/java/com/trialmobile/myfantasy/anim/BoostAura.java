package com.trialmobile.myfantasy.anim;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;

/**
 * 2배 버프 중 영웅을 감싸는 금빛 기운({@code glow-soft}). 밝은 배경에서도 보이도록 안쪽이 넓게 진한 원을 쓴다.
 * 발밑의 납작한 빛 웅덩이 + 몸 뒤로 은은한 빛기둥이 맥박친다. 위치는 {@link #setFeet}로 매 프레임 맞춘다.
 */
public class BoostAura extends Actor {
    private final TextureRegion glow;
    private final Color tint;
    private float time;
    private float bodyHeight = 200f;

    public BoostAura(TextureRegion glow, Color tint) {
        this.glow = glow;
        this.tint = new Color(tint);
    }

    /** 영웅 발 위치(부모 좌표)와 몸 높이. */
    public void setFeet(float x, float y, float bodyHeight) {
        setPosition(x, y);
        this.bodyHeight = bodyHeight;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        time += delta;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        float a = getColor().a * parentAlpha;
        if (a <= 0f) return;
        float pulse = 0.75f + 0.25f * MathUtils.sin(time * 4f);
        // 몸 뒤 빛기둥(옅게)
        float cw = 170f * (0.95f + 0.05f * pulse);
        float ch = bodyHeight * 1.2f;
        batch.setColor(tint.r, tint.g, tint.b, a * 0.22f * pulse);
        batch.draw(glow, getX() - cw / 2f, getY() - ch * 0.08f, cw, ch);
        // 발밑 빛 웅덩이(진하게) + 가운데 밝은 심
        float w = 250f * (0.92f + 0.08f * pulse);
        batch.setColor(tint.r, tint.g, tint.b, a * 0.6f * pulse);
        batch.draw(glow, getX() - w / 2f, getY() - w * 0.13f, w, w * 0.26f);
        batch.setColor(1f, 0.95f, 0.7f, a * 0.55f);
        batch.draw(glow, getX() - w * 0.28f, getY() - w * 0.06f, w * 0.56f, w * 0.12f);
        batch.setColor(Color.WHITE);
    }
}
