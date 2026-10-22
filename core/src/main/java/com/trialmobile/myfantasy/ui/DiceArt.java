package com.trialmobile.myfantasy.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

/**
 * 주사위 연출용 픽셀아트를 코드로 그린다(눈이 정확해야 하므로 그림 파일 대신 직접 찍는다).
 * <ul>
 *   <li>{@code die-<style>-<1..6>}: 17x17 주사위 면. style = {@code hero}(상아색), {@code monster}(흑자색), {@code gold}(2배 버프)</li>
 *   <li>{@code sparkle}: 7x7 반짝이(흰색, 색을 입혀 쓴다)</li>
 *   <li>{@code glow}: 32x32 부드러운 원형 빛(흰색, 색을 입혀 쓴다)</li>
 *   <li>{@code die-shadow}: 16x6 바닥 그림자</li>
 *   <li>{@code glow-soft}: 32x32 안쪽이 넓게 진한 원(오라용)</li>
 * </ul>
 * 모두 한 장의 텍스처에 모아 스킨에 등록하므로 스킨이 dispose할 때 함께 정리된다.
 */
public final class DiceArt {
    public static final String[] STYLES = {"hero", "monster", "gold"};
    public static final int DIE = 17;

    /** 스타일별 색: 외곽선, 밝은 면, 기본 면, 그늘, 눈, 1의 눈(크고 빨간). */
    private static final String[][] COLORS = {
        {"3a2a2a", "fffaec", "f4ead0", "cdbd96", "2b2340", "d63a3a"},
        {"140814", "7a5290", "4e2c5c", "2e1838", "f4e8d2", "ff6464"},
        {"4a2a10", "fff3a8", "f6c640", "c88a1e", "6e2610", "d81e1e"},
    };

    /** 3x3 칸 위치 중 눈이 찍히는 곳(행, 열) — 면 1~6. */
    private static final int[][][] PIPS = {
        {{1, 1}},
        {{0, 2}, {2, 0}},
        {{0, 2}, {1, 1}, {2, 0}},
        {{0, 0}, {0, 2}, {2, 0}, {2, 2}},
        {{0, 0}, {0, 2}, {1, 1}, {2, 0}, {2, 2}},
        {{0, 0}, {0, 2}, {1, 0}, {1, 2}, {2, 0}, {2, 2}},
    };
    /** 3x3 눈의 왼쪽 위 좌표(17칸 기준, 좌우 대칭: 가운데 4, 8, 12). */
    private static final int[] PIP_POS = {3, 7, 11};

    private DiceArt() {
    }

    public static void register(Skin skin) {
        Pixmap p = buildSheet();
        int extrasY = DIE * STYLES.length;
        Texture t = new Texture(p);
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        p.dispose();
        skin.add("dice-tex", t);
        for (int s = 0; s < STYLES.length; s++) {
            for (int face = 1; face <= 6; face++) {
                skin.add("die-" + STYLES[s] + "-" + face, new TextureRegion(t, (face - 1) * DIE, s * DIE, DIE, DIE));
            }
        }
        skin.add("glow", new TextureRegion(t, 0, extrasY, 32, 32));
        skin.add("sparkle", new TextureRegion(t, 32, extrasY, 7, 7));
        skin.add("die-shadow", new TextureRegion(t, 40, extrasY, 16, 6));
        skin.add("glow-soft", new TextureRegion(t, 0, extrasY + 32, 32, 32));
    }

    /** 주사위 면·반짝이·빛·그림자를 모은 한 장(호출자가 dispose). */
    public static Pixmap buildSheet() {
        int sheetW = DIE * 6;
        int sheetH = DIE * STYLES.length + 64;
        Pixmap p = new Pixmap(sheetW, sheetH, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        for (int s = 0; s < STYLES.length; s++) {
            for (int face = 1; face <= 6; face++) drawDie(p, (face - 1) * DIE, s * DIE, face, COLORS[s]);
        }
        int extrasY = DIE * STYLES.length;
        drawGlow(p, 0, extrasY, 32);
        drawSparkle(p, 32, extrasY);
        drawShadow(p, 40, extrasY);
        drawSoftDisc(p, 0, extrasY + 32, 32);
        return p;
    }

    private static void drawDie(Pixmap p, int ox, int oy, int face, String[] hex) {
        Color outline = Color.valueOf(hex[0]);
        Color light = Color.valueOf(hex[1]);
        Color body = Color.valueOf(hex[2]);
        Color shade = Color.valueOf(hex[3]);
        Color pip = Color.valueOf(hex[4]);
        Color pipOne = Color.valueOf(hex[5]);
        int n = DIE;
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                // 모서리를 둥글게: 각 귀퉁이 3픽셀은 비우고, 그 안쪽 한 줄은 외곽선
                int cx = Math.min(x, n - 1 - x);
                int cy = Math.min(y, n - 1 - y);
                if (cx + cy < 2) continue;
                boolean edge = cx == 0 || cy == 0 || cx + cy == 2;
                Color c;
                if (edge) c = outline;
                else if (y >= n - 3 || x >= n - 2) c = shade;      // 아래·오른쪽 두께(그늘)
                else if (y <= 1 || x <= 1) c = light;              // 위·왼쪽 빛
                else c = body;
                p.setColor(c);
                p.drawPixel(ox + x, oy + y);
            }
        }
        // 그늘 경계의 이음매 몇 점(입체감)
        p.setColor(shade);
        p.drawPixel(ox + 2, oy + n - 4);
        p.drawPixel(ox + n - 4, oy + 2);

