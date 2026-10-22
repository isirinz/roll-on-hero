package com.trialmobile.myfantasy.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import com.trialmobile.myfantasy.MainGame;
import com.trialmobile.myfantasy.ads.RewardedAds;
import com.trialmobile.myfantasy.audio.Sfx;
import com.trialmobile.myfantasy.anim.CharacterSprite;
import com.trialmobile.myfantasy.anim.BoostAura;
import com.trialmobile.myfantasy.anim.DiceThrow;
import com.trialmobile.myfantasy.anim.RisingSparkle;
import com.trialmobile.myfantasy.anim.EffectActor;
import com.trialmobile.myfantasy.anim.SpriteLibrary;
import com.trialmobile.myfantasy.battle.BattleSystem;
import com.trialmobile.myfantasy.battle.MonsterTable;
import com.trialmobile.myfantasy.battle.OfflineReport;
import com.trialmobile.myfantasy.battle.PlayerState;
import com.trialmobile.myfantasy.battle.PowerBoost;
import com.trialmobile.myfantasy.battle.StageProgress;
import com.trialmobile.myfantasy.i18n.Lang;
import com.trialmobile.myfantasy.i18n.Names;
import com.trialmobile.myfantasy.rules.Combatant;
import com.trialmobile.myfantasy.rules.Dice;
import com.trialmobile.myfantasy.rules.Hero;
import com.trialmobile.myfantasy.rules.Monster;
import com.trialmobile.myfantasy.rules.RoundResult;
import com.trialmobile.myfantasy.rules.Skill;
import com.trialmobile.myfantasy.rules.SkillBook;
import com.trialmobile.myfantasy.rules.SkillCast;
import com.trialmobile.myfantasy.shop.Shop;
import com.trialmobile.myfantasy.ui.BoostButton;
import com.trialmobile.myfantasy.ui.EquipmentPanel;
import com.trialmobile.myfantasy.ui.FitLabel;
import com.trialmobile.myfantasy.ui.Format;
import com.trialmobile.myfantasy.ui.Gauge;
import com.trialmobile.myfantasy.ui.HeroPanel;
import com.trialmobile.myfantasy.ui.OfflineRewardDialog;
import com.trialmobile.myfantasy.ui.Palette;
import com.trialmobile.myfantasy.ui.RebirthPanel;
import com.trialmobile.myfantasy.ui.ShopPanel;
import com.trialmobile.myfantasy.ui.SkillPanel;
import com.trialmobile.myfantasy.ui.SkillSlot;
import com.trialmobile.myfantasy.ui.TabPanel;
import com.trialmobile.myfantasy.ui.UiSkin;

/**
 * 메인(전투) 화면. {@link BattleSystem}의 자동 전투를 보여 준다.
 * <pre>
 * ┌───────────────────────────────┐
 * │ 상단 바: [방패 1-1] 웨이브 · x1 · 골드 │  어두운 나무 패널
 * ├───────────────────────────────┤
 * │ 전투 영역(초원 + 액자)              │
 * │   영웅  VS  몬스터 (이름·HP)        │
 * │   전투 기록 2줄                   │
 * │   ◯ ◯ ◯ ◯ ◯  스킬 슬롯(쿨타임)     │
 * ├───────────────────────────────┤
 * │ 탭 내용 (스크롤)                  │  양피지 패널, 남는 세로 공간을 모두 쓴다
 * ├───────────────────────────────┤
 * │ 하단 탭 4개                      │
 * └───────────────────────────────┘
 * </pre>
 * 모든 줄은 {@link #COLUMN_WIDTH} 너비 안에 고정 배치한다. 글자가 길면 말줄임(…)이나 줄바꿈으로 처리하고,
 * 탭 내용이 길면 스크롤된다. 그래서 어떤 내용이 와도 화면 밖으로 밀려나지 않는다.
 */
public class BattleScreen extends BaseScreen implements BattleSystem.Listener {
    /** 하단 탭 글자 키(i18n). */
    private static final String[] TABS = {"tab.hero", "tab.equipment", "tab.skill", "tab.shop", "tab.rebirth"};
    private static final int TAB_HERO = 0;
    private static final int TAB_EQUIPMENT = 1;
    private static final int TAB_SKILL = 2;
    private static final int TAB_SHOP = 3;
    private static final int TAB_REBIRTH = 4;
    private static final String[] TAB_ICONS = {"icon-hero", "icon-equipment", "icon-skill", "icon-shop", "icon-rebirth"};
    /** 영웅 그림 id (assets/sprites/hero). */
    private static final String HERO_SPRITE = "hero";

    /** 화면 세로 기둥 너비. 가상 해상도 최소 너비(720)에서 양옆 12px씩 남긴다. */
    public static final float COLUMN_WIDTH = 696f;
    /** 전투 영역: 배경 그림(168 x 146 부분)의 정확히 4배. */
    private static final float FIELD_WIDTH = 672f;
    private static final float FIELD_HEIGHT = 584f;
    /** 캐릭터 그림이 들어가는 칸. 보스까지 들어가도록 높게 잡았다. */
    private static final float BODY_WIDTH = 220f;
    private static final float BODY_HEIGHT = 270f;
    /** 이름 칸 너비(가운데 VS 칸을 빼고 남는 폭). 길면 …로 줄인다. */
    private static final float NAME_WIDTH = 258f;
    private static final float HUD_HEIGHT = 108f;
    private static final float NAV_HEIGHT = 116f;
    private static final float GAP = 8f;
    private static final int LOG_LINES = 2;
    private static final float[] SPEEDS = {1f, 2f, 3f};
    /** 공격 동작의 타격 순간을 기다리는 최대 시간(배속 1 기준). */
    private static final float MAX_IMPACT_DELAY = 0.4f;

