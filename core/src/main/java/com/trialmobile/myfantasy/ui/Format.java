package com.trialmobile.myfantasy.ui;

/** 화면 표시용 숫자 형식. */
public final class Format {
    private Format() {
    }

    /** 1234567 → "1,234,567". */
    public static String number(long value) {
        boolean negative = value < 0;
        String digits = Long.toString(Math.abs(value));
        StringBuilder sb = new StringBuilder();
        int lead = digits.length() % 3;
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && (i - lead) % 3 == 0) sb.append(',');
            sb.append(digits.charAt(i));
        }
        return negative ? "-" + sb : sb.toString();
    }

    /**
     * 골드 표기: 1000 미만은 그대로, 그 위로는 유효숫자 3자리 + 단위 글자.
     * 1,000 = 1.00A, 12,345 = 12.3A, 999,999 = 999A, 1,000,000 = 1.00B … Z 다음은 AA, AB … (내림, 반올림하지 않음).
     */
    public static String gold(long value) {
        if (value < 0) return "-" + gold(-value);
        if (value < 1000) return Long.toString(value);
        int unit = 0;
        long scaled = value;          // 단위로 나눈 정수부
        long rest = 0;                // 바로 아래 단위의 나머지(0~999)
        while (scaled >= 1000) {
            rest = scaled % 1000;
            scaled /= 1000;
            unit++;
        }
        String number;
        if (scaled >= 100) number = Long.toString(scaled);
        else if (scaled >= 10) number = scaled + "." + (rest / 100);
        else number = scaled + "." + pad2(rest / 10);
        return number + unitName(unit);
    }

    /** 1 → A, 26 → Z, 27 → AA, 28 → AB … */
    static String unitName(int unit) {
        StringBuilder sb = new StringBuilder();
        int n = unit;
        while (n > 0) {
            n--;
            sb.insert(0, (char) ('A' + n % 26));
            n /= 26;
        }
        return sb.toString();
    }

    private static String pad2(long v) {
        return v < 10 ? "0" + v : Long.toString(v);
    }

    /** 남은 시간 표시: 1시간 미만 "14:05", 이상 "1:59:12". 0 미만은 0으로. 1초 미만은 올림(0:00이 너무 일찍 보이지 않게). */
    public static String clock(long millis) {
        long total = (Math.max(0L, millis) + 999L) / 1000L;
        long h = total / 3600;
        long m = (total % 3600) / 60;
        long sec = total % 60;
        String ss = (sec < 10 ? "0" : "") + sec;
        if (h > 0) return h + ":" + (m < 10 ? "0" : "") + m + ":" + ss;
        return m + ":" + ss;
    }

    public static String signed(int value) {
        return value >= 0 ? "+" + value : String.valueOf(value);
    }
}
