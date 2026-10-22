# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

MyFantasy (in-game title: 굴려라 용사여! / Roll On, Hero!) is a portrait-mode mobile idle RPG whose rules are inspired by Tunnels & Trolls — do not use the "Tunnels & Trolls" name in any player-facing text (trademark), built with libGDX (gdx 1.14.2, generated with gdx-liftoff). Package root: `com.trialmobile.myfantasy`. UI text is Korean.

## Commands

Use the Gradle wrapper (`./gradlew`). Logging is set to `quiet` in `gradle.properties`, so add `--info` or `--console=plain` if you need output while debugging the build. Daemon is off by default (`--daemon` to enable).

- Run desktop app: `./gradlew lwjgl3:run` (working dir is `assets/`)
- Runnable jar: `./gradlew lwjgl3:jar` → `lwjgl3/build/libs`
- Compile only the shared code: `./gradlew core:compileJava`
- Build everything: `./gradlew build` (the `android` module needs an Android SDK configured in `local.properties`; `ios` needs RoboVM/macOS)
- Tests: `./gradlew core:test` (JUnit 4, `core/src/test/java`). Run one with `./gradlew core:test --tests 'com.trialmobile.myfantasy.rules.CombatTest'`.
- Android lint: `./gradlew android:lint`

Prefix any task with a module name (`core:`, `lwjgl3:`, `android:`, `ios:`) to scope it.

## Architecture

- `core` — all game logic, shared by every platform. Platform-independent code only (no LWJGL/Android/RoboVM APIs).
- `lwjgl3` — desktop launcher (`Lwjgl3Launcher`, window config) plus `StartupHelper`, which relaunches the JVM with `-XstartOnFirstThread` on macOS. Primary platform for development.
- `android`, `ios` — thin launchers that instantiate `MainGame`.

Each launcher just constructs `new MainGame()` with platform config; put new features in `core`.

**Java 8 target**: `core` and `lwjgl3` compile with source/target 8 (`release 8`), so do not use newer language features (`var`, records, switch expressions, `List.of`, etc.).

**Assets**: everything lives in the root `assets/` folder, which is added to the desktop/android resource paths. Load with internal paths relative to `assets/` (e.g. `new Texture("libgdx.png")`). `assets/assets.txt` is regenerated on every `processResources` by the `generateAssetList` task — don't edit it by hand. `assets/ui/uiskin.*` is a Scene2D skin.

**Libraries available in `core`** (declared as `api` in `core/build.gradle`): Ashley (ECS), gdx-freetype, VisUI, and the gdx-lml family (LML UI markup, gdx-autumn DI, gdx-autumn-mvc, gdx-kiwi utilities). None are wired up yet; `lwjgl3` additionally pulls in `gdx-autumn-fcs` for desktop classpath scanning.

Library versions are centralized in `gradle.properties`.

## Game skeleton (core)

