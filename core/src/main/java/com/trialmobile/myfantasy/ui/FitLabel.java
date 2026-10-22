package com.trialmobile.myfantasy.ui;

import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * 칸에 맞춰 글자 크기를 줄이는 한 줄 라벨. 영어처럼 글자가 길어지면 한 단계 작은 글꼴로 바꾸고,
 * 가장 작은 글꼴로도 넘치면 그때 끝을 …로 줄인다.
 * <p>
 * 픽셀 글꼴이라 배율로 줄이면 뭉개지므로, 이미 만들어 둔 작은 글꼴 스타일로 갈아끼운다
 * ({@link #SMALLER}: large → default → small, field-large → field → field-small, ink → ink-small).
 * 글자색(setColor)은 그대로 유지된다.
 */
public class FitLabel extends Label {
    /** 스타일 → 한 단계 작은 스타일. 없으면 그 스타일이 마지막. */
    private static final ObjectMap<String, String> SMALLER = new ObjectMap<String, String>();
    static {
        SMALLER.put("large", "default");
        SMALLER.put("default", "small");
        SMALLER.put("field-large", "field");
        SMALLER.put("field", "field-small");
        SMALLER.put("ink", "ink-small");
        SMALLER.put("gold", "gold-small");
    }

    private final Skin skin;
    private final String[] chain;
    private final GlyphLayout measure = new GlyphLayout();
    private int current;

    public FitLabel(CharSequence text, Skin skin, String style) {
        super(text, skin, style);
        this.skin = skin;
        int n = 1;
        for (String s = style; SMALLER.containsKey(s) && skin.has(SMALLER.get(s), LabelStyle.class); s = SMALLER.get(s)) n++;
        chain = new String[n];
        chain[0] = style;
        for (int i = 1; i < n; i++) chain[i] = SMALLER.get(chain[i - 1]);
        setEllipsis(true);
    }

    @Override
    public void layout() {
        float width = getWidth();
        if (width > 0f && chain.length > 1) {
            int pick = chain.length - 1;
            for (int i = 0; i < chain.length; i++) {
                LabelStyle s = skin.get(chain[i], LabelStyle.class);
                measure.setText(s.font, getText());
                if (measure.width * getFontScaleX() <= width) {
                    pick = i;
                    break;
                }
            }
            if (pick != current) {
                current = pick;
                super.setStyle(skin.get(chain[pick], LabelStyle.class));
            }
        }
        super.layout();
    }
}
