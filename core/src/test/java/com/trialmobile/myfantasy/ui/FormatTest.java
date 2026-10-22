package com.trialmobile.myfantasy.ui;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FormatTest {
    @Test
    public void goldUnderThousandIsPlain() {
        assertEquals("0", Format.gold(0));
        assertEquals("7", Format.gold(7));
        assertEquals("999", Format.gold(999));
    }

    @Test
    public void goldUsesThreeDigitsAndLetters() {
        assertEquals("1.00A", Format.gold(1000));
        assertEquals("1.23A", Format.gold(1234));
        assertEquals("12.3A", Format.gold(12345));
        assertEquals("123A", Format.gold(123456));
        assertEquals("999A", Format.gold(999999));
        assertEquals("1.00B", Format.gold(1000000));
        assertEquals("4.56C", Format.gold(4567890123L));
        assertEquals("9.22F", Format.gold(Long.MAX_VALUE));
    }

    @Test
    public void unitLettersContinuePastZ() {
        assertEquals("A", Format.unitName(1));
        assertEquals("Z", Format.unitName(26));
        assertEquals("AA", Format.unitName(27));
        assertEquals("AB", Format.unitName(28));
    }
}
