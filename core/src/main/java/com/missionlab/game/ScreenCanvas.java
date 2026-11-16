package com.missionlab.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ScreenUtils;

/**
 * Fills the window (or the full screen) without losing pixel-art sharpness.
 *
 * The game is first drawn into an intermediate 640x360 image multiplied by the next integer
 * at or above the window size (x2, x3, x5...). There every pixel of the art and the fonts
 * is an exact block. That image is then copied to the window at exactly the size that fills it,
 * shrinking it slightly with linear filtering ("sharp bilinear"): it stays sharp at any
 * resolution and without the black frame left by integer-only scaling.
 * If the window is not 16:9, the leftover bands are painted in the space colour.
 */
public final class ScreenCanvas implements Disposable {
    public static final int BASE_W = 640, BASE_H = 360;
    /** Largest supersampling factor: keeps the frame buffer at or below 7680 x 4320 (supported by every desktop GPU). */
    private static final int MAX_SCALE = 12;

    private FrameBuffer fbo;
    private TextureRegion region;
    private int scale;
    private boolean active;
    private final OrthographicCamera screenCam = new OrthographicCamera();
    /** Where the last frame was presented, in back-buffer pixels: x, y, width, height. */
    private final int[] presented = new int[4];

    /**
     * Starts drawing a frame. The game cameras must cover the whole canvas
     * (320x180 for the world, 640x360 for text). Returns false if the window is minimised.
     */
    public boolean begin(Color clear) {
        int bw = Gdx.graphics.getBackBufferWidth(), bh = Gdx.graphics.getBackBufferHeight();
        if (bw <= 0 || bh <= 0) {
            // No visible window (minimised, or the test renderer): draw directly.
            active = false;
            ScreenUtils.clear(clear.r, clear.g, clear.b, 1);
            return true;
        }
        // Sizes come from the back buffer (physical pixels), not the logical window size, so a Retina
        // display renders at full resolution instead of a quarter of it.
        int k = Math.max(1, Math.min(MAX_SCALE, (int) Math.ceil(Math.min(bw / (float) BASE_W, bh / (float) BASE_H) - 0.001f)));
        if (fbo == null || k != scale) {
            if (fbo != null) fbo.dispose();
            fbo = new FrameBuffer(Pixmap.Format.RGBA8888, BASE_W * k, BASE_H * k, false);
            fbo.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            region = new TextureRegion(fbo.getColorBufferTexture());
            region.flip(false, true);
            scale = k;
        }
        active = true;
        fbo.begin();
        Gdx.gl.glViewport(0, 0, BASE_W * k, BASE_H * k);
        ScreenUtils.clear(clear.r, clear.g, clear.b, 1);
        return true;
    }

    /** Ends the frame and copies it to the window, centred and as large as possible. */
    public void end(SpriteBatch batch, Color background) {
        if (!active) return;
        fbo.end();
        int bw = Gdx.graphics.getBackBufferWidth(), bh = Gdx.graphics.getBackBufferHeight();
        Gdx.gl.glViewport(0, 0, bw, bh);
        ScreenUtils.clear(background.r, background.g, background.b, 1);
        screenCam.setToOrtho(false, bw, bh);
        batch.setProjectionMatrix(screenCam.combined);
        float s = Math.min(bw / (float) BASE_W, bh / (float) BASE_H);
        int w = Math.round(BASE_W * s), h = Math.round(BASE_H * s);
        batch.begin();
        // No blending: the game image is opaque, so semi-transparent pixels are not darkened.
        batch.disableBlending();
        batch.setColor(Color.WHITE);
        presented[0] = (bw - w) / 2;
        presented[1] = (bh - h) / 2;
        presented[2] = w;
        presented[3] = h;
        batch.draw(region, presented[0], presented[1], w, h);
        batch.end();
        batch.enableBlending();
    }

    /**
     * Converts a pointer position in window coordinates (as given by Gdx.input.getX()/getY(): logical units,
     * origin at the top left) into text-layer coordinates (640 x 360, origin at the bottom left).
     * Works on HiDPI displays and with the letterbox bands. The game is keyboard-only today; this keeps
     * any future mouse or touch input aligned at every window size.
     */
    public float[] unproject(int screenX, int screenY) {
        int lw = Math.max(1, Gdx.graphics.getWidth()), lh = Math.max(1, Gdx.graphics.getHeight());
        float sx = Gdx.graphics.getBackBufferWidth() / (float) lw, sy = Gdx.graphics.getBackBufferHeight() / (float) lh;
        float px = screenX * sx, py = Gdx.graphics.getBackBufferHeight() - screenY * sy;
        if (presented[2] == 0) return new float[] {screenX, lh - screenY};
        return new float[] {(px - presented[0]) * BASE_W / presented[2], (py - presented[1]) * BASE_H / presented[3]};
    }

    @Override
    public void dispose() {
        if (fbo != null) fbo.dispose();
    }
}
