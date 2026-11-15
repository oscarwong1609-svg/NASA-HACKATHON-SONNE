package com.missionlab.game;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import com.missionlab.data.EarthMask;

/**
 * La Tierra en pixel art, vista desde el espacio (proyección ortográfica) con los continentes reales
 * de {@link EarthMask}: mar con sombreado, tierra, desiertos, hielo, nubes, lado de noche con luces de ciudades
 * y un borde de atmósfera. Se vuelve a pintar solo cuando gira lo bastante como para mover un píxel.
 * El sombreado usa un tramado ordenado (Bayer 4x4) fijo en pantalla, así no "hierve" al girar.
 */
public final class Globe implements Disposable {
    private static final int[] BAYER = {0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5};

    // Cuatro tonos por material: noche, sombra, medio, luz.
    private static final int[] OCEAN = tones("08142b", "11305e", "1d4f96", "2f6fb0");
    private static final int[] LAND = tones("0d2219", "22543a", "3f8a4f", "6fb45a");
    private static final int[] DRY = tones("2a2618", "6e6a38", "a8955a", "d0b874");
    private static final int[] ICE = tones("2c3a52", "8ea6c4", "c9d9ec", "f4f8ff");
    private static final int[] CLOUD = tones("26314a", "9aa9c4", "dbe5f3", "ffffff");
    private static final int GLINT = Sprites.rgba("8cc4ff");
    private static final int CITY = Sprites.rgba("ffd36b");
    private static final int RIM = Sprites.rgba("5ec8f2");

    public final int radius, size;
    private final Pixmap pixmap;
    private final Texture texture;
    private final TextureRegion region;
    private double drawnLon = Double.NaN;
    private float tiltDeg = 18;

    Globe(int radius) {
        this.radius = radius;
        this.size = radius * 2 + 5;
        pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        render(0);
        texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        region = new TextureRegion(texture);
    }

    /** Inclinación del eje hacia quien mira (grados): con 18-25 se ven bien Groenlandia y Europa. */
    public Globe tilt(float deg) {
        if (deg != tiltDeg) {
            tiltDeg = deg;
            drawnLon = Double.NaN;
        }
        return this;
    }

    /**
     * La Tierra con la longitud centerLon en el centro. Devuelve una región de size x size píxeles;
     * el centro del planeta está en (size / 2, size / 2).
     */
    public TextureRegion frame(double centerLon) {
        // Un paso = lo que gira el ecuador en un píxel.
        double step = Math.toDegrees(1.0 / radius);
        double q = Math.floor(centerLon / step) * step;
        if (q != drawnLon) {
            render(q);
            texture.draw(pixmap, 0, 0);
        }
        return region;
    }

    /**
     * Dónde cae un punto de la Tierra en el dibujo, para la longitud centerLon que se está mostrando:
     * {x, y, z} en radios del planeta desde el centro (y hacia arriba). z &lt;= 0 = está en la cara de atrás.
     */
    public float[] project(double lat, double lon, double centerLon) {
        double tilt = Math.toRadians(tiltDeg);
        double la = Math.toRadians(lat), lo = Math.toRadians(lon - centerLon);
        double ex = Math.cos(la) * Math.sin(lo), ey = Math.sin(la), ez = Math.cos(la) * Math.cos(lo);
        double y = ey * Math.cos(tilt) + ez * Math.sin(tilt);
        double z = -ey * Math.sin(tilt) + ez * Math.cos(tilt);
        return new float[] {(float) ex, (float) y, (float) z};
    }

