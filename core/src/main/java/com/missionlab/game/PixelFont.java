package com.missionlab.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.ArrayList;
import java.util.List;

/**
 * A true pixel font: each glyph is an unsmoothed bitmap, drawn at integer scale
 * with Nearest filtering and at integer coordinates. Text stays sharp at 1280x720, 1920x1080, etc.
 * (instead of a BitmapFont scaled by 0.8 or 2.0 with linear filtering, which looks blurry).
 *
 * As with BitmapFont, the y passed to draw() is the top of the capital letters.
 */
public final class PixelFont {
    /** Cap height and line spacing, in text-layer units (already scaled). */
    public final int capHeight, lineHeight, scale;
    private final Glyph[] glyphs;

    static final class Glyph {
        int advance, xOffset, top, width, height;
        TextureRegion region;
        /** One region per pixel row (for the logo gradient); created on demand. */
        TextureRegion[] rows;
    }

    private PixelFont(Glyph[] glyphs, int capHeight, int lineHeight, int scale) {
        this.glyphs = glyphs;
        this.capHeight = capHeight * scale;
        this.lineHeight = lineHeight * scale;
        this.scale = scale;
    }

    /** The same font at another integer scale. Shares the texture. */
    public PixelFont scaled(int newScale) {
        return new PixelFont(glyphs, capHeight / scale, lineHeight / scale, newScale);
    }

    // ------------------------------------------------------------------ construction

