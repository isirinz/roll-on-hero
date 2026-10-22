package com.trialmobile.myfantasy.anim;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * 캐릭터 애니메이션과 이펙트를 불러와 보관한다.
 * <p>
 * 파일 규칙 (tools/sprites/convert.py가 만든다):
 * <ul>
 *   <li>{@code sprites/index.json}: 캐릭터별 프레임 크기, 확대 배율, 동작별 프레임 수·프레임 시간·반복 여부</li>
 *   <li>{@code sprites/<id>/<동작>.png}: 같은 크기 프레임을 가로로 이어 붙인 띠. 발이 같은 높이에 오도록 정렬돼 있다</li>
 *   <li>{@code fx/index.json}, {@code fx/<이름>.png}: 이펙트(한 번 재생)</li>
 * </ul>
 * 파일이 없는 캐릭터·이펙트는 조용히 건너뛴다(화면은 자리 표시로 대신한다).
 */
public final class SpriteLibrary implements Disposable {
    private static final String TAG = "SpriteLibrary";

    /** 캐릭터 한 명의 동작 묶음. */
    public static final class CharacterAnims {
        public final String id;
        public final int frameWidth;
        public final int frameHeight;
        /** 화면에 그릴 정수 배율. */
        public final float scale;
        /** 원본이 오른쪽을 보고 있으면 true. */
        public final boolean facesRight;
        /** 대기 자세의 발이 칸 바닥보다 몇 픽셀(원본) 위에 있는지. 그릴 때 이만큼 내려 발을 땅에 붙인다. */
        public final int footY;
        /** 공격 시작부터 무기가 닿는 순간까지(초, 배속 1). index.json의 impact(원본 동작 길이 대비 비율)로 계산. */
        float attackImpactSeconds;
        final ObjectMap<String, Animation<TextureRegion>> anims = new ObjectMap<String, Animation<TextureRegion>>();

        CharacterAnims(String id, int frameWidth, int frameHeight, float scale, boolean facesRight, int footY) {
            this.id = id;
            this.footY = footY;
            this.frameWidth = frameWidth;
            this.frameHeight = frameHeight;
            this.scale = scale;
            this.facesRight = facesRight;
        }

        public Animation<TextureRegion> get(String anim) {
            return anims.get(anim);
        }

        /** 공격 시작부터 타격 순간까지(배속 1 기준, 초). 공격 그림이 없으면 0. */
        public float attackImpactSeconds() {
            return attackImpactSeconds;
        }

        public boolean has(String anim) {
            return anims.containsKey(anim);
        }
    }

    /** 이펙트 하나. */
    public static final class Effect {
        public final String name;
        public final Animation<TextureRegion> animation;
        public final int frameWidth;
        public final int frameHeight;
        public final float scale;
        /** 기준점(0~1, 왼쪽 아래 기준). 이펙트를 놓을 위치가 이 점에 온다. */
        public final float pivotX;
        public final float pivotY;

        Effect(String name, Animation<TextureRegion> animation, int frameWidth, int frameHeight, float scale,
               float pivotX, float pivotY) {
            this.name = name;
            this.animation = animation;
            this.frameWidth = frameWidth;
            this.frameHeight = frameHeight;
            this.scale = scale;
            this.pivotX = pivotX;
            this.pivotY = pivotY;
        }
    }

    private final ObjectMap<String, CharacterAnims> characters = new ObjectMap<String, CharacterAnims>();
    private final ObjectMap<String, Effect> effects = new ObjectMap<String, Effect>();
    private final Array<Texture> textures = new Array<Texture>();

    public void load() {
        loadCharacters();
        loadEffects();
        Gdx.app.log(TAG, "캐릭터 " + characters.size + "종, 이펙트 " + effects.size + "종");
    }

    public CharacterAnims character(String id) {
        return id == null ? null : characters.get(id);
    }

    public Effect effect(String name) {
        return effects.get(name);
    }

