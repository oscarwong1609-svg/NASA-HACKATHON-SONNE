"""Pixel-art source for Mission Lab. Hand-drawn string sprites plus a few procedural ones (medals, patches).
Run:  python3 art.py preview out.png      -> zoomed contact sheet
      python3 art.py java SpriteData.java  -> generated Java data
"""
import sys, math
from PIL import Image, ImageDraw

# ---------------------------------------------------------------- shared palette (char -> hex)
PAL = {
    'k': '141826',  # outline
    'K': '0b0e1a',  # deepest
    'w': 'ffffff', 'W': 'dfe4ef', 'u': 'b4bccc', 'U': '8a93a8',  # suit whites / shades
    's': '9aa3b5', 'S': '5d6680', 'T': '3a4258',  # metal
    'v': '1b2a4a', 'V': '5ec8f2', 'i': 'bfeaff',  # visor
    'b': '3d6ff2', 'B': '1f3d8f', 'c': '86b4ff', 'C': '27428a',  # solar
    'g': 'f2c43d', 'G': 'a8741a', 'y': 'fff1a0', 'Y': 'd89a2a',  # gold foil
    'r': 'e8504f', 'R': '9b2c2c',
    'o': 'f28a3d', 'O': 'b4561e',
    'e': '4fd17a', 'E': '2a8a4c', 'j': 'a6f0c0',
    'p': 'b07ef2', 'P': '6b44a8',
    'a': 'ff00ff', 'A': '990099',  # player accent placeholders (recoloured at runtime)
    'n': '2b3352', 'N': '3a4466',
    'x': '101425',
    'm': 'd0d7e3', 'M': '7d8798', 'L': '4a5266',  # aluminium
    't': '8a93b0',
    'f': '070910', 'h': '2a3a5c', 'q': '6aa9ff',  # lens
    'z': 'c87533', 'Z': '7a3f17', 'Q': 'e8a065',  # copper
}

S = {}  # name -> list of rows

# ---------------------------------------------------------------- astronaut (10 x 14), facing right
S['astro_0'] = [
    "...kkkkk..",
    "..kWwwWWk.",
    ".kWwWWkkkk",
    "kkWWWkviVk",
    "kSkWWkvvVk",
    "kSkWWkvvvk",
    "kSkuWWkkkk",
    "kSkkaaaak.",
    "kSkWWWWWWk",
    "kSkWAaWkuk",
    ".kkWWWWkk.",
    "..kWUkWUk.",
    "..kWUkWUk.",
    "..kAAkAAk.",
]
S['astro_1'] = [
    "...kkkkk..",
    "..kWwwWWk.",
    ".kWwWWkkkk",
    "kkWWWkviVk",
    "kSkWWkvvVk",
    "kSkWWkvvvk",
    "kSkuWWkkkk",
    "kSkkaaaak.",
    "kSkWWWWWWk",
    "kSkWAaWkuk",
    ".kkWWWWkk.",
    ".kWUkkWUk.",
    "kWUk..kWUk",
    "kAAk..kAAk",
]

