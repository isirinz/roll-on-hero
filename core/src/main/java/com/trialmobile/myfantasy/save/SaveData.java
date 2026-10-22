package com.trialmobile.myfantasy.save;

/**
 * 디스크에 기록되는 저장 데이터. JSON으로 직렬화되므로 public 필드와 기본 생성자만 둔다.
 * 필드를 바꾸면 {@link #CURRENT_VERSION}을 올리고 {@link SaveMapper}에 이전 버전 처리를 추가할 것.
 */
public class SaveData {
    /** 1: 첫 버전. 2: 스킬 레벨, 사냥 웨이브 한도 추가. 3: 2배 버프 시각. 4: 환생. 5: 모든 생 최고 기록. */
    public static final int CURRENT_VERSION = 5;

    public int version = CURRENT_VERSION;
    /** 저장 시각(epoch 밀리초). 오프라인 보상 계산에 쓴다. */
    public long savedAt;

    // 영웅
    public String heroName;
    public int level;
    public int ap;
    public int con;
    /** {@link com.trialmobile.myfantasy.rules.Attribute} 순서. */
    public int[] attributes;

    // 장비 (아이템 id, 빈 칸은 null)
    public String weapon;
    public String armor;
    public String shield;
    public int weaponEnhance;
    public int armorEnhance;
    public int shieldEnhance;
    public String[] ownedItems;

    // 스킬 ({@link com.trialmobile.myfantasy.rules.Skill} 순서, 0 = 미해금). 버전 1에는 없음.
    public int[] skillLevels;

    // 진행
    public long gold;
    public int kills;
    public int chapter;
    public int stage;
    public int wave;
    /** 버전 1 호환용. 버전 2부터는 {@link #farmLimit}을 쓴다. */
    public boolean farming;
    /** 사냥 모드에서 반복하는 마지막 웨이브(0 = 사냥 아님). 버전 2. */
    public int farmLimit;

    // 2배 버프 (epoch 밀리초, 0 = 없음). 버전 3.
    public long boostActiveUntil;
    public long boostReadyAt;

    // 환생 (버전 4). perkLevels는 {@link com.trialmobile.myfantasy.battle.Rebirth.Perk} 순서.
    public long souls;
    public long totalSouls;
    public int rebirths;
    public int bestStage;
    public int[] perkLevels;
    /** 모든 생을 통틀어 가장 깊이 간 스테이지 번호(오프라인 보상 최대 시간 기준). 버전 5. */
    public int bestStageEver;
}
