package com.trialmobile.myfantasy.i18n;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.trialmobile.myfantasy.battle.MonsterTable;
import com.trialmobile.myfantasy.rules.Attribute;
import com.trialmobile.myfantasy.rules.Item;
import com.trialmobile.myfantasy.rules.ItemCatalog;
import com.trialmobile.myfantasy.rules.ItemSlot;
import com.trialmobile.myfantasy.rules.Skill;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Properties;
import java.util.TreeSet;

import org.junit.Test;

/** 언어 파일끼리 키가 같은지, 게임의 모든 스킬·아이템·몬스터·능력치에 번역이 있는지. */
public class StringsTest {
    private static Properties load(String name) throws Exception {
        File dir = new File("assets/i18n");
        if (!dir.exists()) dir = new File("../assets/i18n"); // gradle은 core/에서 실행
        Properties p = new Properties();
        Reader r = new InputStreamReader(new FileInputStream(new File(dir, name)), "UTF-8");
        try {
            p.load(r);
        } finally {
            r.close();
        }
        return p;
    }

    @Test
    public void everyLanguageHasTheSameKeys() throws Exception {
        Properties en = load("strings.properties");
        Properties ko = load("strings_ko.properties");
        assertEquals(new TreeSet<Object>(en.keySet()), new TreeSet<Object>(ko.keySet()));
    }

    @Test
    public void gameObjectsAreTranslated() throws Exception {
        for (String file : new String[] {"strings.properties", "strings_ko.properties"}) {
            Properties p = load(file);
            for (Skill s : Skill.values()) {
                assertTrue(file + " skill " + s.id, p.containsKey("skill." + s.id + ".name"));
                assertTrue(file + " skill short " + s.id, p.containsKey("skill." + s.id + ".short"));
                assertTrue(file + " desc " + s.id, p.containsKey("skill.desc." + s.id));
            }
            for (Item i : ItemCatalog.all()) assertTrue(file + " item " + i.id, p.containsKey("item." + i.id));
            for (ItemSlot s : ItemSlot.values()) assertTrue(file + " slot " + s, p.containsKey("slot." + s.name()));
            for (Attribute a : Attribute.values()) assertTrue(file + " attr " + a, p.containsKey("attr." + a.name()));
            for (String kind : MonsterTable.allKindIds()) assertTrue(file + " monster " + kind, p.containsKey("monster." + kind));
        }
    }
}