    private void loadCharacters() {
        FileHandle index = Gdx.files.internal("sprites/index.json");
        if (!index.exists()) return;
        JsonValue root = new JsonReader().parse(index);
        for (JsonValue c = root.child; c != null; c = c.next) {
            CharacterAnims ch = new CharacterAnims(c.name, c.getInt("frameWidth"), c.getInt("frameHeight"),
                c.getFloat("scale", 3f), c.getBoolean("facesRight", true), c.getInt("footY", 0));
            JsonValue anims = c.get("anims");
            for (JsonValue a = anims == null ? null : anims.child; a != null; a = a.next) {
                Animation<TextureRegion> anim = strip("sprites/" + c.name + "/" + a.name + ".png",
                    ch.frameWidth, ch.frameHeight, a.getFloat("duration", 0.1f));
                if (anim == null) continue;
                // recover: 마지막 프레임이 대기 자세와 많이 다르면(휘두른 채로 끝남) 몇 프레임을 되감아 제자리로 돌아간 뒤 대기로
                if ("attack".equals(a.name)) ch.attackImpactSeconds = anim.getAnimationDuration() * a.getFloat("impact", 0.5f);
                if (a.getBoolean("recover", false)) anim = withRecovery(anim);
                anim.setPlayMode(a.getBoolean("loop", false) ? Animation.PlayMode.LOOP : Animation.PlayMode.NORMAL);
                ch.anims.put(a.name, anim);
            }
            if (ch.anims.size > 0) characters.put(ch.id, ch);
        }
    }

    private void loadEffects() {
        FileHandle index = Gdx.files.internal("fx/index.json");
        if (!index.exists()) return;
        JsonValue root = new JsonReader().parse(index);
        for (JsonValue e = root.child; e != null; e = e.next) {
            int w = e.getInt("frameWidth");
            int h = e.getInt("frameHeight");
            Animation<TextureRegion> anim = strip("fx/" + e.name + ".png", w, h, e.getFloat("duration", 0.06f));
            if (anim == null) continue;
            effects.put(e.name, new Effect(e.name, anim, w, h, e.getFloat("scale", 2f),
                e.getFloat("pivotX", 0.5f), e.getFloat("pivotY", 0.5f)));
        }
    }

    /** 끝 프레임에서 첫 프레임 쪽으로 2칸씩 되돌아가는 프레임을 붙인다(예: 0..6 → 0..6,4,2,0). */
    private static Animation<TextureRegion> withRecovery(Animation<TextureRegion> anim) {
        // getKeyFrames()는 제네릭 배열이라 Object[]로 받아 하나씩 꺼낸다(TextureRegion[]로 바로 받으면 ClassCastException)
        Object[] keys = anim.getKeyFrames();
        Array<TextureRegion> frames = new Array<TextureRegion>(keys.length + keys.length / 2 + 1);
        for (Object k : keys) frames.add((TextureRegion) k);
        for (int i = keys.length - 3; i >= 0; i -= 2) frames.add((TextureRegion) keys[i]);
        if (frames.peek() != keys[0]) frames.add((TextureRegion) keys[0]);
        return new Animation<TextureRegion>(anim.getFrameDuration(), frames);
    }

    /** 가로 띠 PNG를 프레임으로 자른다. */
    private Animation<TextureRegion> strip(String path, int frameWidth, int frameHeight, float frameDuration) {
        FileHandle file = Gdx.files.internal(path);
        if (!file.exists()) {
            Gdx.app.log(TAG, "없음: " + path);
            return null;
        }
        Texture texture = new Texture(file);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        textures.add(texture);
        int count = Math.max(1, texture.getWidth() / frameWidth);
        Array<TextureRegion> frames = new Array<TextureRegion>(count);
        for (int i = 0; i < count; i++) frames.add(new TextureRegion(texture, i * frameWidth, 0, frameWidth, frameHeight));
        return new Animation<TextureRegion>(frameDuration, frames);
    }

    @Override
    public void dispose() {
        for (Texture t : textures) t.dispose();
        textures.clear();
        characters.clear();
        effects.clear();
    }
}
