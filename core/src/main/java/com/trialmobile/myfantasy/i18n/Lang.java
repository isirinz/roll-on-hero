package com.trialmobile.myfantasy.i18n;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.I18NBundle;

import java.util.Locale;
import java.util.MissingResourceException;

/**
 * 화면 글자 번역. 문자열은 {@code assets/i18n/strings.properties}(영어, 기본)와
 * {@code strings_ko.properties}(한국어)에 있다. 새 언어는 {@code strings_<언어>.properties}를 추가하고 {@link #LANGUAGES}에 등록한다.
 * <p>
 * 처음 실행하면 기기 언어를 따르고(한국어 기기 → 한국어, 그 밖 → 영어), 설정에서 바꾸면 Preferences에 저장해 다음에도 쓴다.
 * 언어를 바꾼 뒤에는 화면을 새로 만들어야 글자가 바뀐다({@code BattleScreen}이 처리).
 * <p>
 * 형식 문자열은 {@code {0}}, {@code {1}} 자리표시자를 쓴다. 숫자 형식(쉼표, 골드 단위)은 넘기기 전에 코드에서 맞춘다.
 * properties 파일은 값 앞의 공백을 지우므로, 앞에 공백이 필요한 조각은 코드에서 붙인다.
 */
public final class Lang {
    /** 지원 언어 코드(설정 화면 순서). */
    public static final String[] LANGUAGES = {"ko", "en"};
    private static final String PREFS_NAME = "com.trialmobile.myfantasy.settings";
    private static final String KEY_LANGUAGE = "language";

    private static I18NBundle bundle;
    private static String language = "en";

    private Lang() {
    }

    /** 앱 시작 시 한 번. 저장된 언어 → 기기 언어 순으로 고른다. */
    public static void init() {
        String saved = prefs().getString(KEY_LANGUAGE, "");
        load(saved.isEmpty() ? deviceLanguage() : saved);
    }

    /** 언어를 바꾸고 저장한다. 이미 그 언어면 아무것도 하지 않고 false. */
    public static boolean set(String code) {
        if (code.equals(language) && bundle != null) return false;
        load(code);
        Preferences p = prefs();
        p.putString(KEY_LANGUAGE, language);
        p.flush();
        return true;
    }

    public static String language() {
        return language;
    }

    /** 설정 버튼에 쓰는 언어 이름(그 언어 자체로 표기). */
    public static String nativeName(String code) {
        return "ko".equals(code) ? "한국어" : "English";
    }

    public static String get(String key) {
        if (bundle == null) return key;
        try {
            return bundle.get(key);
        } catch (MissingResourceException e) {
            return key;
        }
    }

    public static String format(String key, Object... args) {
        if (bundle == null) return key;
        try {
            return bundle.format(key, args);
        } catch (MissingResourceException e) {
            return key;
        }
    }

    /** 키가 있으면 그 값, 없으면 fallback. */
    public static String getOr(String key, String fallback) {
        if (bundle == null) return fallback;
        try {
            return bundle.get(key);
        } catch (MissingResourceException e) {
            return fallback;
        }
    }

    private static void load(String code) {
        boolean known = false;
        for (String l : LANGUAGES) if (l.equals(code)) known = true;
        language = known ? code : "en";
        I18NBundle.setSimpleFormatter(true); // {0}만 쓰고 작은따옴표 이스케이프 규칙은 쓰지 않는다
        I18NBundle.setExceptionOnMissingKey(true);
        bundle = I18NBundle.createBundle(Gdx.files.internal("i18n/strings"), new Locale(language), "UTF-8");
    }

    private static String deviceLanguage() {
        return "ko".equals(Locale.getDefault().getLanguage()) ? "ko" : "en";
    }

    private static Preferences prefs() {
        return Gdx.app.getPreferences(PREFS_NAME);
    }
}
