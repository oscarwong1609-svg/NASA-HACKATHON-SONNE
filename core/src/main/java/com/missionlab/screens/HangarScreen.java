package com.missionlab.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.missionlab.MissionLabGame;
import com.missionlab.data.Catalog;
import com.missionlab.game.Controls;
import com.missionlab.game.Globe;
import com.missionlab.game.Palette;
import com.missionlab.hangar.BlueprintSlot;
import com.missionlab.hangar.Hangar;
import com.missionlab.hangar.Item;
import com.missionlab.hangar.Player;
import com.missionlab.hangar.Station;
import com.missionlab.model.Design;
import com.missionlab.model.Orbit;
import com.missionlab.model.Part;
import com.missionlab.model.Slot;
import com.missionlab.sim.Budgets;
import com.missionlab.sim.Readiness;
import com.missionlab.sim.Simulator;

import java.util.Locale;

/**
 * The hangar: the team walks around, picks up parts, installs them on the blueprint, votes on problems, uses the three
 * consoles (orbit map, rocket console, ground station desk) and boards the pad when the flight readiness review says GO.
 * At the bottom, a bar always shows the next step.
 */
public class HangarScreen extends BaseScreen implements Hangar.Listener {
    private static final float S = 2f; // world → text

    private final Hangar hangar;
    private String toast;
    private float toastTime;
    /** The last part or fixture someone was facing; its card is shown. */
    private Object focus;
    private float escTime = -10;
    private final OrbitMapView orbitView;
    private final RocketConsoleView rocketView;
    private final GroundDeskView groundView;

    public HangarScreen(MissionLabGame game) {
        super(game);
        hangar = new Hangar(session, this);
        orbitView = new OrbitMapView(draw, assets, session);
        rocketView = new RocketConsoleView(draw, assets, session);
        groundView = new GroundDeskView(draw, assets, session);
        toast("Build the satellite on the blueprint. The bar at the bottom always shows the next step.");
    }

    @Override
    public void toast(String message) {
        toast = message;
        toastTime = 4.5f;
    }

    @Override
    public void sound(String name) {
        assets.play(name);
    }

    // ------------------------------------------------------------------ logic

    @Override
    protected void update(float delta) {
        toastTime -= delta;
        if (hangar.console != null && Controls.back()) {
            hangar.closeConsole();
            return;
        }
        if (Controls.back()) {
            if (time - escTime < 1.5f) {
                game.setScreen(new TitleScreen(game));
                return;
            }
            escTime = time;
            toast("Press Esc again to quit to the title screen.");
        }

        boolean voting = hangar.event != null;
        for (Player p : hangar.players) {
            if (hangar.console != null) {
                // With a console open only whoever opened it is in control: left/right change, action closes.
                if (p != hangar.consoleUser) continue;
                hangar.consoleInput(p, p.keys.leftRightPressed());
                if (p.keys.actionPressed()) hangar.act(p);
                break;
            }
            if (voting) {
                hangar.vote(p, p.keys.leftRightPressed());
                if (p.keys.actionPressed()) hangar.lockVote(p);
                if (hangar.event == null) break;
            } else {
                hangar.move(p, p.keys.dx(), p.keys.dy(), delta);
                if (p.keys.actionPressed()) hangar.act(p);
            }
        }
        hangar.updateCarried();
        hangar.updatePad(delta);

        for (Player p : hangar.players) {
            Object near = p.holding != null ? p.holding : hangar.itemNear(p);
            if (near == null) near = hangar.stationNear(p);
            if (near != null) focus = near;
        }

        if (hangar.launchRequested) {
            session.result = Simulator.run(session.run, session.seed);
            assets.play("launch");
            game.setScreen(new LaunchScreen(game));
        }
    }

    // ------------------------------------------------------------------ world (pixel art)

    private static final Color PLATE_EDGE = Color.valueOf("262d45");
    private static final Color RIVET = Color.valueOf("3a4466");
    private static final Color RACK = Color.valueOf("4a5266");
    private static final Color RACK_LIGHT = Color.valueOf("7d8798");
    private static final Color DESK = Color.valueOf("1f2538");
    private static final Color SCREEN = Color.valueOf("07101f");

    @Override
    protected void drawWorld() {
        draw.rect(0, 0, MissionLabGame.WORLD_W, MissionLabGame.WORLD_H, Palette.SPACE);
        drawFloor();
        drawShelves();
        drawPad();
        drawBlueprint();
        for (Station s : hangar.stations) drawStation(s);

        for (Item it : hangar.items) if (it.carriers.isEmpty()) drawItem(it);
        drawHighlights();
        for (Player p : hangar.players) drawPlayer(p);
        for (Item it : hangar.items) if (!it.carriers.isEmpty()) drawItem(it);
    }

