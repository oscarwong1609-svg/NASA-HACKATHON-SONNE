package com.missionlab.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.List;

/**
 * Shortcuts for drawing rectangles, frames, bars, sprites and text with a single SpriteBatch.
 * Text uses {@link PixelFont}: unsmoothed glyphs at integer scale and integer coordinates.
 */
public final class Draw {
    private final SpriteBatch batch;
    private final Assets assets;

    public Draw(SpriteBatch batch, Assets assets) {
        this.batch = batch;
        this.assets = assets;
    }

    public void rect(float x, float y, float w, float h, Color c) {
        batch.setColor(c);
        batch.draw(assets.pixel, x, y, w, h);
        batch.setColor(Color.WHITE);
    }

    public void rect(float x, float y, float w, float h, Color c, float alpha) {
        batch.setColor(c.r, c.g, c.b, c.a * alpha);
        batch.draw(assets.pixel, x, y, w, h);
        batch.setColor(Color.WHITE);
    }

    public void frame(float x, float y, float w, float h, float t, Color c) {
        rect(x, y, w, t, c);
        rect(x, y + h - t, w, t, c);
        rect(x, y + t, t, h - 2 * t, c);
        rect(x + w - t, y + t, t, h - 2 * t, c);
    }

    /**
     * A pixel-art circle: one row of whole pixels per line, without anti-aliasing,
     * so it has the same stepped edge as the rest of the art (in whichever layer it is drawn).
     */
    public void circle(float cx, float cy, float radius, Color c) {
        if (radius <= 0.75f) {
            rect(Math.round(cx - 0.5f), Math.round(cy - 0.5f), 1, 1, c);
            return;
        }
        batch.setColor(c);
        int x0 = Math.round(cx), y0 = Math.round(cy);
        int r = (int) Math.ceil(radius);
        float r2 = radius * radius;
        for (int dy = -r; dy < r; dy++) {
            float yc = dy + 0.5f;
            if (yc * yc > r2) continue;
            int half = Math.round((float) Math.sqrt(r2 - yc * yc));
            if (half <= 0) continue;
            batch.draw(assets.pixel, x0 - half, y0 + dy, half * 2, 1);
        }
        batch.setColor(Color.WHITE);
    }

    public void region(TextureRegion r, float x, float y, float w, float h) {
        batch.setColor(Color.WHITE);
        batch.draw(r, x, y, w, h);
    }

    // ------------------------------------------------------------------ pixel-art sprites

    /** A sprite at its native size times an integer scale, with its bottom-left corner at (x, y). */
    public void sprite(TextureRegion r, float x, float y, int scale) {
        if (r == null) return;
        batch.setColor(Color.WHITE);
        batch.draw(r, Math.round(x), Math.round(y), r.getRegionWidth() * scale, r.getRegionHeight() * scale);
    }

    /** Same, but tinted (for example grey for "off") or with transparency. */
    public void sprite(TextureRegion r, float x, float y, int scale, Color tint) {
        if (r == null) return;
        batch.setColor(tint);
        batch.draw(r, Math.round(x), Math.round(y), r.getRegionWidth() * scale, r.getRegionHeight() * scale);
        batch.setColor(Color.WHITE);
    }

    /** A sprite centred on (cx, cy). Not rounded: for things that move smoothly (satellites in orbit). */
    public void spriteCentered(TextureRegion r, float cx, float cy, int scale) {
        if (r == null) return;
        batch.setColor(Color.WHITE);
        float w = r.getRegionWidth() * scale, h = r.getRegionHeight() * scale;
        batch.draw(r, cx - w / 2f, cy - h / 2f, w, h);
    }

    /** The game's sprites. */
    public Sprites sprites() {
        return assets.sprites;
    }

    /** A game sprite by name (see {@link Sprites}). */
    public void sprite(String name, float x, float y, int scale) {
        sprite(assets.sprites.get(name), x, y, scale);
    }

    // ------------------------------------------------------------------ bars

    /** Resource bar: green up to 80%, amber up to 100%, red when exceeded. Segmented, instrument style. */
    public void bar(float x, float y, float w, float h, double ratio) {
        rect(x, y, w, h, Palette.DARK);
        float fill = (float) Math.min(1.0, Math.max(0, ratio));
        Color c = Palette.forRatio(ratio);
        float inner = w - 2;
        rect(x + 1, y + 1, Math.round(inner * fill), h - 2, c);
        // Highlight on top and a tick every 10%.
        if (h >= 5) rect(x + 1, y + h - 2, Math.round(inner * fill), 1, Color.WHITE, 0.25f);
        for (int i = 1; i < 10; i++) rect(x + 1 + Math.round(inner * i / 10f), y + 1, 1, h - 2, Palette.DARK, 0.55f);
        rect(x + 1 + Math.round(inner * 0.8f), y - 1, 1, h + 2, Palette.TEXT, 0.7f);
    }

    // ------------------------------------------------------------------ text

    public float text(PixelFont f, String s, float x, float y, Color c) {
        return f.draw(batch, s, x, y, c);
    }

    public void textCenter(PixelFont f, String s, float cx, float y, Color c) {
        f.draw(batch, s, Math.round(cx - f.width(s) / 2f), y, c);
    }

    /** Text right-aligned at rightX. */
    public void textRight(PixelFont f, String s, float rightX, float y, Color c) {
        f.draw(batch, s, rightX - f.width(s), y, c);
    }

    /** Text with a 1-pixel drop shadow to the bottom right: for titles over detailed backgrounds. */
    public float textShadow(PixelFont f, String s, float x, float y, Color c) {
        f.draw(batch, s, x + f.scale, y - f.scale, Palette.DARK);
        return f.draw(batch, s, x, y, c);
    }