        if (face == 1) {
            // 1은 크고 빨간 눈(동양식): 가운데 5x5 원 + 하이라이트
            int c = ox + 6;
            int r = oy + 6;
            fill(p, c + 1, r, 3, 5, pipOne);
            fill(p, c, r + 1, 5, 3, pipOne);
            p.setColor(outline);
            p.drawPixel(c + 1, r + 4);
            p.drawPixel(c + 2, r + 4);
            p.drawPixel(c + 3, r + 4);
            p.drawPixel(c + 4, r + 3);
            p.setColor(light);
            p.drawPixel(c + 1, r + 1);
            return;
        }
        for (int[] rc : PIPS[face - 1]) {
            int px = ox + PIP_POS[rc[1]];
            int py = oy + PIP_POS[rc[0]];
            // 둥근 3x3 눈: 십자는 진하게, 모서리는 그늘색으로 다듬고, 오른쪽 아래를 살짝 밝혀 오목하게
            p.setColor(shade);
            p.drawPixel(px, py);
            p.drawPixel(px + 2, py);
            p.drawPixel(px, py + 2);
            fill(p, px + 1, py, 1, 3, pip);
            fill(p, px, py + 1, 3, 1, pip);
            p.setColor(pip);
            p.drawPixel(px + 2, py + 2);
        }
    }

    private static void fill(Pixmap p, int x, int y, int w, int h, Color c) {
        p.setColor(c);
        p.fillRectangle(x, y, w, h);
    }

    /** 가운데가 밝고 바깥으로 갈수록 투명해지는 원(흰색). */
    private static void drawGlow(Pixmap p, int ox, int oy, int size) {
        float r = size / 2f;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = x + 0.5f - r;
                float dy = y + 0.5f - r;
                float d = (float) Math.sqrt(dx * dx + dy * dy) / r;
                if (d >= 1f) continue;
                float a = (1f - d) * (1f - d);
                p.setColor(1f, 1f, 1f, a);
                p.drawPixel(ox + x, oy + y);
            }
        }
    }

    /** 가장자리만 부드럽게 사라지는 원(흰색). 밝은 배경 위에서도 색이 보이도록 안쪽이 넓게 진하다. */
    private static void drawSoftDisc(Pixmap p, int ox, int oy, int size) {
        float r = size / 2f;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = x + 0.5f - r;
                float dy = y + 0.5f - r;
                float d = (float) Math.sqrt(dx * dx + dy * dy) / r;
                if (d >= 1f) continue;
                p.setColor(1f, 1f, 1f, 1f - d * d * d);
                p.drawPixel(ox + x, oy + y);
            }
        }
    }

    /** 십자 반짝이: 가운데 진하고 끝이 옅다. */
    private static void drawSparkle(Pixmap p, int ox, int oy) {
        int c = 3;
        p.setColor(1f, 1f, 1f, 1f);
        p.drawPixel(ox + c, oy + c);
        for (int i = 1; i <= 3; i++) {
            float a = i == 1 ? 1f : i == 2 ? 0.75f : 0.4f;
            p.setColor(1f, 1f, 1f, a);
            p.drawPixel(ox + c + i, oy + c);
            p.drawPixel(ox + c - i, oy + c);
            p.drawPixel(ox + c, oy + c + i);
            p.drawPixel(ox + c, oy + c - i);
        }
        p.setColor(1f, 1f, 1f, 0.5f);
        p.drawPixel(ox + c + 1, oy + c + 1);
        p.drawPixel(ox + c - 1, oy + c - 1);
        p.drawPixel(ox + c + 1, oy + c - 1);
        p.drawPixel(ox + c - 1, oy + c + 1);
    }

    /** 납작한 타원 그림자. */
    private static void drawShadow(Pixmap p, int ox, int oy) {
        int w = 16, h = 6;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float dx = (x + 0.5f - w / 2f) / (w / 2f);
                float dy = (y + 0.5f - h / 2f) / (h / 2f);
                if (dx * dx + dy * dy > 1f) continue;
                p.setColor(0f, 0f, 0f, 0.35f);
                p.drawPixel(ox + x, oy + y);
            }
        }
    }
}
