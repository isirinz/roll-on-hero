package com.trialmobile.myfantasy.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ObjectFloatMap;
import com.badlogic.gdx.utils.ObjectLongMap;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * 효과음. {@code assets/sfx/<이름>.wav}(tools/sfx/make_sfx.py로 합성)를 불러 짧게 재생한다.
 * <p>
 * 어디서든 {@code Sfx.play(Sfx.COIN)}처럼 부르면 된다(MainGame이 만들고 정리한다. 없거나 꺼져 있으면 아무 일도 없다).
 * 같은 소리가 아주 짧은 간격으로 겹치면(배속 3배 등) 한 번만 낸다. 켜기/끄기는 설정(Preferences)에 저장된다.
 */
public final class Sfx {
    public static final String DICE_SHAKE = "dice_shake";
    public static final String DICE_LAND = "dice_land";
    public static final String DICE_SIX = "dice_six";
    public static final String TOTAL_WIN = "total_win";
    public static final String HIT = "hit";
    public static final String HURT = "hurt";
    public static final String MONSTER_DIE = "monster_die";
    public static final String HERO_DOWN = "hero_down";
    public static final String LEVEL_UP = "level_up";
    public static final String COIN = "coin";
    public static final String STAGE_CLEAR = "stage_clear";
    public static final String BOSS_APPEAR = "boss_appear";
    public static final String SPELL_BOLT = "spell_bolt";
    public static final String SPELL_HEAL = "spell_heal";
    public static final String SPELL_VORPAL = "spell_vorpal";
    public static final String SPELL_FIRE = "spell_fire";
    public static final String SPELL_HELLBOMB = "spell_hellbomb";
    public static final String UI_CLICK = "ui_click";
    public static final String UI_TAB = "ui_tab";
    public static final String UI_BUY = "ui_buy";
    public static final String UI_UPGRADE = "ui_upgrade";
    public static final String BOOST_ON = "boost_on";
    public static final String REBIRTH = "rebirth";

    /** 이름, 기본 음량(소리끼리 크기 맞춤). */
    private static final Object[][] MIX = {
        {DICE_SHAKE, 0.45f}, {DICE_LAND, 0.55f}, {DICE_SIX, 0.45f}, {TOTAL_WIN, 0.4f},
        {HIT, 0.75f}, {HURT, 0.7f}, {MONSTER_DIE, 0.6f}, {HERO_DOWN, 0.6f},
        {LEVEL_UP, 0.6f}, {COIN, 0.4f}, {STAGE_CLEAR, 0.65f}, {BOSS_APPEAR, 0.8f},
        {SPELL_BOLT, 0.55f}, {SPELL_HEAL, 0.5f}, {SPELL_VORPAL, 0.55f}, {SPELL_FIRE, 0.65f}, {SPELL_HELLBOMB, 0.8f},
        {UI_CLICK, 0.45f}, {UI_TAB, 0.5f}, {UI_BUY, 0.55f}, {UI_UPGRADE, 0.55f},
        {BOOST_ON, 0.7f}, {REBIRTH, 0.8f},
    };
    /** 같은 소리를 다시 내기까지 최소 간격(밀리초). */
    private static final long MIN_GAP_MS = 45L;
    private static final String PREFS_NAME = "com.trialmobile.myfantasy.settings";
    private static final String KEY_ON = "sfx.on";

    private static Sfx instance;

    private final ObjectMap<String, Sound> sounds = new ObjectMap<String, Sound>();
    private final ObjectFloatMap<String> volumes = new ObjectFloatMap<String>();
    private final ObjectLongMap<String> lastPlayed = new ObjectLongMap<String>();
    private boolean enabled = true;

    private Sfx() {
    }

    /** 효과음을 모두 불러온다(MainGame.create). 파일이 없거나 오디오가 없으면 그 소리만 건너뛴다. */
    public static void load() {
        Sfx s = new Sfx();
        try {
            s.enabled = prefs().getBoolean(KEY_ON, true);
        } catch (RuntimeException e) {
            s.enabled = true;
        }
        for (Object[] m : MIX) {
            String name = (String) m[0];
            s.volumes.put(name, (Float) m[1]);
            if (Gdx.audio == null) continue;
            FileHandle f = Gdx.files.internal("sfx/" + name + ".wav");
            if (!f.exists()) continue;
            try {
                s.sounds.put(name, Gdx.audio.newSound(f));
            } catch (RuntimeException e) {
                Gdx.app.log("Sfx", "불러오기 실패: " + name + " " + e.getMessage());
            }
        }
        instance = s;
    }

    public static void dispose() {
        if (instance == null) return;
        for (Sound sound : instance.sounds.values()) sound.dispose();
        instance.sounds.clear();
        instance = null;
    }

    /** 기본 음량, 음높이를 살짝 흔들어(같은 소리가 반복돼도 덜 질리게) 재생한다. */
    public static void play(String name) {
        play(name, 1f, MathUtils.random(0.95f, 1.05f));
    }

    /** @param volume 기본 음량에 곱하는 값, @param pitch 0.5~2 */
    public static void play(String name, float volume, float pitch) {
        Sfx s = instance;
        if (s == null || !s.enabled) return;
        Sound sound = s.sounds.get(name);
        if (sound == null) return;
        long now = System.currentTimeMillis();
        if (now - s.lastPlayed.get(name, 0L) < MIN_GAP_MS) return;
        s.lastPlayed.put(name, now);
        try {
            sound.play(s.volumes.get(name, 0.6f) * volume, pitch, 0f);
        } catch (RuntimeException e) {
            // 오디오 장치 문제로 소리를 못 내도 게임은 계속한다
        }
    }

    public static boolean isEnabled() {
        return instance != null && instance.enabled;
    }

    public static void setEnabled(boolean on) {
        if (instance == null) return;
        instance.enabled = on;
        try {
            Preferences p = prefs();
            p.putBoolean(KEY_ON, on);
            p.flush();
        } catch (RuntimeException e) {
            // 설정 저장 실패는 무시(다음 실행에 기본값)
        }
    }

    private static Preferences prefs() {
        return Gdx.app.getPreferences(PREFS_NAME);
    }
}
