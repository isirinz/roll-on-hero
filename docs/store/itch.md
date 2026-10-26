# itch.io 등록 정보

itch.io → Dashboard → Create new project 에 붙여 넣을 내용. 영어로 쓰고 한국어를 아래에 덧붙인다(itch 페이지는 언어가 하나라 둘을 한 페이지에 둔다).

## 기본 항목

| 항목 | 값 |
|---|---|
| Title | Roll On, Hero! |
| Project URL | https://gamebyul.itch.io/roll-on-hero |
| Short description or tagline | A pixel idle RPG where every swing is a roll of the dice. |
| Classification | Games |
| Kind of project | Downloadable |
| Release status | Released |
| Pricing | No payments (또는 $0 or donate) |
| Genre | Role Playing |
| Tags (최대 10) | idle, incremental, rpg, pixel-art, dice, fantasy, singleplayer, auto-battler, retro, korean |
| AI generation disclosure | **Yes** — Graphics (PixelLab으로 만든 캐릭터·배경·아이콘), Code/Text (Claude와 함께 작성). 효과음은 코드로 합성했으므로 Sound는 No |
| Languages | English, Korean |
| Inputs | Mouse, Touchscreen |
| Average session | A few minutes |
| Links | Google Play · Website · Source |
| Community | Comments |
| Visibility | 먼저 Draft로 저장해 미리보기 확인 → Public |

## 업로드 파일 (`dist/`)

| 파일 | 표시 이름 | 플랫폼 체크 |
|---|---|---|
| `RollOnHero-Windows-x64.zip` | Windows (64-bit) | Windows |
| `RollOnHero-Mac-AppleSilicon.zip` | macOS — Apple Silicon (M1 or later) | macOS |
| `RollOnHero-Mac-Intel.zip` | macOS — Intel | macOS |

- Java 설치는 필요 없다(런타임 포함).
- **Android APK는 올리지 않는다.** Google Play는 Play 앱 서명 키로 다시 서명하므로, itch의 APK(업로드 키 서명)를 깔면 Play 버전과 서명이 달라 업데이트가 안 된다. 설명에 Google Play 링크만 둔다.
- 데스크톱 zip은 10-20 빌드(게임 내용은 최신). Mac 앱의 번들 ID만 예전 값(`com.trialmobile.myfantasy.MyFantasy`)이라, 다음 데스크톱 빌드 때 `com.trialmobile.rollonhero`로 맞춰진다.

## 이미지

| 항목 | 규격 | 파일 |
|---|---|---|
| Cover image | 630×500 | `docs/images/itch_cover.png` |
| Screenshots (3~5장 권장) | 자유 | `docs/store/screens/en_1~5.png` (영어 페이지면 en, 한국어 위주면 ko) |
| Gameplay GIF | 스크린샷 칸에 같이 올림 | `docs/images/battle.gif` |
| 페이지 배너 (Edit theme → Banner) | 가로 960 이상 | `docs/images/banner.png` (1280×440) |

Edit theme 추천 색 (게임 UI와 맞춤): Background `#4a2e2a`, Text `#fff4dc`, Link `#ffd36e`, Button `#8fd16a`, 폰트 Jua 대신 비슷한 둥근 글꼴.

## Description

```
Every swing is a roll of the dice!

Roll On, Hero! is a pixel-art idle RPG built on old-school tabletop rules.
Your hero fights on their own. Every round, the hero and the monster each throw a handful of six-sided dice, and the higher total hits. Armor soaks part of the blow, and every 6 you roll bites through anyway.
You decide how your hero grows.

FEATURES
• Dice combat you can see: every die tumbles across the screen, every total and every lucky 6 is right there.
• 10 chapters, 50 monsters: from a green meadow through a graveyard, frozen wastes, a desert, a volcano, a mushroom forest and a shipwreck coast to a shadow castle and the dragon's peak.
• 5 spells that cast themselves: Take That, You Fiend! · Poor Baby · Slice 'n' Dice! · Kaboom, Baby! · Everybody Duck!!
• 19 weapons, armors and shields to buy and enhance.
• Rebirth: start over for soul stones and permanent perks. Every new life goes faster.
• x2 Boost: double attack and gold for 15 minutes.
• Keeps fighting while you're away: 2 hours of offline rewards at first, up to 12 hours as you go deeper.
• Battle speed x1/x2/x3, retro sound effects, English and Korean.

ALSO ON ANDROID
Get it on Google Play: https://play.google.com/store/apps/details?id=com.trialmobile.rollonhero

HOW TO RUN
Windows: unzip and run rollonhero.exe. If SmartScreen appears, click "More info" → "Run anyway".
macOS: unzip and open "Roll On, Hero!". The app is not notarized, so macOS blocks it the first time: open System Settings → Privacy & Security, scroll down and click "Open Anyway". (On older macOS, right-click the app → Open.)
No Java install needed. Saves stay on your computer. No internet connection, no data collected.

---

굴려라 용사여!
휘두를 때마다 주사위를 굴리는 픽셀아트 방치형 RPG입니다. 라운드마다 영웅과 몬스터가 6면체 주사위를 한 움큼씩 던지고, 합이 높은 쪽이 때립니다. 10개 챕터, 몬스터 50종, 자동 스킬 5종, 장비 19종, 환생, 최대 12시간 오프라인 보상. 한국어를 지원합니다.
안드로이드: Google Play에서 "굴려라 용사여!"

Website: https://isirinz.github.io/roll-on-hero/
Source: https://github.com/isirinz/roll-on-hero
Privacy: https://isirinz.github.io/roll-on-hero/privacy/
Contact: isirinz@naver.com

Made with libGDX. Art made with PixelLab.
```

## 올리는 순서

1. Create new project → 위 기본 항목 입력, Kind = Downloadable.
2. Uploads에 zip 3개를 올리고 각각 표시 이름과 플랫폼(Windows / macOS)을 체크.
3. Cover image, Screenshots(PNG 5장 + GIF) 올리기.
4. Description 붙여 넣기 → Save as Draft → View page로 확인.
5. Edit theme에서 배너와 색 지정.
6. Visibility를 Public으로 바꾸고 저장.
