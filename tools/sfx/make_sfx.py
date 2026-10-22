#!/usr/bin/env python3
"""
효과음 합성기: 모든 효과음을 코드로 만든다(외부 음원·라이선스 걱정 없음, 8/16비트 레트로 톤).

    python3 tools/sfx/make_sfx.py            # assets/sfx/*.wav 전부 다시 만든다
    python3 tools/sfx/make_sfx.py coin hit   # 일부만
    python3 tools/sfx/make_sfx.py --preview  # + 전부 이어 붙인 tools/sfx/preview.wav

출력: 22050Hz 모노 16비트 WAV. (iOS 백엔드는 OGG를 못 읽어서 WAV를 쓴다. 짧은 효과음이라 용량은 작다.)
같은 이름이면 매번 같은 소리가 나도록 이름으로 난수 시드를 고정한다.
"""
import os
import sys
import wave
import zlib

import numpy as np

SR = 22050
ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "assets", "sfx")

rng = np.random.default_rng(0)


# ---------------------------------------------------------------------------
# 기본 재료

def t_of(dur):
    return np.arange(int(SR * dur)) / SR


def note(name):
    """'C5', 'F#4' → 주파수."""
    names = {"C": 0, "C#": 1, "D": 2, "D#": 3, "E": 4, "F": 5, "F#": 6, "G": 7, "G#": 8, "A": 9, "A#": 10, "B": 11}
    pitch, octave = name[:-1], int(name[-1])
    n = names[pitch] + (octave + 1) * 12
    return 440.0 * 2 ** ((n - 69) / 12)


def phase(freq, dur):
    """주파수(상수 또는 배열)를 적분한 위상(0~1 주기)."""
    n = int(SR * dur)
    f = np.broadcast_to(np.asarray(freq, dtype=float), (n,)) if np.ndim(freq) == 0 else np.asarray(freq)[:n]
    return np.cumsum(f) / SR


def osc(kind, freq, dur, duty=0.5):
    p = phase(freq, dur) % 1.0
    if kind == "sine":
        return np.sin(2 * np.pi * p)
    if kind == "square":
        return np.where(p < duty, 1.0, -1.0)
    if kind == "saw":
        return 2 * p - 1
    if kind == "tri":
        return 4 * np.abs(p - 0.5) - 1
    raise ValueError(kind)


def noise(dur):
    return rng.uniform(-1, 1, int(SR * dur))


def sweep(f0, f1, dur, curve="exp"):
    n = int(SR * dur)
    x = np.linspace(0, 1, n)
    if curve == "exp":
        return f0 * (f1 / f0) ** x
    return f0 + (f1 - f0) * x


def env(dur, attack=0.005, decay=None, sustain=1.0, release=0.05, curve=4.0):
    """간단한 엔벨로프. decay를 주면 attack 뒤 지수 감쇠(타악기형)."""
    n = int(SR * dur)
    t = np.arange(n) / SR
    e = np.ones(n)
    a = max(1, int(SR * attack))
    e[:a] = np.linspace(0, 1, a)
    if decay is not None:
        e[a:] = np.exp(-(t[a:] - attack) / decay)
    else:
        r = max(1, int(SR * release))
        e[a:] = sustain
        e[-r:] *= np.linspace(1, 0, r) ** (curve / 4)
    return e


def lowpass(x, cutoff):
    """1극 저역 통과. cutoff는 상수 또는 배열(Hz)."""
    c = np.broadcast_to(np.asarray(cutoff, dtype=float), x.shape)
    a = 1 - np.exp(-2 * np.pi * c / SR)
    y = np.empty_like(x)
    acc = 0.0
    for i in range(len(x)):
        acc += a[i] * (x[i] - acc)
        y[i] = acc
    return y


def highpass(x, cutoff):
    return x - lowpass(x, cutoff)


def bandpass(x, lo, hi):
    return highpass(lowpass(x, hi), lo)


def echo(x, delay=0.08, feedback=0.35, taps=4):
    d = int(SR * delay)
    y = np.concatenate([x, np.zeros(d * taps)])
    for k in range(1, taps + 1):
        y[d * k:d * k + len(x)] += x * feedback ** k
    return y