# ---------------------------------------------------------------- parts (12 x 12)
# Instruments: gold-foil cameras / telescopes, getting bigger and more complex.
S['cam_basic'] = [
    "............",
    "...kkkkkk...",
    "..kyygggGk..",
    "..kgkkkkGk..",
    "..kykhhkGk..",
    "..kgkqfkYk..",
    "..kgkhfkGk..",
    "..kykkkkGk..",
    "..kgYgGYGk..",
    "..kGGGGGGk..",
    "...kSkkSk...",
    "....k..k....",
]
S['cam_sharp'] = [
    "..kkkkkkk...",
    ".kmmmmmmMk..",
    ".kkffffkkk..",
    ".kmfqiffMk..",
    ".kmmmmmmMk..",
    "..kmmmmMk...",
    "..kyggGYk...",
    "..kgyGGYk...",
    "..kyggGYk...",
    "..kgygGYk...",
    "..kGGGGGk...",
    "..kkSSSkk...",
]
S['cam_thermal'] = [
    "..kkkkkkkk..",
    ".kygggggGYk.",
    ".kgkkkkkkGk.",
    ".kgkhhhhkGk.",
    ".kgkhpofkGk.",
    ".kgkhofhkGk.",
    ".kgkhhhhkGk.",
    ".kgkkkkkkGk.",
    ".kGGGGGGGYk.",
    "kSkSkSkSkSkk",
    "kskskskskskk",
    ".k.k.k.k.k..",
]
# Solar panels: blue cells, grid lines, aluminium frames; more wings per tier.
S['panels_body'] = [
    "............",
    "............",
    ".kkkkkkkkkk.",
    ".kmmmmmmmmk.",
    ".kmcbbcbbmk.",
    ".kmbBbbBbmk.",
    ".kmBBBBBBmk.",
    ".kmcbbcbbmk.",
    ".kmbBbbBbmk.",
    ".kmmmmmmmmk.",
    ".kkkkkkkkkk.",
    "............",
]
S['panels_mid'] = [
    "............",
    "kkkkk..kkkkk",
    "kmmmk..kmmmk",
    "kcbmk..kcbmk",
    "kbBmk..kbBmk",
    "kmmmkkkkmmmk",
    "kcbmkSSkcbmk",
    "kbBmkkkkbBmk",
    "kmmmk..kmmmk",
    "kkkkk..kkkkk",
    "............",
    "............",
]
S['panels_mid'] = [
    "............",
    "............",
    "kkkkk..kkkkk",
    "kcbBk..kcbBk",
    "kbBBk..kbBBk",
    "kmmmkkkkmmmk",
    "kcbBkmMkcbBk",
    "kbBBkkkkbBBk",
    "kmmmk..kmmmk",
    "kkkkk..kkkkk",
    "............",
    "............",
]
S['panels_fold'] = [
    "kkkkkkkkkkkk",
    "kcbkcbkcbkcb",
    "kbBkbBkbBkbB",
    "kmmkmmkmmkmm",
    "kcbkcbkcbkcb",
    "kbBkbBkbBkbB",
    "kkkkkkkkkkkk",
    ".....kk.....",
    "....kmMk....",
    "....kMLk....",
    ".....kk.....",
    "............",
]
S['panels_fold'] = [
    "kkkkk..kkkkk",
    "kcbBk..kcbBk",
    "kbBBk..kbBBk",
    "kmmmk..kmmmk",
    "kcbBkkkkcbBk",
    "kbBBkmMkbBBk",
    "kmmmkMLkmmmk",
    "kcbBkkkkcbBk",
    "kbBBk..kbBBk",
    "kmmmk..kmmmk",
    "kcbBk..kcbBk",
    "kkkkk..kkkkk",
]
# Batteries: cylindrical cells.
S['batt_small'] = [
    "............",
    ".....kk.....",
    "....kssk....",
    "...kkkkkk...",
    "...kjeeEk...",
    "...kjeeEk...",
    "...kmmmMk...",
    "...kjeeEk...",
    "...kjeeEk...",
    "...kjeeEk...",
    "...kkkkkk...",
    "............",
]
S['batt_large'] = [
    "............",
    "..kk.kk.kk..",
    ".kskkskkskk.",
    "kkkkkkkkkkkk",
    "kjeEkjeEkjeE",
    "kjeEkjeEkjeE",
    "kTTTTTTTTTTk",
    "kTrTTTTTTeTk",
    "kjeEkjeEkjeE",
    "kjeEkjeEkjeE",
    "kkkkkkkkkkkk",
    "............",
]
S['batt_large'] = [
    "............",
    ".kk..kk..kk.",
    "kssk.kssk.kk",
    "kkkkkkkkkkkk",
    "kjeEkjeEkjEk",
    "kjeEkjeEkjEk",
    "kTTTTTTTTTTk",
    "kTrTSSSSTeTk",
    "kjeEkjeEkjEk",
    "kjeEkjeEkjEk",
    "kkkkkkkkkkkk",
    "............",
]
# Pointing: magnetorquer coils, then reaction wheel + star tracker.
S['point_mag'] = [
    "............",
    "............",
    "kkkkkkkkkkk.",
    "kSzQzQzQzSk.",
    "kSZzZzZzZSk.",
    "kkkkkkkkkkk.",
    "............",
    ".kkkkkkkkkkk",
    ".kSzQzQzQzSk",
    ".kSZzZzZzZSk",
    ".kkkkkkkkkkk",
    "............",
]
S['point_wheels'] = [
    ".....kkkk...",
    "....kTTTTk..",
    "...kkkkkkkk.",
    "..kkmmmmmmMk",
    ".kmmMkkkMmMk",
    ".kmMkpppkMMk",
    ".kmkpPiPpkMk",
    ".kmMkpppkMMk",
    ".kmmMkkkMmMk",
    "..kMmmmmmMk.",
    "...kkkkkkk..",
    "............",
]
S['point_wheels'] = [
    "........kkk.",
    ".......kSSSk",
    "..kkkk.khifk",
    ".kmmmmkkkkk.",
    "kmMkkMmkSk..",
    "kmkppkMkk...",
    "kmkpikMk....",
    "kmMkkMMk....",
    ".kMMMMk.....",
    "..kkkk......",
    "............",
    "............",
]
S['point_wheels'] = [
    "........kkkk",
    ".......kSSSk",
    ".......khifk",
    "..kkkk.kkkk.",
    ".kmmmMkkSk..",
    "kmMkkMMkk...",
    "kmkppkMMk...",
    "kmkpikMMk...",
    "kmMkkMMMk...",
    ".kMMMMMk....",
    "..kkkkk.....",
    "............",
]
# Frames: aluminium cages that grow 2U -> 3U -> 6U.
S['frame_2u'] = [
    "............",
    "............",
    "...kkkkkk...",
    "...kmMMmk...",
    "...kM..Mk...",
    "...kmMMmk...",
    "...kM..Mk...",
    "...kmMMmk...",
    "...kkkkkk...",
    "...kk..kk...",
    "............",
    "............",
]
S['frame_3u'] = [
    "...kkkkkk...",
    "...kmMMmk...",
    "...kM..Mk...",
    "...kmMMmk...",
    "...kM..Mk...",
    "...kmMMmk...",
    "...kM..Mk...",
    "...kmMMmk...",
    "...kkkkkk...",
    "...kk..kk...",
    "............",
    "............",
]
S['frame_6u'] = [
    "kkkkkkkkkkk.",
    "kmMMmMMmMMk.",
    "kM..M..M.Mk.",
    "kmMMmMMmMMk.",
    "kM..M..M.Mk.",
    "kmMMmMMmMMk.",
    "kM..M..M.Mk.",
    "kmMMmMMmMMk.",
    "kkkkkkkkkkk.",
    "kk.......kk.",
    "............",
    "............",
]
S['frame_6u'] = [
    "kkkkkkkkkkkk",
    "kmMMmkkmMMmk",
    "kM..Mk.M..Mk",
    "kmMMmkkmMMmk",
    "kM..Mk.M..Mk",
    "kmMMmkkmMMmk",
    "kM..Mk.M..Mk",
    "kmMMmkkmMMmk",
    "kkkkkkkkkkkk",
    "kk..kk.kk.kk",
    "............",
    "............",
]
# Radios: parabolic dishes with a transceiver box, bigger dish per tier.
S['radio_s'] = [
    "............",
    "............",
    "............",
    ".....kk.....",
    "....kmMk....",
    "...kmmMMk...",
    "....kkkk....",
    ".....kk.....",
    "...kkkkkk...",
    "...kpPpPk...",
    "...kPpPpk...",
    "...kkkkkk...",
]
S['radio_s_fast'] = [
    "............",
    "........k...",
    ".......ks...",
    "..kkkkkkk...",
    ".kmmmmmMMk..",
    "..kmmmMMk...",
    "...kkkkk....",
    ".....kk.....",
    "..kkkkkkkk..",
    "..kpPpPpek..",
    "..kPpPpPpk..",
    "..kkkkkkkk..",
]
S['radio_x'] = [
    ".........kk.",
    "........kgk.",
    ".kkkkkkkgk..",
    "kmmmmmmMMk..",
    "kmwmmmMMMk..",
    ".kmmmMMMk...",
    "..kkkkkk....",
    "....kSk.....",
    ".kkkkkkkkk..",
    ".kygggGGek..",
    ".kgGgGgGGk..",
    ".kkkkkkkkk..",
]

