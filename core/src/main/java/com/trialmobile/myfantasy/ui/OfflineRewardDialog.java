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
import com.trialmobile.myfantasy.i18n.Lang;
import com.trialmobile.myfantasy.i18n.Names;
import com.trialmobile.myfantasy.battle.OfflineReport;

/** 오프라인 보상 요약 팝업. 화면 전체를 어둡게 덮고, 확인을 누르면 닫힌다. */
public final class OfflineRewardDialog extends Table {

    public OfflineRewardDialog(Skin skin, OfflineReport r) {
        setFillParent(true);
        setTouchable(Touchable.enabled); // 뒤의 버튼이 눌리지 않게 막는다
        setBackground(skin.newDrawable("white", Palette.OVERLAY));

        Table panel = new Table();
        panel.setBackground(skin.getDrawable("panel"));
        panel.pad(44f, 48f, 40f, 48f);
        panel.add(new Label(Lang.get("offline.welcome"), skin, "gold")).padBottom(8f).row();
        panel.add(new Label(Lang.format("offline.result", Names.duration(r.seconds)), skin, "dim")).padBottom(r.capSeconds > 0 ? 6f : 28f).row();
        if (r.capSeconds > 0) {
            // 최대 인정 시간 안내. 한도까지 채웠으면 금색으로 눈에 띄게.
            boolean max = r.capSeconds >= OfflineReport.MAX_SECONDS;
            Label cap = new Label(Lang.format(max ? "offline.capMax" : "offline.cap", Names.duration(r.capSeconds)),
                skin, r.reachedCap() ? "gold-small" : "dim");
            cap.setWrap(true);
            cap.setAlignment(com.badlogic.gdx.utils.Align.center);
            panel.add(cap).width(504f).padBottom(24f).row();
        }

        Table lines = new Table();
        line(lines, skin, Lang.get("offline.kills"), Lang.format("offline.killsValue", Format.number(r.kills)));
        line(lines, skin, Lang.get("offline.gold"), "+" + Format.gold(r.goldGained));
        line(lines, skin, Lang.get("offline.ap"), "+" + Format.number(r.apGained));
        if (r.endLevel != r.startLevel) line(lines, skin, Lang.get("offline.level"), r.startLevel + " → " + r.endLevel);
        line(lines, skin, Lang.get("offline.stage"), r.startStage.equals(r.endStage) ? r.endStage : r.startStage + " → " + r.endStage);
        if (r.skillCasts > 0) line(lines, skin, Lang.get("offline.casts"), Lang.format("offline.timesValue", Format.number(r.skillCasts)));
        if (r.defeats > 0) line(lines, skin, Lang.get("offline.defeats"), Lang.format("offline.timesValue", r.defeats));
        panel.add(lines).growX().padBottom(32f).row();

        TextButton ok = new TextButton(Lang.get("btn.ok"), skin, "accent");
        ok.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                close();
            }
        });
        panel.add(ok).size(240f, 80f);

        add(panel).width(600f);
    }

    private static void line(Table t, Skin skin, String label, String value) {
        t.add(new Label(label, skin, "default")).left().expandX().padBottom(10f);
        t.add(new Label(value, skin, "large")).right().padBottom(10f).row();
    }

    public void show(Stage stage) {
        getColor().a = 0f;
        stage.addActor(this);
        addAction(Actions.fadeIn(0.25f));
    }

    private void close() {
        setTouchable(Touchable.disabled);
        addAction(Actions.sequence(Actions.fadeOut(0.2f), Actions.removeActor()));
    }
}
