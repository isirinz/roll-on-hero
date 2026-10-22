package com.trialmobile.myfantasy.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.trialmobile.myfantasy.battle.PowerBoost;
import com.trialmobile.myfantasy.i18n.Lang;

/**
 * 2배 버프 버튼(전투 영역 가운데 위). 상태에 따라 모양이 바뀐다.
 * <ul>
 *   <li>사용 가능: 금색 버튼 + 뒤에서 빛이 맥박친다, "2배 버프"</li>
 *   <li>켜짐: 초록 버튼 + 남은 시간(14:59)</li>
 *   <li>쿨타임: 회색(누를 수 없음) + 다시 쓸 수 있을 때까지(1:59:12)</li>
 *   <li>광고 준비 중: 회색 + "광고 준비 중"</li>
 * </ul>
 * 상태 판단은 화면이 {@link #update}로 매 프레임 넘겨 준다.
 */
public class BoostButton extends Button {
    public enum State { READY, ACTIVE, COOLDOWN, AD_LOADING }

    private static final Color READY_GLOW = new Color(1f, 0.82f, 0.3f, 1f);
    private static final Color ACTIVE_GLOW = new Color(0.45f, 1f, 0.5f, 1f);

    private final Skin skin;
    private final TextButtonStyle ready;
    private final TextButtonStyle active;
    private final Image icon;
    private final Label label;
    private State state;
    private float time;

    public BoostButton(Skin skin) {
        super(skin.get("default", TextButtonStyle.class));
        this.skin = skin;
        this.ready = skin.get("default", TextButtonStyle.class);
        this.active = skin.get("accent", TextButtonStyle.class);
        icon = skin.has("icon-boost", TextureRegion.class) ? new Image(skin.getDrawable("icon-boost")) : new Image();
        icon.setScaling(Scaling.fit);
        label = new Label("", skin, "field-small");
        label.setAlignment(Align.center);
        label.setEllipsis(true);
        pad(4f, 2f, 8f, 2f);
        add(icon).size(48f).row();
        add(label).growX().minWidth(0f).padTop(0f);
        setTransform(false);
    }

    /** 매 프레임: 지금 시각의 버프 상태와 광고 준비 여부로 모양을 맞춘다. */
    public void update(PowerBoost boost, long now, boolean adReady) {
        State next;
        String text;
        if (boost.isActive(now)) {
            next = State.ACTIVE;
            text = Format.clock(boost.activeRemaining(now));
        } else if (!boost.isReady(now)) {
            next = State.COOLDOWN;
            text = Format.clock(boost.cooldownRemaining(now));
        } else if (!adReady) {
            next = State.AD_LOADING;
            text = Lang.get("boost.adLoading");
        } else {
            next = State.READY;
            text = Lang.get("boost.ready");
        }
        if (next != state) {
            state = next;
            setStyle(next == State.ACTIVE ? active : ready);
            setDisabled(next == State.COOLDOWN || next == State.AD_LOADING);
            icon.setColor(isDisabled() ? new Color(0.55f, 0.55f, 0.6f, 0.85f) : Color.WHITE);
            label.setColor(next == State.ACTIVE ? Palette.TEXT_LIGHT : next == State.READY ? Palette.TEXT_LIGHT : Palette.TEXT_DIM);
        }
        label.setText(text);
    }

    public State getState() {
        return state;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        time += delta;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        // 사용 가능·켜짐일 때 뒤에서 빛이 맥박친다(눈길을 끌도록)
        if ((state == State.READY || state == State.ACTIVE) && skin.has("glow", TextureRegion.class)) {
            Color g = state == State.READY ? READY_GLOW : ACTIVE_GLOW;
            float pulse = state == State.READY
                ? 0.55f + 0.45f * (0.5f + 0.5f * MathUtils.sin(time * 4f))
                : 0.35f + 0.15f * (0.5f + 0.5f * MathUtils.sin(time * 2.5f));
            float s = Math.max(getWidth(), getHeight()) * (1.55f + 0.12f * MathUtils.sin(time * 4f));
            batch.setColor(g.r, g.g, g.b, g.a * parentAlpha * pulse);
            batch.draw(skin.getRegion("glow"), getX() + getWidth() / 2f - s / 2f, getY() + getHeight() / 2f - s / 2f, s, s);
            batch.setColor(Color.WHITE);
        }
        // 사용 가능하면 아이콘이 살짝 통통 튄다
        float bob = state == State.READY ? Math.abs(MathUtils.sin(time * 3f)) * 5f : 0f;
        icon.setY(icon.getY() + bob);
        super.draw(batch, parentAlpha);
        icon.setY(icon.getY() - bob);
    }
}
