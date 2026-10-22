package com.trialmobile.myfantasy.anim;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;
import com.trialmobile.myfantasy.rules.DiceRoll;

/**
 * 한 쪽의 공격 굴림 연출: 주사위가 전투원 손에서 포물선으로 날아가며 굴러가고(눈이 바뀌며 회전, 반짝이 꼬리),
 * 바닥에 튕겨 실제 눈으로 멈춘다. 6은 금빛으로 빛나며 터진다. 모두 멈추면 타격 총합이 튀어나온다.
 * <p>
 * 좌표는 이 그룹을 담은 부모(전투 영역 레이어) 기준. {@link #setTimeScale}로 배속에 맞춘다.
 * 끝나면 스스로 사라진다.
 */
public class DiceThrow extends Group {
    /** 화면에 실제로 던지는 최대 주사위 수(그 이상은 +N으로 표시). */
    public static final int MAX_SHOWN = 6;
    /** 주사위 칸 간격과 확대 배율. */
    private static final float SPACING = 54f;
    private static final float SCALE = 3f;
    private static final float SIZE = com.trialmobile.myfantasy.ui.DiceArt.DIE * SCALE;
    private static final float FLIGHT = 0.34f;
    private static final float STAGGER = 0.035f;
    private static final float BOUNCE = 0.12f;
    private static final float HOLD = 0.5f;

    private static final Color HERO_TRAIL = new Color(1f, 0.86f, 0.4f, 1f);
    private static final Color MONSTER_TRAIL = new Color(0.82f, 0.5f, 1f, 1f);
    private static final Color SIX_GLOW = new Color(1f, 0.78f, 0.2f, 1f);

    private final Skin skin;
    private final DiceRoll roll;
    private final String style;
    private final Color trail;
    private final DieActor[] dice;
    private float totalX;
    private float totalY;
    /** 총합 글자를 기준점의 어느 쪽에 붙일지(-1 왼쪽으로, 0 가운데, +1 오른쪽으로). */
    private int totalSide;
    private float timeScale = 1f;
    private float time;
    private boolean totalShown;
    private boolean winner;
    private boolean finishing;

    /**
     * @param style  {@code hero}, {@code monster}, {@code gold}
     * @param fromX  던지는 위치(손)
     * @param landX  주사위 무더기의 가운데
     * @param landY  바닥(주사위 아랫면) 높이
     * @param dir    날아가는 방향(+1 오른쪽, -1 왼쪽) — 회전 방향에 쓴다
     */
    public DiceThrow(Skin skin, DiceRoll roll, String style, float fromX, float fromY,
                     float landX, float landY, float dir) {
        this.skin = skin;
        this.roll = roll;
        this.style = style;
        this.trail = "monster".equals(style) ? MONSTER_TRAIL : HERO_TRAIL;
        setTouchable(Touchable.disabled);

        int n = Math.max(1, Math.min(MAX_SHOWN, roll.count));
        int cols = Math.min(3, n);
        int rows = (n + cols - 1) / cols;
        dice = new DieActor[n];
        for (int i = 0; i < n; i++) {
            int row = i / cols;
            int inRow = Math.min(cols, n - row * cols);
            int col = i % cols;
            float tx = landX + (col - (inRow - 1) / 2f) * SPACING + MathUtils.random(-4f, 4f);
            float ty = landY + row * (SPACING - 14f);
            DieActor d = new DieActor(roll.face(i), fromX + MathUtils.random(-12f, 12f),
                fromY + MathUtils.random(-10f, 10f), tx, ty, dir, i * STAGGER);
            dice[i] = d;
            addActor(d);
        }
        // 총합 숫자 기본 위치는 맨 윗줄 위({@link #setTotalAnchor}로 바꿀 수 있다)
        this.totalX = landX;
        this.totalY = landY + (rows - 1) * (SPACING - 14f) + SIZE + 6f;
    }

