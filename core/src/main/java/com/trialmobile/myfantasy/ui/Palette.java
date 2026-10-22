package com.trialmobile.myfantasy.ui;

import com.badlogic.gdx.graphics.Color;

/**
 * 게임 UI 색상표 — 따뜻한 나무·크림 양피지·파스텔 톤.
 * PixelLab 에셋을 만들 때 강제한 16색 팔레트(assets/ui/palette.png)와 같은 색을 쓴다.
 */
public final class Palette {
    // --- 픽셀아트 팔레트 원색 ---
    public static final Color OUTLINE = Color.valueOf("2b1d2e");
    public static final Color PLUM = Color.valueOf("4a3245");
    public static final Color WOOD_DARK = Color.valueOf("7a4f3a");
    public static final Color WOOD = Color.valueOf("a8704a");
    public static final Color WOOD_LIGHT = Color.valueOf("d49a6a");
    public static final Color PARCHMENT = Color.valueOf("f2d3a2");
    public static final Color CREAM = Color.valueOf("fff4dc");
    public static final Color PINK = Color.valueOf("ff9eaa");
    public static final Color ROSE = Color.valueOf("e8607a");
    public static final Color GOLD = Color.valueOf("ffd36e");
    public static final Color ORANGE = Color.valueOf("f0a830");
    public static final Color LEAF = Color.valueOf("8fd16a");
    public static final Color GREEN = Color.valueOf("4f9e5a");
    public static final Color SKY = Color.valueOf("7ec8f0");
    public static final Color BLUE = Color.valueOf("4a7fc8");

    // --- 용도별 ---
    /** 화면 바탕(패널 바깥). */
    public static final Color BACKGROUND = Color.valueOf("8a5a3c");
    /** 크림색 패널 위 본문 글자. */
    public static final Color TEXT = PLUM;
    /** 크림색 패널 위 보조 글자. */
    public static final Color TEXT_DIM = WOOD_DARK;
    /** 어두운 바탕이나 전투 배경 위 글자(외곽선 폰트와 함께). */
    public static final Color TEXT_LIGHT = CREAM;
    /** 패널 위 소제목·강조. */
    public static final Color HEADER = Color.valueOf("c0602a");
    public static final Color HP = ROSE;
    public static final Color GOOD = GREEN;
    /** 스킬(주문) 관련 표시. 크림색 패널 위에서도 읽히는 진한 파랑. */
    public static final Color SKILL = BLUE;
    public static final Color OVERLAY = new Color(0.10f, 0.06f, 0.10f, 0.65f);

    /** 캐릭터 그림이 생기기 전 자리 표시 색. */
    public static final Color HERO = Color.valueOf("4a7fc8");
    public static final Color MONSTER = Color.valueOf("4f9e5a");

    private Palette() {
    }
}
