package com.trialmobile.myfantasy.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeBitmapFontData;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.Hinting;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

/**
 * FreeType 기반 한글 픽셀 폰트 관리자 (갈무리11, SIL OFL).
 * <p>
 * 한글은 완성형 글자만 11,172자라 전부 미리 굽기엔 텍스처가 너무 커진다.
 * 그래서 {@code incremental} 모드로 생성해, 화면에 처음 등장하는 글자를 그때그때 아틀라스에 추가한다.
 * 이 모드에서는 폰트를 쓰는 동안 generator를 dispose하면 안 되므로 이 클래스가 끝까지 들고 있다.
 * <p>
 * 픽셀 폰트는 설계 격자({@link #PIXEL_GRID}px)의 정수배로 그려야 선명하다.
 * 그래서 "가상 크기 × renderScale"에 가장 가까운 12의 배수 크기로 굽고, 폰트 스케일을 1/renderScale로 맞춘다.
 * 결과적으로 화면 픽셀 기준으로는 항상 정수배라 또렷하고, 가상 좌표계에서의 크기는 요청값에 가깝다.
 */
public final class Fonts implements Disposable {
    public static final String REGULAR_PATH = "fonts/Galmuri11.ttf";
    public static final String BOLD_PATH = "fonts/Galmuri11-Bold.ttf";
    /** 영어용 본문 글꼴: 같은 갈무리11의 좁은(Condensed) 판. 라틴 글자가 약 3분의 1 좁아 긴 영어 문장이 들어간다. */
    public static final String CONDENSED_PATH = "fonts/Galmuri11-Condensed.ttf";
    /** 갈무리11의 설계 격자(px). */
    public static final int PIXEL_GRID = 12;

    /** 화면에 처음부터 자주 나오는 글자. 미리 구워 두면 첫 프레임 끊김이 줄어든다. */
    private static final String PRELOAD_CHARS = FreeTypeFontGenerator.DEFAULT_CHARS
        + "굴려라용사여주사위로싸우는방치형로딩중화면을터치하여시작스테이지골드영웅장비킬상점전투자동기록체력레벨경험치";

    private FreeTypeFontGenerator regularGenerator;
    private FreeTypeFontGenerator boldGenerator;
    private final Array<BitmapFont> fonts = new Array<BitmapFont>();

    /** 작은 글씨(보조 정보, 로그). */
    public BitmapFont small;
    /** 본문 기본 글씨. */
    public BitmapFont normal;
    /** 강조/헤더 글씨(굵게). */
    public BitmapFont large;
    /** 타이틀 글씨(굵게 + 외곽선). */
    public BitmapFont title;
    /** 전투 배경처럼 알록달록한 바탕 위에 쓰는 외곽선 글씨. */
    public BitmapFont smallOutline;
    public BitmapFont normalOutline;
    public BitmapFont largeOutline;

    public void load(float renderScale) {
        load(renderScale, false);
    }

    /** @param condensed 본문(굵지 않은) 글꼴을 좁은 판으로(영어 화면) */
    public void load(float renderScale, boolean condensed) {
        regularGenerator = new FreeTypeFontGenerator(Gdx.files.internal(condensed ? CONDENSED_PATH : REGULAR_PATH));
        boldGenerator = new FreeTypeFontGenerator(Gdx.files.internal(BOLD_PATH));

        small = create(regularGenerator, 22, 0, renderScale);
        normal = create(regularGenerator, 28, 0, renderScale);
        large = create(boldGenerator, 40, 0, renderScale);
        title = create(boldGenerator, 72, 2, renderScale);
        smallOutline = create(regularGenerator, 22, 1, renderScale);
        normalOutline = create(regularGenerator, 28, 1, renderScale);
        largeOutline = create(boldGenerator, 40, 1, renderScale);
    }

    /**
     * @param size   가상 해상도 기준 원하는 크기(px)
     * @param border 외곽선 두께(설계 격자 1칸 기준 픽셀 수, 0이면 없음)
     */
    private BitmapFont create(FreeTypeFontGenerator generator, int size, int border, float renderScale) {
        int multiple = Math.max(1, Math.round(size * renderScale / PIXEL_GRID));
        FreeTypeFontParameter p = new FreeTypeFontParameter();
        p.size = multiple * PIXEL_GRID;
        p.characters = PRELOAD_CHARS;
        p.incremental = true;
        p.mono = true; // 안티앨리어싱 없이 또렷한 픽셀
        p.hinting = Hinting.None;
        p.minFilter = Texture.TextureFilter.Nearest;
        p.magFilter = Texture.TextureFilter.Nearest;
        if (border > 0) {
            p.borderWidth = border * multiple;
            p.borderColor = Color.valueOf("2b1d2e");
            p.borderStraight = true;
        }

        BitmapFont font = generator.generateFont(p);
        font.getData().setScale(1f / renderScale);
        // 스케일이 1이 아니면 정수 좌표 스냅 때문에 글자가 떨리므로 끈다.
        font.setUseIntegerPositions(false);
        fonts.add(font);
        return font;
    }

    @Override
    public void dispose() {
        for (BitmapFont font : fonts) {
            font.dispose(); // 페이지 텍스처(+픽스맵)
            if (font.getData() instanceof FreeTypeBitmapFontData) {
                ((FreeTypeBitmapFontData) font.getData()).dispose(); // 패커, 외곽선 stroker
            }
        }
        fonts.clear();
        if (regularGenerator != null) regularGenerator.dispose();
        if (boldGenerator != null) boldGenerator.dispose();
    }
}