    /** 총합 숫자를 (x, y) 기준으로 띄운다. side -1 = 기준점 왼쪽에 오른쪽 맞춤, +1 = 오른쪽에 왼쪽 맞춤. */
    public void setTotalAnchor(float x, float y, int side) {
        totalX = x;
        totalY = y;
        totalSide = side;
    }

    public void setTimeScale(float timeScale) {
        this.timeScale = Math.max(0.1f, timeScale);
    }

    /** 이긴 쪽이면 총합을 금색으로 크게 보여 준다. */
    public void setWinner(boolean winner) {
        this.winner = winner;
    }

    /** 다음 굴림이 시작되면 이전 연출을 빨리 정리한다. */
    public void finishQuickly() {
        if (finishing) return;
        finishing = true;
        clearActions();
        addAction(Actions.sequence(Actions.fadeOut(0.08f), Actions.removeActor()));
    }

    /** 첫 주사위를 던진 뒤 모든 주사위가 멈출 때까지 걸리는 시간(배속 1 기준, 초). */
    public static float landTime(int diceCount) {
        int n = Math.max(1, Math.min(MAX_SHOWN, diceCount));
        return (n - 1) * STAGGER + FLIGHT + BOUNCE * 0.5f;
    }

    @Override
    public void act(float delta) {
        float d = delta * timeScale;
        super.act(d);
        time += d;
        if (!totalShown && time >= landTime(roll.count) + 0.04f) {
            totalShown = true;
            showTotal();
            addAction(Actions.sequence(Actions.delay(HOLD), Actions.fadeOut(0.22f), Actions.removeActor()));
        }
    }

    private void showTotal() {
        Label total = new Label(String.valueOf(roll.total()), skin, "field-large");
        total.setColor(winner ? SIX_GLOW : Color.WHITE);
        Container<Label> box = new Container<Label>(total);
        box.setTransform(true);
        box.pack();
        box.setOrigin(totalSide < 0 ? Align.right : totalSide > 0 ? Align.left : Align.center);
        float bx = totalSide < 0 ? totalX - box.getWidth() : totalSide > 0 ? totalX : totalX - box.getWidth() / 2f;
        box.setPosition(bx, totalY - box.getHeight() / 2f);
        box.setScale(0.2f);
        float big = winner ? 1.45f : 1.05f;
        box.addAction(Actions.sequence(
            Actions.scaleTo(big * 1.25f, big * 1.25f, 0.09f, Interpolation.pow2Out),
            Actions.scaleTo(big, big, 0.12f, Interpolation.swingOut)));
        addActor(box);
        if (winner) burst(bx + box.getWidth() / 2f, totalY, 10, SIX_GLOW, 150f);

        StringBuilder tag = new StringBuilder();
        if (roll.count > dice.length) tag.append("+").append(roll.count - dice.length).append("D ");
        if (roll.multiplier > 1) tag.append("x").append(roll.multiplier);
        if (tag.length() > 0) {
            Label extra = new Label(tag.toString().trim(), skin, "field-small");
            extra.setColor(roll.multiplier > 1 ? SIX_GLOW : Color.WHITE);
            extra.pack();
            // 총합의 바깥쪽 옆, 아래로 살짝
            float ex = totalSide < 0 ? bx + box.getWidth() - box.getWidth() * big - extra.getWidth() - 4f
                : totalSide > 0 ? bx + box.getWidth() * big + 4f
                : bx + box.getWidth() / 2f + box.getWidth() * big / 2f + 4f;
            extra.setPosition(ex, totalY - box.getHeight() * big / 2f);
            extra.getColor().a = 0f;
            extra.addAction(Actions.fadeIn(0.15f));
            addActor(extra);
        }
    }

    /** 반짝이 여러 개를 사방으로 뿌린다. */
    void burst(float x, float y, int count, Color color, float speed) {
        for (int i = 0; i < count; i++) {
            float a = MathUtils.random(MathUtils.PI2);
            float v = speed * MathUtils.random(0.5f, 1f);
            addActor(new Spark(x, y, MathUtils.cos(a) * v, MathUtils.sin(a) * v + 40f,
                MathUtils.random(0.35f, 0.6f), color, MathUtils.random(2f, 3.5f)));
        }
    }

