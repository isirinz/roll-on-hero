package com.trialmobile.myfantasy.anim;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;

/** 위로 천천히 떠오르며 반짝이다 사라지는 반짝이 하나(2배 버프 중 영웅 주변). 끝나면 스스로 사라진다. */
public class RisingSparkle extends Actor {
    private final TextureRegion region;
    private final Color tint;
    private final float life;
    private final float size;
    private final float sway;
    private float age;

    public RisingSparkle(TextureRegion region, Color tint, float x, float y) {
        this.region = region;
        this.tint = new Color(tint);
        this.life = MathUtils.random(0.7f, 1.2f);
        this.size = MathUtils.random(14f, 24f);
        this.sway = MathUtils.random(MathUtils.PI2);
        setPosition(x, y);
    }

    @Override
    public void act(float delta) {
        age += delta;
        if (age >= life) {
            remove();
            return;
        }
        moveBy(MathUtils.sin(age * 6f + sway) * 18f * delta, 70f * delta);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        float p = age / life;
        float twinkle = 0.6f + 0.4f * MathUtils.sin(age * 22f + sway);
        float a = (p < 0.2f ? p / 0.2f : 1f - (p - 0.2f) / 0.8f) * twinkle;
        float s = size * (1f - 0.4f * p);
        batch.setColor(tint.r, tint.g, tint.b, tint.a * parentAlpha * a);
        batch.draw(region, getX() - s / 2f, getY() - s / 2f, s / 2f, s / 2f, s, s, 1f, 1f, age * 90f);
        batch.setColor(Color.WHITE);
    }
}