    /** Riveted metal floor plates and the back wall with its safety stripe. */
    private void drawFloor() {
        float fx = Hangar.FLOOR_X, fy = Hangar.FLOOR_Y, fw = Hangar.FLOOR_W, fh = Hangar.FLOOR_H;
        draw.rect(fx, fy, fw, fh, Palette.FLOOR);
        for (float x = fx; x < fx + fw; x += 16) {
            for (float y = fy; y < fy + fh; y += 16) {
                draw.rect(x, y, 1, Math.min(16, fy + fh - y), Palette.FLOOR_LINE);
                draw.rect(x, y, Math.min(16, fx + fw - x), 1, Palette.FLOOR_LINE);
                draw.rect(x + 1, y + 14, Math.min(14, fx + fw - x - 1), 1, PLATE_EDGE);
                if (x + 14 < fx + fw && y + 14 < fy + fh) {
                    draw.rect(x + 2, y + 13, 1, 1, RIVET);
                    draw.rect(x + 13, y + 2, 1, 1, RIVET);
                }
            }
        }
        float top = fy + fh;
        draw.rect(fx, top, fw, 4, Palette.WALL);
        draw.rect(fx, top + 3, fw, 1, RIVET);
        for (float x = fx; x < fx + fw; x += 8) draw.rect(x, top, 4, 1, Palette.WARN, 0.55f);
        draw.rect(fx, fy - 1, fw, 1, Palette.DARK);
    }

    private void drawShelves() {
        for (Slot s : Slot.values()) {
            float minX = Float.MAX_VALUE, maxX = 0, y = 0;
            for (Item it : hangar.items) {
                if (it.part.slot != s) continue;
                minX = Math.min(minX, it.homeX);
                maxX = Math.max(maxX, it.homeX + Item.SIZE);
                y = it.homeY;
            }
            // Metal shelving: shelf with highlight, two posts and shadow.
            draw.rect(minX - 3, y - 4, maxX - minX + 6, 1, Palette.DARK, 0.5f);
            draw.rect(minX - 3, y - 3, maxX - minX + 6, 3, RACK);
            draw.rect(minX - 3, y - 1, maxX - minX + 6, 1, RACK_LIGHT);
            draw.rect(minX - 3, y - 3, 2, Item.SIZE + 4, RACK);
            draw.rect(maxX + 1, y - 3, 2, Item.SIZE + 4, RACK);
            draw.rect(minX - 3, y + Item.SIZE, 2, 1, RACK_LIGHT);
            draw.rect(maxX + 1, y + Item.SIZE, 2, 1, RACK_LIGHT);
            for (Item it : hangar.items) {
                if (it.part.slot == s && !it.atHome()) {
                    // Empty spot: dotted outline of the missing part.
                    for (int k = 0; k < Item.SIZE; k += 2) {
                        draw.rect(it.homeX + k, it.homeY, 1, 1, Palette.WALL);
                        draw.rect(it.homeX + k, it.homeY + Item.SIZE - 1, 1, 1, Palette.WALL);
                        draw.rect(it.homeX, it.homeY + k, 1, 1, Palette.WALL);
                        draw.rect(it.homeX + Item.SIZE - 1, it.homeY + k, 1, 1, Palette.WALL);
                    }
                }
            }
        }
    }

    private void drawBlueprint() {
        float bx = Hangar.BP_X, by = Hangar.BP_Y, bw = Hangar.BP_W, bh = Hangar.BP_H;
        draw.rect(bx, by, bw, bh, Palette.BLUEPRINT);
        for (float x = bx; x < bx + bw; x += 6) draw.rect(x, by, 1, bh, Palette.BLUEPRINT_LINE, ((x - bx) % 24 == 0) ? 0.3f : 0.12f);
        for (float y = by; y < by + bh; y += 6) draw.rect(bx, y, bw, 1, Palette.BLUEPRINT_LINE, ((y - by) % 24 == 0) ? 0.3f : 0.12f);
        draw.frame(bx, by, bw, bh, 1, Palette.BLUEPRINT_LINE);
        draw.frame(bx + 2, by + 2, bw - 4, bh - 4, 1, new Color(Palette.BLUEPRINT_LINE.r, Palette.BLUEPRINT_LINE.g, Palette.BLUEPRINT_LINE.b, 0.45f));
        drawSchematic();
        for (BlueprintSlot s : hangar.slots.values()) {
            draw.rect(s.x, s.y, BlueprintSlot.W, BlueprintSlot.H, Palette.DARK, 0.3f);
            if (s.installed != null) {
                draw.frame(s.x, s.y, BlueprintSlot.W, BlueprintSlot.H, 1, new Color(Palette.GOOD.r, Palette.GOOD.g, Palette.GOOD.b, 0.35f));
                draw.brackets(s.x, s.y, BlueprintSlot.W, BlueprintSlot.H, 4, Palette.GOOD);
            } else {
                float blink = 0.55f + 0.45f * (float) Math.sin(time * 3);
                Color c = new Color(Palette.BLUEPRINT_LINE.r, Palette.BLUEPRINT_LINE.g, Palette.BLUEPRINT_LINE.b, blink);
                for (int k = 0; k < BlueprintSlot.W; k += 3) {
                    draw.rect(s.x + k, s.y, 2, 1, c);
                    draw.rect(s.x + k, s.y + BlueprintSlot.H - 1, 2, 1, c);
                }
                for (int k = 0; k < BlueprintSlot.H; k += 3) {
                    draw.rect(s.x, s.y + k, 1, 2, c);
                    draw.rect(s.x + BlueprintSlot.W - 1, s.y + k, 1, 2, c);
                }
            }
        }
    }

