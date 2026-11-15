package com.missionlab.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Todos los sprites pixel art del juego, sin archivos externos: se leen de {@link SpriteData}
 * (dibujos con una paleta por sprite) y se empaquetan en una sola textura con filtro Nearest.
 * Los astronautas se recolorean con el color de cada jugador; la Tierra se pinta por código ({@link Globe}).
 */
public final class Sprites implements Disposable {
    /** Colores de relleno en los dibujos del astronauta que se cambian por el color del jugador. */
    private static final String ACCENT = "ff00ff", ACCENT_DARK = "990099";

    private final Map<String, TextureRegion> regions = new HashMap<>();
    private final Map<String, String[]> sources = new HashMap<>();
    private final Map<Integer, TextureRegion[]> astronauts = new HashMap<>();
    private final Map<Integer, Globe> globes = new HashMap<>();
    private final List<Texture> textures = new ArrayList<>();

    public Sprites() {
        for (String[] s : SpriteData.SPRITES) sources.put(s[0], s);
        regions.putAll(pack(new ArrayList<>(sources.keySet()), null, null));
    }

    /** Un sprite por nombre (por ejemplo "cam_sharp", "medal_gold", "rocket_big"), o null si no existe. */
    public TextureRegion get(String name) {
        return regions.get(name);
    }

    /**
     * El astronauta con el color del jugador: frame 0 = quieto o paso A, 1 = paso B.
     * Se generan la primera vez que se pide cada color.
     */
    public TextureRegion astronaut(Color accent, int frame, boolean faceLeft) {
        int key = Color.rgba8888(accent.r, accent.g, accent.b, 1);
        TextureRegion[] set = astronauts.get(key);
        if (set == null) {
            List<String> names = new ArrayList<>();
            names.add("astro_0");
            names.add("astro_1");
            String hex = hex(accent), dark = hex(new Color(accent.r * 0.6f, accent.g * 0.6f, accent.b * 0.6f, 1));
            Map<String, TextureRegion> right = pack(names, new String[] {ACCENT, ACCENT_DARK}, new String[] {hex, dark});
            Map<String, TextureRegion> left = packMirrored(names, new String[] {ACCENT, ACCENT_DARK}, new String[] {hex, dark});
            set = new TextureRegion[] {right.get("astro_0"), right.get("astro_1"), left.get("astro_0"), left.get("astro_1")};
            astronauts.put(key, set);
        }
        return set[(faceLeft ? 2 : 0) + (frame & 1)];
    }

    /** Una Tierra pixel art de este radio (en píxeles), con continentes, nubes, noche y atmósfera. */
    public Globe globe(int radius) {
        Globe g = globes.get(radius);
        if (g == null) {
            g = new Globe(radius);
            globes.put(radius, g);
        }
        return g;
    }

    // ------------------------------------------------------------------ empaquetado

    private Map<String, TextureRegion> pack(List<String> names, String[] from, String[] to) {
        return pack(names, from, to, false);
    }

    private Map<String, TextureRegion> packMirrored(List<String> names, String[] from, String[] to) {
        return pack(names, from, to, true);
    }

    private Map<String, TextureRegion> pack(List<String> names, String[] from, String[] to, boolean mirror) {
        names.sort((a, b) -> Integer.parseInt(sources.get(b)[2]) - Integer.parseInt(sources.get(a)[2]));
        int atlasW = 256, x = 1, y = 1, rowH = 0;
        Map<String, int[]> pos = new HashMap<>();
        for (String n : names) {
            String[] s = sources.get(n);
            int w = Integer.parseInt(s[1]), h = Integer.parseInt(s[2]);
            if (x + w + 1 > atlasW) {
                x = 1;
                y += rowH + 1;
                rowH = 0;
            }
            pos.put(n, new int[] {x, y, w, h});
            x += w + 1;
            rowH = Math.max(rowH, h);
        }
        int atlasH = 1;
        while (atlasH < y + rowH + 2) atlasH *= 2;
        Pixmap pm = new Pixmap(atlasW, atlasH, Pixmap.Format.RGBA8888);
        pm.setBlending(Pixmap.Blending.None);
        for (String n : names) {
            String[] s = sources.get(n);
            int[] p = pos.get(n);
            Map<Character, Integer> pal = new HashMap<>();
            for (String entry : s[3].split(",")) {
                String hx = entry.substring(2);
                if (from != null) for (int i = 0; i < from.length; i++) if (hx.equals(from[i])) hx = to[i];
                pal.put(entry.charAt(0), rgba(hx));
            }
            for (int r = 0; r < p[3]; r++) {
                String row = s[4 + r];
                for (int c = 0; c < p[2]; c++) {
                    char ch = row.charAt(mirror ? p[2] - 1 - c : c);
                    if (ch != '.') pm.drawPixel(p[0] + c, p[1] + r, pal.get(ch));
                }
            }
        }
        Texture tex = new Texture(pm);
        tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pm.dispose();
        textures.add(tex);
        Map<String, TextureRegion> out = new HashMap<>();
        for (String n : names) {
            int[] p = pos.get(n);
            out.put(n, new TextureRegion(tex, p[0], p[1], p[2], p[3]));
        }
        return out;
    }

    static int rgba(String hex) {
        int v = Integer.parseInt(hex, 16);
        return (v << 8) | 0xff;
    }

    private static String hex(Color c) {
        int r = Math.round(c.r * 255), g = Math.round(c.g * 255), b = Math.round(c.b * 255);
        return String.format("%02x%02x%02x", r, g, b);
    }

    @Override
    public void dispose() {
        for (Texture t : textures) t.dispose();
        for (Globe g : globes.values()) g.dispose();
    }
}
