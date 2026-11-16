package com.missionlab.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import com.missionlab.data.EarthMask;
import com.missionlab.model.GroundStation;
import com.missionlab.model.Mission;
import com.missionlab.sim.OrbitMechanics;
import com.missionlab.sim.OrbitMechanics.State;
import com.missionlab.sim.OrbitMechanics.Track;

/**
 * The world map (equirectangular projection: longitude across, latitude up) and everything drawn
 * on top of it: night, mission area, orbit ground track, coverage circles, stations and satellite.
 * Shared by the orbit map, the ground station desk and Mission Control.
 */
public final class WorldMap implements Disposable {
    private static final Color OCEAN = Color.valueOf("0e2544");
    private static final Color OCEAN_DEEP = Color.valueOf("0a1c36");
    private static final Color LAND = Color.valueOf("2f6b47");
    private static final Color LAND_DRY = Color.valueOf("5b7a45");
    private static final Color ICE = Color.valueOf("d6e6f2");

    private final Texture texture;
    private final TextureRegion region;
    private float x, y, w, h;

    public WorldMap() {
        Pixmap p = new Pixmap(EarthMask.WIDTH, EarthMask.HEIGHT, Pixmap.Format.RGBA8888);
        for (int row = 0; row < EarthMask.HEIGHT; row++) {
            double lat = 89.5 - row;
            for (int col = 0; col < EarthMask.WIDTH; col++) {
                double lon = col - 179.5;
                Color c;
                if (EarthMask.land(row, col)) {
                    boolean ice = lat < -62 || (lat > 59 && lon > -75 && lon < -10) || lat > 78;
                    boolean dry = Globe.desert(lat, lon);
                    c = ice ? ICE : dry ? LAND_DRY : LAND;
                } else {
                    // Sea near the coast, slightly lighter.
                    boolean coast = EarthMask.land(row, col - 1) || EarthMask.land(row, col + 1)
                            || EarthMask.land(row - 1, col) || EarthMask.land(row + 1, col);
                    c = coast ? OCEAN : OCEAN_DEEP;
                }
                p.drawPixel(col, row, Color.rgba8888(c.r, c.g, c.b, 1f));
            }
        }
        texture = new Texture(p);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        p.dispose();
        region = new TextureRegion(texture);
    }

