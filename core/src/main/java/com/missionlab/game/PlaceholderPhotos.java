package com.missionlab.game;

import com.badlogic.gdx.graphics.Pixmap;

import java.util.Random;

/**
 * PLACEHOLDER images drawn in code, so the game works from minute one.
 * They should be replaced by real NASA photos saved in assets/photos/ (see README),
 * credited in the presentation.
 */
final class PlaceholderPhotos {
    private PlaceholderPhotos() {}

    private static final int SIZE = 256;

    static Pixmap iceSheet() {
        Random r = new Random(7);
        Pixmap p = new Pixmap(SIZE, SIZE, Pixmap.Format.RGBA8888);
        p.setColor(0.08f, 0.20f, 0.38f, 1);
        p.fill();
        // rocky coast
        p.setColor(0.42f, 0.44f, 0.46f, 1);
        blob(p, r, 128, 132, 92, 26);
        // ice
        p.setColor(0.90f, 0.94f, 0.98f, 1);
        blob(p, r, 128, 132, 78, 30);
        // glaciers: ice tongues flowing down to the sea
        p.setColor(0.70f, 0.82f, 0.92f, 1);
        for (int i = 0; i < 9; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            for (int k = 0; k < 30; k++) {
                int x = (int) (128 + Math.cos(a) * (60 + k));
                int y = (int) (132 + Math.sin(a) * (60 + k));
                p.fillCircle(x, y, 2);
            }
        }
        noise(p, r, 0.05f);
        return p;
    }

    static Pixmap nightFires() {
        Random r = new Random(11);
        Pixmap p = new Pixmap(SIZE, SIZE, Pixmap.Format.RGBA8888);
        p.setColor(0.02f, 0.04f, 0.03f, 1);
        p.fill();
        // forest warmer than the river (as a thermal camera sees it)
        p.setColor(0.10f, 0.16f, 0.10f, 1);
        blob(p, r, 128, 128, 110, 40);
        p.setColor(0.03f, 0.05f, 0.10f, 1);
        double y = 40;
        for (int x = 0; x < SIZE; x++) {
            y += Math.sin(x / 18.0) * 1.6;
            p.fillCircle(x, (int) y + 60, 4);
        }
        // fires: hot spots
        for (int i = 0; i < 26; i++) {
            int fx = 20 + r.nextInt(SIZE - 40);
            int fy = 20 + r.nextInt(SIZE - 40);
            p.setColor(0.95f, 0.45f, 0.10f, 1);
            p.fillCircle(fx, fy, 3 + r.nextInt(4));
            p.setColor(1f, 0.90f, 0.40f, 1);
            p.fillCircle(fx, fy, 1 + r.nextInt(2));
        }
        noise(p, r, 0.04f);
        return p;
    }

    static Pixmap ocean() {
        Random r = new Random(3);
        Pixmap p = new Pixmap(SIZE, SIZE, Pixmap.Format.RGBA8888);
        p.setColor(0.06f, 0.22f, 0.42f, 1);
        p.fill();
        p.setColor(0.85f, 0.88f, 0.92f, 1);
        for (int i = 0; i < 7; i++) blob(p, r, r.nextInt(SIZE), r.nextInt(SIZE), 14 + r.nextInt(20), 10);
        noise(p, r, 0.05f);
        return p;
    }

    private static void blob(Pixmap p, Random r, int cx, int cy, int radius, int pieces) {
        for (int i = 0; i < pieces; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            double d = r.nextDouble() * radius * 0.6;
            int rad = (int) (radius * (0.3 + r.nextDouble() * 0.5));
            p.fillCircle((int) (cx + Math.cos(a) * d), (int) (cy + Math.sin(a) * d), rad);
        }
    }

    private static void noise(Pixmap p, Random r, float amount) {
        for (int i = 0; i < SIZE * SIZE / 6; i++) {
            float v = r.nextFloat() * amount;
            p.setColor(v, v, v, 0.5f);
            p.drawPixel(r.nextInt(SIZE), r.nextInt(SIZE));
        }
    }
}
