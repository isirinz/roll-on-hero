package com.trialmobile.myfantasy.rules;

import java.util.Random;

/**
 * 6면체 주사위. 터널스 앤 트롤스는 d6만 쓴다.
 * 테스트와 시뮬레이션을 위해 시드를 지정할 수 있다.
 */
public final class Dice {
    private final Random random;

    public Dice() {
        this(new Random());
    }

    public Dice(long seed) {
        this(new Random(seed));
    }

    public Dice(Random random) {
        this.random = random;
    }

    public int d6() {
        return random.nextInt(6) + 1;
    }

    /** {@code count}D6 + {@code adds}를 굴린다. 6이 나온 개수(스파이트 데미지용)도 함께 센다. */
    public DiceRoll roll(int count, int adds) {
        int[] faces = new int[Math.max(0, count)];
        for (int i = 0; i < faces.length; i++) faces[i] = d6();
        return DiceRoll.ofFaces(adds, faces);
    }

    /** 3D6 TARO(Triples Add and Roll Over): 세 개가 모두 같으면 더하고 다시 굴린다. 능력치 생성용. */
    public int rollTaro() {
        int total = 0;
        while (true) {
            int a = d6(), b = d6(), c = d6();
            total += a + b + c;
            if (!(a == b && b == c)) return total;
        }
    }

    /**
     * 2D6 DARO(Doubles Add and Roll Over): 두 개가 같으면 더하고 다시 굴린다. 세이빙 롤용.
     * 단, 첫 굴림이 1-2(합 3)면 자동 실패로 간주하도록 호출자가 처리한다.
     */
    public int rollDaro() {
        int total = 0;
        while (true) {
            int a = d6(), b = d6();
            total += a + b;
            if (a != b) return total;
        }
    }

    /** 0 이상 {@code bound} 미만의 정수. */
    public int nextInt(int bound) {
        return bound <= 0 ? 0 : random.nextInt(bound);
    }

    /** 0 이상 1 미만의 실수. */
    public float nextFloat() {
        return random.nextFloat();
    }
}
