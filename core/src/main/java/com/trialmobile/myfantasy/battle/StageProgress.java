package com.trialmobile.myfantasy.battle;

/**
 * 진행 위치: 챕터 - 스테이지 - 웨이브.
 * 스테이지마다 웨이브가 {@link #WAVES_PER_STAGE}개 있고, 마지막 웨이브는 보스다.
 * <p>
 * 사냥 모드: 영웅이 쓰러지면 쓰러진 웨이브 바로 앞까지만 반복하며 힘을 기른다({@link #retreatAfterDefeat()}).
 * 첫 웨이브에서 쓰러졌다면 이전 스테이지로 물러나 사냥한다. 레벨이 오르거나 플레이어가 재도전하면 사냥 모드가 풀린다.
 */
public final class StageProgress {
    public static final int WAVES_PER_STAGE = 5;
    public static final int STAGES_PER_CHAPTER = 10;

    private int chapter = 1;
    private int stage = 1;
    private int wave = 1;
    /** 사냥 모드에서 반복할 마지막 웨이브. 0이면 사냥 모드가 아니다. */
    private int farmLimit;

    public int getChapter() {
        return chapter;
    }

    public int getStage() {
        return stage;
    }

    public int getWave() {
        return wave;
    }

    public boolean isFarming() {
        return farmLimit > 0;
    }

    public int getFarmLimit() {
        return farmLimit;
    }

    /** 사냥 모드를 켜거나(보스 직전까지 반복) 끈다. 끄면 다음 웨이브부터 다시 앞으로 나아간다. */
    public void setFarming(boolean farming) {
        this.farmLimit = farming ? WAVES_PER_STAGE - 1 : 0;
    }

    /** 반복할 마지막 웨이브를 직접 지정한다(저장 데이터 복원용). 0이면 사냥 모드 해제. */
    public void setFarmLimit(int limit) {
        this.farmLimit = Math.max(0, Math.min(WAVES_PER_STAGE - 1, limit));
    }

    public boolean isBossWave() {
        return wave == WAVES_PER_STAGE;
    }

    /** 처음부터 센 스테이지 번호(0부터). 몬스터 강도 계산에 쓴다. */
    public int stageIndex() {
        return (chapter - 1) * STAGES_PER_CHAPTER + (stage - 1);
    }

    /** "1-3" 같은 표기. */
    public String label() {
        return chapter + "-" + stage;
    }

    /**
     * 다음 웨이브로 넘어간다.
     * @return 이번 웨이브로 스테이지를 클리어했으면 true (다음 스테이지로 이동함)
     */
    public boolean advance() {
        if (farmLimit > 0 && wave >= farmLimit) {
            wave = 1;
            return false;
        }
        if (wave < WAVES_PER_STAGE) {
            wave++;
            return false;
        }
        wave = 1;
        farmLimit = 0;
        stage++;
        if (stage > STAGES_PER_CHAPTER) {
            stage = 1;
            chapter++;
        }
        return true;
    }

    /**
     * 영웅이 쓰러졌을 때: 쓰러진 웨이브 앞까지만 사냥하도록 한다.
     * 첫 웨이브에서 쓰러졌다면 이전 스테이지로 물러나 보스 직전까지 사냥한다(1-1이면 그대로).
     */
    public void retreatAfterDefeat() {
        if (wave > 1) {
            farmLimit = wave - 1;
        } else if (stageIndex() > 0) {
            stage--;
            if (stage < 1) {
                stage = STAGES_PER_CHAPTER;
                chapter--;
            }
            farmLimit = WAVES_PER_STAGE - 1;
        }
        wave = 1;
    }

    /** 현재 스테이지의 첫 웨이브로 되돌린다. */
    public void restartStage() {
        wave = 1;
    }

    public void set(int chapter, int stage, int wave) {
        this.chapter = Math.max(1, chapter);
        this.stage = Math.min(STAGES_PER_CHAPTER, Math.max(1, stage));
        this.wave = Math.min(WAVES_PER_STAGE, Math.max(1, wave));
    }
}
