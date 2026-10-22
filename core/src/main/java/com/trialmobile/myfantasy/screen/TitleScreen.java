package com.trialmobile.myfantasy.screen;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.trialmobile.myfantasy.MainGame;
import com.trialmobile.myfantasy.i18n.Lang;

/** 타이틀 화면. 아무 곳이나 터치하면 전투 화면으로 넘어간다. */
public class TitleScreen extends BaseScreen {
    private boolean leaving;

    public TitleScreen(MainGame game) {
        super(game);

        Label title = new Label(Lang.get("app.title"), skin, "title");
        addBackdrop();
        Label subtitle = new Label(Lang.get("app.subtitle"), skin, "field-large");
        Label hint = new Label(Lang.get("title.tap"), skin, "field");
        hint.setAlignment(Align.center);
        hint.addAction(Actions.forever(Actions.sequence(
            Actions.alpha(0.25f, 0.8f),
            Actions.alpha(1f, 0.8f))));

        root.add(title).padBottom(24f).row();
        root.add(subtitle).padBottom(360f).row();
        root.add(hint).row();
        root.add(new Label("v1.0.0", skin, "field-small")).padTop(48f);

        stage.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                start();
            }
        });
    }

    private void start() {
        if (leaving) return;
        leaving = true;
        root.addAction(Actions.sequence(
            Actions.fadeOut(0.2f),
            Actions.run(new Runnable() {
                @Override
                public void run() {
                    game.changeScreen(new BattleScreen(game));
                }
            })));
    }
}