    private final PlayerState state;
    private final BattleSystem battle;
    private final Shop shop;

    private final Label chapterLabel;
    private final Label stageLabel;
    private final Label waveLabel;
    private final Label goldLabel;
    private final Button speedButton;
    private final Image[] speedArrows = new Image[SPEEDS.length];
    private int speedIndex;

    private final FighterView heroView;
    private final FighterView monsterView;
    /** 전투 기록 줄(한 줄에 한 항목, 길면 …로 줄인다). */
    private final Label[] logLabels = new Label[LOG_LINES];
    private final SkillSlot[] skillSlots = new SkillSlot[Skill.values().length];
    private final Array<String> logLines = new Array<String>();
    private final TextButton bossButton;
    /** 2배 버프 버튼(전투 영역 가운데 위). */
    private final BoostButton boostButton;
    /** 버프 중 영웅 발밑의 금빛 기운. */
    private final BoostAura heroAura;
    private boolean boostWasActive;
    private float sparkleTimer;

    /** 주사위 연출 레이어(전투원 위, 액자 아래). */
    private final Group diceLayer = new Group();
    /** 가운데 VS 글자. 양쪽 타격 총합을 이 옆에 띄운다("11 VS 18"). */
    private Label vsLabel;
    private DiceThrow heroThrow;
    private DiceThrow monsterThrow;
    /**
     * 이번 프레임에 주사위를 던졌다면, 주사위가 멈출 때까지 피격·처치 같은 연출을 미루는 시간(초).
     * 전투 계산은 즉시 끝나고, 화면만 "굴림 → 착지 → 타격" 순서로 보여 준다.
     */
    private float visualDelay;

    private final Container<Actor> tabBody = new Container<Actor>();
    private final ScrollPane tabScroll;
    private final TabPanel[] panels = new TabPanel[TABS.length];
    private int currentTab;

    private final Vector2 tmp = new Vector2();
    /** 전투 배경 그림과 지금 보여 주는 테마(-1 = 아직 없음). */
    private Image fieldBg;
    private int shownTheme = -1;

