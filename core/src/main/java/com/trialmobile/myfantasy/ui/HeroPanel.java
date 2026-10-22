package com.trialmobile.myfantasy.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.trialmobile.myfantasy.audio.Sfx;
import com.trialmobile.myfantasy.battle.PlayerState;
import com.trialmobile.myfantasy.i18n.Lang;
import com.trialmobile.myfantasy.i18n.Names;
import com.trialmobile.myfantasy.rules.Attribute;
import com.trialmobile.myfantasy.rules.Attributes;
import com.trialmobile.myfantasy.rules.DiceRoll;
import com.trialmobile.myfantasy.rules.Hero;

/** 영웅 탭: 캐릭터 시트(레벨·모험 점수 막대, 능력치 8개, 전투 수치)와 언어 설정. */
public class HeroPanel extends TabPanel {
    private static final int STAT_COLUMNS = 4;

    private final PlayerState state;
    private final Label name;
    private final Label level;
    private final Gauge exp;
    private final Label[] statValues = new Label[Attribute.values().length];
    private final Label hp;
    private final Label attack;
    private final Label defense;
    private final Label extra;
    private final Label offlineCap;

    /**
     * @param onLanguage 언어 버튼을 눌러 언어가 실제로 바뀌었을 때(화면을 새로 만들어야 글자가 바뀐다)
     */
    public HeroPanel(Skin skin, PlayerState state, final Runnable onLanguage) {
        this.state = state;
        top();

        name = line(skin, "large");
        level = new Label("", skin, "gold");
        Table head = new Table();
        head.add(name).growX().minWidth(0f).left();
        head.add(level).right();
        add(head).growX().minWidth(0f).row();

        exp = new Gauge(skin, "exp", UiSkin.SCALE, 0);
        add(exp).growX().minWidth(0f).height(exp.getPrefHeight()).padTop(8f).row();

        Table stats = card(skin);
        Attribute[] attrs = Attribute.values();
        for (int i = 0; i < attrs.length; i++) {
            statValues[i] = new Label("", skin, "default");
            stats.add(new Label(Names.attr(attrs[i]), skin, "dim")).left().padRight(8f);
            stats.add(statValues[i]).left().expandX();
            if (i % STAT_COLUMNS == STAT_COLUMNS - 1) stats.row();
        }
        add(stats).growX().minWidth(0f).padTop(14f).row();

        hp = line(skin, "default");
        attack = line(skin, "default");
        defense = line(skin, "default");
        extra = wrapped(skin, "dim");
        offlineCap = wrapped(skin, "dim");
        Table combat = new Table();
        combat.defaults().growX().minWidth(0f).left();
        combat.add(hp);
        combat.add(defense).row();
        combat.add(attack).colspan(2).padTop(6f).row();
        combat.add(extra).colspan(2).padTop(6f).row();
        combat.add(offlineCap).colspan(2).padTop(6f);
        add(combat).growX().minWidth(0f).padTop(14f).row();

        // --- 언어 설정: 각 언어 이름은 그 언어로 표기, 지금 언어는 눌린 상태 ---
        Table language = card(skin);
        Label languageTitle = line(skin, "dim");
        languageTitle.setText(Lang.get("settings.language"));
        language.add(languageTitle).growX().minWidth(0f);
        for (final String code : Lang.LANGUAGES) {
            // 지금 언어는 금색, 다른 언어는 파란색. 지금 언어를 다시 눌러도 아무 일 없다.
            TextButton b = new TextButton(Lang.nativeName(code), skin, code.equals(Lang.language()) ? "small" : "blue-small");
            b.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    Sfx.play(Sfx.UI_CLICK);
                    if (Lang.set(code) && onLanguage != null) onLanguage.run();
                }
            });
            language.add(b).size(150f, 60f).padLeft(10f);
        }
        add(language).growX().minWidth(0f).padTop(16f).row();

        // --- 효과음 켜기/끄기 ---
        Table sound = card(skin);
        Label soundTitle = line(skin, "dim");
        soundTitle.setText(Lang.get("settings.sound"));
        sound.add(soundTitle).growX().minWidth(0f);
        final TextButton soundButton = new TextButton("", skin, "small");
        refreshSoundButton(soundButton, skin);
        soundButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Sfx.setEnabled(!Sfx.isEnabled());
                Sfx.play(Sfx.UI_CLICK);
                refreshSoundButton(soundButton, skin);
            }
        });
        sound.add(soundButton).size(150f, 60f).padLeft(10f);
        add(sound).growX().minWidth(0f).padTop(10f);
        refresh();
    }

    /** 켜져 있으면 금색 "켜짐", 꺼져 있으면 파란색 "꺼짐". */
    private static void refreshSoundButton(TextButton b, Skin skin) {
        boolean on = Sfx.isEnabled();
        b.setText(Lang.get(on ? "settings.on" : "settings.off"));
        b.setStyle(skin.get(on ? "small" : "blue-small", TextButton.TextButtonStyle.class));
    }

    @Override
    public void refresh() {
        Hero h = state.hero;
        Attributes a = h.getAttributes();
        name.setText(Names.hero(h));
        level.setText("Lv " + h.getLevel());
        int need = Math.max(1, h.apToNextLevel());
        exp.setValue(Math.min(1f, (float) h.getAp() / need));
        exp.setText(Lang.format("hero.ap", Format.number(h.getAp()), Format.number(need)));
        Attribute[] attrs = Attribute.values();
        for (int i = 0; i < attrs.length; i++) statValues[i].setText(String.valueOf(a.get(attrs[i])));
        hp.setText(Lang.format("hero.hp", h.getHp(), h.maxCon()));
        defense.setText(Lang.format("hero.armor", h.armorHits()));
        attack.setText(Lang.format("hero.attack", DiceRoll.notation(h.combatDice(), h.combatAdds())));
        extra.setText(Lang.format("hero.extra", Format.signed(a.personalAdds()), Format.number(state.getKills())));
        double cap = state.offlineCapSeconds();
        offlineCap.setText(Lang.format(cap >= com.trialmobile.myfantasy.battle.OfflineReport.MAX_SECONDS
            ? "hero.offlineCapMax" : "hero.offlineCap", Names.duration(cap)));
    }
}
