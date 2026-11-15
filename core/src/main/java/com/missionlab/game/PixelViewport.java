package com.missionlab.game;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * Un viewport "pixel perfect": escala la imagen solo por números enteros (x2 en 1280x720, x3 en 1920x1080...)
 * y deja bandas oscuras si sobra ventana. Así cada píxel del pixel art y de las fuentes ocupa siempre
 * el mismo número de píxeles de pantalla y nada se ve borroso ni irregular.
 *
 * Las dos capas (mundo 320x180 y texto 640x360) usan el mismo rectángulo de pantalla,
 * calculado sobre la capa de texto. Si la ventana es más pequeña que 640x360, escala como un FitViewport.
 */
public final class PixelViewport extends Viewport {
    public static final int BASE_W = 640, BASE_H = 360;

    public PixelViewport(float worldWidth, float worldHeight, Camera camera) {
        setWorldSize(worldWidth, worldHeight);
        setCamera(camera);
    }

    /** Cuántos píxeles de pantalla mide una unidad de la capa de texto (1, 2, 3...), o 0 si no cabe entero. */
    public static int integerScale(int screenWidth, int screenHeight) {
        return (int) Math.floor(Math.min(screenWidth / (float) BASE_W, screenHeight / (float) BASE_H));
    }

    @Override
    public void update(int screenWidth, int screenHeight, boolean centerCamera) {
        int k = integerScale(screenWidth, screenHeight);
        int w, h;
        if (k >= 1) {
            w = BASE_W * k;
            h = BASE_H * k;
        } else {
            float s = Math.min(screenWidth / (float) BASE_W, screenHeight / (float) BASE_H);
            w = Math.max(1, Math.round(BASE_W * s));
            h = Math.max(1, Math.round(BASE_H * s));
        }
        setScreenBounds((screenWidth - w) / 2, (screenHeight - h) / 2, w, h);
        apply(centerCamera);
    }
}
