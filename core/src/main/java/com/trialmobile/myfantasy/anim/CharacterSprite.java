package com.trialmobile.myfantasy.anim;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;

/**
 * 애니메이션 캐릭터 한 명. 대기(idle)를 반복하다가, 공격·피격·시전 같은 한 번짜리 동작을 끝내면 대기로 돌아간다.
 * 쓰러짐(death)은 마지막 프레임에서 멈춘다.
 * <p>
 * 그림은 위젯 영역의 아래 가운데에 발을 맞춰 그린다. 원본 방향과 원하는 방향이 다르면 좌우로 뒤집는다.
 * 액터 색(tint)과 알파가 그대로 적용되므로 Actions.color/fadeOut으로 번쩍임·사라짐을 줄 수 있다.
 * <p>
 * 짧은 움직임(앞으로 튀어 나가기, 흔들림, 미끄러져 등장)은 레이아웃 위치를 건드리지 않고 그리는 위치만 옮긴다.
 * 그래서 옆의 라벨이 바뀌어 표가 다시 배치돼도 연출이 끊기지 않는다.
 */
public class CharacterSprite extends Widget {
    public static final String IDLE = "idle";
    public static final String ATTACK = "attack";
    public static final String HURT = "hurt";
    public static final String CAST = "cast";
    public static final String DEATH = "death";

    private SpriteLibrary.CharacterAnims anims;
    private boolean faceRight;
    private String current = IDLE;
    private Animation<TextureRegion> animation;
    private float stateTime;

    private static final float LUNGE_TIME = 0.22f;
    private static final float SHAKE_TIME = 0.25f;
    private static final float SLIDE_TIME = 0.35f;
    /** 동작 재생 속도(전투 배속). */
    private float timeScale = 1f;
    private float lungeTime;
    private float lungeDistance;
    private float shakeTime;
    private float slideTime;
    private float slideFrom;

    public CharacterSprite(SpriteLibrary.CharacterAnims anims, boolean faceRight) {
        this.faceRight = faceRight;
        setAnims(anims);
    }

    /** 다른 캐릭터로 바꾼다(새 몬스터 등장). 대기 동작부터 시작한다. */
    public void setAnims(SpriteLibrary.CharacterAnims anims) {
        this.anims = anims;
        play(IDLE);
        invalidateHierarchy();
    }

    public SpriteLibrary.CharacterAnims getAnims() {
        return anims;
    }

    /** 동작을 처음부터 재생한다. 없는 동작이면 대기로 대신한다(쓰러짐은 예외: 대기 그대로 둔다). */
    public void play(String name) {
        if (anims == null) return;
        Animation<TextureRegion> next = anims.get(name);
        if (next == null) {
            if (DEATH.equals(name)) {
                current = DEATH; // 쓰러짐 그림이 없으면 대기 프레임에서 멈춘 채로 사라진다
                animation = anims.get(IDLE);
                stateTime = 0f;
                return;
            }
            name = IDLE;
            next = anims.get(IDLE);
        }
        current = name;
        animation = next;
        stateTime = 0f;
    }

    /** 전투 배속에 맞춰 동작을 빨리 재생한다(배속 3이면 공격 동작도 3배 빠르게). */
    public void setTimeScale(float timeScale) {
        this.timeScale = Math.max(0.1f, timeScale);
    }

    /** 상대 쪽으로 잠깐 튀어 나갔다 돌아온다(양수 = 오른쪽). */
    public void lunge(float distance) {
        lungeDistance = distance;
        lungeTime = LUNGE_TIME;
    }

    /** 맞았을 때 좌우로 잘게 흔들린다. */
    public void shake() {
        shakeTime = SHAKE_TIME;
    }

    /** {@code fromOffset}만큼 떨어진 곳에서 제자리로 미끄러져 들어온다. */
    public void slideIn(float fromOffset) {
        slideFrom = fromOffset;
        slideTime = SLIDE_TIME;
    }

    /** 그림의 실제 높이(확대 반영). 이펙트 위치를 잡을 때 쓴다. */
    public float getSpriteHeight() {
        return anims == null ? getHeight() : anims.frameHeight * anims.scale;
    }

    public String getCurrent() {
        return current;
    }

    public boolean isDead() {
        return DEATH.equals(current);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        delta *= timeScale;
        lungeTime = Math.max(0f, lungeTime - delta);
        shakeTime = Math.max(0f, shakeTime - delta);
        slideTime = Math.max(0f, slideTime - delta);
        if (animation == null) return;
        stateTime += delta;
        if (!IDLE.equals(current) && !DEATH.equals(current) && animation.isAnimationFinished(stateTime)) {
            play(IDLE);
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (animation == null) return;
        TextureRegion frame = DEATH.equals(current) && animation.getPlayMode() == Animation.PlayMode.LOOP
            ? animation.getKeyFrame(0f)
            : animation.getKeyFrame(stateTime);
        float scale = anims.scale;
        float w = anims.frameWidth * scale;
        float h = anims.frameHeight * scale;
        float x = getX() + (getWidth() - w) / 2f + motionOffset();
        float y = getY() - anims.footY * scale; // 사망 동작 등이 발보다 아래로 내려가 칸이 커진 만큼 내려 발을 땅에 붙인다
        boolean flip = faceRight != anims.facesRight;

        Color c = getColor();
        batch.setColor(c.r, c.g, c.b, c.a * parentAlpha);
        if (flip) batch.draw(frame, x + w, y, -w, h);
        else batch.draw(frame, x, y, w, h);
        batch.setColor(Color.WHITE);
    }

    private float motionOffset() {
        float ox = 0f;
        if (lungeTime > 0f) ox += lungeDistance * MathUtils.sin(MathUtils.PI * (1f - lungeTime / LUNGE_TIME));
        if (shakeTime > 0f) ox += MathUtils.random(-4f, 4f) * (shakeTime / SHAKE_TIME);
        if (slideTime > 0f) {
            float t = slideTime / SLIDE_TIME;
            ox += slideFrom * t * t;
        }
        return ox;
    }

    @Override
    public float getPrefWidth() {
        return anims == null ? 0f : anims.frameWidth * anims.scale;
    }

    @Override
    public float getPrefHeight() {
        return anims == null ? 0f : anims.frameHeight * anims.scale;
    }
}