    /** Text with a vertical gradient by pixel rows (logo and metallic headlines). */
    public void textGradient(PixelFont f, String s, float x, float y, Color top, Color bottom) {
        f.drawGradient(batch, s, x, y, top, bottom);
    }

    /** The text truncated with ".." so it fits in maxWidth. */
    public String fit(PixelFont f, String s, float maxWidth) {
        if (f.width(s) <= maxWidth) return s;
        String t = s;
        while (t.length() > 1 && f.width(t + "..") > maxWidth) t = t.substring(0, t.length() - 1);
        return t.trim() + "..";
    }

    /** Width of a text in this font. */
    public float width(PixelFont f, String s) {
        return f.width(s);
    }

    /** Text with automatic word wrap. Returns the height used (like GlyphLayout.height). */
    public float wrap(PixelFont f, String s, float x, float y, float width, Color c) {
        List<String> lines = f.lines(s, width);
        float ly = y;
        for (String line : lines) {
            f.draw(batch, line, x, ly, c);
            ly -= f.lineHeight;
        }
        return f.blockHeight(lines.size());
    }

    /** Section header: a small coloured square and the uppercase label (label font). Returns the width. */
    public float heading(String s, float x, float y, Color c) {
        rect(Math.round(x), Math.round(y) - 4, 3, 3, c);
        return assets.label.draw(batch, s, x + 6, y, c) + 6;
    }

    /** A coloured tag with dark text inside (GO, WARN, NO-GO...). Returns its width. */
    public float chip(PixelFont f, String s, float x, float y, Color c) {
        float w = f.width(s) + 8;
        int pad = Math.max(2, f.scale * 2);
        float top = Math.round(y) + pad, bottom = Math.round(y) - f.capHeight - pad;
        rect(x, bottom, w, top - bottom, c);
        rect(x, top - 1, w, 1, Color.WHITE, 0.3f);
        rect(x, bottom, w, 1, Palette.DARK, 0.35f);
        f.draw(batch, s, x + 4, y, Palette.DARK);
        return w;
    }

    // ------------------------------------------------------------------ terminal-style panels

    /** A dark panel with a thin border and marked corners, like the displays in a control room. */
    public void panel(float x, float y, float w, float h, Color border) {
        rect(x, y, w, h, Palette.PANEL, 0.96f);
        frame(x, y, w, h, 1, dim(border, 0.55f));
        brackets(x, y, w, h, 5, border);
    }

    /**
     * A terminal panel: title bar with its name, three LEDs, scanlines and corner brackets.
     * Returns the y where content can start (below the bar).
     */
    public float terminal(float x, float y, float w, float h, String title, Color accent, float time) {
        rect(x, y, w, h, Palette.PANEL, 0.97f);
        scanlines(x + 1, y + 1, w - 2, h - 2, 0.05f);
        float barH = 13;
        rect(x, y + h - barH, w, barH, dim(accent, 0.22f));
        rect(x, y + h - barH, w, 1, dim(accent, 0.6f));
        frame(x, y, w, h, 1, dim(accent, 0.55f));
        brackets(x, y, w, h, 6, accent);
        text(assets.label, title, x + 6, y + h - 4, accent);
        // LEDs: steady green, blinking amber, breathing blue.
        float lx = x + w - 30;
        led(lx, y + h - 9, Palette.GOOD, true);
        led(lx + 8, y + h - 9, Palette.WARN, ((int) (time * 2.5f)) % 2 == 0);
        led(lx + 16, y + h - 9, Palette.ACCENT, Math.sin(time * 3) > -0.3);
        return y + h - barH - 6;
    }

    /** A 4x4 LED with glow. */
    public void led(float x, float y, Color c, boolean on) {
        rect(x - 1, y - 1, 6, 6, Palette.DARK);
        rect(x, y, 4, 4, on ? c : dim(c, 0.25f));
        if (on) {
            rect(x, y + 3, 1, 1, Color.WHITE, 0.8f);
            rect(x - 2, y - 2, 8, 8, c, 0.12f);
        }
    }

    /** Faint horizontal lines every 2 units, like a CRT monitor. */
    public void scanlines(float x, float y, float w, float h, float alpha) {
        for (float yy = y; yy < y + h; yy += 2) rect(x, yy, w, 1, Palette.DARK, alpha * 4);
    }

    /** L-shaped brackets on the four corners of a rectangle. */
    public void brackets(float x, float y, float w, float h, float len, Color c) {
        rect(x, y, len, 1, c);
        rect(x, y, 1, len, c);
        rect(x + w - len, y, len, 1, c);
        rect(x + w - 1, y, 1, len, c);
        rect(x, y + h - 1, len, 1, c);
        rect(x, y + h - len, 1, len, c);
        rect(x + w - len, y + h - 1, len, 1, c);
        rect(x + w - 1, y + h - len, 1, len, c);
    }

    /** A darker colour (k = 0..1 brightness). */
    public Color dim(Color c, float k) {
        return new Color(c.r * k + Palette.PANEL.r * (1 - k), c.g * k + Palette.PANEL.g * (1 - k), c.b * k + Palette.PANEL.b * (1 - k), c.a);
    }

    /** A dotted line between two points (graph edges, trajectories). */
    public void dots(float x1, float y1, float x2, float y2, float spacing, float size, Color c, float alpha) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        int n = Math.max(1, (int) (len / spacing));
        for (int i = 0; i <= n; i++) {
            float k = i / (float) n;
            rect(Math.round(x1 + dx * k - size / 2), Math.round(y1 + dy * k - size / 2), size, size, c, alpha);
        }
    }
}