    /** 원형 빛이 퍼지며 사라진다. */
    void ring(float x, float y, Color color, float size) {
        addActor(new Ring(x, y, color, size));
    }

    // ------------------------------------------------------------------

    /** 주사위 한 개. 대기 → 비행(포물선·회전·눈 바뀜·꼬리) → 튕김 → 정지. */
    private final class DieActor extends Actor {
        final int face;
        final float x0, y0, x1, y1;
        final float peak;
        final float spin;
        final float delay;
        float t;
        int shownFace;
        float faceTimer;
        float trailTimer;
        boolean landed;
        float landedTime;
        float rotation;

        DieActor(int face, float x0, float y0, float x1, float y1, float dir, float delay) {
            this.face = face;
            this.x0 = x0;
            this.y0 = y0;
            this.x1 = x1;
            this.y1 = y1;
            this.delay = delay;
            this.peak = MathUtils.random(110f, 150f);
            this.spin = -Math.signum(dir == 0 ? 1f : dir) * MathUtils.random(540f, 900f);
            this.shownFace = MathUtils.random(1, 6);
            setSize(SIZE, SIZE);
        }

        @Override
        public void act(float delta) {
            super.act(delta);
            t += delta;
            float ft = t - delay;
            if (ft < 0f) return;
            if (ft < FLIGHT) {
                float p = ft / FLIGHT;
                float e = Interpolation.pow2Out.apply(p);
                float x = MathUtils.lerp(x0, x1, e);
                float y = MathUtils.lerp(y0, y1, p) + peak * 4f * p * (1f - p);
                setPosition(x - getWidth() / 2f, y);
                rotation = spin * Interpolation.pow2Out.apply(p);
                faceTimer -= delta;
                if (faceTimer <= 0f) {
                    faceTimer = 0.045f;
                    shownFace = shownFace % 6 + 1 + MathUtils.random(0, 2);
                    if (shownFace > 6) shownFace -= 6;
                }
                trailTimer -= delta;
                if (trailTimer <= 0f) {
                    trailTimer = 0.018f;
                    DiceThrow.this.addActorBefore(this, new Spark(x + MathUtils.random(-8f, 8f),
                        y + getHeight() / 2f + MathUtils.random(-8f, 8f),
                        MathUtils.random(-30f, 30f), MathUtils.random(-10f, 40f),
                        MathUtils.random(0.22f, 0.4f), trail, MathUtils.random(1.5f, 2.5f)));
                }
                return;
            }
            if (!landed) {
                landed = true;
                landedTime = ft;
                shownFace = face;
                rotation = 0f; // 반듯하게 내려앉는다
                float cx = x1;
                float cy = y1 + getHeight() / 2f;
                if (face == 6) {
                    ring(cx, cy, SIX_GLOW, 150f);
                    burst(cx, cy, 14, SIX_GLOW, 220f);
                } else {
                    burst(cx, y1 + 4f, 5, trail, 90f);
                }
            }
            // 작은 튕김
            float bt = ft - FLIGHT;
            float hop = bt < BOUNCE ? 18f * 4f * (bt / BOUNCE) * (1f - bt / BOUNCE) : 0f;
            setPosition(x1 - getWidth() / 2f, y1 + hop);
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            float ft = t - delay;
            if (ft < 0f) return;
            Color c = getColor();
            float alpha = c.a * parentAlpha;
            float w = getWidth();
            float h = getHeight();
            float x = getX();
            float y = getY();

            // 바닥 그림자: 높이 뜰수록 작고 옅다
            TextureRegion shadow = skin.getRegion("die-shadow");
            float height = Math.max(0f, y - y1);
            float k = MathUtils.clamp(1f - height / 220f, 0.35f, 1f);
            float groundY = landed ? y1 : MathUtils.lerp(y0, y1, MathUtils.clamp(ft / FLIGHT, 0f, 1f));
            batch.setColor(1f, 1f, 1f, alpha * k);
            batch.draw(shadow, x + w / 2f - 24f * k, groundY - 8f, 48f * k, 18f * k);

            // 6이 나오면 뒤에서 금빛이 맥박친다
            if (landed && face == 6) {
                float pulse = 0.75f + 0.25f * MathUtils.sin((ft - landedTime) * 14f);
                TextureRegion glow = skin.getRegion("glow");
                float gs = w * 2.1f * pulse;
                batch.setColor(SIX_GLOW.r, SIX_GLOW.g, SIX_GLOW.b, alpha * 0.85f);
                batch.draw(glow, x + w / 2f - gs / 2f, y + h / 2f - gs / 2f, gs, gs);
            }

            // 착지 순간 납작해졌다 돌아온다
            float sx = 1f, sy = 1f;
            if (landed) {
                float bt = ft - FLIGHT;
                if (bt < 0.08f) {
                    float q = 1f - bt / 0.08f;
                    sx = 1f + 0.3f * q;
                    sy = 1f - 0.3f * q;
                } else if (bt < BOUNCE + 0.06f && bt > BOUNCE) {
                    float q = 1f - (bt - BOUNCE) / 0.06f;
                    sx = 1f + 0.12f * q;
                    sy = 1f - 0.12f * q;
                }
            }
            TextureRegion region = skin.getRegion("die-" + style + "-" + shownFace);
            batch.setColor(1f, 1f, 1f, alpha);
            batch.draw(region, x, y, w / 2f, 0f, w, h, sx, sy, rotation);

            // 착지 직후 흰 번쩍임
            if (landed && ft - landedTime < 0.1f) {
                float f = 1f - (ft - landedTime) / 0.1f;
                TextureRegion glow = skin.getRegion("glow");
                batch.setColor(1f, 1f, 1f, alpha * 0.8f * f);
                batch.draw(glow, x - w * 0.25f, y - h * 0.25f, w * 1.5f, h * 1.5f);
            }
            batch.setColor(Color.WHITE);
        }
    }

