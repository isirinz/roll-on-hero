package com.trialmobile.myfantasy;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.trialmobile.myfantasy.ads.RewardedAds;
import com.trialmobile.myfantasy.anim.SpriteLibrary;
import com.trialmobile.myfantasy.assets.Fonts;
import com.trialmobile.myfantasy.battle.OfflineReport;
import com.trialmobile.myfantasy.i18n.Lang;
import com.trialmobile.myfantasy.battle.PlayerState;
import com.trialmobile.myfantasy.save.SaveData;
import com.trialmobile.myfantasy.save.SaveManager;
import com.trialmobile.myfantasy.save.SaveMapper;
import com.trialmobile.myfantasy.screen.LoadingScreen;
import com.trialmobile.myfantasy.ui.UiSkin;

/**
 * 모든 플랫폼이 공유하는 진입점.
 * 공용 리소스(SpriteBatch, AssetManager, 폰트, Skin)와 플레이어 상태를 소유하고, 화면 전환과 저장을 담당한다.
 * <p>
 * 저장 시점: {@link #AUTOSAVE_INTERVAL}초마다, 앱이 백그라운드로 갈 때(pause), 종료할 때(dispose),
 * 그리고 구매·강화처럼 중요한 행동 직후({@link #save()} 호출).
 * 앱을 다시 켜거나 백그라운드에서 돌아오면, 비운 시간만큼 오프라인 보상 시간이 쌓인다({@link #consumeOfflineSeconds()}).
 */
public class MainGame extends Game {
    private SpriteBatch batch;
    private AssetManager assets;
    private Fonts fonts;
    private Skin skin;
    private PlayerState playerState;
    private SaveManager saveManager;
    private SpriteLibrary sprites;
    private RewardedAds rewardedAds = RewardedAds.NONE;

    /** 자동 저장 간격(초). */
    public static final float AUTOSAVE_INTERVAL = 30f;
    private float autosaveTimer;
    /** 아직 화면에 반영하지 않은 오프라인 시간(초). */
    private double pendingOfflineSeconds;
    /** pause된 시각(epoch 밀리초). 0이면 pause 상태가 아님. */
    private long pausedAt;

    /** 플랫폼 런처가 실제 보상형 광고 구현을 넣는다(없으면 광고 없이 바로 보상). */
    public void setRewardedAds(RewardedAds ads) {
        this.rewardedAds = ads == null ? RewardedAds.NONE : ads;
    }

    public RewardedAds getRewardedAds() {
        return rewardedAds;
    }

    @Override
    public void create() {
        batch = new SpriteBatch();
        assets = new AssetManager();

        Lang.init(); // 화면 글자 언어(저장된 설정 → 기기 언어)
        fonts = new Fonts();
        fonts.load(computeRenderScale(), useCondensedFont());
        skin = UiSkin.create(fonts);
        sprites = new SpriteLibrary();
        sprites.load();
        com.trialmobile.myfantasy.audio.Sfx.load();
        loadGame();

        setScreen(new LoadingScreen(this));
    }

    private void loadGame() {
        saveManager = new SaveManager();
        SaveData data = saveManager.load();
        if (data == null) {
            playerState = PlayerState.newGame();
            return;
        }
        playerState = SaveMapper.fromData(data);
        addOfflineTime(System.currentTimeMillis() - data.savedAt);
    }

    /** 현재 상태를 즉시 저장한다. */
    public void save() {
        if (playerState == null || saveManager == null) return;
        saveManager.save(SaveMapper.toData(playerState, System.currentTimeMillis()));
        autosaveTimer = 0f;
    }

    private void addOfflineTime(long awayMillis) {
        if (awayMillis <= 0) return; // 시계가 뒤로 간 경우 무시
        // 최대 인정 시간은 진행도(모든 생 최고 챕터)에 따라 2~12시간
        double cap = playerState != null ? playerState.offlineCapSeconds() : OfflineReport.MAX_SECONDS;
        pendingOfflineSeconds = OfflineReport.clampSeconds(pendingOfflineSeconds + awayMillis / 1000.0, cap);
    }