    /** Builds the game's fonts into a single texture. Returns {small, body, title, label}. */
    static PixelFont[] buildAll(List<Texture> owned) {
        String[] data = {FontData.SMALL, FontData.BODY, FontData.TITLE, FontData.LABEL};
        int[] caps = {FontData.SMALL_CAP, FontData.BODY_CAP, FontData.TITLE_CAP, FontData.LABEL_CAP};
        int[] lines = {FontData.SMALL_LINE, FontData.BODY_LINE, FontData.TITLE_LINE, FontData.LABEL_LINE};

        List<int[]> parsedHeaders = new ArrayList<>();
        List<String[]> parsedRows = new ArrayList<>();
        int[] fontStart = new int[data.length + 1];
        for (int f = 0; f < data.length; f++) {
            fontStart[f] = parsedHeaders.size();
            for (String entry : data[f].split(";")) {
                String[] p = entry.split(",", -1);
                parsedHeaders.add(new int[] {Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]),
                        Integer.parseInt(p[3]), Integer.parseInt(p[4]), Integer.parseInt(p[5])});
                parsedRows.add(p[6].isEmpty() ? new String[0] : p[6].split("\\."));
            }
        }
        fontStart[data.length] = parsedHeaders.size();

        // Packed in rows with 1 pixel of padding so filtering never mixes glyphs.
        final int atlasW = 512;
        int x = 1, y = 1, rowH = 0;
        int[][] pos = new int[parsedHeaders.size()][];
        for (int i = 0; i < parsedHeaders.size(); i++) {
            int w = parsedHeaders.get(i)[4], h = parsedHeaders.get(i)[5];
            if (x + w + 1 > atlasW) {
                x = 1;
                y += rowH + 1;
                rowH = 0;
            }
            pos[i] = new int[] {x, y};
            x += w + 1;
            rowH = Math.max(rowH, h);
        }
        int atlasH = 1;
        while (atlasH < y + rowH + 2) atlasH *= 2;

        Pixmap pm = new Pixmap(atlasW, atlasH, Pixmap.Format.RGBA8888);
        int white = Color.rgba8888(1, 1, 1, 1);
        for (int i = 0; i < parsedHeaders.size(); i++) {
            int[] h = parsedHeaders.get(i);
            String[] rows = parsedRows.get(i);
            for (int r = 0; r < rows.length; r++) {
                long bits = Long.parseLong(rows[r], 16);
                for (int c = 0; c < h[4]; c++) {
                    if (((bits >> (h[4] - 1 - c)) & 1) != 0) pm.drawPixel(pos[i][0] + c, pos[i][1] + r, white);
                }
            }
        }
        Texture tex = new Texture(pm);
        tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pm.dispose();
        owned.add(tex);

        PixelFont[] fonts = new PixelFont[data.length];
        for (int f = 0; f < data.length; f++) {
            Glyph[] glyphs = new Glyph[0x2100];
            for (int i = fontStart[f]; i < fontStart[f + 1]; i++) {
                int[] h = parsedHeaders.get(i);
                Glyph g = new Glyph();
                g.advance = h[1];
                g.xOffset = h[2];
                g.top = h[3];
                g.width = h[4];
                g.height = h[5];
                if (g.width > 0) g.region = new TextureRegion(tex, pos[i][0], pos[i][1], g.width, g.height);
                if (h[0] < glyphs.length) glyphs[h[0]] = g;
            }
            fonts[f] = new PixelFont(glyphs, caps[f], lines[f], 1);
        }
        return fonts;
    }

    private Glyph glyph(char c) {
        Glyph g = c < glyphs.length ? glyphs[c] : null;
        if (g == null) g = glyphs['?'];
        return g;
    }

    // ------------------------------------------------------------------ metrics

    /** Text width (without the spacing after the last character). */
    public float width(String s) {
        if (s == null || s.isEmpty()) return 0;
        int w = 0, best = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\n') {
                best = Math.max(best, w);
                w = 0;
                continue;
            }
            Glyph g = glyph(c);
            boolean last = i == s.length() - 1 || s.charAt(i + 1) == '\n';
            w += last && g.width > 0 ? Math.max(g.xOffset + g.width, 1) : g.advance;
        }
        return Math.max(best, w) * scale;
    }

    /** Splits the text into lines that fit in maxWidth (by words; honours \n line breaks). */
    public List<String> lines(String s, float maxWidth) {
        List<String> out = new ArrayList<>();
        for (String para : s.split("\n", -1)) {
            String[] words = para.split(" ");
            StringBuilder line = new StringBuilder();
            for (String word : words) {
                String candidate = line.length() == 0 ? word : line + " " + word;
                if (line.length() > 0 && width(candidate) > maxWidth) {
                    out.add(line.toString());
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(candidate);
                }
            }
            out.add(line.toString());
        }
        return out;
    }

    /** Height of a block of n lines, like GlyphLayout.height: cap height of the first line plus the remaining lines. */
    public float blockHeight(int lines) {
        return capHeight + Math.max(0, lines - 1) * lineHeight;
    }

    // ------------------------------------------------------------------ drawing

    /** Draws one line with the top of the capitals at y. Returns the width. */
    public float draw(SpriteBatch batch, String s, float x, float y, Color c) {
        batch.setColor(c);
        int px = Math.round(x);
        int baseline = Math.round(y) - capHeight;
        for (int i = 0; i < s.length(); i++) {
            Glyph g = glyph(s.charAt(i));
            if (g.region != null) {
                batch.draw(g.region, px + g.xOffset * scale, baseline + (g.top - g.height) * scale, g.width * scale, g.height * scale);
            }
            px += g.advance * scale;
        }
        batch.setColor(Color.WHITE);
        return width(s);
    }

    /**
     * Draws with a vertical gradient by pixel rows (top to bottom), for signs with a metallic sheen.
     * Rows are counted from the top of the capitals.
     */
    public void drawGradient(SpriteBatch batch, String s, float x, float y, Color top, Color bottom) {
        int px = Math.round(x);
        int capRows = capHeight / scale;
        int baseline = Math.round(y) - capHeight;
        Color tmp = new Color(1, 1, 1, 1);
        for (int i = 0; i < s.length(); i++) {
            Glyph g = glyph(s.charAt(i));
            if (g.region != null) {
                if (g.rows == null) {
                    g.rows = new TextureRegion[g.height];
                    for (int r = 0; r < g.height; r++) {
                        TextureRegion reg = g.region;
                        g.rows[r] = new TextureRegion(reg.getTexture(), reg.getRegionX(), reg.getRegionY() + r, g.width, 1);
                    }
                }
                for (int r = 0; r < g.height; r++) {
                    int rowFromCapTop = capRows - g.top + r;
                    float k = capRows <= 1 ? 0 : Math.max(0, Math.min(1, rowFromCapTop / (float) (capRows - 1)));
                    tmp.r = top.r + (bottom.r - top.r) * k;
                    tmp.g = top.g + (bottom.g - top.g) * k;
                    tmp.b = top.b + (bottom.b - top.b) * k;
                    tmp.a = top.a + (bottom.a - top.a) * k;
                    batch.setColor(tmp);
                    batch.draw(g.rows[r], px + g.xOffset * scale, baseline + (g.top - 1 - r) * scale, g.width * scale, scale);
                }
            }
            px += g.advance * scale;
        }
        batch.setColor(Color.WHITE);
    }
}
