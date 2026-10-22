package com.trialmobile.myfantasy.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.trialmobile.myfantasy.audio.Sfx;
import com.trialmobile.myfantasy.i18n.Lang;

/** 예/아니오 확인 팝업. 화면을 어둡게 덮고, 확인을 누르면 {@code onConfirm}을 실행한 뒤 닫힌다. */
public final class ConfirmDialog extends Table {

    public ConfirmDialog(Skin skin, String title, String message, String confirmText, String confirmStyle,
                         final Runnable onConfirm) {
        setFillParent(true);
        setTouchable(Touchable.enabled);
        setBackground(skin.newDrawable("white", Palette.OVERLAY));

        Table panel = new Table();
        panel.setBackground(skin.getDrawable("panel"));
        panel.pad(44f, 48f, 40f, 48f);
        panel.add(new Label(title, skin, "gold")).padBottom(18f).row();
        Label text = new Label(message, skin, "default");
        text.setWrap(true);
        text.setAlignment(Align.center);
        panel.add(text).width(500f).padBottom(32f).row();

        TextButton cancel = new TextButton(Lang.get("btn.cancel"), skin, "blue");
        cancel.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Sfx.play(Sfx.UI_CLICK);
                close();
            }
        });
        TextButton ok = new TextButton(confirmText, skin, confirmStyle);
        ok.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                close();
                onConfirm.run();
            }
        });
        Table buttons = new Table();
        buttons.add(cancel).size(220f, 80f).padRight(20f);
        buttons.add(ok).size(220f, 80f);
        panel.add(buttons);
        add(panel).width(600f);
    }

    public void show(Stage stage) {
        getColor().a = 0f;
        stage.addActor(this);
        addAction(Actions.fadeIn(0.2f));
    }

    private void close() {
        setTouchable(Touchable.disabled);
        addAction(Actions.sequence(Actions.fadeOut(0.15f), Actions.removeActor()));
    }
}