def place(total, part, at):
    """part를 at초 위치에 더한다(넘치면 늘린다)."""
    i = int(SR * at)
    need = i + len(part)
    if need > len(total):
        total = np.concatenate([total, np.zeros(need - len(total))])
    total[i:i + len(part)] += part
    return total


def blank(dur=0.0):
    return np.zeros(int(SR * dur))


def crush(x, bits=6):
    """살짝 비트를 깎아 레트로 질감."""
    q = 2 ** (bits - 1)
    return np.round(x * q) / q


def tone(kind, f, dur, vol=0.5, attack=0.004, decay=None, release=0.04, duty=0.5, vibrato=0.0, vib_rate=7.0):
    t = t_of(dur)
    freq = f * (1 + vibrato * np.sin(2 * np.pi * vib_rate * t)) if vibrato else f
    return osc(kind, freq, dur, duty) * env(dur, attack, decay, release=release) * vol


def arpeggio(notes, step, kind="square", vol=0.35, hold=None, duty=0.5, decay=0.12):
    out = blank()
    for i, nm in enumerate(notes):
        d = hold if (hold and i == len(notes) - 1) else step * 2.2
        out = place(out, tone(kind, note(nm), d, vol, decay=decay if not (hold and i == len(notes) - 1) else None,
                              release=0.15, duty=duty), i * step)
    return out


# ---------------------------------------------------------------------------
# 효과음들

def click(f_lo=1800, f_hi=5000, dur=0.012, vol=0.9):
    """나무·뼈 주사위 딸깍 한 번."""
    x = bandpass(noise(dur), f_lo, f_hi) * env(dur, 0.0005, decay=dur / 3) * vol
    x += tone("tri", rng.uniform(1100, 1700), dur, 0.25, attack=0.0005, decay=dur / 4)
    return x


def sfx_dice_shake():
    """주사위가 손에서 굴러 나가는 달그락 소리(여러 번 딸깍)."""
    out = blank(0.28)
    t = 0.0
    while t < 0.24:
        out = place(out, click(vol=rng.uniform(0.35, 0.8)), t)
        t += rng.uniform(0.018, 0.05)
    return lowpass(out, 7000)


def sfx_dice_land():
    """바닥에 탁 떨어져 한 번 튀고 멈춤."""
    thud = lowpass(noise(0.06), 900) * env(0.06, 0.001, decay=0.015) * 0.9
    thud += tone("sine", 180, 0.06, 0.5, attack=0.001, decay=0.02)
    out = place(blank(0.22), thud, 0.0)
    out = place(out, click(1500, 4500, 0.014, 0.9), 0.0)
    out = place(out, click(1800, 5000, 0.012, 0.45), 0.09)
    out = place(out, click(2000, 5200, 0.010, 0.2), 0.15)
    return out


def sfx_dice_six():
    """6이 나왔을 때 반짝."""
    out = blank()
    for i, nm in enumerate(["E6", "B6", "E7"]):
        out = place(out, tone("square", note(nm), 0.16, 0.18, decay=0.06, duty=0.25), i * 0.05)
        out = place(out, tone("sine", note(nm) * 2, 0.2, 0.12, decay=0.08), i * 0.05)
    return echo(out, 0.07, 0.3, 3)


def sfx_total_win():
    """이긴 쪽 총합이 튀어나올 때 '띵'."""
    out = tone("square", note("A5"), 0.08, 0.22, decay=0.04, duty=0.25)
    out = place(out, tone("square", note("E6"), 0.22, 0.25, decay=0.09, duty=0.25), 0.06)
    out = place(out, tone("sine", note("E7"), 0.25, 0.1, decay=0.08), 0.06)
    return out


def sfx_hit():
    """칼로 베어 맞힘: 바람 가르는 소리 + 묵직한 타격."""
    d = 0.16
    swish = bandpass(noise(d), 800, 6000) * env(d, 0.01, decay=0.04)
    swish = lowpass(swish, sweep(6000, 1200, d)) * 0.8
    thump = osc("sine", sweep(160, 50, 0.14), 0.14) * env(0.14, 0.001, decay=0.05) * 0.9
    out = place(blank(0.2), swish, 0.0)
    out = place(out, thump, 0.035)
    out = place(out, lowpass(noise(0.05), 2500) * env(0.05, 0.001, decay=0.012) * 0.7, 0.035)
    return out