- `MainGame` extends `Game`; owns the shared `SpriteBatch`, `AssetManager`, `Fonts` and `Skin`. Switch screens with `game.changeScreen(next)` — it defers the swap via `postRunnable` and disposes the old screen, so it is safe to call from `render`/actions.
- `GameConfig`: virtual resolution 720x1280 (portrait). Desktop window is 450x800; Android `sensorPortrait`; iOS portrait only.
- `screen/BaseScreen`: `Stage` + `ExtendViewport(720,1280)` on the shared batch, a fill-parent `root` Table, fade-in on show. Override `update(delta)` for per-frame logic.
- Flow: `LoadingScreen` (queue assets in `queueAssets`) → `TitleScreen` (tap to start) → `BattleScreen`. `BattleScreen` layout, top to bottom: HUD (`panel-dark`: shield badge with stage, wave text, speed button, gold `pill`), battle field 672×560 (background, fighters with name + HP bar, a 2-line log band, five `ui/SkillSlot` rings showing cooldown with the skill level (or unlock level, dimmed) in small text under each ring), tab content in a `ScrollPane` on `panel` (grows to fill the remaining height), bottom tabs 영웅/장비/스킬/상점. `BattleScreen` implements `BattleSystem.Listener` and only renders events; tab content is a `ui/TabPanel` (`HeroPanel`, `EquipmentPanel`, `SkillPanel`, `ShopPanel`) refreshed on battle/shop events.
- `MainGame` also holds the `PlayerState` (hero, equipment, owned items, gold, stage progress) so it survives screen changes, and owns saving (see below).
- `assets/Fonts`: FreeType **Galmuri11** pixel font (regular + bold, `assets/fonts/`, SIL OFL) generated in **incremental** mode so any Hangul glyph is rasterized on first use, `mono` + Nearest. Glyphs are baked at the multiple of 12 px (Galmuri's grid) closest to `size * renderScale`, then scaled by 1/renderScale, so they stay crisp on screen. Fonts: `small` 22 / `normal` 28 / `large` 40 (bold) / `title` 72 (bold + outline) plus outlined `smallOutline/normalOutline/largeOutline` for text over the battle background. Generators must stay alive while fonts are used; `Fonts` owns and disposes them.
- Look: cozy pixel art — wooden frames with gold corners, cream parchment panels, glossy candy buttons. Panels/buttons/tabs/slots were generated with PixelLab Pro; picked originals are in `tools/ui-assets/raw2/` and `tools/ui-assets/build_ui2.py` trims them, flattens the nine-patch stretch areas and recolours the one button shape into every colour. `assets/ui/README.md` lists every file and its nine-patch insets. Draw pixel art with Nearest filtering at integer scale (UI ×3, bars ×2).
- `ui/UiSkin`: Skin built in code. Nine-patches (×3) `panel`, `panel-dark`, `frame` (transparent centre), `ribbon`, `pill`, `tab`/`tab-on`, `btn-<blue|green|gold|rose|purple|off>[-down]`; `bar-<back|hp|skill|gold|green>` (×2); regions `slot`, `badge`, `icon-<coin|hero|equipment|skill|shop|flag>`, `skill-<Skill.id>`, `battle-bg`; tintable `round`/`white`. Missing region files are skipped. Labels `default/small/dim/large/gold/title` = dark text for cream panels, `ink` = white-based (tint with setColor), `field-small/field/field-large` = light outlined text for dark/battle backgrounds. TextButton `default`/`small` (gold), `accent` (green, buy/upgrade), `blue`, `rose`, `tab`; disabled = grey `off`. ProgressBar `hp/loading/exp/cooldown`, ScrollPane `default`. Fonts are not registered in the skin.
- **Layout rule (nothing may overflow the screen)**: `BattleScreen` places every row at a fixed `COLUMN_WIDTH` (696) inside the 720-wide minimum world. Tab panels never scroll themselves (the screen wraps them in one ScrollPane) and must stay within the given width: build text with `TabPanel.line()` (ellipsis) or `TabPanel.wrapped()` (wrap), give text cells `minWidth(0)`, and keep row buttons at `BUTTON_WIDTH`×`BUTTON_HEIGHT`. Long content scrolls vertically.
- HUD details: the speed button shows one `arrow` (drawn in code by `UiSkin`) per speed step (▶ / ▶▶ / ▶▶▶). Gold is shown with `Format.gold`: plain below 1,000, otherwise 3 significant digits + unit letters (1.23A = thousands, B = millions … Z, then AA, AB; truncated, never rounded up). There is no "G" suffix — a coin icon marks gold, so price buttons use `ui/PriceButton` (`setPrice(verb, gold)` shows verb + coin + amount, `setPlain(text)` for non-price states).
- `ui/Gauge`: gold-framed bar used for HP (hero `hero` green, monster `hp` red, 10 tick marks) and the hero-tab exp bar (`exp`). Drops are instant with a pale trail that drains after 0.35 s; rises fill smoothly; a shine sweeps across; text is drawn on top. Use `setValue(v, true)` when a new monster appears so the bar does not animate from the previous one.
- `BattleScreen` layout (virtual 720x1280, root pad 24): top bar panel (flag + stage, speed button, coin + gold) → battle field `Stack` 672x520 (`battle-bg` bottom 168x130 at ×4, fighters, `frame` on top) → panel with skill cooldown strip (icons) + 3-line log → tab content panel → icon tab buttons. Fighters are animated sprites (see below). Title/Loading screens use `BaseScreen.addBackdrop()`.
## Characters, animation, effects

- Art: PixelLab v3 characters (side view, 64px; bosses 96px) and PixelLab VFX. Converted by `tools/sprites/convert_chars.py` / `convert_fx.py` (see `tools/sprites/README.md`; `chars_list.json` / `fx_list.json` hold the PixelLab ids).
- `assets/sprites/<id>/<anim>.png` horizontal strips (all anims of a character share one frame size, feet at the bottom) + `assets/sprites/index.json` (frame size, scale ×3, facesRight, per-anim frames/duration/loop). Anims: `idle` (loop), `attack`, `hurt`, `cast` (hero only), `death` (holds last frame). Sprite id = `Monster.getKindId()` (`MonsterTable` kinds, e.g. `goblin`, `goblin_chief`); hero = `hero`. The hero faces east (side view); monsters face south-west (3/4 view, looking toward the lower left, i.e. at the hero and the viewer). `tools/sprites/convert_chars.py` strips the `_2` suffix PixelLab adds when an animation name exists in several direction groups.
- `assets/fx/<name>.png` + `assets/fx/index.json` (frame size, duration, scale, pivot). Names used by `BattleScreen`: `hit_slash`, `spell_bolt`, `spell_heal`, `spell_vorpal`, `spell_fire`, `spell_hellbomb`, `level_up`, `defeat_poof`.
- `anim/SpriteLibrary` (owned by `MainGame`, `getSprites()`) loads both indexes; missing files are skipped and the screen falls back to the coloured placeholder box / no effect. `anim/CharacterSprite` (Widget) plays one-shot anims then returns to idle, flips to face the requested side, and does draw-offset motion (`lunge`, `shake`, `slideIn`) that survives table re-layout. `anim/EffectActor` plays once at a pivot and removes itself.
- `BattleScreen` wiring: round winner attacks + lunges, loser plays hurt + `hit_slash` (spite damage on the winner only flashes); skills → hero `cast` + matching effect; kill → monster `death` + `defeat_poof` + fade; level-up → `level_up` at the hero's feet; new monster slides in from the right.

## Chapters, monsters, backgrounds

- 10 chapter themes in `battle/MonsterTable` (`NORMALS[theme]` = 4 normal kinds, `BOSSES[theme]` = 1 boss; `THEMES = BOSSES.length`, `themeIndex(chapter) = (chapter-1) % THEMES`, so chapter 11 wraps to theme 1 with stronger stats). Themes: 1 초원(goblin…) · 2 묘지 · 3 늪 · 4 설원(yeti, ice_golem, frost_harpy, snowman / frost_giant) · 5 사막(mummy, gnoll, sand_bandit, scorpion_man / jackal_lord) · 6 화산(fire_imp, salamander, magma_golem, demon_brute / efreet) · 7 버섯 숲(shroomling, wood_spirit, toad_warrior, thorn_dryad / spider_queen) · 8 난파선 해안(crab_warrior, merfolk, drowned_pirate, shark_man / sea_king) · 9 어둠의 성(gargoyle, bat_fiend, cultist, wraith / vampire_lord) · 10 용의 봉우리(drake_warrior, dragon_mage, wyvern_knight, obsidian_golem / dragon_king).
- Every kind id needs: sprites in `assets/sprites/<id>/` + `index.json` entry (PixelLab id in `tools/sprites/chars_list.json`, direction south-west), and `monster.<id>` in both `strings*.properties` (`StringsTest` checks names). Chapter names are `chapter.<N>`.
- Backgrounds: `assets/ui/bg/chapter_<N>.png` (168x148, PixelLab `create_image_pro` with the original `battle_bg` as style reference, open ground in the lower half). `UiSkin` registers them as `bg-1..10`; `BattleScreen.showChapterBackground` cross-fades when the theme changes; the HUD shows the chapter name.
- Hit flash only tints RGB (custom `TemporalAction`), never alpha — overwriting alpha during the spawn fade-in used to leave monsters invisible.

## Dice throw effect, x2 boost, rewarded ads

- Every combat round both sides' dice are thrown on screen (`anim/DiceThrow`, one per side, in `BattleScreen.diceLayer` above the fighters): dice arc out of the fighter's hand spinning and flickering faces with a sparkle trail, bounce and settle on the real faces (`DiceRoll.face(i)`; `Dice.roll` now keeps the faces), sixes flare gold, then the hit totals pop up beside the VS label ("11 VS 18", winner gold and bigger). At most 6 dice are thrown; more show as "+ND". Dice art is drawn in code by `ui/DiceArt` (17x17 faces in styles `hero`/`monster`/`gold`, plus `sparkle`, `glow`, `glow-soft`, `die-shadow`, all in the skin). Effects run at battle speed (`setTimeScale`).
- `BattleScreen` shows rounds as roll → land → hit: `onRound` throws dice and sets `visualDelay` (= `DiceThrow.landTime / speed`); `later(...)` defers the round's hit/HP visuals and any defeat/level-up/stage/skill visuals raised in the same frame. The battle logic itself is not delayed. `BattleSystem.SPAWN_DELAY_MS` is 1000 so the death animation finishes before the next monster.
- x2 boost (`battle/PowerBoost`, in `PlayerState.boost`, saved since save v3): 15 min of x2 hit totals (`Combat.resolveRound(..., aMultiplier)` via `DiceRoll.multiplied`, spite sixes unchanged), x2 attack-skill damage (`Spells.cast(..., damageMultiplier)`) and x2 gold; usable again 2 h after activation. Uses wall-clock time through `BattleSystem.Clock` (tests swap it); `fastForward` replays the offline period with simulated time so only the boosted part of the absence is doubled. Restored times are clamped to savedAt + duration/cooldown.
- UI: `ui/BoostButton` at the top of the field's centre column (gold + pulsing glow = ready, green + mm:ss = active, grey + h:mm:ss = cooldown, grey "ad loading" when the ad is not ready). While active the hero has `anim/BoostAura` + `anim/RisingSparkle`, hero dice are gold, and activation shows a banner.
- Rewarded ads: `ads/RewardedAds` (`isReady()`, `show(placement, Callback)`), default `RewardedAds.NONE` grants instantly. To add AdMob etc., implement it in the Android/iOS launcher and call `game.setRewardedAds(impl)`; callbacks may come from any thread (the screen re-posts them with `Gdx.app.postRunnable`). Placement id used: `boost`.

- Item icons: `assets/ui/items/<Item.id>.png` (32x32 PixelLab Pro, style ref = skill icon), registered as `item-<id>`. `ui/ItemIcon` = `slot` frame + icon at x2 + gold "+N" enhance badge; empty slot shows the slot's first item as a dark silhouette, locked shop items are greyed. Used in the equipment tab (84px) and shop rows (72px). A new item needs its icon file (missing files are skipped).

## Rebirth (prestige)

- `battle/Rebirth` (pure Java, `PlayerState.rebirth`, save v4: souls, totalSouls, rebirths, bestStage, perkLevels). `BattleSystem.spawn` records the deepest stage reached this life (`recordStage`). Unlocked at stage 2-1 (`UNLOCK_STAGE_INDEX` 10). Soul stones earned = floor((bestStageIndex / 5)^1.6): 2-1 → 3, 5-1 → 27, 10-1 → 101.
- `Rebirth.reborn(old)` builds a fresh `PlayerState` (starter hero with the same name, level 1, starter gear, 0 gold, skills reset, stage 1-1) and carries over souls (+earned), totals, rebirth count, perk levels and the x2 boost timers; the Hero Soul attribute bonus is added to the starting attributes. `MainGame.rebirth()` swaps the state, saves and reopens `BattleScreen` (start log "환생했습니다! N번째 삶").
- Perks (`Rebirth.Perk`, cost = baseCost x next level, paid in soul stones, permanent): MIGHT hit total +5%/lv (`DiceRoll.withBonus`, via `Combat.resolveRound(..., aBonusPercent)`), FORTUNE gold +10%/lv, WISDOM adventure points +10%/lv (both in `BattleSystem.resolveOutcome` with `Rebirth.withBonus`), HEROIC all attributes +1/lv (applied immediately on purchase and to every new life).
- UI: 5th tab `ui/RebirthPanel` (icon `icon-rebirth`, soul stones `icon-soul`): soul count + rebirth count, description, best stage this life, "rebirth now: +N", purple rebirth button → `ui/ConfirmDialog`; perk rows use `PriceButton(skin, "purple-small", "icon-soul")`. Button styles `purple`/`purple-small` added to `UiSkin`.

## Sound effects

- `assets/sfx/<name>.wav` (22050 Hz mono 16-bit; WAV because the iOS backend cannot play OGG) are synthesised by `tools/sfx/make_sfx.py` (numpy, seeded per name; `--preview` writes `tools/sfx/preview.wav`). Edit a `sfx_*` function and rerun to change a sound.
- `audio/Sfx` (static facade, loaded/disposed by `MainGame`): `Sfx.play(Sfx.HIT)` uses the per-sound mix volume from `MIX` and a random ±5% pitch; the same sound is skipped if replayed within 45 ms (3x speed). On/off is stored in the settings prefs (`sfx.on`) and toggled at the bottom of the hero tab. Missing files or no audio device = silent, never a crash.
- Hooks: dice throw → `dice_shake`, landing → `dice_land` (+ `dice_six` if any 6); round visuals → `hit` (monster damaged) / `hurt` (hero damaged); kill → `monster_die` + `coin`; `hero_down`, `level_up`, `stage_clear`, `boss_appear`; each skill → `spell_<id>`; boost → `boost_on`; rebirth → `rebirth`; UI: tabs `ui_tab`, buy `ui_buy`, enhance/skill/perk upgrade `ui_upgrade`, other buttons `ui_click`. `total_win` exists but is unused.

## Fitting English text

- English uses `assets/fonts/Galmuri11-Condensed.ttf` for the regular (non-bold) faces: `Fonts.load(renderScale, condensed)`, chosen by `MainGame` from `Lang.language()` (anything but `ko`). Latin text is about a third narrower, so names like "Shipwreck Coast" / "Take That, You Fiend!" fit at full size. Changing language calls `MainGame.reloadForLanguage()`, which rebuilds fonts + skin and the battle screen, then disposes the old ones.
- `ui/FitLabel`: one-line label that steps down to the next smaller pre-baked style when the text does not fit (large → default → small, field-large → field → field-small, ink → ink-small, gold → gold-small), and only then uses an ellipsis. Pixel fonts are never scaled. `TabPanel.line()` returns a FitLabel; the HUD stage name and fighter names use it too. Skill names in the skill tab wrap to two lines.
- Equipment rows show just the item name (the icon shows the slot). Keep English strings short where possible (chapter names were shortened: Haunted Graves, Scorched Desert, Shadow Castle).

## Attack animation timing

- `tools/sprites/attack_meta.py` (run after `convert_chars.py`) writes `impact` and `recover` into each `attack` entry of `assets/sprites/index.json`. `impact` is when the weapon is fully extended toward the opponent: the frame with the furthest reach (monsters reach left, the hero right), as a fraction of the original clip. `recover` is true when the clip ends away from the idle pose. Wrong guesses go in `tools/sprites/attack_overrides.json` (`{"name": fraction}`).
- `SpriteLibrary`: `recover` appends rewind frames (..., n-3, n-5, ..., 0) so the swing returns to idle instead of snapping. `CharacterAnims.attackImpactSeconds()` = original clip length x `impact`.
- `BattleScreen.showRound` starts the attack animation when the dice land, then shows lunge, damage numbers, hit effect, sound and HP bars at the impact moment (capped at 0.4 s, divided by battle speed). `visualDelay` also grows by that time, so kill/level-up visuals wait for the hit. `CharacterSprite.setTimeScale` plays animations at battle speed. `BattleSystem.SPAWN_DELAY_MS` is 1300.
- Weak or broken attacks were regenerated as 10-frame PixelLab v3 clips named `strike` ("wind up, strike toward the lower left, return to the starting pose"). `convert_chars.py` uses `strike` as `attack` when present and caps an attack clip at about 0.6 s. Regenerated: cultist, bat_fiend, crab_warrior, demon_brute, drowned_pirate, ghoul, giant_rat, shark_man, snowman, spider_queen, wraith, yeti, jackal_lord, troll_king, mummy, wood_spirit, fire_imp.
- Second pass (2026-10-20): `chars_list.json` can name the clip to use as `"attack": "<animation>"` (default `strike`). `strike2`: sea_king (trident now points at the hero), gnoll, orc_warrior, orc_scout, thorn_dryad, vampire_lord (bigger motions), mummy (no shrinking), jackal_lord, troll_king, spider_queen (end in the idle pose). `strike3`: skeleton_knight (thrust, no turning away), cultist (no design drift), kobold (spearhead toward the hero). Also `strike2`: shroomling, werewolf, magma_golem. Old clips stay on PixelLab untouched. `hurt`, `death` and `idle` can be swapped the same way (`"hurt": "hurt2"`); fire_imp uses `hurt2` (the old hurt flashed white/brown).
- `footY` in `index.json` (written by `attack_meta.py`): how many source pixels the idle feet sit above the frame bottom (other anims such as death can reach lower). `CharacterSprite` draws that much lower so every character stands on the same ground line.
- `attack_overrides.json` takes `{"name": impact}` or `{"name": {"impact": x, "recover": true}}`. Current: crab_warrior, ogre, obsidian_golem, wraith, yeti, vampire_lord, magma_golem (impact) and sea_king, skeleton_knight (recover).
- Extracting archives into this folder needs `tar --overwrite` (deleting files is not allowed here).

## Combat rules (core, pure Java — no libGDX imports, keep it that way so it stays unit-testable)

- `rules/`: Tunnels & Trolls mechanics. `Dice` (d6 only; seedable; `rollTaro` 3d6 triples-add-and-roll-over, `rollDaro` 2d6 doubles-add-and-roll-over), `Attributes` (STR CON DEX SPD LK IQ WIZ CHR; personal adds = +1 per point over 12 / −1 per point under 9 in STR, LK, DEX, SPD), `Item`/`ItemSlot`/`Equipment` (weapon dice+adds, armor & shield hits absorbed per round, STR/DEX requirements, per-slot enhancement levels that survive swapping items; no weapon = 1D6 fists), `ItemCatalog` (all items; ids are persisted — never rename them), `Hero` (CON = max HP, AP & level), `Monster` (Monster Rating: dice = initial MR/10+1, adds = ceil(current MR/2), MR is HP).
- `Combat.resolveRound`: both sides roll; the lower Hit Point Total takes the difference minus armor. Spite damage (7th ed.): each 6 rolled deals 1 through armor — the loser's 6s hit the winner, the winner's 6s are a floor on the damage it deals; on a tie only spite is exchanged.
- Balance knobs: `rules/Progression` (AP curve 30·L^1.6, level-up STR+1 CON+2 DEX+1 LK+1) and `battle/MonsterTable` (MR = 9·1.10^stageIndex·(1+0.06·(wave−1)), boss ×1.8, ±10%; AP reward = MR, gold ≈ MR/2..). Starter hero: STR13 CON14 DEX12 SPD12 LK12, 숏소드 3D6, 누비 갑옷 3. Item prices/requirements in `ItemCatalog`; enhancement in `shop/Shop` (cost base 40/30/25 × 1.13^level; weapon +4 adds, armor/shield +1 hit per level).
- `battle/BattleSystem`: event-driven idle loop on an **integer millisecond clock** (round every 900 ms, speed ×1–3). Each `advance` jumps to whichever comes first — the next step (spawn/round/revive) or the next skill cast (skills win ties) — so `update()` in frames and `fastForward()` give identical results (tested). Keep time in integer ms; floating time breaks that equivalence. Stage = 5 waves, wave 5 is the boss; 10 stages per chapter. Kill → AP/gold, heal 20%; stage clear → full heal. Death → revive after 2 s and `StageProgress.retreatAfterDefeat()`: farm waves before the one you died on (`farmLimit`), or the previous stage if you died on wave 1. Farming ends on level-up or when the player taps 다시 도전 (`challengeBoss()`).
- Skills (`rules/Skill`, `SkillBook`, `Spells`, `SkillCast`): 5 T&T spells — 받아라 이 악마야! (IQ dmg, 6 s, Lv1), 아이고 가엾어라 (heal WIZ×1.5 when ≤60% HP, 12 s, Lv3), 보팔 블레이드 (weapon dice ×2 for 4 s, 15 s, Lv5), 폭발의 힘 (dice + WIZ dmg, 10 s, Lv8), 헬봄 버스트 ((IQ+WIZ)×2 dmg, 30 s, Lv15). Auto-unlock at hero level (skill Lv 1), upgrade with gold (`Shop.upgradeSkill`, base × 1.15^(lv−1)); power ×(1 + 0.3·(lv−1)). Skill damage hits MR directly (no armor/spite). Level-ups now also give IQ+1, WIZ+1. Cooldowns are not saved. **Append new skills at the end of the enum** (save data stores levels by ordinal).
- Simulated pacing (greedy buy/enhance/skill-upgrade policy): ~2-3 at 5 min, ~3-5 at 1 h, ~5-3 at 2 h, ~6-1 at 8 h; ~270 deaths in 8 h (mostly at walls, ~1–2 per level).

## Shop, saving, offline rewards

- `shop/Shop` (pure Java): `status(item)` → EQUIPPED/OWNED/OWNED_LOCKED/LOCKED/AFFORDABLE/TOO_EXPENSIVE; `buy` (also equips), `equip`, `enhanceCost`/`enhance`. Items you lack STR/DEX for cannot be bought.
- `save/SaveData` (plain public fields, versioned; v2 added `skillLevels` and `farmLimit`, v1 saves migrate) ↔ `save/SaveMapper` (pure Java, tolerant of unknown/broken values) ↔ `save/SaveManager` (libGDX `Preferences` "com.trialmobile.myfantasy.save", key "save", JSON). Bump `SaveData.CURRENT_VERSION` and migrate in `SaveMapper` when the format changes.
- `MainGame` saves every 30 s, on `pause()`, on `dispose()`, and after level-ups, stage clears and shop actions.
- Offline: time away (from `savedAt` on launch, or since `pause()` on resume) accumulates in `MainGame`; `BattleScreen.update` consumes it and calls `BattleSystem.fastForward(seconds)`, which runs the real combat loop silently and returns an `OfflineReport` shown by `ui/OfflineRewardDialog`. Ignored under 60 s. The cap grows with progress: `OfflineReport.maxHoursFor(bestStageEver)` = 2 h + 1 h per chapter reached (1-x 2 h, 5-x 6 h, 10-x 11 h, 11-x+ 12 h = `MAX_HOURS`). `Rebirth.bestStageEver` is the deepest stage across all lives (survives rebirth, save v5; v4 saves use this life's best). `MainGame.addOfflineTime` clamps with `PlayerState.offlineCapSeconds()`; `BattleScreen.applyOfflineReward` reads the cap before fast-forwarding and the dialog shows it (gold when the cap was hit). The hero tab shows the current cap.

## Languages (i18n)

- All on-screen text lives in `assets/i18n/strings.properties` (English, the fallback) and `strings_ko.properties` (Korean); both files must have the same keys (`i18n/StringsTest` checks this and that every skill, item, slot, attribute and monster has a name). Placeholders are `{0}`, `{1}` (simple formatter, no quote escaping); format numbers/gold in code first. Values lose leading spaces, so add separators in code.
- Keep `strings_en.properties` (empty on purpose): without it, libGDX I18NBundle falls back to the device locale when "en" is requested, so English showed Korean on a Korean Mac.
- `i18n/Lang`: `init()` at startup (saved choice in Preferences `com.trialmobile.myfantasy.settings` → device language: Korean device → ko, otherwise en), `get(key)`, `format(key, args)`, `set(code)`. To add a language: add `strings_<code>.properties`, add the code to `Lang.LANGUAGES` and a name in `Lang.nativeName`.
- `i18n/Names`: display names for rule objects by id (`skill.<id>.name/short`, `item.<id>`, `monster.<kindId>`, `slot.<SLOT>`, `attr.<ATTR>`), plus item stat/requirement text, skill effect text and durations. The Korean `label`/`name` fields in `rules/` are only fallbacks — UI code must never show them directly. Never put user-facing text in `rules/` or `battle/` (they stay pure Java).
- The language buttons are at the bottom of the Hero tab. Text is set when widgets are built, so a language change saves and rebuilds `BattleScreen` (progress lives in `PlayerState`; the fight in progress restarts with a fresh monster).
- Skill names follow the T&T habit of silly incantations: 받아라 이 악마야! / Take That, You Fiend!, 아이고 가엾어라 / Poor Baby, 썰고 다지고 썰어라! / Slice 'n' Dice!, 펑! 하고 터져라 / Kaboom, Baby!, 다들 엎드려!! / Everybody Duck!!.

## Style

`.editorconfig`: 4-space indent for Java, 2-space for Gradle files, LF line endings, UTF-8.

## App icons
- Source art: `tools/icon/knight_bust.png` (PixelLab create_image_pro, 64px). Rebuild every platform's icons from the repo root with `python3 tools/icon/build_icons.py`.
- Android: legacy `drawable-*/ic_launcher.png` (48dp, rounded); adaptive (API 26+) `drawable-anydpi-v26/ic_launcher.xml` = `ic_launcher_bg` + `ic_launcher_fg` (108dp canvas, bust inside the 66dp safe zone). `android/ic_launcher-web.png` is the 512 Play Store icon.
- iOS: `AppIcon.appiconset` (opaque, same filenames); launch-screen `Logo.imageset` is the "Roll On, Hero!" title (`logo@1x/2x/3x`).
- Desktop window: `lwjgl3/src/main/resources/icon16/32/64/128.png`.
- Master 1024 and the adaptive mask preview are in `docs/icon/`.

## App ID
- Store/app identity is `com.trialmobile.rollonhero` (Android `applicationId`, iOS `app.id`, macOS bundle identifier). Never change it after the first Play upload. Java packages and the Android `namespace` stay `com.trialmobile.myfantasy` (internal only). The desktop save name (`com.trialmobile.myfantasy.save`) also stays, so existing desktop saves keep working.

## Google Play release
- Build: double-click `tools/android/build_release.command` (or `./gradlew android:bundleRelease`) → `dist/RollOnHero-<version>.aab` (+ `.apk` for testing). `tools/android/test_release.command` installs the APK on the emulator, launches it and saves screenshots/logs in `_to_delete/`.
- Signing: upload key `keystore/rollonhero-upload.jks` + `keystore/keystore.properties` (git-ignored; back both up — losing them means asking Google to reset the upload key). `android/build.gradle` signs release builds when the properties file exists. Upload cert SHA-1 `DE:23:A0:FF:CC:12:EF:59:AE:BC:78:BA:9B:C8:A9:24:0B:E8:08:00`.
- Bump `versionCode` (and `versionName`) in `android/build.gradle` for every upload.
- App name: `android/res/values*/strings.xml` (en "Roll On, Hero!", ko "굴려라 용사여!"); iOS `ios/data/<lang>.lproj/InfoPlist.strings`.
- Store texts, policy answers and the release steps: `docs/store/listing.md`. Images: `docs/store/` (feature graphic, screenshots). Privacy policy page: `docs/privacy/index.html` (replace `CONTACT_EMAIL`, host it publicly).