    /** Where the map is drawn on screen (text-layer coordinates, 640 x 360). */
    public void bounds(float x, float y, float w, float h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public float px(double lon) {
        return x + (float) ((lon + 180) / 360.0) * w;
    }

    public float py(double lat) {
        return y + (float) ((lat + 90) / 180.0) * h;
    }

    private boolean inside(float sx, float sy) {
        return sx >= x && sx <= x + w && sy >= y && sy <= y + h;
    }

    // ------------------------------------------------------------------ layers

    public void drawBase(Draw draw) {
        draw.region(region, x, y, w, h);
        // Grid every 30 degrees, with the equator slightly stronger.
        for (int lon = -150; lon <= 150; lon += 30) draw.rect(px(lon), y, 1, h, Palette.TEXT, 0.06f);
        for (int lat = -60; lat <= 60; lat += 30) draw.rect(x, py(lat), w, 1, Palette.TEXT, lat == 0 ? 0.14f : 0.06f);
    }

    /** Shades the half of the Earth where it is night at instant t. */
    public void drawNight(Draw draw, double trackT, double sunDeclinationDeg, float alpha) {
        double sub = OrbitMechanics.subsolarLongitude(trackT);
        double d = Math.toRadians(sunDeclinationDeg);
        float colW = w / 180f;
        for (int i = 0; i < 180; i++) {
            double lon = -180 + i * 2 + 1;
            double cosH = Math.cos(Math.toRadians(lon - sub));
            // Whole-pixel columns that touch without overlapping (otherwise darker stripes appear).
            float cx = Math.round(x + i * colW), cw = Math.round(x + (i + 1) * colW) - cx;
            if (cw <= 0) continue;
            if (Math.abs(sunDeclinationDeg) < 0.01) {
                if (cosH < 0) draw.rect(cx, y, cw, h, Palette.DARK, alpha);
                continue;
            }
            // Terminator latitude: sin(elevation) = 0.
            double phiT = Math.toDegrees(Math.atan(-cosH / Math.tan(d)));
            if (sunDeclinationDeg > 0) {
                float top = Math.round(py(phiT));
                draw.rect(cx, y, cw, top - y, Palette.DARK, alpha);
            } else {
                float bottom = Math.round(py(phiT));
                draw.rect(cx, bottom, cw, y + h - bottom, Palette.DARK, alpha);
            }
        }
    }

    /** The mission area: a pulsing rectangle with its name. */
    public void drawRegion(Draw draw, Mission m, Color c, float pulse) {
        float x0 = px(m.regionLonMin), x1 = px(m.regionLonMax), y0 = py(m.regionLatMin), y1 = py(m.regionLatMax);
        draw.rect(x0, y0, x1 - x0, y1 - y0, c, 0.12f + 0.10f * pulse);
        draw.frame(x0, y0, x1 - x0, y1 - y0, 1, new Color(c.r, c.g, c.b, 0.6f + 0.4f * pulse));
        draw.circle(px(m.targetLongitude), py(m.targetLatitude), 2.5f, c);
    }

    /** Ground-track dots between two instants. */
    public void drawTrack(Draw draw, Track track, double from, double to, double stepS, Color c, float alpha, float size) {
        for (double t = from; t <= to; t += stepS) {
            State s = track.at(t);
            float sx = px(s.lon), sy = py(s.lat);
            if (inside(sx, sy)) draw.rect(sx - size / 2, sy - size / 2, size, size, c, alpha);
        }
    }

    /** A circle on the sphere (for example, what an antenna can hear), drawn with dots. */
    public void drawCircle(Draw draw, double lat, double lon, double radiusDeg, Color c, float alpha) {
        double p1 = Math.toRadians(lat), l1 = Math.toRadians(lon), dr = Math.toRadians(radiusDeg);
        for (int i = 0; i < 72; i++) {
            double brg = Math.toRadians(i * 5);
            double p2 = Math.asin(Math.sin(p1) * Math.cos(dr) + Math.cos(p1) * Math.sin(dr) * Math.cos(brg));
            double l2 = l1 + Math.atan2(Math.sin(brg) * Math.sin(dr) * Math.cos(p1), Math.cos(dr) - Math.sin(p1) * Math.sin(p2));
            float sx = px(OrbitMechanics.wrapLon(Math.toDegrees(l2))), sy = py(Math.toDegrees(p2));
            if (inside(sx, sy)) draw.rect(sx - 0.5f, sy - 0.5f, 1.5f, 1.5f, c, alpha);
        }
    }

    public void drawSite(Draw draw, GroundStation.Site s, boolean active) {
        float sx = Math.round(px(s.lon)), sy = Math.round(py(s.lat));
        Color c = active ? Palette.GOOD : Palette.TEXT;
        draw.rect(sx - 3, sy - 3, 7, 7, Palette.DARK);
        draw.rect(sx - 2, sy - 2, 5, 5, c);
        draw.rect(sx - 1, sy - 1, 3, 3, Palette.DARK);
        draw.rect(sx, sy, 1, 1, c);
    }

    /** The satellite: the CubeSat sprite with a pulsing halo in the colour of what it is doing. */
    public void drawSatellite(Draw draw, double lat, double lon, Color c, float time) {
        float sx = px(lon), sy = py(lat);
        float halo = 6 + 2 * (float) Math.abs(Math.sin(time * 4));
        draw.circle(sx, sy, halo, new Color(c.r, c.g, c.b, 0.22f));
        draw.circle(sx, sy, 3, new Color(c.r, c.g, c.b, 0.5f));
        draw.spriteCentered(draw.sprites().get(c == Palette.DIM ? "cubesat_dim" : "cubesat"), Math.round(sx) + 0.5f, Math.round(sy) + 0.5f, 1);
    }

    public void frame(Draw draw, Color c) {
        draw.frame(x - 1, y - 1, w + 2, h + 2, 1, c);
    }

    @Override
    public void dispose() {
        texture.dispose();
    }
}