    /**
     * The satellite's technical drawing under the slots: the chassis grows with the frame (2U, 3U, 6U),
     * the solar wings with the chosen panel and the antenna with the radio.
     */
    private void drawSchematic() {
        Design d = session.design;
        float cx = Hangar.BP_X + Hangar.BP_W / 2f, cy = Hangar.BP_Y + Hangar.BP_H / 2f;
        Part frame = d.get(Slot.STRUCTURE), power = d.get(Slot.POWER), radio = d.get(Slot.RADIO);
        int fr = frame == null ? 1 : Catalog.partsFor(Slot.STRUCTURE).indexOf(frame);
        float hw = fr >= 2 ? 22 : 10, hh = fr == 0 ? 20 : 30;
        Color line = new Color(0.75f, 0.88f, 1f, frame == null ? 0.25f : 0.6f);
        dashedBox(cx - hw, cy - hh, hw * 2, hh * 2, line);
        int units = fr == 0 ? 2 : 3;
        for (int u = 1; u < units; u++) draw.dots(cx - hw, cy - hh + u * 2 * hh / units, cx + hw, cy - hh + u * 2 * hh / units, 3, 1, line, 1);
        if (fr >= 2) draw.dots(cx, cy - hh, cx, cy + hh, 3, 1, line, 1);

        if (power != null) {
            int tier = Catalog.partsFor(Slot.POWER).indexOf(power);
            Color cell = new Color(0.55f, 0.75f, 1f, 0.55f);
            if (tier == 0) {
                for (float y = cy - hh + 3; y < cy + hh - 2; y += 4) draw.rect(cx - hw + 2, y, hw * 2 - 4, 1, cell);
            } else {
                int segs = tier == 1 ? 1 : 2;
                float segW = tier == 1 ? 12 : 14, wh = tier == 1 ? 14 : 22;
                for (int side = -1; side <= 1; side += 2) {
                    float edge = side < 0 ? cx - hw : cx + hw;
                    draw.rect(side < 0 ? edge - 3 : edge, cy, 3, 1, line);
                    for (int k = 0; k < segs; k++) {
                        float x0 = side < 0 ? edge - 3 - (k + 1) * (segW + 1) : edge + 3 + k * (segW + 1) + 1;
                        dashedBox(x0, cy - wh / 2, segW, wh, line);
                        for (float y = cy - wh / 2 + 2; y < cy + wh / 2 - 1; y += 3) draw.rect(x0 + 1, y, segW - 2, 1, cell);
                    }
                }
            }
        }
        if (radio != null) {
            int tier = Catalog.partsFor(Slot.RADIO).indexOf(radio);
            float r = 3 + tier * 2, top = Math.min(cy + hh + 2, Hangar.BP_Y + Hangar.BP_H - 4 - r);
            for (int i = 0; i <= 8; i++) {
                double a = Math.PI * (0.15 + 0.7 * i / 8.0);
                draw.rect(Math.round(cx + (float) Math.cos(a) * r), Math.round(top + r - (float) Math.sin(a) * r * 0.6f), 1, 1, line);
            }
        }
    }

    private void dashedBox(float x, float y, float w, float h, Color c) {
        draw.dots(x, y, x + w, y, 3, 1, c, 1);
        draw.dots(x, y + h, x + w, y + h, 3, 1, c, 1);
        draw.dots(x, y, x, y + h, 3, 1, c, 1);
        draw.dots(x + w, y, x + w, y + h, 3, 1, c, 1);
    }

    /** The three consoles: a desk with a display (orbit map and rocket) and the ground dish antenna. */
    private void drawStation(Station s) {
        Design d = session.design;
        boolean ready = s.current(d) != null;
        float cx = s.cx(), cy = s.cy();
        Color status = ready ? Palette.GOOD : ((int) (time * 3)) % 2 == 0 ? Palette.WARN : Palette.FIRE;
        if (s.kind == Station.Kind.GROUND) {
            drawDishStation(s, ready, status);
            return;
        }
        draw.rect(s.x, s.y - 1, s.w, 1, Palette.DARK, 0.5f);
        draw.rect(s.x, s.y, s.w, s.h, DESK);
        draw.rect(s.x, s.y + s.h - 1, s.w, 1, RACK_LIGHT);
        draw.rect(s.x + 2, s.y + 2, s.w - 4, s.h - 5, Palette.DARK);
        draw.rect(s.x + 3, s.y + 3, s.w - 6, s.h - 7, SCREEN);
        draw.rect(s.x + s.w - 4, s.y + 1, 2, 1, status);
        draw.rect(s.x + 3, s.y + 1, 6, 1, RIVET);
        if (s.kind == Station.Kind.ORBIT) {
            Globe g = assets.sprites.globe(6).tilt(20);
            draw.sprite(g.frame(time * 30), cx - g.size / 2f, cy - g.size / 2f, 1);
            Orbit o = d.orbit;
            if (o != null) {
                boolean polar = o.maxLatitude > 60;
                float rx = polar ? 3 : 11, ry = polar ? (o.altitudeKm > 700 ? 8 : 7) : 3;
                for (int i = 0; i < 20; i++) {
                    double a = i / 20.0 * Math.PI * 2;
                    boolean back = Math.sin(a) > 0 && !polar;
                    if (back) continue;
                    draw.rect(Math.round(cx + (float) Math.cos(a) * rx), Math.round(cy + (float) Math.sin(a) * ry), 1, 1, Palette.ACCENT, 0.6f);
                }
                double a = time * 2;
                draw.rect(Math.round(cx + (float) Math.cos(a) * rx), Math.round(cy + (float) Math.sin(a) * ry), 1, 1, Palette.TEXT);
            }
        } else {
            // Rocket console: a rocket on the display and a dot per route leg (rocket, tug...).
            draw.sprite(assets.sprites.get("ic_launch"), cx - 9, cy - 5, 1);
            if (d.route != null) {
                for (int i = 0; i < d.route.legs().size(); i++) draw.rect(cx + 3 + i * 4, cy - 2 + i * 3, 2, 2, Palette.GOOD);
                for (float k = 0; k < 8; k += 2) draw.rect(cx - 2 + k, cy + 2 + k / 2, 1, 1, Palette.ACCENT, 0.5f);
            } else {
                draw.rect(cx + 4, cy - 1, 6, 1, Palette.WARN, 0.6f);
            }
        }
    }

