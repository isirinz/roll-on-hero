package com.trialmobile.myfantasy.battle;

import com.trialmobile.myfantasy.rules.Dice;
import com.trialmobile.myfantasy.rules.Monster;

/**
 * 진행 위치에 맞는 몬스터를 만든다. 몬스터 밸런스 조정은 이 클래스에서 한다.
 */
public final class MonsterTable {
    /** 1-1 첫 웨이브 몬스터의 MR. */
    public static final double BASE_MR = 9.0;
    /** 스테이지가 하나 오를 때마다 MR이 곱해지는 비율. */
    public static final double STAGE_GROWTH = 1.10;
    /** 같은 스테이지 안에서 웨이브마다 더해지는 비율. */
    public static final double WAVE_STEP = 0.06;
    /** 보스 MR 배율. */
    public static final double BOSS_MULTIPLIER = 1.8;
    /** MR 무작위 편차(±). */
    public static final double VARIANCE = 0.10;

    /** 챕터별 일반 몬스터: {종류 id, 이름}. id는 그림 폴더 이름(assets/sprites/&lt;id&gt;)과 같다. */
    private static final String[][][] NORMALS = {
        {{"giant_rat", "거대 쥐"}, {"kobold", "코볼트"}, {"goblin", "고블린"}, {"orc_scout", "오크 척후병"}},
        {{"skeleton", "해골 전사"}, {"ghoul", "구울"}, {"orc_warrior", "오크 전사"}, {"werewolf", "늑대인간"}},
        {{"troll", "트롤"}, {"lizardman", "리저드맨"}, {"ogre", "오우거"}, {"minotaur", "미노타우로스"}},
        {{"yeti", "예티"}, {"ice_golem", "얼음 골렘"}, {"frost_harpy", "서리 하피"}, {"snowman", "눈사람 투사"}},
        {{"mummy", "미라"}, {"gnoll", "놀"}, {"sand_bandit", "사막 도적"}, {"scorpion_man", "전갈인간"}},
        {{"fire_imp", "불꽃 임프"}, {"salamander", "샐러맨더"}, {"magma_golem", "마그마 골렘"}, {"demon_brute", "악마 투사"}},
        {{"shroomling", "버섯돌이"}, {"wood_spirit", "나무 정령"}, {"toad_warrior", "두꺼비 전사"}, {"thorn_dryad", "가시 드라이어드"}},
        {{"crab_warrior", "게 전사"}, {"merfolk", "인어 창병"}, {"drowned_pirate", "수장된 해적"}, {"shark_man", "상어인간"}},
        {{"gargoyle", "가고일"}, {"bat_fiend", "박쥐 마귀"}, {"cultist", "광신도"}, {"wraith", "망령"}},
        {{"drake_warrior", "드레이크 전사"}, {"dragon_mage", "용인 마법사"}, {"wyvern_knight", "와이번 기사"}, {"obsidian_golem", "흑요석 골렘"}},
    };
    /** 챕터 보스. 순서가 챕터 순서이고, 챕터 배경(assets/ui/bg/chapter_N.png)도 같은 순서다. */
    private static final String[][] BOSSES = {
        {"goblin_chief", "고블린 족장"}, {"skeleton_knight", "해골 기사"}, {"troll_king", "트롤 왕"},
        {"frost_giant", "서리 거인"}, {"jackal_lord", "자칼 군주"}, {"efreet", "이프리트"},
        {"spider_queen", "거미 여왕"}, {"sea_king", "심해의 왕"}, {"vampire_lord", "뱀파이어 군주"},
        {"dragon_king", "용왕"},
    };
    /** 챕터 테마 수. 이보다 뒤 챕터는 처음 테마부터 다시 돈다(강도는 계속 오른다). */
    public static final int THEMES = BOSSES.length;

    private MonsterTable() {
    }

    /** 챕터(1부터)의 테마 번호(0부터): 몬스터 구성과 배경을 정한다. */
    public static int themeIndex(int chapter) {
        return (Math.max(1, chapter) - 1) % THEMES;
    }

    /** 등장하는 모든 몬스터 종류 id(일반 + 보스). 그림·번역 키 확인용. */
    public static java.util.List<String> allKindIds() {
        java.util.List<String> ids = new java.util.ArrayList<String>();
        for (String[][] chapter : NORMALS) for (String[] kind : chapter) ids.add(kind[0]);
        for (String[] boss : BOSSES) ids.add(boss[0]);
        return ids;
    }

    public static Monster create(StageProgress progress, Dice dice) {
        int index = progress.stageIndex();
        int theme = themeIndex(progress.getChapter());
        boolean boss = progress.isBossWave();

        double mr = BASE_MR * Math.pow(STAGE_GROWTH, index) * (1.0 + WAVE_STEP * (progress.getWave() - 1));
        if (boss) mr *= BOSS_MULTIPLIER;
        mr *= 1.0 + (dice.nextFloat() * 2.0 - 1.0) * VARIANCE;

        String[] kind = boss ? BOSSES[theme] : NORMALS[theme][dice.nextInt(NORMALS[theme].length)];
        return new Monster(kind[0], kind[1], (int) Math.round(mr), 0, boss);
    }

    /** 처치 시 모험 점수: 원작처럼 몬스터의 처음 MR만큼. */
    public static int apReward(Monster monster) {
        return monster.getMaxMr();
    }

    /** 처치 시 골드. */
    public static int goldReward(Monster monster, Dice dice) {
        int mr = monster.getMaxMr();
        int gold = mr / 2 + 1 + dice.nextInt(mr / 3 + 1);
        return monster.isBoss() ? gold * 3 : gold;
    }
}