# ---------------------------------------------------------------- stations, rockets, satellite
S['dish_ground'] = [  # 16 x 14 ground-station dish
    "..........kk....",
    ".....kkkkkgk....",
    "...kkmmmmkk.....",
    "..kmmwmmmMk.....",
    ".kmmmmmmMMk.....",
    ".kmmmmmMMMk.....",
    "kmmmmmMMMk......",
    "kmmmmMMMLk......",
    ".kmMMMMLk.......",
    "..kkkkkk........",
    "....kSSk........",
    "....kSSk........",
    "..kkSSSSkk......",
    ".kTTTTTTTTk.....",
]
S['cubesat'] = [  # 19 x 9 : 3U body, two solar wings, antenna
    ".........k.........",
    ".........s.........",
    "kkkkkkkkkkkkkkkkkkk",
    "kcbBcbBkkggkkcbBcbBk"[:19],
    "kbBBbBBkgyGkkbBBbBBk"[:19],
    "kmmmmmmkgyGkmmmmmmk"[:19],
    "kcbBcbBkkGGkkcbBcbBk"[:19],
    "kkkkkkkkkkkkkkkkkkk",
    "...................",
]
S['cubesat'] = [
    ".........k.........",
    ".........s.........",
    "kkkkkkk.kkk.kkkkkkk",
    "kcbBcbBkkgYkkcbBcbk",
    "kbBBbBBSkyGkSbBBbBk",
    "kmmmmmmkkgGkkmmmmmk",
    "kcbBcbBkkGYkkcbBcbk",
    "kkkkkkk.kkk.kkkkkkk",
    "...................",
]
S['cubesat_dim'] = [r.replace('c', 'U').replace('b', 'U').replace('B', 'S').replace('g', 'u').replace('y', 'u')
                    .replace('Y', 'U').replace('G', 'S').replace('m', 'U') for r in S['cubesat']]

