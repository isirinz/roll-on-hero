package com.trialmobile.myfantasy.anim;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;

/** 한 번 재생하고 스스로 사라지는 이펙트. 주어진 위치에 이펙트의 기준점(pivot)이 오도록 놓는다. */
public class EffectActor extends Actor {
    private final SpriteLibrary.Effect effect;
    private final boolean flip;
    private float stateTime;

    public EffectActor(SpriteLibrary.Effect effect, float x, float y, float extraScale, boolean flip) {
        this.effect = effect;
        this.flip = flip;
        float s = effect.scale * extraScale;
        setSize(effect.frameWidth * s, effect.frameHeight * s);
        float px = flip ? 1f - effect.pivotX : effect.pivotX;
        setPosition(x - getWidth() * px, y - getHeight() * effect.pivotY);
        setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        stateTime += delta;
        if (effect.animation.isAnimationFinished(stateTime)) remove();
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        TextureRegion frame = effect.animation.getKeyFrame(stateTime);
        Color c = getColor();
        batch.setColor(c.r, c.g, c.b, c.a * parentAlpha);
        if (flip) batch.draw(frame, getX() + getWidth(), getY(), -getWidth(), getHeight());
        else batch.draw(frame, getX(), getY(), getWidth(), getHeight());
        batch.setColor(Color.WHITE);
    }
}