    private void drawDishStation(Station s, boolean ready, Color status) {
        // Concrete base, the dish and a hut with its LED.
        draw.rect(s.x, s.y - 1, s.w, 1, Palette.DARK, 0.5f);
        draw.rect(s.x, s.y, s.w, 3, Palette.WALL);
        draw.rect(s.x, s.y + 2, s.w, 1, RIVET);
        TextureRegion dish = assets.sprites.get("dish_ground");
        draw.sprite(dish, s.x + 2, s.y + 3, 1);
        float hx = s.x + s.w - 11, hy = s.y + 3;
        draw.rect(hx, hy, 9, 8, DESK);
        draw.rect(hx, hy + 7, 9, 1, RACK_LIGHT);
        draw.rect(hx + 2, hy + 3, 5, 2, SCREEN);
        draw.rect(hx + 6, hy + 1, 2, 1, status);
        if (ready) {
            // Radio waves leaving the feed horn.
            float k = (time * 1.5f) % 1f;
            for (int w = 0; w < 2; w++) {
                float r = 3 + ((k + w * 0.5f) % 1f) * 8;
                float a = 1 - ((k + w * 0.5f) % 1f);
                for (int i = 0; i < 5; i++) {
                    double ang = Math.PI * (0.15 + 0.2 * i / 4.0);
                    draw.rect(Math.round(s.x + 13 + (float) Math.cos(ang) * r), Math.round(s.y + 16 + (float) Math.sin(ang) * r), 1, 1, Palette.GOOD, a);
                }
            }
        }
    }

    private void drawPad() {
        float x = Hangar.PAD_X, y = Hangar.PAD_Y, w = Hangar.PAD_W, h = Hangar.PAD_H;
        draw.rect(x, y - 1, w, 1, Palette.DARK, 0.5f);
        draw.rect(x, y, w, h, Palette.WALL);
        draw.rect(x + 3, y + 3, w - 6, h - 6, PLATE_EDGE);
        for (float i = 0; i < w; i += 6) {
            draw.rect(x + i, y, 3, 2, Palette.WARN);
            draw.rect(x + i, y + h - 2, 3, 2, Palette.WARN);
        }
        // Flame trench under the rocket.
        draw.rect(x + w / 2 - 12, y + 3, 16, 3, Palette.DARK);

        // Rideshares fly on large rockets; ISS deliveries on a smaller one with a capsule.
        boolean big = session.design.route != null && session.design.route.launchVehicle().maxKg >= 20;
        TextureRegion rocket = assets.sprites.get(big ? "rocket_big" : "rocket_small");
        float rw = rocket.getRegionWidth(), rh = rocket.getRegionHeight();
        float rx = Math.round(x + w / 2 - rw / 2 - 4), ry = y + 5;

        // Lattice service tower with an arm reaching the fairing.
        float tx = rx + rw + 3, tTop = ry + rh - 6;
        draw.rect(tx, ry, 1, tTop - ry, RACK_LIGHT);
        draw.rect(tx + 4, ry, 1, tTop - ry, RACK_LIGHT);
        for (float yy = ry; yy < tTop - 2; yy += 4) {
            draw.rect(tx + 1, yy, 1, 1, RACK);
            draw.rect(tx + 2, yy + 1, 1, 1, RACK);
            draw.rect(tx + 3, yy + 2, 1, 1, RACK);
            draw.rect(tx, yy + 3, 5, 1, RACK);
        }
        draw.rect(tx - 3, tTop - 8, 4, 1, RACK_LIGHT);
        draw.rect(tx - 1, tTop, 7, 1, RACK_LIGHT);
        draw.rect(tx + 2, tTop + 1, 1, 3, Palette.BAD);

        draw.sprite(rocket, rx, ry, 1);
        drawPayloadWindow(rx, ry, rw, rh, big);

        // If the route uses a space tug, it can be seen waiting on the pad.
        if (session.design.route != null && session.design.route.legs().size() > 1) {
            draw.sprite(assets.sprites.get("tug"), x + 4, y + 8, 1);
        }
        if (hangar.padTimer > 0) {
            float k = hangar.padTimer / Hangar.LAUNCH_HOLD;
            draw.rect(x, y - 3, w, 2, Palette.DARK);
            draw.rect(x, y - 3, w * k, 2, Palette.GOOD);
        }
    }

