package com.missionlab.game;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import com.missionlab.data.EarthMask;

/**
 * Pixel-art Earth as seen from space (orthographic projection) with the real continents
 * from {@link EarthMask}: shaded ocean, land, deserts, ice, clouds, a night side with city lights
 * and an atmosphere rim. It is only repainted when it has rotated enough to move one pixel.
 * Shading uses ordered dithering (Bayer 4x4) fixed in screen space, so it does not "boil" while rotating.
 */
public final class Globe implements Disposable {
    private static final int[] BAYER = {0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5};

    // Four tones per material: night, shadow, mid, light.
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

    /** Axial tilt towards the viewer (degrees): 18-25 shows Greenland and Europe well. */
    public Globe tilt(float deg) {
        if (deg != tiltDeg) {
            tiltDeg = deg;
            drawnLon = Double.NaN;
        }
        return this;
    }

    /**
     * The Earth with longitude centerLon at the centre. Returns a region of size x size pixels;
     * the centre of the planet is at (size / 2, size / 2).
     */
    public TextureRegion frame(double centerLon) {
        // One step = how far the equator turns in one pixel.
        double step = Math.toDegrees(1.0 / radius);
        double q = Math.floor(centerLon / step) * step;
        if (q != drawnLon) {
            render(q);
            texture.draw(pixmap, 0, 0);
        }
        return region;
    }

    /**
     * Where a point on the Earth lands in the drawing, for the longitude centerLon being shown:
     * {x, y, z} in planet radii from the centre (y up). z &lt;= 0 = it is on the far side.
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
        // Sun at the upper left and slightly in front.
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
                    // Atmosphere: a thin ring, brighter on the sunlit side.
                    double d = (Math.sqrt(r2) - 1) * radius;
                    double lit = Math.max(0, (x * lx + y * ly) / Math.sqrt(r2));
                    double a = d < 1.0 ? 0.15 + 0.75 * lit : d < 2.0 ? 0.25 * lit : 0;
                    pixmap.drawPixel(px, py, a > 0.05 ? (RIM & 0xffffff00) | (int) (Math.min(1, a) * 255) : 0);
                    continue;
                }
                double z = Math.sqrt(1 - r2);
                // From view to Earth frame: undo the tilt (rotation about the X axis).
                double ey = y * ct - z * st, ez = y * st + z * ct;
                double lat = Math.toDegrees(Math.asin(Math.max(-1, Math.min(1, ey))));
                double lon = wrap(Math.toDegrees(Math.atan2(x, ez)) + centerLon);
                double light = x * lx + y * ly + z * lz;

                // Tone 0..3 with dithering.
                // Dither only in a narrow band between tones: clean bands, soft edges.
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

                // Rim: the air looks bluish on the sunlit side.
                if (r2 > 0.88 && light > 0.1) col = mix(col, RIM, 0.3);
                pixmap.drawPixel(px, py, col);
            }
        }
    }

    /** The great deserts (Sahara, Arabia, Australia, Kalahari, Atacama, northern Mexico, Gobi...). */
    static boolean desert(double lat0, double lon0) {
        // Irregular edges: the boxes are warped by gentle waves so they do not look like rectangles.
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

    /** Clouds: wavy bands over latitude (more at the equator and the mid-latitudes). */
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
