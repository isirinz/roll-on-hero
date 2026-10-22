package com.trialmobile.myfantasy.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.trialmobile.myfantasy.audio.Sfx;
import com.trialmobile.myfantasy.battle.PlayerState;
import com.trialmobile.myfantasy.battle.Rebirth;
import com.trialmobile.myfantasy.i18n.Lang;

/**
 * 환생 탭: 영혼석·환생 횟수, 지금 환생하면 얻는 영혼석과 환생 버튼(확인 팝업), 영혼석으로 배우는 혼(영구 특성) 목록.
 */
public class RebirthPanel extends TabPanel {
    private final PlayerState state;
    private final Skin skin;
    private final Runnable onChange;
    private final Runnable onRebirth;

    private final Label souls;
    private final Label count;
    private final Label best;
    private final Label gain;
    private final TextButton rebirthButton;
    private final PerkRow[] rows = new PerkRow[Rebirth.Perk.values().length];

    /**
     * @param onChange  혼을 배워 능력치 등이 바뀌었을 때(다른 탭·전투 화면 갱신)
     * @param onRebirth 확인 팝업에서 환생을 눌렀을 때
     */
    public RebirthPanel(Skin skin, PlayerState state, Runnable onChange, Runnable onRebirth) {
        this.skin = skin;
        this.state = state;
        this.onChange = onChange;
        this.onRebirth = onRebirth;
        top();

        // --- 영혼석 / 환생 횟수 ---
        souls = line(skin, "large");
        count = line(skin, "ink");
        count.setColor(Palette.TEXT_DIM);
        Table head = card(skin);
        head.add(picture(skin, "icon-soul")).size(72f).padRight(14f);
        Table headText = new Table();
        Label soulName = line(skin, "ink");
        soulName.setColor(Palette.TEXT_DIM);
        soulName.setText(Lang.get("rebirth.souls"));
        headText.add(soulName).growX().minWidth(0f).left().row();
        headText.add(souls).growX().minWidth(0f).left();
        head.add(headText).growX().minWidth(0f);
        head.add(count).right();
        add(head).growX().minWidth(0f).padBottom(12f).row();

        // --- 환생 ---
        Table main = card(skin);
        Label desc = wrapped(skin, "small");
        desc.setText(Lang.get("rebirth.desc"));
        main.add(desc).growX().minWidth(0f).row();
        best = line(skin, "ink");
        best.setColor(Palette.TEXT_DIM);
        main.add(best).growX().minWidth(0f).padTop(10f).row();
        gain = wrapped(skin, "ink");
        gain.setAlignment(Align.center);
        main.add(gain).growX().minWidth(0f).padTop(6f).row();
        rebirthButton = new TextButton(Lang.get("rebirth.button"), skin, "purple");
        rebirthButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Sfx.play(Sfx.UI_CLICK);
                confirm();
            }
        });
        main.add(rebirthButton).size(320f, 84f).padTop(12f);
        add(main).growX().minWidth(0f).padBottom(14f).row();

        // --- 혼(영구 특성) ---
        add(header(skin, Lang.get("rebirth.perks"))).padBottom(8f).row();
        for (Rebirth.Perk perk : Rebirth.Perk.values()) {
            PerkRow row = new PerkRow(perk);
            rows[perk.ordinal()] = row;
            add(row).growX().minWidth(0f).padBottom(8f).row();
        }
        refresh();
    }

    @Override
    public void refresh() {
        Rebirth r = state.rebirth;
        souls.setText(Format.gold(r.getSouls()));
        count.setText(Lang.format("rebirth.count", r.getRebirths()));
        best.setText(Lang.format("rebirth.best", Rebirth.stageLabel(r.getBestStage())));
        if (r.canRebirth()) {
            gain.setText(Lang.format("rebirth.gain", Format.gold(r.soulsForRebirth())));
            gain.setColor(Palette.SKILL);
        } else {
            gain.setText(Lang.format("rebirth.locked", Rebirth.stageLabel(Rebirth.UNLOCK_STAGE_INDEX)));
            gain.setColor(Palette.TEXT_DIM);
        }
        rebirthButton.setDisabled(!r.canRebirth());
        for (PerkRow row : rows) row.refresh();
    }

    private void confirm() {
        final Rebirth r = state.rebirth;
        if (!r.canRebirth() || getStage() == null) return;
        new ConfirmDialog(skin, Lang.get("rebirth.confirmTitle"),
            Lang.format("rebirth.confirmText", Format.gold(r.soulsForRebirth())),
            Lang.get("rebirth.button"), "purple", onRebirth).show(getStage());
    }

    private static String effect(Rebirth.Perk perk, int level) {
        return Lang.format("perk." + perk.id + ".desc", level * perk.perLevel);
    }

    private final class PerkRow extends Table {
        final Rebirth.Perk perk;
        final Label title;
        final Label detail;
        final PriceButton button;

        PerkRow(Rebirth.Perk p) {
            this.perk = p;
            title = line(skin, "default");
            detail = line(skin, "ink");
            button = new PriceButton(skin, "purple-small", "icon-soul");
            button.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (state.rebirth.buy(perk, state.hero)) {
                        Sfx.play(Sfx.UI_UPGRADE);
                        onChange.run();
                    }
                }
            });
            Image icon = picture(skin, "icon-soul");
            Table text = new Table();
            text.add(title).growX().minWidth(0f).row();
            text.add(detail).growX().minWidth(0f).padTop(4f);
            Table c = card(skin);
            c.add(icon).size(48f).padRight(10f);
            c.add(text).growX().minWidth(0f).padRight(10f);
            c.add(button).size(BUTTON_WIDTH, BUTTON_HEIGHT);
            add(c).growX().minWidth(0f);
        }

        void refresh() {
            Rebirth r = state.rebirth;
            int level = r.getLevel(perk);
            title.setText(Lang.get("perk." + perk.id) + "  " + Lang.format("perk.level", level));
            // "골드 +10% → +20%" (처음이면 "→ 골드 +10%")
            String nextValue = "+" + (level + 1) * perk.perLevel + (perk == Rebirth.Perk.HEROIC ? "" : "%");
            detail.setText(level == 0 ? "→ " + effect(perk, 1) : effect(perk, level) + " → " + nextValue);
            detail.setColor(Palette.GOOD);
            button.setPrice(Lang.get("btn.upgrade"), r.cost(perk));
            button.setDisabled(!r.canBuy(perk));
        }
    }
}