    /** A small window in the rocket fairing with the CubeSat inside, sized to the chosen frame. */
    private void drawPayloadWindow(float rx, float ry, float rw, float rh, boolean big) {
        Part frame = session.design.get(Slot.STRUCTURE);
        if (frame == null) return;
        int fr = Catalog.partsFor(Slot.STRUCTURE).indexOf(frame);
        // Rows 6-12 of the fairing on the large rocket; rows 13-19, below the capsule, on the small one.
        float wx = rx + rw / 2 - 3, wy = ry + rh - (big ? 13 : 20);
        draw.rect(wx - 1, wy - 1, 8, 9, Palette.DARK);
        draw.rect(wx, wy, 6, 7, SCREEN);
        float sw = fr >= 2 ? 4 : 2, sh = fr == 0 ? 3 : fr == 1 ? 5 : 4;
        float sx = wx + 3 - sw / 2, sy = wy + 1;
        draw.rect(sx, sy, sw, sh, Palette.WARN);
        draw.rect(sx, sy + sh - 1, sw, 1, Palette.TEXT);
        if (session.design.get(Slot.POWER) != null) draw.rect(sx - 1, sy + 1, 1, Math.max(2, sh - 2), Palette.ACCENT);
    }

    private void drawItem(Item it) {
        Part p = it.part;
        TextureRegion sprite = assets.sprite(p.id);
        boolean carried = !it.carriers.isEmpty();
        if (carried) draw.rect(Math.round(it.x + 2), Math.round(it.y - 4), Item.SIZE - 4, 2, Palette.DARK, 0.45f);
        if (sprite != null) {
            float w = sprite.getRegionWidth(), h = sprite.getRegionHeight();
            if (w == Item.SIZE && h == Item.SIZE) draw.sprite(sprite, it.x, it.y, 1);
            else draw.region(sprite, it.x, it.y, Item.SIZE, Item.SIZE);
        } else {
            draw.rect(it.x, it.y, Item.SIZE, Item.SIZE, Palette.slot(p.slot));
        }
        if (p.heavy) {
            // "Heavy" tag: black and yellow stripes in the corner.
            float hx = Math.round(it.x + Item.SIZE - 4), hy = Math.round(it.y + Item.SIZE - 2);
            draw.rect(hx - 1, hy - 1, 6, 4, Palette.DARK);
            draw.rect(hx, hy, 1, 2, Palette.WARN);
            draw.rect(hx + 2, hy, 1, 2, Palette.WARN);
            draw.rect(hx + 4, hy, 1, 2, Palette.WARN);
        }
    }

    private void drawPlayer(Player p) {
        TextureRegion custom = assets.sprite("player_" + p.index);
        int frame = p.walkTime > 0 && ((int) (p.walkTime * 8)) % 2 == 1 ? 1 : 0;
        draw.rect(Math.round(p.cx() - 4), Math.round(p.y - 1), 8, 2, Palette.DARK, 0.45f);
        if (custom != null) {
            draw.region(custom, p.x, p.y, Player.W, Player.H);
        } else {
            draw.sprite(assets.sprites.astronaut(p.color, frame, p.facingLeft), p.cx() - 5, p.y, 1);
        }
        if (hangar.waitingForHelp(p)) {
            float a = 0.5f + 0.5f * (float) Math.sin(time * 8);
            draw.rect(Math.round(p.cx() - 1), Math.round(p.y + 24), 2, 2, Palette.WARN, a);
        }
    }

    private void drawHighlights() {
        for (Player p : hangar.players) {
            if (hangar.event != null) break;
            if (p.holding != null) {
                BlueprintSlot s = hangar.nearestSlot(p.holding);
                if (s != null) {
                    Color c = s.slot == p.holding.part.slot ? Palette.GOOD : Palette.BAD;
                    draw.frame(s.x - 1, s.y - 1, BlueprintSlot.W + 2, BlueprintSlot.H + 2, 1, c);
                    draw.brackets(s.x - 2, s.y - 2, BlueprintSlot.W + 4, BlueprintSlot.H + 4, 4, c);
                }
                continue;
            }
            Item it = hangar.itemNear(p);
            if (it != null) {
                draw.brackets(Math.round(it.x) - 2, Math.round(it.y) - 2, Item.SIZE + 4, Item.SIZE + 4, 3, p.color);
                continue;
            }
            Station st = hangar.stationNear(p);
            if (st != null) draw.brackets(st.x - 2, st.y - 2, st.w + 4, st.h + 4, 4, p.color);
        }
    }

    // ------------------------------------------------------------------ text

