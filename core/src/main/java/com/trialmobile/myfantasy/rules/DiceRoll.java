package com.trialmobile.myfantasy.rules;

/** 주사위 굴림 결과 (불변). */
public final class DiceRoll {
    public final int count;
    public final int adds;
    /** 주사위 눈의 합(보정치 제외). */
    public final int sum;
    /** 6이 나온 주사위 수. */
    public final int sixes;
    /** 타격 총합 배율(2배 버프 등). 1이 기본. */
    public final int multiplier;
    /** 타격 총합 퍼센트 보너스(환생 특성 등). 0이 기본. */
    public final int bonusPercent;
    /** 각 주사위의 눈(화면 연출용). 테스트에서 합만 지정해 만들면 null. */
    private final int[] faces;

    public DiceRoll(int count, int adds, int sum, int sixes) {
        this(count, adds, sum, sixes, null, 1, 0);
    }

    private DiceRoll(int count, int adds, int sum, int sixes, int[] faces, int multiplier, int bonusPercent) {
        this.count = count;
        this.adds = adds;
        this.sum = sum;
        this.sixes = sixes;
        this.faces = faces;
        this.multiplier = Math.max(1, multiplier);
        this.bonusPercent = Math.max(0, bonusPercent);
    }

    /** 실제로 나온 눈들로 만든다. */
    public static DiceRoll ofFaces(int adds, int... faces) {
        int sum = 0;
        int sixes = 0;
        for (int f : faces) {
            sum += f;
            if (f == 6) sixes++;
        }
        return new DiceRoll(faces.length, adds, sum, sixes, faces.clone(), 1, 0);
    }

    /** 타격 총합에 배율을 건 결과. 눈과 6의 개수(스파이트 데미지)는 그대로다. */
    public DiceRoll multiplied(int factor) {
        if (factor <= 1) return this;
        return new DiceRoll(count, adds, sum, sixes, faces, multiplier * factor, bonusPercent);
    }

    /** 타격 총합에 퍼센트 보너스를 더한 결과(내림). */
    public DiceRoll withBonus(int percent) {
        if (percent <= 0) return this;
        return new DiceRoll(count, adds, sum, sixes, faces, multiplier, bonusPercent + percent);
    }

    /** (눈의 합 + 보정치) x 배율 x (100 + 보너스)%. 음수가 되지 않는다. */
    public int total() {
        int base = Math.max(0, sum + adds) * multiplier;
        return bonusPercent <= 0 ? base : (int) ((long) base * (100 + bonusPercent) / 100);
    }

    /** i번째 주사위 눈. 눈을 모르면 합을 고르게 나눈 근사값(1~6). */
    public int face(int i) {
        if (faces != null && i >= 0 && i < faces.length) return faces[i];
        if (count <= 0) return 1;
        int base = sum / count;
        int extra = sum % count;
        return Math.max(1, Math.min(6, base + (i < extra ? 1 : 0)));
    }

    /** "3D6+2" 같은 표기. */
    public static String notation(int count, int adds) {
        if (adds > 0) return count + "D6+" + adds;
        if (adds < 0) return count + "D6" + adds;
        return count + "D6";
    }

    @Override
    public String toString() {
        return notation(count, adds) + (multiplier > 1 ? " x" + multiplier : "") + " = " + total();
    }
}
