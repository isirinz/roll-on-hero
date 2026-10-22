package com.trialmobile.myfantasy.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.trialmobile.myfantasy.audio.Sfx;
import com.trialmobile.myfantasy.i18n.Lang;
import com.trialmobile.myfantasy.i18n.Names;
import com.trialmobile.myfantasy.battle.PlayerState;
import com.trialmobile.myfantasy.rules.Hero;
import com.trialmobile.myfantasy.rules.Skill;
import com.trialmobile.myfantasy.rules.SkillBook;
import com.trialmobile.myfantasy.shop.Shop;

/** 스킬 탭: 스킬별 효과, 쿨타임, 골드 강화. 스킬은 전투 중 자동으로 발동한다. */
public class SkillPanel extends TabPanel {
    private final PlayerState state;
    private final Shop shop;
    private final Runnable onChange;
    private final SkillRow[] rows = new SkillRow[Skill.values().length];

    public SkillPanel(Skin skin, PlayerState state, Shop shop, Runnable onChange) {
        this.state = state;
        this.shop = shop;
        this.onChange = onChange;
        top();
        Label hint = wrapped(skin, "dim");
        hint.setText(Lang.get("skill.hint"));
        add(hint).growX().minWidth(0f).padBottom(12f).row();
        for (Skill skill : Skill.values()) {
            SkillRow row = new SkillRow(skin, skill);
            rows[skill.ordinal()] = row;
            add(row).growX().minWidth(0f).padBottom(10f).row();
        }
        refresh();
    }

    @Override
    public void refresh() {
        for (SkillRow row : rows) row.refresh();
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        SkillBook book = state.hero.getSkills();
        for (SkillRow row : rows) {
            row.cooldown.setValue(book.isUnlocked(row.skill) ? book.readiness(row.skill) : 0f);
        }
    }

    private final class SkillRow extends Table {
        final Skill skill;
        final Image icon;
        /** 아이콘 아래 작은 글씨 레벨(이름 줄에 붙이면 긴 이름에서 잘려서 따로 뺐다). */
        final Label levelLabel;
        final Label title;
        final Label detail;
        final ProgressBar cooldown;
        final PriceButton button;

        SkillRow(Skin skin, Skill skill) {
            this.skill = skill;
            icon = picture(skin, "skill-" + skill.id);
            levelLabel = new Label("", skin, "small");
            levelLabel.setAlignment(Align.center);
            title = wrapped(skin, "ink"); // 영어 스킬 이름은 길어서 두 줄까지 허용
            detail = wrapped(skin, "dim");
            cooldown = new ProgressBar(0f, 1f, 0.001f, false, skin, "cooldown");
            button = new PriceButton(skin, "accent");
            button.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (shop.upgradeSkill(SkillRow.this.skill)) {
                        Sfx.play(Sfx.UI_UPGRADE);
                        onChange.run();
                    }
                }
            });
            Table text = new Table();
            text.add(title).growX().minWidth(0f).row();
            text.add(detail).growX().minWidth(0f).padTop(4f).row();
            text.add(cooldown).growX().minWidth(0f).padTop(6f);
            Table card = card(skin);
            Table iconBox = new Table();
            iconBox.add(icon).size(64f).row();
            iconBox.add(levelLabel).width(76f).padTop(2f);
            card.add(iconBox).width(76f).padRight(10f).top();
            card.add(text).growX().minWidth(0f).padRight(10f);
            card.add(button).size(BUTTON_WIDTH, BUTTON_HEIGHT);
            add(card).growX().minWidth(0f);
        }

        void refresh() {
            Hero hero = state.hero;
            int level = hero.getSkills().getLevel(skill);
            if (level <= 0) {
                title.setText(Names.skill(skill));
                title.setColor(Palette.TEXT_DIM);
                levelLabel.setText(Lang.format("skill.slotLocked", skill.unlockLevel));
                levelLabel.setColor(Palette.TEXT_DIM);
                icon.setColor(0.35f, 0.3f, 0.35f, 0.8f);
                detail.setText(Lang.format("skill.locked", skill.unlockLevel, Names.skillEffect(skill, hero, 1)));
                button.setPlain(Lang.format("btn.needLevel", skill.unlockLevel));
                button.setDisabled(true);
                return;
            }
            title.setText(Names.skill(skill));
            levelLabel.setText(Lang.format("skill.slotLevel", level));
            levelLabel.setColor(Palette.TEXT);
            title.setColor(Palette.SKILL);
            icon.setColor(Color.WHITE);
            detail.setText(Names.skillEffect(skill, hero, level));
            button.setPrice(Lang.get("btn.upgrade"), shop.skillUpgradeCost(skill));
            button.setDisabled(!shop.canUpgradeSkill(skill));
        }
    }
}