    @Override
    protected void drawUi() {
        if (hangar.console != null) {
            ConsoleView view = hangar.console.kind == Station.Kind.ORBIT ? orbitView
                    : hangar.console.kind == Station.Kind.ROCKET ? rocketView : groundView;
            Player p = hangar.consoleUser;
            view.draw(time, p.role == null ? "P" + (p.index + 1) : p.role.label + " (P" + (p.index + 1) + ")");
            return;
        }
        drawLabels();
        drawGauges();
        drawCard();
        drawPadStatus();
        drawObjective();
        drawToast();
        if (hangar.event != null) drawEvent();
    }

    private void drawToast() {
        if (toastTime > 0 && toast != null) {
            java.util.List<String> lines = assets.small.lines(toast, 404);
            float h = 10 + lines.size() * assets.small.lineHeight;
            float a = Math.min(1, toastTime * 3);
            draw.rect(110, 36, 420, h, Palette.PANEL, 0.9f * a);
            draw.rect(110, 36, 3, h, Palette.ACCENT, a);
            draw.brackets(110, 36, 420, h, 4, draw.dim(Palette.ACCENT, a));
            draw.wrap(assets.small, toast, 120, 36 + h - 5, 404, new Color(1, 1, 1, a));
        }
    }

    /** The bottom bar: which phase the team is in, what the flight readiness review says and what the next step is. */
    private void drawObjective() {
        Readiness review = hangar.readiness();
        draw.rect(0, 0, MissionLabGame.UI_W, 30, Palette.PANEL, 0.97f);
        draw.rect(0, 30, MissionLabGame.UI_W, 1, draw.dim(Palette.ACCENT, 0.5f));
        draw.scanlines(0, 0, MissionLabGame.UI_W, 30, 0.04f);
        float w = draw.chip(assets.label, "STEP 2 OF 5: BUILD + REVIEW", 8, 25, Palette.WARN);
        draw.text(assets.small, "Mission " + session.mission.number + ": " + session.mission.title, 16 + w, 25, Palette.DIM);
        boolean go = review.go();
        String verdict = "FLIGHT REVIEW: " + ConsoleView.reviewSummary(review);
        float vw = draw.width(assets.label, verdict);
        draw.led(632 - vw - 12, 20, go ? Palette.GOOD : Palette.WARN, go || ((int) (time * 3)) % 2 == 0);
        draw.textRight(assets.label, verdict, 632, 25, go ? Palette.GOOD : Palette.WARN);
        draw.text(assets.label, "NEXT >", 8, 12, Palette.WARN);
        draw.text(assets.small, draw.fit(assets.small, review.nextStep(), 584), 50, 12, Palette.TEXT);
    }

    private void drawLabels() {
        label(Slot.INSTRUMENT, "SCIENCE LAB");
        label(Slot.POWER, "POWER");
        label(Slot.BATTERY, "BATTERIES");
        label(Slot.POINTING, "POINTING");
        label(Slot.STRUCTURE, "FRAMES");
        label(Slot.RADIO, "RADIOS");
        for (Station s : hangar.stations) {
            boolean top = s.y > 60;
            String name = s.kind.label.toUpperCase(Locale.ROOT);
            if (top) sign(name, s.x * S, (s.y - 3) * S, Palette.DIM);
            else sign(name, (s.x + s.w) * S + 6, (s.y + s.h / 2) * S + 3, Palette.DIM);
        }
        sign("LAUNCH PAD", Hangar.PAD_X * S + 8, (Hangar.PAD_Y - 5) * S, Palette.WARN);
        for (BlueprintSlot s : hangar.slots.values()) {
            if (s.installed == null) draw.text(assets.label, s.slot.label.toUpperCase(Locale.ROOT), s.x * S + 4, (s.y + 6) * S, Palette.BLUEPRINT_LINE);
        }
        Part frame = session.design.get(Slot.STRUCTURE);
        String block = "SAT-1  //  " + (frame == null ? "NO FRAME" : frame.name.toUpperCase(Locale.ROOT));
        draw.text(assets.label, block, Hangar.BP_X * S + 6, (Hangar.BP_Y + Hangar.BP_H) * S - 5, new Color(0.75f, 0.88f, 1f, 0.8f));
        for (Player p : hangar.players) {
            float tx = p.cx() * S, ty = (p.y + 14) * S + 11;
            float tw = draw.width(assets.label, p.tag());
            draw.rect(Math.round(tx - tw / 2 - 2), ty - 7, tw + 4, 9, Palette.DARK, 0.75f);
            draw.textCenter(assets.label, p.tag(), tx, ty, p.color);
            if (hangar.waitingForHelp(p)) draw.textCenter(assets.label, "HELP LIFT!", tx, ty + 10, Palette.WARN);
        }
    }

    /** A wall sign: dark plate with uppercase text. */
    private void sign(String text, float x, float y, Color c) {
        float w = draw.width(assets.label, text);
        draw.rect(x - 3, y - 7, w + 6, 9, Palette.DARK, 0.85f);
        draw.text(assets.label, text, x, y, c);
    }