    public BattleScreen(MainGame game) {
        super(game);
        state = game.getPlayerState();
        battle = new BattleSystem(state, new Dice());
        battle.setListener(this);
        shop = new Shop(state);

        root.top().pad(GAP, 0f, GAP, 0f);

        // --- 상단 바 ---
        chapterLabel = new Label("", skin, "field");
        chapterLabel.setAlignment(Align.center);
        stageLabel = new FitLabel("", skin, "field");
        waveLabel = new Label("", skin, "field-small");
        waveLabel.setColor(Palette.GOLD);
        waveLabel.setEllipsis(true);
        goldLabel = new Label("", skin, "field");
        goldLabel.setAlignment(Align.right);
        goldLabel.setEllipsis(true);
        // 배속 버튼: ▶ / ▶▶ / ▶▶▶ (화살표 개수 = 배속)
        speedButton = new Button(skin.get("blue", TextButtonStyle.class));
        for (int i = 0; i < SPEEDS.length; i++) {
            speedArrows[i] = image("arrow");
            speedButton.add(speedArrows[i]).size(18f, 27f).padLeft(i == 0 ? 0f : 2f);
        }
        speedButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Sfx.play(Sfx.UI_CLICK);
                speedIndex = (speedIndex + 1) % SPEEDS.length;
                battle.setSpeed(SPEEDS[speedIndex]);
                refreshSpeedButton();
            }
        });
        refreshSpeedButton();

        Stack badge = new Stack();
        badge.add(image("badge"));
        Container<Label> chapterBox = new Container<Label>(chapterLabel);
        chapterBox.padBottom(10f);
        badge.add(chapterBox);

        Table stageInfo = new Table();
        stageInfo.add(stageLabel).growX().left().minWidth(0f).row();
        stageInfo.add(waveLabel).growX().left().minWidth(0f).padTop(2f);

        Table goldBox = new Table();
        goldBox.setBackground(skin.getDrawable("pill"));
        goldBox.pad(0f, 14f, 0f, 22f);
        goldBox.add(image("icon-coin")).size(40f).padRight(4f);
        goldBox.add(goldLabel).growX().minWidth(0f);

        Table hud = new Table();
        hud.setBackground(skin.getDrawable("panel-dark"));
        hud.pad(12f, 22f, 12f, 22f);
        hud.add(badge).size(78f, 84f).padRight(14f);
        hud.add(stageInfo).growX().minWidth(0f);
        hud.add(speedButton).size(84f, 66f).padRight(8f);
        hud.add(goldBox).size(206f, 60f);
        root.add(hud).size(COLUMN_WIDTH, HUD_HEIGHT).row();

        // --- 전투 영역 ---
        heroView = new FighterView(Palette.HERO, true);
        monsterView = new FighterView(Palette.MONSTER, false);
        heroView.setCharacter(HERO_SPRITE);
        bossButton = new TextButton(Lang.get("battle.retry"), skin, "rose");
        bossButton.setVisible(false);
        bossButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Sfx.play(Sfx.UI_CLICK);
                battle.challengeBoss();
                bossButton.setVisible(false);
                log(Lang.get("log.retry"));
                refreshTopBar();
            }
        });

        Table fighters = new Table();
        fighters.top().pad(34f, 24f, 0f, 24f);
        fighters.setClip(true); // 미끄러져 들어오는 큰 몬스터가 액자 밖으로 삐져나오지 않게
        boostButton = new BoostButton(skin);
        boostButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                onBoostPressed();
            }
        });
        Table center = new Table();
        center.add(boostButton).size(96f, 90f).padTop(8f).row();
        vsLabel = new Label("VS", skin, "field-large");
        center.add(vsLabel).padTop(32f).row();
        center.add(bossButton).size(164f, 66f).padTop(30f);
        fighters.add(heroView.root).expandX().uniformX().top();
        fighters.add(center).width(104f).top();
        fighters.add(monsterView.root).expandX().uniformX().top();


        Table logBand = new Table();
        logBand.setBackground(skin.newDrawable("white", new Color(0.10f, 0.06f, 0.12f, 0.45f)));
        logBand.pad(6f, 14f, 6f, 14f);
        for (int i = 0; i < LOG_LINES; i++) {
            logLabels[i] = new Label("", skin, "field-small");
            logLabels[i].setEllipsis(true);
            logBand.add(logLabels[i]).growX().minWidth(0f).height(26f).row();
        }

        Table skillRow = new Table();
        for (Skill skill : Skill.values()) {
            SkillSlot slot = new SkillSlot(skin, skill.id, UiSkin.SCALE);
            skillSlots[skill.ordinal()] = slot;
            skillRow.add(slot).expandX();
        }

        Table overlay = new Table();
        overlay.bottom().pad(0f, 24f, 22f, 24f);
        overlay.setTouchable(Touchable.childrenOnly);
        overlay.add(logBand).growX().row();
        overlay.add(skillRow).growX().padTop(8f);

        Stack field = new Stack();
        fieldBg = new Image(skin.newDrawable("round", Palette.SKY));
        fieldBg.setScaling(Scaling.stretch);
        field.add(fieldBg);
        Group auraLayer = new Group();
        auraLayer.setTouchable(Touchable.disabled);
        heroAura = new BoostAura(skin.getRegion("glow-soft"), new Color(1f, 0.72f, 0.18f, 1f));
        heroAura.setVisible(false);
        auraLayer.addActor(heroAura);
        field.add(fighters);
        // 기운은 전투원 위에 옅게 덮는다(전투원 칸 아래에 두면 가려져 보이지 않았다)
        field.add(auraLayer);
        field.add(overlay);
        diceLayer.setTouchable(Touchable.disabled);
        field.add(diceLayer);
        Image frame = new Image(skin.getDrawable("frame"));
        frame.setTouchable(Touchable.disabled);
        field.add(frame);
        root.add(field).size(FIELD_WIDTH, FIELD_HEIGHT).padTop(GAP).row();

        // --- 탭 내용 ---
        Runnable onShopChanged = new Runnable() {
            @Override
            public void run() {
                onEquipmentChanged();
            }
        };
        panels[TAB_HERO] = new HeroPanel(skin, state, new Runnable() {
            @Override
            public void run() {
                // 글자와 글꼴(영어는 좁은 글꼴)은 화면을 만들 때 정해지므로, 언어가 바뀌면 글꼴·스킨·전투 화면을 새로 만든다.
                game.reloadForLanguage();
            }
        });
        panels[TAB_EQUIPMENT] = new EquipmentPanel(skin, state, shop, onShopChanged);
        panels[TAB_SKILL] = new SkillPanel(skin, state, shop, onShopChanged);
        panels[TAB_SHOP] = new ShopPanel(skin, state, shop, onShopChanged);
        panels[TAB_REBIRTH] = new RebirthPanel(skin, state, onShopChanged, new Runnable() {
            @Override
            public void run() {
                game.rebirth(); // 새 상태로 전투 화면을 다시 연다
            }
        });
        tabBody.fill().top();
        tabScroll = new ScrollPane(tabBody, skin);
        tabScroll.setScrollingDisabled(true, false);
        tabScroll.setFadeScrollBars(true);
        tabScroll.setOverscroll(false, true);
        Table content = new Table();
        content.setBackground(skin.getDrawable("panel"));
        content.pad(30f, 30f, 30f, 30f);
        content.add(tabScroll).grow().minWidth(0f);
        root.add(content).width(COLUMN_WIDTH).growY().padTop(GAP).row();

        // --- 하단 탭 버튼 ---
        Table nav = new Table();
        ButtonGroup<TextButton> group = new ButtonGroup<TextButton>();
        group.setMinCheckCount(1);
        group.setMaxCheckCount(1);
        for (int i = 0; i < TABS.length; i++) {
            final int index = i;
            TextButton button = new TextButton(Lang.get(TABS[i]), skin, "tab");
            // 아이콘을 글자 위에 얹는다
            Label text = button.getLabel();
            button.clearChildren();
            button.add(image(TAB_ICONS[i])).size(56f).padTop(2f).row();
            button.add(text).padTop(2f);
            button.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (((TextButton) actor).isChecked()) {
                        Sfx.play(Sfx.UI_TAB);
                        showTab(index);
                    }
                }
            });
            group.add(button);
            nav.add(button).growX().uniformX().height(NAV_HEIGHT).padLeft(i == 0 ? 0f : 8f);
        }
        root.add(nav).width(COLUMN_WIDTH).padTop(GAP);

        showTab(TAB_HERO);
        refreshAll();
        if (state.getKills() == 0 && state.rebirth.getRebirths() > 0) log(Lang.format("log.reborn", state.rebirth.getRebirths() + 1));
        else log(state.getKills() == 0 ? Lang.get("log.start") : Lang.format("log.resume", state.progress.label()));
    }

    @Override
    protected void update(float delta) {
        double away = game.consumeOfflineSeconds();
        if (away > 0) applyOfflineReward(away);
        visualDelay = 0f;
        battle.update(delta);
        updateSkillSlots();
        updateBoost(delta);
    }

    // ------------------------------------------------------------------
    // 2배 버프
    // ------------------------------------------------------------------

    private void updateBoost(float delta) {
        long now = battle.now();
        PowerBoost boost = state.boost;
        boostButton.update(boost, now, game.getRewardedAds().isReady());
        boolean on = boost.isActive(now);
        if (boostWasActive && !on) log(Lang.format("log.boostEnd", Format.clock(boost.cooldownRemaining(now))));
        boostWasActive = on;

        // 영웅 발밑 금빛 기운
        heroAura.setVisible(on);
        if (on) {
            heroView.anchor(0f, tmp);
            heroAura.getParent().stageToLocalCoordinates(tmp);
            heroAura.setFeet(tmp.x, tmp.y, heroView.spriteHeight());
            // 영웅 둘레로 금빛 반짝이가 떠오른다
            sparkleTimer -= delta;
            if (sparkleTimer <= 0f && skin.has("sparkle", TextureRegion.class)) {
                sparkleTimer = 0.09f;
                heroView.anchor(com.badlogic.gdx.math.MathUtils.random(0.05f, 0.7f), tmp);
                heroAura.getParent().stageToLocalCoordinates(tmp);
                heroAura.getParent().addActor(new RisingSparkle(skin.getRegion("sparkle"), Palette.GOLD,
                    tmp.x + com.badlogic.gdx.math.MathUtils.random(-70f, 70f), tmp.y));
            }
        }
    }

    private void onBoostPressed() {
        long now = battle.now();
        PowerBoost boost = state.boost;
        if (boost.isActive(now)) {
            log(Lang.format("log.boostActive", Format.clock(boost.activeRemaining(now))));
            return;
        }
        if (!boost.isReady(now)) {
            log(Lang.format("log.boostCooldown", Format.clock(boost.cooldownRemaining(now))));
            return;
        }
        RewardedAds ads = game.getRewardedAds();
        if (!ads.isReady()) return;
        boostButton.setDisabled(true); // 광고가 떠 있는 동안 중복 누름 방지(다음 프레임 update가 되돌린다)
        ads.show("boost", new RewardedAds.Callback() {
            @Override
            public void onRewarded() {
                // 광고 SDK는 다른 스레드에서 부를 수 있으니 렌더 스레드로 옮긴다
                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        if (state.boost.activate(battle.now())) startBoostCelebration();
                    }
                });
            }

            @Override
            public void onCancelled() {
                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        log(Lang.get("log.adCancelled"));
                    }
                });
            }
        });
    }

    /** 버프가 켜진 순간: 큰 글씨 배너 + 반짝이 + 영웅 위 레벨업 빛. */
    private void startBoostCelebration() {
        Sfx.play(Sfx.BOOST_ON, 1f, 1f);
        game.save();
        log(Lang.format("log.boostStart", Names.duration(PowerBoost.DURATION_MS / 1000.0)));
        effect("level_up", heroView, 0f, 1f, false);
        Label bannerText = new Label(Lang.get("boost.banner"), skin, "field-large");
        bannerText.setColor(Palette.GOLD);
        Container<Label> banner = new Container<Label>(bannerText);
        banner.setTransform(true);
        banner.pack();
        banner.setOrigin(Align.center);
        banner.setTouchable(Touchable.disabled);
        diceLayer.addActor(banner);
        banner.setPosition((diceLayer.getWidth() - banner.getWidth()) / 2f, diceLayer.getHeight() * 0.7f);
        banner.setScale(0.2f);
        banner.addAction(Actions.sequence(
            Actions.scaleTo(1.45f, 1.45f, 0.18f, com.badlogic.gdx.math.Interpolation.pow2Out),
            Actions.scaleTo(1.15f, 1.15f, 0.15f, com.badlogic.gdx.math.Interpolation.swingOut),
            Actions.delay(0.9f),
            Actions.parallel(Actions.moveBy(0f, 60f, 0.4f), Actions.fadeOut(0.4f)),
            Actions.removeActor()));
    }

    private void updateSkillSlots() {
        SkillBook book = state.hero.getSkills();
        for (Skill skill : Skill.values()) {
            boolean unlocked = book.isUnlocked(skill);
            boolean buff = skill == Skill.VORPAL_BLADE && book.isVorpalActive();
            skillSlots[skill.ordinal()].setState(unlocked, unlocked ? book.readiness(skill) : 0f,
                unlocked ? Lang.format("skill.slotLevel", book.getLevel(skill))
                    : Lang.format("skill.slotLocked", skill.unlockLevel), buff);
        }
    }

    /** 비운 시간만큼 전투를 빨리 돌리고 결과 팝업을 띄운다. */
    private void applyOfflineReward(double seconds) {
        double cap = state.offlineCapSeconds(); // 빨리 감기로 더 깊이 가도 이번 보상의 한도는 떠날 때 기준
        OfflineReport report = battle.fastForward(seconds);
        report.capSeconds = cap;
        refreshAll();
        log(Lang.format("log.offline", Names.duration(seconds)));
        new OfflineRewardDialog(skin, report).show(stage);
        game.save();
    }

    /** 상점·장비 탭에서 구매/장착/강화가 일어났을 때. */
    private void onEquipmentChanged() {
        refreshAll();
        game.save();
    }

    private void refreshAll() {
        Hero hero = state.hero;
        heroView.setName(Lang.format("battle.heroName", Names.hero(hero), hero.getLevel()), Palette.TEXT_LIGHT);
        heroView.setHp(hero);
        if (!heroView.body.hasActions()) heroView.body.getColor().a = hero.isDead() ? 0.3f : 1f;
        Monster monster = battle.getMonster();
        if (monster != null) {
            monsterView.setCharacter(monster.getKindId());
            if (!monsterView.body.hasActions()) monsterView.body.getColor().a = 1f;
            onMonsterShown(monster);
        } else {
            monsterView.clear();
        }
        refreshTopBar();
        refreshPanel();
    }

    private void refreshPanel() {
        TabPanel panel = panels[currentTab];
        if (panel != null) panel.refresh();
    }

    // ------------------------------------------------------------------
    // 전투 이벤트
    // ------------------------------------------------------------------

    @Override
    public void onMonsterAppeared(Monster monster) {
        monsterView.setCharacter(monster.getKindId());
        monsterView.body.clearActions();
        monsterView.body.getColor().set(1f, 1f, 1f, 0f);
        monsterView.body.addAction(Actions.fadeIn(0.25f));
        if (monsterView.sprite != null && monsterView.body == monsterView.sprite) monsterView.sprite.slideIn(160f);
        onMonsterShown(monster);
        if (monster.isBoss()) {
            log(Lang.format("log.bossAppears", Names.monster(monster)));
            Sfx.play(Sfx.BOSS_APPEAR, 1f, 1f);
        }
        refreshTopBar();
    }

    private void onMonsterShown(Monster monster) {
        monsterView.setName(monster.isBoss() ? Lang.format("battle.boss", Names.monster(monster)) : Names.monster(monster),
            monster.isBoss() ? Palette.GOLD : Palette.TEXT_LIGHT);
        monsterView.setHp(monster, true);
    }

    @Override
    public void onRound(final RoundResult r, final Hero hero, final Monster monster) {
        throwDice(r);
        later(new Runnable() {
            @Override
            public void run() {
                showRound(r, hero, monster);
            }
        });
        // 휘두르는 동작이 타격 순간에 닿을 때까지 처치·레벨업 같은 뒤 연출도 기다리게 한다
        visualDelay += impactDelay(r);
    }

    /** 양쪽 주사위를 던진다. 다음 연출은 주사위가 멈춘 뒤로 미룬다({@link #visualDelay}). */
    private void throwDice(RoundResult r) {
        if (heroThrow != null) heroThrow.finishQuickly();
        if (monsterThrow != null) monsterThrow.finishQuickly();
        float speed = SPEEDS[speedIndex];
        float mid = diceLayer.getWidth() / 2f;
        // 바닥: 체력 막대 바로 위
        heroView.hp.localToStageCoordinates(tmp.set(0f, heroView.hp.getHeight()));
        diceLayer.stageToLocalCoordinates(tmp);
        float ground = tmp.y + 16f;

        heroView.anchor(0.55f, tmp);
        diceLayer.stageToLocalCoordinates(tmp);
        heroThrow = new DiceThrow(skin, r.rollA, r.rollA.multiplier > 1 ? "gold" : "hero",
            tmp.x + 30f, tmp.y, mid - 96f, ground, 1f);
        vsLabel.localToStageCoordinates(tmp.set(0f, vsLabel.getHeight() / 2f));
        diceLayer.stageToLocalCoordinates(tmp);
        float vsLeft = tmp.x - 10f;
        float vsY = tmp.y;
        float vsRight = vsLeft + vsLabel.getWidth() + 20f;
        heroThrow.setTotalAnchor(vsLeft, vsY, -1);
        heroThrow.setWinner(r.aWon());
        heroThrow.setTimeScale(speed);
        diceLayer.addActor(heroThrow);

        monsterView.anchor(0.55f, tmp);
        diceLayer.stageToLocalCoordinates(tmp);
        monsterThrow = new DiceThrow(skin, r.rollB, "monster", tmp.x - 30f, tmp.y, mid + 96f, ground, -1f);
        monsterThrow.setTotalAnchor(vsRight, vsY, 1);
        monsterThrow.setWinner(!r.aWon() && !r.tie());
        monsterThrow.setTimeScale(speed);
        diceLayer.addActor(monsterThrow);

        int most = Math.max(r.rollA.count, r.rollB.count);
        float land = DiceThrow.landTime(most) / speed;
        visualDelay = Math.max(visualDelay, land);

        // 소리: 굴리는 달그락 → 착지 탁(6이 있으면 반짝)
        Sfx.play(Sfx.DICE_SHAKE);
        final boolean six = r.rollA.sixes > 0 || r.rollB.sixes > 0;
        stage.addAction(Actions.sequence(Actions.delay(land * 0.85f), Actions.run(new Runnable() {
            @Override
            public void run() {
                Sfx.play(Sfx.DICE_LAND);
                if (six) Sfx.play(Sfx.DICE_SIX);
            }
        })));
    }

    /** 주사위가 멈춘 뒤 보여 줄 연출이면 미루고, 아니면 바로 실행한다. */
    private void later(Runnable action) {
        if (visualDelay <= 0f) {
            action.run();
            return;
        }
        stage.addAction(Actions.sequence(Actions.delay(visualDelay), Actions.run(action)));
    }

    /** 라운드 결과 연출(공격·피격·기록). 주사위가 멈춘 뒤 실행된다. */
    /** 이번 라운드에 휘두르는 쪽 공격 동작의 타격 순간까지(현재 배속 반영, 초). */
    private float impactDelay(RoundResult r) {
        float t = 0f;
        if (r.aWon() || r.tie()) t = Math.max(t, heroView.impactSeconds());
        if (!r.aWon()) t = Math.max(t, monsterView.impactSeconds());
        return Math.min(t, MAX_IMPACT_DELAY) / SPEEDS[speedIndex];
    }

    private void showRound(final RoundResult r, final Hero hero, final Monster monster) {
        // 이긴 쪽이 휘두르고(동점이면 둘 다), 진 쪽은 움찔한다. 이긴 쪽이 스파이트로 받은 피해는 번쩍임만.
        // 동작은 바로 시작하고, 타격(튀어 나감·피해 숫자·소리·체력 막대)은 무기가 닿는 순간에 맞춘다.
        final boolean heroSwings = r.aWon() || r.tie();
        final boolean monsterSwings = !r.aWon();
        if (heroSwings) heroView.attack();
        if (monsterSwings) monsterView.attack();
        Runnable impact = new Runnable() {
            @Override
            public void run() {
                heroView.setHp(hero);
                monsterView.setHp(monster);
                if (heroSwings) heroView.lunge(1f);
                if (monsterSwings) monsterView.lunge(-1f);
                if (r.damageToB > 0) Sfx.play(Sfx.HIT);
                if (r.damageToA > 0) Sfx.play(Sfx.HURT);
                if (r.damageToA > 0) hit(heroView, r.damageToA, !heroSwings || r.tie());
                if (r.damageToB > 0) hit(monsterView, r.damageToB, !monsterSwings || r.tie());
            }
        };
        float delay = impactDelay(r);
        if (delay <= 0f) impact.run();
        else stage.addAction(Actions.delay(delay, Actions.run(impact)));

        String score = " " + Lang.format("log.score", r.rollA.total(), r.rollB.total());
        String name = Names.monster(monster);
        if (r.aWon()) {
            log(Lang.format("log.heroHits", name, r.damageToB) + score
                + (r.damageToA > 0 ? " " + Lang.format("log.spite", r.damageToA) : ""));
        } else if (r.tie()) {
            log(Lang.get("log.tie") + score + (r.damageToA + r.damageToB > 0
                ? " " + Lang.format("log.spiteBoth", r.damageToB, r.damageToA) : ""));
        } else {
            log(Lang.format("log.monsterHits", name, r.damageToA) + score
                + (r.damageToB > 0 ? " " + Lang.format("log.spite", r.damageToB) : ""));
        }
    }

    @Override
    public void onSkillCast(final SkillCast cast, final Hero hero, final Monster monster) {
        later(new Runnable() {
            @Override
            public void run() {
            Skill skill = cast.skill;
            heroView.play(CharacterSprite.CAST);
            skillSlots[skill.ordinal()].pulse();
            switch (skill) {
                case TAKE_THAT:
                    Sfx.play(Sfx.SPELL_BOLT);
                    effect("spell_bolt", monsterView, 0.5f, 1f, false);
                    break;
                case POOR_BABY:
                    Sfx.play(Sfx.SPELL_HEAL);
                    effect("spell_heal", heroView, 0.4f, 1f, false);
                    break;
                case VORPAL_BLADE:
                    Sfx.play(Sfx.SPELL_VORPAL);
                    effect("spell_vorpal", heroView, 0.45f, 1f, false);
                    break;
                case BLASTING_POWER:
                    Sfx.play(Sfx.SPELL_FIRE);
                    effect("spell_fire", monsterView, 0.45f, 1f, false);
                    break;
                case HELLBOMB:
                    Sfx.play(Sfx.SPELL_HELLBOMB);
                    effect("spell_hellbomb", monsterView, 0.45f, 1f, false);
                    break;
            }
            switch (skill.kind) {
                case HEAL:
                    heroView.setHp(hero);
                    popup(heroView, "+" + cast.heal, Palette.GOOD, 1.05f);
                    log(Lang.format("log.skillHeal", Names.skill(skill), cast.heal));
                    break;
                case BUFF:
                    popup(heroView, Names.skillShort(skill) + "!", Palette.GOLD, 1.05f);
                    log(Lang.format("log.skillBuff", Names.skill(skill), Math.round(cast.buffSeconds)));
                    break;
                default:
                    monsterView.setHp(monster);
                    flash(monsterView);
                    monsterView.hurt();
                    popup(monsterView, Names.skillShort(skill) + " -" + cast.damage, Palette.SKY, 1.05f);
                    log(Lang.format("log.skillDamage", Names.skill(skill), Names.monster(monster), cast.damage));
                    break;
            }
            }
        });
    }

    @Override
    public void onMonsterDefeated(final Monster monster, final int ap, final int gold) {
        later(new Runnable() {
            @Override
            public void run() {
            monsterView.play(CharacterSprite.DEATH);
            Sfx.play(Sfx.MONSTER_DIE);
            stage.addAction(Actions.delay(0.18f, Actions.run(new Runnable() {
                @Override
                public void run() {
                    Sfx.play(Sfx.COIN);
                }
            })));
            monsterView.body.addAction(Actions.sequence(Actions.delay(0.3f), Actions.fadeOut(0.3f)));
            effect("defeat_poof", monsterView, 0.35f, 1f, false);
            log(Lang.format("log.kill", Names.monster(monster), Format.number(ap), Format.gold(gold)));
            heroView.setHp(state.hero);
            refreshTopBar();
            refreshPanel();
            }
        });
    }

    @Override
    public void onLevelUp(final Hero hero) {
        later(new Runnable() {
            @Override
            public void run() {
            log(Lang.format("log.levelUp", hero.getLevel()));
            heroView.setName(Lang.format("battle.heroName", Names.hero(hero), hero.getLevel()), Palette.TEXT_LIGHT);
            heroView.setHp(hero);
            popup(heroView, Lang.get("popup.levelUp"), Palette.GOLD);
            Sfx.play(Sfx.LEVEL_UP, 1f, 1f);
            effect("level_up", heroView, 0f, 1f, false);
            game.save();
            refreshPanel();
            }
        });
    }

    @Override
    public void onStageCleared(final String clearedLabel) {
        later(new Runnable() {
            @Override
            public void run() {
            log(Lang.format("log.stageClear", clearedLabel));
            Sfx.play(Sfx.STAGE_CLEAR, 1f, 1f);
            game.save();
            heroView.setHp(state.hero);
            bossButton.setVisible(false);
            refreshTopBar();
            }
        });
    }

    @Override
    public void onHeroDefeated(final Monster monster) {
        later(new Runnable() {
            @Override
            public void run() {
            heroView.play(CharacterSprite.DEATH);
            Sfx.play(Sfx.HERO_DOWN, 1f, 1f);
            heroView.body.addAction(Actions.sequence(Actions.delay(0.5f), Actions.alpha(0.5f, 0.4f)));
            monsterView.body.addAction(Actions.fadeOut(0.6f));
            if (monster.isBoss()) {
                log(Lang.format("log.bossDefeat", Names.monster(monster)));
                bossButton.setVisible(true);
            } else {
                log(Lang.format("log.heroDown", Names.monster(monster)));
            }
            refreshTopBar();
            }
        });
    }

    @Override
    public void onHeroRevived(Hero hero) {
        heroView.play(CharacterSprite.IDLE);
        heroView.body.addAction(Actions.fadeIn(0.3f));
        heroView.setHp(hero);
        monsterView.clear();
        log(Lang.get("log.revive"));
    }

    // ------------------------------------------------------------------
    // UI 보조
    // ------------------------------------------------------------------

    /** 배속만큼 화살표를 보여 준다. 숨긴 화살표는 칸도 비워 남은 화살표가 가운데 오게 한다. */
    private void refreshSpeedButton() {
        // 동작 재생 속도도 배속에 맞춘다
        if (heroView != null && heroView.sprite != null) heroView.sprite.setTimeScale(SPEEDS[speedIndex]);
        if (monsterView != null && monsterView.sprite != null) monsterView.sprite.setTimeScale(SPEEDS[speedIndex]);
        for (int i = 0; i < speedArrows.length; i++) {
            boolean on = i <= speedIndex;
            speedArrows[i].setVisible(on);
            speedButton.getCell(speedArrows[i]).size(on ? 18f : 0f, 27f).padLeft(on && i > 0 ? 2f : 0f);
        }
        speedButton.invalidateHierarchy();
    }

    private void refreshTopBar() {
        StageProgress p = state.progress;
        chapterLabel.setText(p.label());
        stageLabel.setText(Lang.getOr("chapter." + (MonsterTable.themeIndex(p.getChapter()) + 1), Lang.format("hud.stage", p.label())));
        showChapterBackground(p.getChapter());
        String wave;
        if (p.isFarming()) wave = Lang.get("hud.farming");
        else if (p.isBossWave()) wave = Lang.get("hud.bossWave");
        else wave = Lang.format("hud.wave", p.getWave(), StageProgress.WAVES_PER_STAGE);
        waveLabel.setText(wave);
        goldLabel.setText(Format.gold(state.getGold()));
        bossButton.setVisible(p.isFarming());
    }

    private void log(String line) {
        logLines.add(line);
        while (logLines.size > LOG_LINES) logLines.removeIndex(0);
        // 최근 항목이 아래 줄
        for (int i = 0; i < LOG_LINES; i++) {
            int index = logLines.size - LOG_LINES + i;
            logLabels[i].setText(index >= 0 ? logLines.get(index) : "");
        }
    }

    /** 피격 효과: 몸체가 붉게 번쩍이고 피해 숫자가 떠오른다. {@code recoil}이면 움찔 동작과 타격 이펙트도. */
    private void hit(FighterView view, int damage, boolean recoil) {
        flash(view);
        popup(view, "-" + damage, Palette.HP);
        if (recoil) {
            view.hurt();
            effect("hit_slash", view, 0.5f, 1f, view == heroView);
        }
    }

    /** 이펙트를 전투원 위에 띄운다. {@code heightRatio}는 그림 높이 대비 위치(0 = 발, 1 = 머리 위). */
    private void effect(String name, FighterView view, float heightRatio, float extraScale, boolean flip) {
        SpriteLibrary.Effect fx = game.getSprites().effect(name);
        if (fx == null) return;
        view.anchor(heightRatio, tmp);
        stage.addActor(new EffectActor(fx, tmp.x, tmp.y, extraScale, flip));
    }

    private void flash(FighterView view) {
        // 색(RGB)만 바꾸고 투명도는 건드리지 않는다. 예전에는 Actions.color로 알파까지 덮어써서,
        // 몬스터가 나타나며 페이드인하는 도중(알파≈0)에 맞으면 알파 0으로 되돌아가 몬스터가 사라졌다.
        view.body.addAction(new TemporalAction(0.25f) {
            @Override
            protected void update(float percent) {
                float k = percent < 0.2f ? percent / 0.2f : 1f - (percent - 0.2f) / 0.8f;
                Color c = getActor().getColor();
                c.r = 1f;
                c.g = 1f - 0.55f * k;
                c.b = 1f - 0.55f * k;
            }
        });
    }

    private void popup(FighterView view, String text, Color color) {
        popup(view, text, color, 0.75f);
    }

    /** @param height 몸체 높이 대비 떠오르기 시작하는 위치(0~1). 스킬은 라운드 피해와 겹치지 않게 더 높게 띄운다. */
    private void popup(FighterView view, String text, Color color, float height) {
        Label label = new Label(text, skin, "field-large");
        label.setColor(color);
        label.pack();
        view.anchor(height, tmp);
        label.setPosition(tmp.x - label.getWidth() / 2f, tmp.y);
        label.setTouchable(Touchable.disabled);
        label.addAction(Actions.sequence(
            Actions.parallel(Actions.moveBy(0f, 70f, 0.7f), Actions.fadeOut(0.7f)),
            Actions.removeActor()));
        stage.addActor(label);
    }

    /** 픽셀아트 그림(아이콘 등). 에셋이 아직 없으면 빈 칸이 된다. */
    private Image image(String name) {
        Image image = skin.has(name, TextureRegion.class) ? new Image(skin.getDrawable(name)) : new Image();
        image.setScaling(Scaling.fit);
        return image;
    }

    /** 전투 배경 그림을 챕터 테마에 맞춘다. 테마가 바뀔 때만 바꾸고, 살짝 어두워졌다 밝아지며 넘어간다. */
    private void showChapterBackground(int chapter) {
        int theme = MonsterTable.themeIndex(chapter);
        if (theme == shownTheme) return;
        boolean first = shownTheme < 0;
        shownTheme = theme;
        final TextureRegionDrawable next = backgroundFor(theme);
        if (next == null) return;
        if (first) {
            fieldBg.setDrawable(next);
            return;
        }
        fieldBg.clearActions();
        fieldBg.addAction(Actions.sequence(
            Actions.color(new Color(0.2f, 0.2f, 0.25f, 1f), 0.25f),
            Actions.run(new Runnable() {
                @Override
                public void run() {
                    fieldBg.setDrawable(next);
                }
            }),
            Actions.color(Color.WHITE, 0.35f)));
    }

    /** 테마(0부터)의 배경: bg-N이 없으면 기본 초원. 그림 아랫부분을 전투 영역 비율로 잘라 4배로 그린다. */
    private TextureRegionDrawable backgroundFor(int theme) {
        String name = "bg-" + (theme + 1);
        if (!skin.has(name, TextureRegion.class)) name = "battle-bg";
        if (!skin.has(name, TextureRegion.class)) return null;
        TextureRegion full = skin.getRegion(name);
        int w = full.getRegionWidth();
        int h = Math.min(full.getRegionHeight(), Math.round(w * FIELD_HEIGHT / FIELD_WIDTH));
        return new TextureRegionDrawable(new TextureRegion(full, 0, full.getRegionHeight() - h, w, h));
    }

    private void showTab(int index) {
        currentTab = index;
        tabBody.setActor(panels[index]);
        panels[index].refresh();
        tabScroll.layout();
        tabScroll.setScrollY(0f);
        tabScroll.updateVisualScroll();
    }

    /** 전투원 한 명의 표시: 이름, 캐릭터 그림(없으면 색 상자), 체력 막대(숫자는 막대 위에). */
    private final class FighterView {
        final Table root = new Table();
        final Container<Actor> bodyBox = new Container<Actor>();
        final Image placeholder;
        final boolean faceRight;
        /** 그림이 있는 캐릭터면 그 스프라이트(재사용), 없으면 null. */
        CharacterSprite sprite;
        /** 지금 화면에 보이는 몸체(스프라이트 또는 색 상자). 번쩍임·사라짐은 이것에 건다. */
        Actor body;
        final Label name = new FitLabel("", skin, "field");
        final Gauge hp;

        FighterView(Color color, boolean faceRight) {
            this.faceRight = faceRight;
            placeholder = new Image(skin.newDrawable("round", color));
            body = placeholder;
            bodyBox.setActor(placeholder);
            bodyBox.bottom().size(170f, 200f);
            hp = new Gauge(skin, faceRight ? "hero" : "hp", UiSkin.SCALE, 10);
            name.setAlignment(Align.center);
            name.setEllipsis(true);
            root.add(name).width(NAME_WIDTH).padBottom(2f).row();
            root.add(bodyBox).size(BODY_WIDTH, BODY_HEIGHT).padBottom(4f).row();
            root.add(hp).size(200f, hp.getPrefHeight());
        }

        /** 그림 id로 몸체를 바꾼다. 그림이 없으면 색 상자. */
        void setCharacter(String id) {
            SpriteLibrary.CharacterAnims anims = game.getSprites().character(id);
            Actor next;
            if (anims == null) {
                next = placeholder;
                bodyBox.size(170f, 200f);
            } else {
                if (sprite == null) sprite = new CharacterSprite(anims, faceRight);
                else if (sprite.getAnims() != anims) sprite.setAnims(anims);
                else sprite.play(CharacterSprite.IDLE);
                sprite.setTimeScale(SPEEDS[speedIndex]);
                next = sprite;
                bodyBox.size(BODY_WIDTH, BODY_HEIGHT);
            }
            if (body != next) {
                next.getColor().set(body.getColor());
                body = next;
                bodyBox.setActor(next);
            }
        }

        void play(String anim) {
            if (body == sprite && sprite != null) sprite.play(anim);
        }

        /** 공격 동작 시작(튀어 나가기는 타격 순간에 {@link #lunge}). */
        void attack() {
            if (body == sprite && sprite != null) sprite.play(CharacterSprite.ATTACK);
        }

        void lunge(float direction) {
            if (body == sprite && sprite != null) sprite.lunge(direction * 30f);
        }

        /** 공격 시작부터 타격 순간까지(배속 1 기준). */
        float impactSeconds() {
            return body == sprite && sprite != null ? sprite.getAnims().attackImpactSeconds() : 0f;
        }

        void hurt() {
            if (body == sprite && sprite != null) {
                sprite.play(CharacterSprite.HURT);
                sprite.shake();
            }
        }

        /** 몸체 위의 한 점(가운데, 그림 높이 × ratio)을 무대 좌표로. */
        float spriteHeight() {
            return body == sprite && sprite != null ? sprite.getSpriteHeight() : body.getHeight();
        }

        void anchor(float ratio, Vector2 out) {
            float h = body == sprite && sprite != null ? sprite.getSpriteHeight() : body.getHeight();
            body.localToStageCoordinates(out.set(body.getWidth() / 2f, h * ratio));
        }

        void setName(String text, Color color) {
            name.setText(text);
            name.setColor(color);
        }

        void setHp(Combatant c) {
            setHp(c, false);
        }

        /** @param instant 새 몬스터처럼 막대가 바로 그 값이어야 할 때 */
        void setHp(Combatant c, boolean instant) {
            hp.setValue(c.getMaxHp() <= 0 ? 0f : (float) c.getHp() / c.getMaxHp(), instant);
            hp.setText(c.getHp() + " / " + c.getMaxHp());
        }

        void clear() {
            body.getColor().a = 0f;
            name.setText("");
            hp.setValue(0f, true);
            hp.setText("");
        }
    }
}