    /** 반짝이 입자: 날아가며 중력에 떨어지고, 돌며 작아지고 옅어진다. */
    private final class Spark extends Actor {
        float vx, vy;
        final float life;
        final float size;
        float age;
        final Color tint;

        Spark(float x, float y, float vx, float vy, float life, Color tint, float size) {
            setPosition(x, y);
            this.vx = vx;
            this.vy = vy;
            this.life = life;
            this.tint = tint;
            this.size = size;
        }

        @Override
        public void act(float delta) {
            age += delta;
            if (age >= life) {
                remove();
                return;
            }
            vy -= 420f * delta;
            moveBy(vx * delta, vy * delta);
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            float p = age / life;
            float s = 7f * size * (1f - 0.6f * p);
            batch.setColor(tint.r, tint.g, tint.b, getColor().a * parentAlpha * (1f - p * p));
            batch.draw(skin.getRegion("sparkle"), getX() - s / 2f, getY() - s / 2f, s / 2f, s / 2f, s, s,
                1f, 1f, p * 180f);
            batch.setColor(Color.WHITE);
        }
    }

    /** 퍼지는 빛 고리. */
    private final class Ring extends Actor {
        final Color tint;
        final float max;
        float age;

        Ring(float x, float y, Color tint, float max) {
            setPosition(x, y);
            this.tint = tint;
            this.max = max;
        }

        @Override
        public void act(float delta) {
            age += delta;
            if (age > 0.35f) remove();
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            float p = age / 0.35f;
            float s = max * Interpolation.pow2Out.apply(p);
            batch.setColor(tint.r, tint.g, tint.b, getColor().a * parentAlpha * (1f - p));
            batch.draw(skin.getRegion("glow"), getX() - s / 2f, getY() - s / 2f, s, s);
            batch.setColor(Color.WHITE);
        }
    }
}