def sfx_hurt():
    """영웅이 맞음: 둔탁한 소리 + 짧게 내려가는 삑."""
    out = osc("sine", sweep(120, 45, 0.16), 0.16) * env(0.16, 0.001, decay=0.06) * 0.9
    out = place(out, lowpass(noise(0.07), 1200) * env(0.07, 0.001, decay=0.02) * 0.8, 0.0)
    out = place(out, osc("square", sweep(500, 220, 0.12), 0.12, 0.3) * env(0.12, 0.002, decay=0.05) * 0.25, 0.02)
    return out


def sfx_monster_die():
    """몬스터 처치: 펑 + 아래로 떨어지는 삐융."""
    d = 0.45
    poof = lowpass(noise(d), sweep(3000, 200, d)) * env(d, 0.003, decay=0.12) * 0.8
    fall = osc("square", sweep(700, 90, 0.35), 0.35, 0.4) * env(0.35, 0.002, decay=0.14) * 0.3
    return crush(place(poof, fall, 0.02), 7)


def sfx_hero_down():
    """영웅이 쓰러짐: 단조로 내려가는 소리."""
    out = arpeggio(["G4", "D#4", "C4", "G3"], 0.13, "square", 0.3, hold=0.45, duty=0.5)
    return lowpass(out, 3500)


def sfx_level_up():
    out = arpeggio(["C5", "E5", "G5", "C6", "E6", "G6"], 0.055, "square", 0.25, hold=0.35, duty=0.25)
    out = place(out, arpeggio(["C4", "G4", "C5"], 0.11, "tri", 0.3, hold=0.3), 0.0)
    out = place(out, sfx_dice_six() * 0.6, 0.3)
    return echo(out, 0.09, 0.25, 2)


def sfx_coin():
    out = tone("square", note("B5"), 0.07, 0.25, duty=0.5, release=0.01)
    out = place(out, tone("square", note("E6"), 0.28, 0.25, decay=0.1, duty=0.5), 0.07)
    return out


def sfx_stage_clear():
    out = arpeggio(["C5", "E5", "G5"], 0.09, "square", 0.25, duty=0.25)
    out = place(out, tone("square", note("C6"), 0.6, 0.25, release=0.25, duty=0.25, vibrato=0.006), 0.27)
    out = place(out, tone("tri", note("C4"), 0.9, 0.35, release=0.3), 0.0)
    out = place(out, tone("tri", note("G4"), 0.6, 0.25, release=0.25), 0.27)
    return echo(out, 0.1, 0.2, 2)


def sfx_boss_appear():
    """보스 등장: 북 두 번 + 낮게 으르렁 올라가는 소리."""
    def drum(vol):
        return (osc("sine", sweep(110, 40, 0.3), 0.3) * env(0.3, 0.001, decay=0.1)
                + lowpass(noise(0.3), 400) * env(0.3, 0.001, decay=0.05) * 0.6) * vol
    out = place(blank(1.1), drum(1.0), 0.0)
    out = place(out, drum(0.9), 0.22)
    growl_d = 0.8
    growl = osc("saw", sweep(55, 110, growl_d), growl_d) + osc("saw", sweep(55.6, 111, growl_d), growl_d)
    growl = lowpass(growl, sweep(300, 1400, growl_d)) * env(growl_d, 0.15, release=0.3) * 0.35
    return place(out, growl, 0.3)


def sfx_spell_bolt():
    """테이크 댓: 찌릿한 번개."""
    d = 0.3
    zap = osc("square", sweep(1800, 180, d), d, 0.3) * env(d, 0.002, decay=0.1) * 0.35
    zap *= 0.6 + 0.4 * np.sign(np.sin(2 * np.pi * 45 * t_of(d)))
    crackle = highpass(noise(d), 2500) * env(d, 0.001, decay=0.06) * 0.4
    return crush(zap + crackle, 6)