    private void render(double centerLon) {
        drawnLon = centerLon;
        double c = size / 2.0;
        double tilt = Math.toRadians(tiltDeg);
        double ct = Math.cos(tilt), st = Math.sin(tilt);
        // Sol arriba a la izquierda y un poco por delante.
        double lx = -0.52, ly = 0.38, lz = 0.78;
        double ll = Math.sqrt(lx * lx + ly * ly + lz * lz);
        lx /= ll;
        ly /= ll;
        lz /= ll;
        for (int py = 0; py < size; py++) {
            for (int px = 0; px < size; px++) {
                double x = (px + 0.5 - c) / radius, y = (c - (py + 0.5)) / radius;
                double r2 = x * x + y * y;
                double threshold = (BAYER[(py & 3) * 4 + (px & 3)] + 0.5) / 16.0;
                if (r2 > 1) {
                    // Atmósfera: un anillo fino, más brillante en el lado del sol.
                    double d = (Math.sqrt(r2) - 1) * radius;
                    double lit = Math.max(0, (x * lx + y * ly) / Math.sqrt(r2));
                    double a = d < 1.0 ? 0.15 + 0.75 * lit : d < 2.0 ? 0.25 * lit : 0;
                    pixmap.drawPixel(px, py, a > 0.05 ? (RIM & 0xffffff00) | (int) (Math.min(1, a) * 255) : 0);
                    continue;
                }
                double z = Math.sqrt(1 - r2);
                // De vista a Tierra: deshacer la inclinación (giro alrededor del eje X).
                double ey = y * ct - z * st, ez = y * st + z * ct;
                double lat = Math.toDegrees(Math.asin(Math.max(-1, Math.min(1, ey))));
                double lon = wrap(Math.toDegrees(Math.atan2(x, ez)) + centerLon);
                double light = x * lx + y * ly + z * lz;

                // Tono 0..3 con tramado.
                // Tramado solo en una franja estrecha entre tonos: bandas limpias, bordes suaves.
                double shade = Math.max(0, Math.min(1, (light + 0.12) / 1.0)) * 3;
                int tone = (int) Math.floor(shade + (threshold - 0.5) * 0.55 + 0.25);
                tone = Math.max(0, Math.min(3, tone));

                boolean land = EarthMask.landAt(lat, lon);
                int[] mat;
                if (land) {
                    boolean ice = lat < -62 || lat > 76 || (lat > 59 && lon > -75 && lon < -12);
                    boolean dry = !ice && desert(lat, lon);
                    mat = ice ? ICE : dry ? DRY : LAND;
                } else {
                    mat = lat > 80 || lat < -70 ? ICE : OCEAN;
                }
                int col = mat[tone];

                double cl = cloud(lat, lon);
                if (cl > 0.70 || (cl > 0.66 && threshold < 0.5)) col = CLOUD[Math.max(tone, cl > 0.74 ? Math.min(3, tone + 1) : tone)];
                else if (!land && mat == OCEAN && light > 0.95 && threshold < 0.5) col = GLINT;
                else if (land && tone == 0 && mat != ICE && hash(lat, lon) < 0.035) col = CITY;

                // Borde: el aire se ve azulado en el lado iluminado.
                if (r2 > 0.88 && light > 0.1) col = mix(col, RIM, 0.3);
                pixmap.drawPixel(px, py, col);
            }
        }
    }

    /** Los grandes desiertos (Sahara, Arabia, Australia, Kalahari, Atacama, norte de México, Gobi...). */
    static boolean desert(double lat0, double lon0) {
        // Bordes irregulares: las cajas se deforman con ondas suaves para que no parezcan rectángulos.
        double lat = lat0 + 2.5 * Math.sin(Math.toRadians(lon0 * 9)) + 1.5 * Math.sin(Math.toRadians(lon0 * 23));
        double lon = lon0 + 3.0 * Math.sin(Math.toRadians(lat0 * 11)) + 2.0 * Math.sin(Math.toRadians(lat0 * 29));
        return (lat > 15 && lat < 32 && lon > -17 && lon < 34)
                || (lat > 14 && lat < 32 && lon > 34 && lon < 60)
                || (lat > 25 && lat < 40 && lon > 50 && lon < 72 && lat < 38)
                || (lat > -32 && lat < -19 && lon > 117 && lon < 145)
                || (lat > -29 && lat < -17 && lon > 12 && lon < 26)
                || (lat > -28 && lat < -16 && lon > -71 && lon < -68)
                || (lat > 24 && lat < 37 && lon > -117 && lon < -103)
                || (lat > 38 && lat < 47 && lon > 92 && lon < 112);
    }

    /** Nubes: bandas onduladas sobre la latitud (más en el ecuador y en las latitudes medias). */
    private static double cloud(double lat, double lon) {
        double a = Math.toRadians(lat), o = Math.toRadians(lon);
        double v = 0.5 + 0.22 * Math.sin(o * 3 + a * 5 + Math.sin(o * 2) * 1.5)
                + 0.18 * Math.sin(o * 7 - a * 9 + 1.3) + 0.12 * Math.sin(a * 14 + o * 2);
        double band = Math.abs(Math.sin(a * 3)) * 0.18;
        return v * 0.8 + band;
    }

    private static double hash(double lat, double lon) {
        long h = (long) Math.floor(lat) * 73856093L ^ (long) Math.floor(lon) * 19349663L;
        h = (h ^ (h >>> 13)) * 0x5bd1e995L;
        return ((h ^ (h >>> 15)) & 0xffff) / 65536.0;
    }

    private static double wrap(double lon) {
        lon = ((lon + 180) % 360 + 360) % 360 - 180;
        return lon;
    }

    private static int[] tones(String... hex) {
        int[] t = new int[hex.length];
        for (int i = 0; i < hex.length; i++) t[i] = Sprites.rgba(hex[i]);
        return t;
    }

    private static int mix(int a, int b, double k) {
        int r = (int) (((a >>> 24) & 255) * (1 - k) + ((b >>> 24) & 255) * k);
        int g = (int) (((a >>> 16) & 255) * (1 - k) + ((b >>> 16) & 255) * k);
        int bl = (int) (((a >>> 8) & 255) * (1 - k) + ((b >>> 8) & 255) * k);
        return (r << 24) | (g << 16) | (bl << 8) | 0xff;
    }

    @Override
    public void dispose() {
        texture.dispose();
        pixmap.dispose();
    }
}