S['rocket_big'] = [  # 14 x 58 rideshare launcher with fairing
    "......kk......",
    ".....kwWk.....",
    "....kwWWuk....",
    "....kwWWuk....",
    "...kwWWWuUk...",
    "...kwWWWuUk...",
    "..kwWWWWuUUk..",
    "..kwWWWWuUUk..",
    "..kwWWWWuUUk..",
    "..kwWWWWuUUk..",
    "..kwWWWWuUUk..",
    "..kwWWWWuUUk..",
    "..kwWWWWuUUk..",
    "..kwWWWWuUUk..",
    "..kkkkkkkkkk..",
    "...kTTTTTTk...",
    "...kwWWWuUk...",
    "...kwWWWuUk...",
    "...kwWWWuUk...",
    "...kwWWWuUk...",
    "...kwWWWuUk...",
    "...kwWWWuUk...",
    "...kTTTTTTk...",
    "...kTSTTSTk...",
    "...kwWWWuUk...",
    "...kwWWWuUk...",
    "...kwWWWuUk...",
    "...kwWWWuUk...",
    "...kwWWWuUk...",
    "...kwWWWuUk...",
    "...kwWWWuUk...",
    "...kwWWWuUk...",
    "...kwWWWuUk...",
    "...kwWWWuUk...",
    "...kwrrrRRk...",
    "...kwWWWuUk...",
    "...kwWWWuUk...",
    "...kwWWWuUk...",
    "...kwWWWuUk...",
    "...kwWWWuUk...",
    "...kwWWWuUk...",
    "...kwWWWuUk...",
    "...kwWWWuUk...",
    "...kwWWWuUk...",
    "...kwWWWuUk...",
    "...kwWWWuUk...",
    "...kwWWWuUk...",
    "...kTTTTTTk...",
    "..kkwWWWuUkk..",
    ".kSkwWWWuUkSk.",
    ".kSkwWWWuUkSk.",
    "kSSkwWWWuUkSSk",
    "kSSkTTTTTTkSSk",
    "kkkkkTTTTkkkkk",
    "....kSTTSk....",
    "...kSSTTSSk...",
    "...kTTTTTTk...",
    "...kkkkkkkk...",
]
S['rocket_small'] = [  # 12 x 46 ISS cargo launcher with capsule
    ".....kk.....",
    "....kssk....",
    "....kTTk....",
    "...kwWuUk...",
    "...kwWuUk...",
    "..kwWWuUUk..",
    "..kwvVuvUk..",
    "..kwWWuUUk..",
    ".kwWWWuUUUk.",
    ".kkkkkkkkkk.",
    "..kbBkkbBk..",
    "..kbBkkbBk..",
    "..kkkkkkkk..",
    "..kwWWWuUk..",
    "..kwWWWuUk..",
    "..kwWWWuUk..",
    "..kTTTTTTk..",
    "..kwWWWuUk..",
    "..kwWWWuUk..",
    "..kwWWWuUk..",
    "..kwWWWuUk..",
    "..kwWWWuUk..",
    "..kwWWWuUk..",
    "..kwWWWuUk..",
    "..kworrRRk..",
    "..kwWWWuUk..",
    "..kwWWWuUk..",
    "..kwWWWuUk..",
    "..kwWWWuUk..",
    "..kwWWWuUk..",
    "..kwWWWuUk..",
    "..kwWWWuUk..",
    "..kwWWWuUk..",
    "..kwWWWuUk..",
    "..kwWWWuUk..",
    "..kwWWWuUk..",
    "..kTTTTTTk..",
    ".kkwWWWuUkk.",
    "kSkwWWWuUkSk",
    "kSkwWWWuUkSk",
    "kSkTTTTTTkSk",
    "kkkkSTTSkkkk",
    "...kSTTSk...",
    "..kSSTTSSk..",
    "..kTTTTTTk..",
    "..kkkkkkkk..",
]
S['tug'] = [
    "..kkkk..",
    ".kggGGk.",
    "kSkkkkSk",
    "kSgyGGSk",
    ".kkkkkk.",
    "..kTTk..",
    "...kk...",
]