def sfx_spell_heal():
    out = blank()
    for i, nm in enumerate(["C6", "E6", "G6", "C7"]):
        out = place(out, tone("sine", note(nm), 0.5, 0.22, attack=0.02, decay=0.18), i * 0.07)
        out = place(out, tone("tri", note(nm) / 2, 0.4, 0.12, attack=0.02, decay=0.15), i * 0.07)
    return echo(out, 0.11, 0.35, 3)


def sfx_spell_vorpal():
    """보팔(썰고 다지고): 칼날이 '챙' 하고 빛남."""
    d = 0.6
    t = t_of(d)
    ring = sum(np.sin(2 * np.pi * f * t) * a for f, a in [(2350, 0.4), (3520, 0.3), (5210, 0.2), (7040, 0.1)])
    ring *= env(d, 0.002, decay=0.16) * 0.6
    swoosh = lowpass(bandpass(noise(0.18), 1500, 9000), sweep(9000, 2000, 0.18)) * env(0.18, 0.02, decay=0.05) * 0.6
    return place(swoosh, ring, 0.05)


def sfx_spell_fire():
    """펑 하고 터져라: 불덩이 휙 + 화르륵."""
    d = 0.6
    whoosh = lowpass(noise(d), sweep(300, 3000, d)) * env(d, 0.25, release=0.2) * 0.7
    crackle = np.zeros(int(SR * d))
    for _ in range(18):
        crackle = place(crackle, click(2500, 7000, 0.006, rng.uniform(0.2, 0.5)), rng.uniform(0.2, d - 0.02))
    boom = lowpass(noise(0.35), 900) * env(0.35, 0.002, decay=0.1) * 0.9
    out = place(whoosh + crackle[:len(whoosh)], boom, 0.42)
    return out


def sfx_spell_hellbomb():
    """다들 엎드려!!: 큰 폭발."""
    d = 1.1
    boom = lowpass(noise(d), sweep(4000, 120, d)) * env(d, 0.002, decay=0.28) * 1.0
    sub = osc("sine", sweep(90, 28, d), d) * env(d, 0.002, decay=0.35) * 0.9
    debris = np.zeros(int(SR * d))
    for _ in range(25):
        debris = place(debris, click(800, 3000, 0.01, rng.uniform(0.1, 0.3)), rng.uniform(0.15, d - 0.05))
    pre = osc("square", sweep(200, 1600, 0.15), 0.15, 0.5) * env(0.15, 0.01, release=0.02) * 0.2
    out = place(blank(1.3), pre, 0.0)
    return crush(place(out, boom + sub + debris[:len(boom)], 0.14), 8)


def sfx_ui_click():
    return tone("square", 1200, 0.03, 0.25, attack=0.001, decay=0.008, duty=0.25)


def sfx_ui_tab():
    """탭 넘김: 종이 스치는 소리 + 작은 톡."""
    out = bandpass(noise(0.06), 1500, 6000) * env(0.06, 0.005, decay=0.015) * 0.5
    return place(out, tone("tri", 700, 0.04, 0.3, attack=0.001, decay=0.012), 0.0)


def sfx_ui_buy():
    """구매: 짤랑 + 계산대 '칭'."""
    out = sfx_coin()
    out = place(out, tone("sine", note("E7"), 0.35, 0.15, decay=0.12), 0.08)
    out = place(out, tone("sine", note("B6"), 0.35, 0.12, decay=0.12), 0.1)
    return out


def sfx_ui_upgrade():
    """강화: 올라가는 세 음 + 망치 톡."""
    out = place(blank(), lowpass(noise(0.03), 3000) * env(0.03, 0.001, decay=0.008) * 0.6, 0.0)
    out = place(out, arpeggio(["G5", "C6", "E6"], 0.045, "square", 0.22, duty=0.25, decay=0.06), 0.02)
    return out


