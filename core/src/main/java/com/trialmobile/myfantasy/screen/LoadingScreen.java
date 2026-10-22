package com.trialmobile.myfantasy.screen;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.trialmobile.myfantasy.MainGame;
import com.trialmobile.myfantasy.i18n.Lang;

/**
 * 에셋 로딩 화면.
 * 텍스처/사운드 등은 {@link #queueAssets(AssetManager)}에 추가하면 진행률 막대에 반영된다.
 */
public class LoadingScreen extends BaseScreen {
    /** 로딩이 즉시 끝나도 화면이 깜빡이지 않도록 최소 표시 시간(초). */
    private static final float MIN_DURATION = 0.8f;

    private final ProgressBar bar;
    private final Label percentLabel;
    private float elapsed;
    private boolean finished;

    public LoadingScreen(MainGame game) {
        super(game);

        queueAssets(game.getAssets());

        bar = new ProgressBar(0f, 1f, 0.001f, false, skin, "loading");
        bar.setAnimateDuration(0.1f);
        percentLabel = new Label(Lang.format("loading", 0), skin, "field-small");

        addBackdrop();
        root.add(new Label(Lang.get("app.title"), skin, "title")).padBottom(16f).row();
        root.add(new Label(Lang.get("app.subtitle"), skin, "field")).padBottom(120f).row();
        root.add(bar).width(480f).padBottom(16f).row();
        root.add(percentLabel);
    }

    private void queueAssets(AssetManager assets) {
        // 예: assets.load("atlas/game.atlas", TextureAtlas.class);
    }

    @Override
    protected void update(float delta) {
        if (finished) return;
        elapsed += delta;

        boolean loaded = game.getAssets().update(16);
        float progress = Math.min(game.getAssets().getProgress(), elapsed / MIN_DURATION);
        bar.setValue(progress);
        percentLabel.setText(Lang.format("loading", Math.round(progress * 100f)));

        if (loaded && elapsed >= MIN_DURATION) {
            finished = true;
            game.changeScreen(new TitleScreen(game));
        }
    }
}
