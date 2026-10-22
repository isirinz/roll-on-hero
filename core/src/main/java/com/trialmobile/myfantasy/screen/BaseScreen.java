package com.trialmobile.myfantasy.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.trialmobile.myfantasy.GameConfig;
import com.trialmobile.myfantasy.MainGame;
import com.trialmobile.myfantasy.ui.Palette;

/**
 * 모든 화면의 공통 기반.
 * <p>
 * ExtendViewport(720x1280 최소)를 쓰므로, 기기 화면비가 더 길면(예: 19.5:9) 위아래 공간이 늘어나고
 * 레터박스 없이 꽉 찬다. 레이아웃은 {@link #root} Table에 배치하면 화면 크기에 맞춰 자동 정렬된다.
 */
public abstract class BaseScreen extends ScreenAdapter {
    protected final MainGame game;
    protected final Skin skin;
    protected final Stage stage;
    protected final Table root;

    protected BaseScreen(MainGame game) {
        this.game = game;
        this.skin = game.getSkin();
        this.stage = new Stage(new ExtendViewport(GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT), game.getBatch());
        this.root = new Table();
        root.setFillParent(true);
        stage.addActor(root);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        root.getColor().a = 0f;
        root.addAction(Actions.fadeIn(0.25f));
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(Palette.BACKGROUND);
        update(delta);
        stage.getViewport().apply();
        stage.act(delta);
        stage.draw();
    }

    /** 화면 전체에 초원 배경 그림을 깐다(타이틀·로딩용). 비율을 유지한 채 화면을 꽉 채운다. */
    protected void addBackdrop() {
        if (!skin.has("battle-bg", TextureRegion.class)) return;
        Image backdrop = new Image(skin.getDrawable("battle-bg"));
        backdrop.setScaling(Scaling.fill);
        backdrop.setFillParent(true);
        stage.getRoot().addActorAt(0, backdrop);
    }

    /** 매 프레임 게임 로직 갱신. 그리기 전에 호출된다. */
    protected void update(float delta) {
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void hide() {
        if (Gdx.input.getInputProcessor() == stage) Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        stage.dispose(); // 공유 batch는 Stage가 소유하지 않으므로 dispose되지 않는다.
    }
}