def sfx_boost_on():
    """2배 버프 켜짐: 위로 휘몰아 올라가는 파워업."""
    d = 0.7
    rise = osc("square", sweep(200, 1600, d), d, 0.3) * env(d, 0.01, release=0.05) * 0.25
    rise *= 0.7 + 0.3 * osc("square", 24, d)
    out = place(blank(1.3), rise, 0.0)
    out = place(out, arpeggio(["C6", "E6", "G6", "C7"], 0.06, "square", 0.22, hold=0.45, duty=0.25), 0.62)
    out = place(out, tone("tri", note("C5"), 0.6, 0.35, release=0.3), 0.62)
    return echo(out, 0.08, 0.3, 3)


def sfx_rebirth():
    """환생: 낮은 울림에서 빛이 차오르는 신비로운 화음."""
    d = 2.0
    t = t_of(d)
    out = np.zeros(len(t))
    for nm, a in [("A3", 0.25), ("E4", 0.2), ("A4", 0.18), ("C5", 0.15), ("B5", 0.1), ("E6", 0.08)]:
        f = note(nm)
        out += (np.sin(2 * np.pi * f * t) + 0.3 * np.sin(2 * np.pi * f * 2.003 * t)) * a
    swell = np.clip(t / 1.1, 0, 1) ** 2 * np.clip((d - t) / 0.7, 0, 1)
    out *= swell
    shimmer = np.zeros(len(t))
    for i in range(14):
        f = note(["E6", "A6", "B6", "E7", "C7"][i % 5])
        shimmer = place(shimmer, tone("sine", f, 0.25, 0.08, decay=0.08), 0.5 + i * 0.08)
    out = out + shimmer[:len(out)]
    return echo(out, 0.13, 0.3, 4)


SOUNDS = {
    "dice_shake": sfx_dice_shake,
    "dice_land": sfx_dice_land,
    "dice_six": sfx_dice_six,
    "total_win": sfx_total_win,
    "hit": sfx_hit,
    "hurt": sfx_hurt,
    "monster_die": sfx_monster_die,
    "hero_down": sfx_hero_down,
    "level_up": sfx_level_up,
    "coin": sfx_coin,
    "stage_clear": sfx_stage_clear,
    "boss_appear": sfx_boss_appear,
    "spell_bolt": sfx_spell_bolt,
    "spell_heal": sfx_spell_heal,
    "spell_vorpal": sfx_spell_vorpal,
    "spell_fire": sfx_spell_fire,
    "spell_hellbomb": sfx_spell_hellbomb,
    "ui_click": sfx_ui_click,
    "ui_tab": sfx_ui_tab,
    "ui_buy": sfx_ui_buy,
    "ui_upgrade": sfx_ui_upgrade,
    "boost_on": sfx_boost_on,
    "rebirth": sfx_rebirth,
}


def finish(x, peak=0.85):
    x = np.asarray(x, dtype=float)
    # 앞뒤 무음 정리 + 끝을 아주 짧게 페이드(딸깍 소리 방지)
    nz = np.nonzero(np.abs(x) > 1e-4)[0]
    if len(nz):
        x = x[:nz[-1] + 1]
    fade = min(len(x), int(SR * 0.005))
    x[-fade:] *= np.linspace(1, 0, fade)
    m = np.max(np.abs(x)) or 1.0
    return x / m * peak


def write_wav(path, x):
    data = (np.clip(x, -1, 1) * 32767).astype("<i2").tobytes()
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(data)


def main(argv):
    global rng
    preview = "--preview" in argv
    names = [a for a in argv if not a.startswith("--")] or list(SOUNDS)
    os.makedirs(OUT, exist_ok=True)
    clips = []
    for name in names:
        rng = np.random.default_rng(zlib.crc32(name.encode()))
        x = finish(SOUNDS[name]())
        write_wav(os.path.join(OUT, name + ".wav"), x)
        clips.append(x)
        print(f"{name:16s} {len(x) / SR:5.2f}s")
    if preview:
        gap = np.zeros(int(SR * 0.35))
        write_wav(os.path.join(os.path.dirname(__file__), "preview.wav"),
                  np.concatenate([np.concatenate([c, gap]) for c in clips]))


if __name__ == "__main__":
    main(sys.argv[1:])