    /** Shelf name, above its parts. Drawn in the owner's colour if that role is taken. */
    private void label(Slot slot, String text) {
        for (Item it : hangar.items) {
            if (it.part.slot != slot) continue;
            Color c = session.claimed(slot.owner) ? Palette.role(slot.owner) : Palette.DIM;
            sign(text, it.homeX * S, (it.homeY + Item.SIZE + 2) * S + 12, c);
            return;
        }
    }

    /** The design's seven budgets, in two columns. Green up to 80%, amber up to 100%, red when exceeded. */
    private void drawGauges() {
        draw.rect(0, 290, MissionLabGame.UI_W, 70, Palette.PANEL, 0.96f);
        draw.scanlines(0, 290, MissionLabGame.UI_W, 70, 0.04f);
        draw.rect(0, 290, MissionLabGame.UI_W, 1, draw.dim(Palette.ACCENT, 0.5f));
        Budgets b = Budgets.of(session.run);
        Design d = session.design;
        double limit = b.massLimitKg();
        float a = 8, c = 214, y = 352, step = 15;
        gauge(a, y, "WEIGHT", b.massRatio(), limit <= 0 ? f("%.1f kg", b.massKg) : f("%.1f of %.0f kg", b.massKg, limit));
        gauge(a, y - step, "VOLUME", b.volumeRatio(), b.frameVolumeU <= 0 ? f("%.1fU", b.volumeU) : f("%.1f of %.0fU", b.volumeU, b.frameVolumeU));
        gauge(a, y - 2 * step, "POWER", b.powerRatio(), f("%.1f of %.1f W", b.drawW, b.generatedW));
        gauge(a, y - 3 * step, "BATTERY", b.batteryRatio(), f("%.1f of %.1f Wh", b.eclipseNeedWh, b.batteryUsableWh));
        gauge(c, y, "DATA", d.station == null ? 0 : b.dataRatio(),
                d.station == null ? f("%.1f GB/day", b.dataGbPerDay) : f("%.1f of %.1f GB", b.dataGbPerDay, b.downlinkGbPerDay));
        gauge(c, y - step, "BUDGET", b.budgetRatio(), budget(b.costK) + " of " + budget(b.budgetK));
        gauge(c, y - 2 * step, "TIME", b.timeRatio(), f("%.1f of %.0f months", b.months, b.deadlineMonths));
        draw.rect(c, y - 3 * step - 4, 4, 4, Palette.TEXT, 0.7f);
        draw.text(assets.small, "= 80% line.  Used of available.", c + 8, y - 3 * step, Palette.DIM);
    }

    private void gauge(float x, float y, String name, double ratio, String value) {
        draw.text(assets.label, name, x, y, Palette.DIM);
        draw.bar(x + 58, y - 6, 50, 7, ratio);
        draw.text(assets.small, draw.fit(assets.small, value, 90), x + 114, y, ratio > 1 ? Palette.BAD : Palette.TEXT);
    }

    private static String f(String fmt, Object... args) {
        return String.format(Locale.ROOT, fmt, args);
    }

    private void drawCard() {
        float x = 420, y = 354, w = 212;
        draw.rect(x - 8, 292, 1, 66, draw.dim(Palette.ACCENT, 0.5f));
        if (focus instanceof Item) {
            Part p = ((Item) focus).part;
            TextureRegion sprite = assets.sprite(p.id);
            draw.rect(x, y - 25, 26, 26, Palette.DARK);
            draw.brackets(x, y - 25, 26, 26, 3, Palette.slot(p.slot));
            if (sprite != null) draw.sprite(sprite, x + 1, y - 24, 2);
            draw.text(assets.font, draw.fit(assets.font, p.name, w - 32), x + 32, y, Palette.slot(p.slot));
            String who = session.claimed(p.slot.owner) ? p.slot.owner.label + "'s shelf" : "anyone can take it";
            draw.text(assets.small, p.slot.label + " - " + who, x + 32, y - 13, Palette.DIM);
            if (p.heavy) draw.chip(assets.label, "HEAVY", x + 32, y - 21, Palette.WARN);
            float h = draw.wrap(assets.small, p.description, x, y - 32, w, Palette.TEXT);
            draw.text(assets.small, draw.fit(assets.small, stats(p), w), x, y - 36 - h, Palette.ACCENT);
        } else if (focus instanceof Station) {
            Station s = (Station) focus;
            Object cur = s.current(session.design);
            String icon = s.kind == Station.Kind.ORBIT ? "ic_science" : s.kind == Station.Kind.ROCKET ? "ic_launch" : "ic_target";
            draw.sprite(assets.sprites.get(icon), x, y - 8, 1);
            draw.text(assets.font, s.kind.label, x + 14, y, Palette.ACCENT);
            String now = cur != null ? "Now: " + cur : "Nothing chosen yet.";
            draw.text(assets.small, draw.fit(assets.small, now, w), x, y - 15, cur != null ? Palette.TEXT : Palette.WARN);
            String help = s.kind == Station.Kind.ORBIT ? "Paths, night side, coverage."
                    : s.kind == Station.Kind.ROCKET ? "Flight graph and GO / NO-GO review."
                    : "Antennas, data and battery.";
            draw.text(assets.small, help, x, y - 28, Palette.DIM);
            draw.text(assets.label, "> PRESS ACTION TO OPEN", x, y - 42, Palette.ACCENT);
        } else {
            draw.sprite(assets.sprites.get(patchSprite(session.mission)), x, y - 22, 1);
            draw.text(assets.font, draw.fit(assets.font, session.mission.title, w - 30), x + 30, y, Palette.ACCENT);
            draw.text(assets.small, session.mission.targetName + (session.mission.night ? ", at night" : ", in daylight"), x + 30, y - 13, Palette.DIM);
            draw.wrap(assets.small, "Walk to a shelf and press your action key to pick up a part.", x, y - 30, w, Palette.TEXT);
        }
    }