# ---------------------------------------------------------------- UI icons (9 x 9, drawn at font scale)
S['ic_target'] = [
    "....V....",
    "..VVVVV..",
    ".V..V..V.",
    ".V.....V.",
    "VVV.r.VVV",
    ".V.....V.",
    ".V..V..V.",
    "..VVVVV..",
    "....V....",
]
S['ic_budget'] = [
    "..kkkkk..",
    ".kygggGk.",
    "kygkkkGGk",
    "kgkgGGgGk",
    "kgGkkkGGk",
    "kgGGGkgGk",
    "kgkkkkGYk",
    ".kGGGGYk.",
    "..kkkkk..",
]
S['ic_launch'] = [
    "....k....",
    "...kWk...",
    "...kWk...",
    "..kWVWk..",
    "..kWWWk..",
    ".kkWWWkk.",
    ".kSkWkSk.",
    "..k.o.k..",
    "....r....",
]
S['ic_life'] = [
    ".kkkkkkk.",
    ".kmmmmmk.",
    "..kyyyk..",
    "...kgk...",
    "....k....",
    "...k.k...",
    "..kgggk..",
    ".kmmmmmk.",
    ".kkkkkkk.",
]
S['ic_detail'] = [
    "..kkkk...",
    ".kiVVqk..",
    "kiVqVqhk.",
    "kVqVqVhk.",
    "kqVqVhhk.",
    ".khhhhkk.",
    "..kkkkSSk",
    ".......kS",
    "........k",
]
S['ic_science'] = [
    "....k....",
    "....s....",
    "kkkkkkkkk",
    "kbBkgkbBk",
    "kbBkGkbBk",
    "kkkkkkkkk",
    ".........",
    ".........",
    ".........",
]
S['ic_globe'] = [
    "..kkkkk..",
    ".kbeebbk.",
    "kbeeebbbk",
    "kbbeebeek",
    "kbbbebeek",
    "kbbbbbebk",
    "kbbebbbbk",
    ".kbbbbbk.",
    "..kkkkk..",
]
S['ic_dish'] = [
    "......kVk",
    "..kkkk.k.",
    ".kmmmMk..",
    "kmmmMMk..",
    "kmmMMk...",
    ".kMMk....",
    "..kSk....",
    ".kSSSk...",
    "kTTTTTk..",
]
S['ic_led_on'] = [".kk.", "kwek", "keEk", ".kk."]

# ---------------------------------------------------------------- procedural: medals (26 x 32) and mission patches (24 x 24)
def blank(w, h):
    return [[None] * w for _ in range(h)]

