package com.trialmobile.myfantasy.save;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

/**
 * 저장 데이터를 libGDX {@link Preferences}에 JSON 문자열로 기록한다.
 * 데스크톱은 사용자 홈의 .prefs/ 폴더, 안드로이드는 SharedPreferences, iOS는 NSUserDefaults에 저장된다.
 */
public final class SaveManager {
    private static final String PREFS_NAME = "com.trialmobile.myfantasy.save";
    private static final String KEY = "save";
    private static final String TAG = "SaveManager";

    private final Json json = new Json();

    public SaveManager() {
        json.setOutputType(JsonWriter.OutputType.json);
        json.setUsePrototypes(false);
        json.setIgnoreUnknownFields(true);
    }

    private Preferences prefs() {
        return Gdx.app.getPreferences(PREFS_NAME);
    }

    public void save(SaveData data) {
        try {
            Preferences prefs = prefs();
            prefs.putString(KEY, json.toJson(data));
            prefs.flush();
        } catch (RuntimeException e) {
            Gdx.app.error(TAG, "저장 실패", e);
        }
    }

    /** 저장 데이터가 없거나 읽을 수 없으면 null. */
    public SaveData load() {
        try {
            String text = prefs().getString(KEY, null);
            if (text == null || text.isEmpty()) return null;
            return json.fromJson(SaveData.class, text);
        } catch (RuntimeException e) {
            Gdx.app.error(TAG, "저장 데이터를 읽을 수 없어 새 게임으로 시작합니다", e);
            return null;
        }
    }

    public void delete() {
        Preferences prefs = prefs();
        prefs.remove(KEY);
        prefs.flush();
    }
}