    private static String stats(Part p) {
        StringBuilder sb = new StringBuilder(String.format(Locale.ROOT, "%.2f kg  %s  %.0f mo lead", p.massKg, budget(p.costK), p.leadMonths));
        if (p.volumeU > 0) sb.append(String.format(Locale.ROOT, "  %.1fU", p.volumeU));
        if (p.powerW > 0) sb.append(String.format(Locale.ROOT, "  %.1f W", p.powerW));
        if (p.generatesW > 0) sb.append(String.format(Locale.ROOT, "  makes %.0f W in sun", p.generatesW));
        if (p.batteryWh > 0) sb.append(String.format(Locale.ROOT, "  %.0f Wh", p.batteryWh));
        if (p.ifovMicroRad > 0) sb.append(String.format(Locale.ROOT, "  %.0f m pixels at 550 km  %.1f GB/day", p.groundResolutionM(550), p.dataGbPerDay));
        if (p.downlinkMbps > 0) sb.append(String.format(Locale.ROOT, "  %.0f Mbps", p.downlinkMbps));
        if (p.frameVolumeU > 0) sb.append(String.format(Locale.ROOT, "  %.0fU, up to %.0f kg", p.frameVolumeU, p.frameMaxKg));
        return sb.toString();
    }

    private void drawPadStatus() {
        int on = hangar.playersOnPad(), n = hangar.players.size();
        String s;
        Color c = Palette.DIM;
        if (hangar.padTimer > 0) {
            s = String.format(Locale.ROOT, "LAUNCH IN %.1f", Hangar.LAUNCH_HOLD - hangar.padTimer);
            c = Palette.GOOD;
        } else if (on > 0) {
            s = "ON PAD: " + on + " OF " + n;
            c = Palette.WARN;
        } else {
            s = "ALL ABOARD TO LAUNCH";
        }
        sign(s, Hangar.PAD_X * S + 4, (Hangar.PAD_Y + Hangar.PAD_H) * S + 13, c);
    }

    private void drawEvent() {
        draw.rect(0, 0, MissionLabGame.UI_W, MissionLabGame.UI_H, Palette.DARK, 0.65f);
        float x = 110, y = 66, w = 420, h = 218;
        float top = draw.terminal(x, y, w, h, "PROBLEM DURING TESTING", Palette.WARN, time);
        draw.text(assets.font, hangar.event.title, x + 14, top, Palette.TEXT);
        float th = draw.wrap(assets.small, hangar.event.text, x + 14, top - 18, w - 28, Palette.TEXT);

        float bw = 190, bh = 60, by = Math.max(y + 50, top - 18 - th - 22 - bh);
        option(x + 14, by, bw, bh, "FIX IT", hangar.event.fixLabel, 0);
        option(x + w - 14 - bw, by, bw, bh, "TAKE THE RISK", hangar.event.riskLabel, 1);
        String how = hangar.players.size() == 1 ? "Left or right to choose, action to decide."
                : "Each player: left or right to vote, action to lock in. A tie means fix it.";
        draw.wrap(assets.small, how, x + 14, y + 32, w - 28, Palette.DIM);
    }

    private void option(float x, float y, float w, float h, String title, String text, int value) {
        Color c = value == 0 ? Palette.GOOD : Palette.BAD;
        boolean chosen = false;
        for (Player p : hangar.players) chosen |= hangar.vote[p.index] == value;
        draw.rect(x, y, w, h, Palette.DARK);
        if (chosen) draw.rect(x, y, w, h, c, 0.08f);
        draw.frame(x, y, w, h, 1, draw.dim(c, chosen ? 0.8f : 0.4f));
        if (chosen) draw.brackets(x - 2, y - 2, w + 4, h + 4, 6, c);
        draw.text(assets.label, title, x + 8, y + h - 7, c);
        draw.wrap(assets.small, text, x + 8, y + h - 20, w - 16, Palette.TEXT);
        int i = 0;
        for (Player p : hangar.players) {
            if (hangar.vote[p.index] != value) continue;
            float px = x + 8 + i * 14;
            boolean locked = hangar.locked[p.index];
            draw.sprite(assets.sprites.astronaut(p.color, 0, false), px, y + 4, 1,
                    locked ? Color.WHITE : new Color(1, 1, 1, 0.35f + 0.3f * (float) Math.abs(Math.sin(time * 4))));
            i++;
        }
    }
}