    /**
     * 보상으로 처리할 오프라인 시간을 꺼내고 0으로 비운다. 전투 화면이 호출한다.
     * @return 초 (보상할 것이 없으면 0)
     */
    public double consumeOfflineSeconds() {
        double s = pendingOfflineSeconds;
        pendingOfflineSeconds = 0.0;
        return s;
    }

    @Override
    public void render() {
        super.render();
        autosaveTimer += Gdx.graphics.getDeltaTime();
        if (autosaveTimer >= AUTOSAVE_INTERVAL) save();
    }

    @Override
    public void pause() {
        super.pause();
        save();
        pausedAt = System.currentTimeMillis();
    }

    @Override
    public void resume() {
        super.resume();
        if (pausedAt > 0) {
            addOfflineTime(System.currentTimeMillis() - pausedAt);
            pausedAt = 0;
        }
    }

    /**
     * 가상 해상도 1 단위가 실제 화면에서 몇 픽셀인지 계산한다.
     * 폰트를 이 배율로 생성하면 고해상도 기기에서도 글자가 흐려지지 않는다.
     */
    private float computeRenderScale() {
        float sx = Gdx.graphics.getBackBufferWidth() / GameConfig.WORLD_WIDTH;
        float sy = Gdx.graphics.getBackBufferHeight() / GameConfig.WORLD_HEIGHT;
        return MathUtils.clamp(Math.min(sx, sy), 0.5f, 3f);
    }

    /**
     * 현재 화면을 교체하고 이전 화면을 정리한다.
     * 화면의 render/act 도중에 호출되어도 안전하도록 실제 교체는 현재 프레임이 끝난 뒤에 수행한다.
     */
    public void changeScreen(final Screen next) {
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                Screen old = getScreen();
                setScreen(next);
                if (old != null) old.dispose();
            }
        });
    }

    /** 영어는 라틴 글자가 넓어서 좁은 글꼴을 쓴다. */
    private static boolean useCondensedFont() {
        return !"ko".equals(Lang.language());
    }

    /**
     * 언어를 바꾼 뒤: 그 언어에 맞는 글꼴로 스킨을 새로 만들고 전투 화면을 다시 연다.
     * 이전 화면이 정리된 다음에 이전 글꼴·스킨을 버린다.
     */
    public void reloadForLanguage() {
        save();
        final Fonts oldFonts = fonts;
        final Skin oldSkin = skin;
        fonts = new Fonts();
        fonts.load(computeRenderScale(), useCondensedFont());
        skin = UiSkin.create(fonts);
        final Screen next = new com.trialmobile.myfantasy.screen.BattleScreen(this);
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                Screen old = getScreen();
                setScreen(next);
                if (old != null) old.dispose();
                oldSkin.dispose();
                oldFonts.dispose();
            }
        });
    }

    public SpriteBatch getBatch() {
        return batch;
    }

    public AssetManager getAssets() {
        return assets;
    }

    public Fonts getFonts() {
        return fonts;
    }

    public Skin getSkin() {
        return skin;
    }

    /** 캐릭터 애니메이션·이펙트. */
    public SpriteLibrary getSprites() {
        return sprites;
    }

    public PlayerState getPlayerState() {
        return playerState;
    }

    /**
     * 환생: 새 상태로 바꾸고 저장한 뒤 전투 화면을 새로 연다.
     * @return 환생했으면 true(조건 미달이면 아무 일도 없다)
     */
    public boolean rebirth() {
        PlayerState next = com.trialmobile.myfantasy.battle.Rebirth.reborn(playerState);
        if (next == null) return false;
        playerState = next;
        com.trialmobile.myfantasy.audio.Sfx.play(com.trialmobile.myfantasy.audio.Sfx.REBIRTH, 1f, 1f);
        save();
        changeScreen(new com.trialmobile.myfantasy.screen.BattleScreen(this));
        return true;
    }

    @Override
    public void dispose() {
        save();
        Screen current = getScreen();
        super.dispose(); // current.hide()
        if (current != null) current.dispose();
        skin.dispose();
        sprites.dispose();
        com.trialmobile.myfantasy.audio.Sfx.dispose();
        fonts.dispose();
        assets.dispose();
        batch.dispose();
    }
}