STAR = [
    ".....y.....",
    "....yyy....",
    "....yyy....",
    "yyyyyyyyyyy",
    ".yyyyyyyyy.",
    "..yyyyyyy..",
    "...yyyyy...",
    "..yyy.yyy..",
    "..yy...yy..",
    ".y.......y.",
]

def medal(light, mid, dark, deep, ribbon1, ribbon2):
    W, H = 26, 32
    g = blank(W, H)
    # ribbon: a V-shaped band above the coin
    for y in range(0, 12):
        for x in range(W):
            lx = 7 + y * 0.35
            rx = W - 8 - y * 0.35
            if lx - 1 <= x <= lx + 4 or rx - 4 <= x <= rx + 1:
                left = x < W / 2
                g[y][x] = ribbon1 if (x % 3 != 0) == left else ribbon2
            if abs(x - (lx - 1)) < 0.5 or abs(x - (lx + 4)) < 0.5 or abs(x - (rx - 4)) < 0.5 or abs(x - (rx + 1)) < 0.5:
                if g[y][x] is not None: g[y][x] = '141826'
    cx, cy, r = 12.5, 20.5, 10.6
    for y in range(H):
        for x in range(W):
            dx, dy = x - cx, y - cy
            d = math.hypot(dx, dy)
            if d > r + 0.6: continue
            if d > r - 0.4:
                g[y][x] = '141826'; continue
            if d > r - 2.2:  # rim, lit from the top-left
                ang = math.atan2(dy, dx)
                k = math.cos(ang + 2.36)  # +1 top-left, -1 bottom-right
                g[y][x] = light if k > 0.45 else mid if k > -0.35 else dark
                continue
            if d > r - 3.0:
                g[y][x] = dark; continue
            # face: flat mid tone, a lighter crescent along the top-left edge, a short sheen streak
            t = (dx + dy) / (2 * r)
            col = mid
            if d > r - 4.2 and t < -0.2: col = light
            if d > r - 4.2 and t > 0.3: col = dark
            if -0.48 < t < -0.40 and d > r - 6.5: col = '#sheen'
            g[y][x] = col
    for y in range(H):
        for x in range(W):
            if g[y][x] == '#sheen': g[y][x] = 'ffffff' if (x + y) % 2 == 0 else light
    # star emblem: outlined in the dark tone so it reads at 1x, face in the light tone, one white glint
    sx, sy = 7, 15
    pts = {(sx + xx, sy + yy) for yy, row in enumerate(STAR) for xx, ch in enumerate(row) if ch == 'y'}
    for (px, py) in pts:
        for ox, oy in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1)):
            q = (px + ox, py + oy)
            if q not in pts and g[q[1]][q[0]] is not None: g[q[1]][q[0]] = dark
    for (px, py) in pts: g[py][px] = light
    g[sy + 1][sx + 5] = 'ffffff'; g[sy + 3][sx + 3] = 'ffffff'
    return g

PROC = {}
PROC['medal_gold'] = medal('ffe066', 'f2b52a', 'b47712', '7a4a08', 'e8504f', '9b2c2c')
PROC['medal_silver'] = medal('f4f7fb', 'b9c2d0', '7d8798', '4a5266', '5ec8f2', '2a6f9a')
PROC['medal_bronze'] = medal('f0a868', 'c87533', '8a4b1f', '5a2c0e', '4fd17a', '2a8a4c')
PROC['medal_none'] = medal('5d6680', '3a4258', '2b3352', '1b2033', '3a4258', '2b3352')

