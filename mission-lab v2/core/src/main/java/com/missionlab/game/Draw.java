package com.missionlab.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Align;

/** Atajos para dibujar rectángulos, marcos, barras y texto con un solo SpriteBatch. */
public final class Draw {
    private final SpriteBatch batch;
    private final Assets assets;
    private final GlyphLayout layout = new GlyphLayout();

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
        batch.setColor(c.r, c.g, c.b, alpha);
        batch.draw(assets.pixel, x, y, w, h);
        batch.setColor(Color.WHITE);
    }

    public void frame(float x, float y, float w, float h, float t, Color c) {
        rect(x, y, w, t, c);
        rect(x, y + h - t, w, t, c);
        rect(x, y, t, h, c);
        rect(x + w - t, y, t, h, c);
    }

    public void circle(float cx, float cy, float radius, Color c) {
        batch.setColor(c);
        batch.draw(assets.circle, cx - radius, cy - radius, radius * 2, radius * 2);
        batch.setColor(Color.WHITE);
    }

    public void region(TextureRegion r, float x, float y, float w, float h) {
        batch.setColor(Color.WHITE);
        batch.draw(r, x, y, w, h);
    }

    /** Barra de recurso: verde hasta 80%, ámbar hasta 100%, roja si se pasa. */
    public void bar(float x, float y, float w, float h, double ratio) {
        rect(x, y, w, h, Palette.DARK);
        float fill = (float) Math.min(1.0, Math.max(0, ratio));
        rect(x + 1, y + 1, (w - 2) * fill, h - 2, Palette.forRatio(ratio));
        rect(x + (w - 2) * 0.8f + 1, y, 1, h, Palette.DIM);
    }

    public float text(BitmapFont f, String s, float x, float y, Color c) {
        f.setColor(c);
        layout.setText(f, s);
        f.draw(batch, s, x, y);
        return layout.width;
    }

    public void textCenter(BitmapFont f, String s, float cx, float y, Color c) {
        f.setColor(c);
        layout.setText(f, s);
        f.draw(batch, s, cx - layout.width / 2f, y);
    }

    /** Texto alineado a la derecha en rightX. */
    public void textRight(BitmapFont f, String s, float rightX, float y, Color c) {
        f.setColor(c);
        layout.setText(f, s);
        f.draw(batch, s, rightX - layout.width, y);
    }

    /** El texto recortado con ".." para que quepa en maxWidth. */
    public String fit(BitmapFont f, String s, float maxWidth) {
        if (width(f, s) <= maxWidth) return s;
        String t = s;
        while (t.length() > 1 && width(f, t + "..") > maxWidth) t = t.substring(0, t.length() - 1);
        return t.trim() + "..";
    }

    /** Ancho que ocupa un texto con esta fuente. */
    public float width(BitmapFont f, String s) {
        layout.setText(f, s);
        return layout.width;
    }

    /** Un panel oscuro con borde. */
    public void panel(float x, float y, float w, float h, Color border) {
        rect(x, y, w, h, Palette.PANEL, 0.96f);
        frame(x, y, w, h, 1, border);
    }

    /** Una etiqueta de color con texto oscuro dentro (GO, WARN, NO-GO...). Devuelve su ancho. */
    public float chip(BitmapFont f, String s, float x, float y, Color c) {
        layout.setText(f, s);
        float w = layout.width + 8;
        rect(x, y - layout.height - 4, w, layout.height + 7, c);
        f.setColor(Palette.DARK);
        f.draw(batch, s, x + 4, y);
        return w;
    }

    /** Una línea de puntos entre dos puntos (aristas de grafos, trayectorias). */
    public void dots(float x1, float y1, float x2, float y2, float spacing, float size, Color c, float alpha) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        int n = Math.max(1, (int) (len / spacing));
        for (int i = 0; i <= n; i++) {
            float k = i / (float) n;
            rect(x1 + dx * k - size / 2, y1 + dy * k - size / 2, size, size, c, alpha);
        }
    }

    /** Texto con salto de línea automático. Devuelve la altura usada. */
    public float wrap(BitmapFont f, String s, float x, float y, float width, Color c) {
        f.setColor(c);
        layout.setText(f, s, c, width, Align.left, true);
        f.draw(batch, layout, x, y);
        return layout.height;
    }
}