def patch(kind):
    W = H = 24
    g = blank(W, H)
    cx = cy = 11.5
    for y in range(H):
        for x in range(W):
            d = math.hypot(x - cx, y - cy)
            if d > 11.6: continue
            if d > 10.6: g[y][x] = '141826'
            elif d > 9.2: g[y][x] = 'f2c43d' if kind == 'fire' else 'dfe4ef'
            elif d > 8.4: g[y][x] = '141826'
            else:
                g[y][x] = '1b2a4a' if y < 13 else ('2f6b47' if kind == 'fire' else 'd6e6f2')
                if y == 13: g[y][x] = '5ec8f2'
    if kind == 'fire':
        flame = [
            "....r....",
            "...rr....",
            "...ror...",
            "..roor.r.",
            ".rooyorr.",
            ".royyyor.",
            "rooyyyoor",
            ".ryyyyyr.",
            "..rrrrr..",
        ]
        cmap = {'r': 'e8504f', 'o': 'f28a3d', 'y': 'fff1a0'}
        ox, oy = 7, 7
    else:
        flame = [
            "....w....",
            ".w..w..w.",
            "..w.w.w..",
            "...www...",
            "wwwwVwwww",
            "...www...",
            "..w.w.w..",
            ".w..w..w.",
            "....w....",
        ]
        cmap = {'w': 'ffffff', 'V': '5ec8f2'}
        ox, oy = 7, 5
    for yy, row in enumerate(flame):
        for xx, ch in enumerate(row):
            if ch != '.': g[oy + yy][ox + xx] = cmap[ch]
    # little satellite crossing the top of the patch
    for x, y, c in [(15, 4, 'f2c43d'), (14, 4, '3d6ff2'), (16, 4, '3d6ff2'), (13, 4, '3d6ff2'), (17, 4, '3d6ff2')]:
        g[y][x] = c
    return g

PROC['patch_fire'] = patch('fire')
PROC['patch_ice'] = patch('ice')

# ---------------------------------------------------------------- helpers
def grid(name):
    if name in PROC:
        return [[(('#' + c) if c and not c.startswith('#') else c) for c in row] for row in PROC[name]]
    rows = S[name]
    w = max(len(r) for r in rows)
    out = []
    for r in rows:
        out.append([None if ch == '.' else '#' + PAL[ch] for ch in r.ljust(w, '.')])
    return out

def names():
    return list(S.keys()) + list(PROC.keys())

def check():
    for n, rows in S.items():
        ws = {len(r) for r in rows}
        if len(ws) != 1: print('WARN width', n, ws)
        for r in rows:
            for ch in r:
                if ch != '.' and ch not in PAL: print('WARN char', n, ch)

def preview(path, zoom=6):
    items = [(n, grid(n)) for n in names()]
    x = y = 8; rowh = 0; W = 1500
    placed = []
    for n, g in items:
        w, h = len(g[0]) * zoom, len(g) * zoom
        if x + w + 8 > W: x = 8; y += rowh + 22; rowh = 0
        placed.append((n, g, x, y)); x += max(w, 70) + 14; rowh = max(rowh, h)
    im = Image.new('RGB', (W, y + rowh + 30), (11, 14, 26))
    d = ImageDraw.Draw(im)
    for n, g, x, y in placed:
        for yy, row in enumerate(g):
            for xx, c in enumerate(row):
                if c: d.rectangle([x + xx * zoom, y + 12 + yy * zoom, x + xx * zoom + zoom - 1, y + 12 + yy * zoom + zoom - 1], fill=c)
        d.text((x, y), n, fill=(138, 147, 176))
    im.save(path)

def java(path):
    out = ["package com.missionlab.game;", "",
           "/** Pixel-art sprites as palette strings (generated by tools/art.py; edit the art there and regenerate). */",
           "final class SpriteData {", "    private SpriteData() {}", "",
           "    /** name, width, height, palette (char=rrggbb,...), then rows top to bottom ('.' = clear). */",
           "    static final String[][] SPRITES = {"]
    for n in names():
        g = grid(n)
        cols = []
        for row in g:
            for c in row:
                if c and c not in cols: cols.append(c)
        chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        assert len(cols) <= len(chars), n
        # keep the accent placeholders recognisable at runtime
        cm = {}
        for i, c in enumerate(cols): cm[c] = chars[i]
        pal = ",".join(f"{cm[c]}={c[1:]}" for c in cols)
        rows = ["".join(cm[c] if c else '.' for c in row) for row in g]
        body = ", ".join(f'"{r}"' for r in rows)
        out.append(f'        {{"{n}", "{len(g[0])}", "{len(g)}", "{pal}",\n            {body}}},')
    out.append("    };")
    out.append("}")
    open(path, "w").write("\n".join(out) + "\n")

if __name__ == '__main__':
    check()
    if sys.argv[1] == 'preview': preview(sys.argv[2])
    else: java(sys.argv[2])
