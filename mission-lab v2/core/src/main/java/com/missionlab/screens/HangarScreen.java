package com.missionlab.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.missionlab.MissionLabGame;
import com.missionlab.data.Catalog;
import com.missionlab.game.Controls;
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
 * El hangar: el equipo camina, coge piezas, las monta en el plano, vota los problemas, usa las tres consolas
 * (mapa de órbitas, consola del cohete, mesa de estaciones) y sube a la plataforma cuando la revisión de vuelo dice GO.
 * Abajo, una barra dice siempre cuál es el siguiente paso.
 */
public class HangarScreen extends BaseScreen implements Hangar.Listener {
    private static final float S = 2f; // mundo → texto

    private final Hangar hangar;
    private String toast;
    private float toastTime;
    /** La última pieza o mueble que alguien tuvo delante; se muestra su tarjeta. */
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

    // ------------------------------------------------------------------ lógica

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
                // Con una consola abierta solo manda quien la abrió: izquierda/derecha cambian, acción cierra.
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

    // ------------------------------------------------------------------ mundo (pixel art)

    @Override
    protected void drawWorld() {
        draw.rect(0, 0, MissionLabGame.WORLD_W, MissionLabGame.WORLD_H, Palette.SPACE);
        draw.rect(Hangar.FLOOR_X, Hangar.FLOOR_Y, Hangar.FLOOR_W, Hangar.FLOOR_H, Palette.FLOOR);
        for (float x = Hangar.FLOOR_X; x < Hangar.FLOOR_X + Hangar.FLOOR_W; x += 16)
            draw.rect(x, Hangar.FLOOR_Y, 1, Hangar.FLOOR_H, Palette.FLOOR_LINE);
        for (float y = Hangar.FLOOR_Y; y < Hangar.FLOOR_Y + Hangar.FLOOR_H; y += 16)
            draw.rect(Hangar.FLOOR_X, y, Hangar.FLOOR_W, 1, Palette.FLOOR_LINE);
        draw.rect(Hangar.FLOOR_X, Hangar.FLOOR_Y + Hangar.FLOOR_H, Hangar.FLOOR_W, 4, Palette.WALL);

        drawShelves();
        drawPad();
        drawBlueprint();
        for (Station s : hangar.stations) drawStation(s);

        for (Item it : hangar.items) if (it.carriers.isEmpty()) drawItem(it);
        drawHighlights();
        for (Player p : hangar.players) drawPlayer(p);
        for (Item it : hangar.items) if (!it.carriers.isEmpty()) drawItem(it);
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
            draw.rect(minX - 3, y - 3, maxX - minX + 6, 3, Palette.SHELF);
            draw.rect(minX - 3, y - 3, 2, Item.SIZE + 3, Palette.SHELF);
            draw.rect(maxX + 1, y - 3, 2, Item.SIZE + 3, Palette.SHELF);
            for (Item it : hangar.items) {
                if (it.part.slot == s && !it.atHome()) draw.frame(it.homeX, it.homeY, Item.SIZE, Item.SIZE, 1, Palette.WALL);
            }
        }
    }

    private void drawBlueprint() {
        draw.rect(Hangar.BP_X, Hangar.BP_Y, Hangar.BP_W, Hangar.BP_H, Palette.BLUEPRINT);
        for (float x = Hangar.BP_X; x < Hangar.BP_X + Hangar.BP_W; x += 8)
            draw.rect(x, Hangar.BP_Y, 1, Hangar.BP_H, Palette.BLUEPRINT_LINE, 0.25f);
        for (float y = Hangar.BP_Y; y < Hangar.BP_Y + Hangar.BP_H; y += 8)
            draw.rect(Hangar.BP_X, y, Hangar.BP_W, 1, Palette.BLUEPRINT_LINE, 0.25f);
        draw.frame(Hangar.BP_X, Hangar.BP_Y, Hangar.BP_W, Hangar.BP_H, 1, Palette.BLUEPRINT_LINE);
        for (BlueprintSlot s : hangar.slots.values()) {
            Color c = s.installed != null ? Palette.GOOD : Palette.BLUEPRINT_LINE;
            float blink = s.installed == null ? 0.6f + 0.4f * (float) Math.sin(time * 3) : 1f;
            draw.rect(s.x, s.y, BlueprintSlot.W, BlueprintSlot.H, Palette.DARK, 0.25f);
            draw.frame(s.x, s.y, BlueprintSlot.W, BlueprintSlot.H, 1, new Color(c.r, c.g, c.b, blink));
        }
    }

    private void drawStation(Station s) {
        Design d = session.design;
        draw.rect(s.x, s.y, s.w, s.h, Palette.PANEL);
        draw.frame(s.x, s.y, s.w, s.h, 1, s.current(d) != null ? Palette.METAL : Palette.WARN);
        float cx = s.cx(), cy = s.cy();
        switch (s.kind) {
            case ORBIT: {
                draw.circle(cx, cy, 5, Palette.EARTH);
                Orbit o = d.orbit;
                if (o != null) {
                    boolean polar = o.maxLatitude > 60;
                    float rx = polar ? 3 : 11, ry = polar ? (o.altitudeKm > 700 ? 9 : 8) : 3;
                    for (int i = 0; i < 16; i++) {
                        double a = i / 16.0 * Math.PI * 2 + time;
                        draw.rect(cx + (float) Math.cos(a) * rx, cy + (float) Math.sin(a) * ry, 1, 1, Palette.ACCENT);
                    }
                }
                break;
            }
            case ROCKET: {
                draw.rect(cx - 2, cy - 6, 4, 10, Palette.TEXT);
                draw.rect(cx - 1, cy + 4, 2, 2, Palette.BAD);
                draw.rect(cx - 4, cy - 6, 2, 3, Palette.METAL);
                draw.rect(cx + 2, cy - 6, 2, 3, Palette.METAL);
                // Un punto por cada tramo de la ruta elegida (cohete, remolcador...).
                if (d.route != null) {
                    for (int i = 0; i < d.route.legs().size(); i++) draw.rect(cx + 6, cy - 5 + i * 4, 2, 2, Palette.ACCENT);
                }
                break;
            }
            default: {
                draw.circle(cx, cy + 2, 5, Palette.METAL);
                draw.circle(cx + 1, cy + 3, 3, Palette.PANEL);
                draw.rect(cx - 1, cy - 7, 2, 5, Palette.METAL);
                break;
            }
        }
    }

    private void drawPad() {
        float x = Hangar.PAD_X, y = Hangar.PAD_Y, w = Hangar.PAD_W, h = Hangar.PAD_H;
        draw.rect(x, y, w, h, Palette.WALL);
        for (float i = 0; i < w; i += 6) {
            draw.rect(x + i, y, 3, 2, Palette.WARN);
            draw.rect(x + i, y + h - 2, 3, 2, Palette.WARN);
        }
        // Los viajes compartidos van en cohetes grandes; los envíos a la ISS, en una nave de carga.
        boolean big = session.design.route != null && session.design.route.launchVehicle().maxKg >= 20;
        float rw = big ? 12 : 9, rh = big ? 46 : 36;
        float rx = x + w / 2 - rw / 2, ry = y + h / 2 - rh / 2 + 4;
        draw.rect(rx, ry, rw, rh, Palette.TEXT);
        draw.rect(rx + 1, ry + rh, rw - 2, 3, Palette.TEXT);
        draw.rect(rx + 3, ry + rh + 3, rw - 6, 3, Palette.BAD);
        draw.rect(rx - 3, ry, 3, 8, Palette.METAL);
        draw.rect(rx + rw, ry, 3, 8, Palette.METAL);
        draw.rect(rx + 2, ry + rh - 10, rw - 4, 4, Palette.ACCENT);
        if (hangar.padTimer > 0) {
            float k = hangar.padTimer / Hangar.LAUNCH_HOLD;
            draw.rect(x, y - 3, w * k, 2, Palette.GOOD);
        }
    }

    private void drawItem(Item it) {
        Part p = it.part;
        TextureRegion sprite = assets.sprite(p.id);
        if (sprite != null) {
            draw.region(sprite, it.x, it.y, Item.SIZE, Item.SIZE);
            return;
        }
        Color c = Palette.slot(p.slot);
        draw.rect(it.x, it.y, Item.SIZE, Item.SIZE, Palette.DARK);
        draw.rect(it.x + 1, it.y + 1, Item.SIZE - 2, Item.SIZE - 2, c);
        // Un detalle por tamaño, para distinguir las piezas del mismo estante.
        int rank = Catalog.partsFor(p.slot).indexOf(p);
        for (int i = 0; i <= rank; i++) draw.rect(it.x + 3 + i * 3, it.y + 3, 2, 2, Palette.DARK);
        if (p.heavy) {
            draw.rect(it.x, it.y + Item.SIZE - 3, Item.SIZE, 2, Palette.DARK);
            draw.rect(it.x + 2, it.y + Item.SIZE - 3, 2, 2, Palette.WARN);
            draw.rect(it.x + Item.SIZE - 4, it.y + Item.SIZE - 3, 2, 2, Palette.WARN);
        }
    }

    private void drawPlayer(Player p) {
        TextureRegion sprite = assets.sprite("player_" + p.index);
        float bob = (p.walkTime > 0 && ((int) (p.walkTime * 8)) % 2 == 0) ? 1 : 0;
        if (sprite != null) {
            draw.region(sprite, p.x, p.y + bob, Player.W, Player.H);
        } else {
            draw.rect(p.x + 1, p.y, 2, 3, Palette.DARK);
            draw.rect(p.x + 5, p.y, 2, 3, Palette.DARK);
            draw.rect(p.x, p.y + 2 + bob, Player.W, Player.H - 2, p.color);
            float ex = p.facingLeft ? p.x + 1 : p.x + 4;
            draw.rect(ex, p.y + 6 + bob, 3, 2, Palette.TEXT);
            draw.rect(ex + (p.facingLeft ? 0 : 2), p.y + 6 + bob, 1, 2, Palette.DARK);
        }
        if (hangar.waitingForHelp(p)) {
            float a = 0.5f + 0.5f * (float) Math.sin(time * 8);
            draw.rect(p.x + 3, p.y + Player.H + 14, 2, 2, Palette.WARN, a);
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
                }
                continue;
            }
            Item it = hangar.itemNear(p);
            if (it != null) {
                draw.frame(it.x - 1, it.y - 1, Item.SIZE + 2, Item.SIZE + 2, 1, p.color);
                continue;
            }
            Station st = hangar.stationNear(p);
            if (st != null) draw.frame(st.x - 1, st.y - 1, st.w + 2, st.h + 2, 1, p.color);
        }
    }

    // ------------------------------------------------------------------ texto

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
            draw.rect(100, 40, 440, 34, Palette.PANEL, 0.92f);
            draw.frame(100, 40, 440, 34, 1, Palette.WALL);
            draw.wrap(assets.small, toast, 110, 68, 420, Palette.TEXT);
        }
    }

    /** La barra de abajo: en qué fase está el equipo, qué dice la revisión de vuelo y cuál es el siguiente paso. */
    private void drawObjective() {
        Readiness review = hangar.readiness();
        draw.rect(0, 0, MissionLabGame.UI_W, 30, Palette.PANEL, 0.95f);
        draw.rect(0, 30, MissionLabGame.UI_W, 1, Palette.WALL);
        float w = draw.chip(assets.small, "STEP 2 OF 5: BUILD & REVIEW", 8, 27, Palette.WARN);
        draw.text(assets.small, "Mission " + session.mission.number + ": " + session.mission.title, 16 + w, 27, Palette.DIM);
        draw.textRight(assets.small, "FLIGHT REVIEW: " + ConsoleView.reviewSummary(review), 632, 27,
                review.go() ? Palette.GOOD : Palette.WARN);
        draw.text(assets.small, "NEXT:", 8, 13, Palette.WARN);
        draw.text(assets.small, draw.fit(assets.small, review.nextStep(), 584), 44, 13, Palette.TEXT);
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
            if (top) draw.text(assets.small, s.kind.label.toUpperCase(Locale.ROOT), s.x * S, (s.y - 2) * S, Palette.DIM);
            else draw.text(assets.small, s.kind.label.toUpperCase(Locale.ROOT), (s.x + s.w) * S + 6, (s.y + s.h / 2) * S + 5, Palette.DIM);
        }
        draw.text(assets.small, "LAUNCH PAD", Hangar.PAD_X * S + 8, (Hangar.PAD_Y - 4) * S, Palette.WARN);
        for (BlueprintSlot s : hangar.slots.values()) {
            if (s.installed == null) draw.text(assets.small, s.slot.label, s.x * S + 3, (s.y + 6) * S, Palette.BLUEPRINT_LINE);
        }
        for (Player p : hangar.players) {
            draw.textCenter(assets.small, p.tag(), p.cx() * S, (p.y + Player.H + 12) * S, p.color);
            if (hangar.waitingForHelp(p))
                draw.textCenter(assets.small, "HELP LIFT!", p.cx() * S, (p.y + Player.H + 20) * S, Palette.WARN);
        }
    }

    /** Nombre del estante, encima de sus piezas. Se pinta del color del dueño si ese papel está ocupado. */
    private void label(Slot slot, String text) {
        for (Item it : hangar.items) {
            if (it.part.slot != slot) continue;
            Color c = session.claimed(slot.owner) ? Palette.role(slot.owner) : Palette.DIM;
            draw.text(assets.small, text, it.homeX * S, (it.homeY + Item.SIZE + 2) * S + 12, c);
            return;
        }
    }

    /** Las siete cuentas del diseño, en dos columnas. Verde hasta el 80%, ámbar hasta el 100%, rojo si se pasa. */
    private void drawGauges() {
        draw.rect(0, 290, MissionLabGame.UI_W, 70, Palette.PANEL, 0.94f);
        Budgets b = Budgets.of(session.run);
        Design d = session.design;
        double limit = b.massLimitKg();
        float a = 8, c = 214, y = 354, step = 16;
        gauge(a, y, "WEIGHT", b.massRatio(), limit <= 0 ? f("%.1f kg", b.massKg) : f("%.1f of %.0f kg", b.massKg, limit));
        gauge(a, y - step, "VOLUME", b.volumeRatio(), b.frameVolumeU <= 0 ? f("%.1fU", b.volumeU) : f("%.1f of %.0fU", b.volumeU, b.frameVolumeU));
        gauge(a, y - 2 * step, "POWER", b.powerRatio(), f("%.1f of %.1f W", b.drawW, b.generatedW));
        gauge(a, y - 3 * step, "BATTERY", b.batteryRatio(), f("%.1f of %.1f Wh", b.eclipseNeedWh, b.batteryUsableWh));
        gauge(c, y, "DATA", d.station == null ? 0 : b.dataRatio(),
                d.station == null ? f("%.1f GB/day", b.dataGbPerDay) : f("%.1f of %.1f GB", b.dataGbPerDay, b.downlinkGbPerDay));
        gauge(c, y - step, "BUDGET", b.budgetRatio(), budget(b.costK) + " of " + budget(b.budgetK));
        gauge(c, y - 2 * step, "TIME", b.timeRatio(), f("%.1f of %.0f months", b.months, b.deadlineMonths));
        draw.text(assets.small, "used of available", c, y - 3 * step, Palette.DIM);
    }

    private void gauge(float x, float y, String name, double ratio, String value) {
        draw.text(assets.small, name, x, y, Palette.DIM);
        draw.bar(x + 62, y - 8, 46, 7, ratio);
        draw.text(assets.small, draw.fit(assets.small, value, 90), x + 114, y, ratio > 1 ? Palette.BAD : Palette.TEXT);
    }

    private static String f(String fmt, Object... args) {
        return String.format(Locale.ROOT, fmt, args);
    }

    private void drawCard() {
        float x = 420, y = 355, w = 212;
        draw.rect(x - 6, 292, 1, 66, Palette.WALL);
        if (focus instanceof Item) {
            Part p = ((Item) focus).part;
            draw.text(assets.font, p.name, x, y, Palette.slot(p.slot));
            String who = session.claimed(p.slot.owner) ? p.slot.owner.label + "'s shelf" : "anyone can take it";
            draw.text(assets.small, p.slot.label + " - " + who + (p.heavy ? " - HEAVY" : ""), x, y - 15, Palette.DIM);
            float h = draw.wrap(assets.small, p.description, x, y - 27, w, Palette.TEXT);
            draw.text(assets.small, stats(p), x, y - 31 - h, Palette.ACCENT);
        } else if (focus instanceof Station) {
            Station s = (Station) focus;
            Object cur = s.current(session.design);
            draw.text(assets.font, s.kind.label, x, y, Palette.ACCENT);
            String now = cur != null ? "Now: " + cur : "Nothing chosen yet.";
            draw.text(assets.small, draw.fit(assets.small, now, w), x, y - 15, cur != null ? Palette.TEXT : Palette.WARN);
            String help = s.kind == Station.Kind.ORBIT ? "Paths, night side, coverage."
                    : s.kind == Station.Kind.ROCKET ? "Flight graph and GO / NO-GO review."
                    : "Antennas, data and battery.";
            draw.text(assets.small, help, x, y - 28, Palette.DIM);
            draw.text(assets.small, "Press action to open.", x, y - 41, Palette.ACCENT);
        } else {
            draw.text(assets.font, session.mission.title, x, y, Palette.ACCENT);
            draw.wrap(assets.small, "Target: " + session.mission.targetName + (session.mission.night ? ", at night" : ", in daylight")
                    + ". Walk to a shelf and press your action key to pick up a part.", x, y - 16, w, Palette.TEXT);
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
            s = "On pad: " + on + " of " + n;
            c = Palette.WARN;
        } else {
            s = "All aboard to launch";
        }
        draw.text(assets.small, s, Hangar.PAD_X * S, (Hangar.PAD_Y + Hangar.PAD_H) * S + 14, c);
    }

    private void drawEvent() {
        draw.rect(0, 0, MissionLabGame.UI_W, MissionLabGame.UI_H, Palette.DARK, 0.6f);
        float x = 110, y = 70, w = 420, h = 210;
        draw.rect(x, y, w, h, Palette.PANEL);
        draw.frame(x, y, w, h, 2, Palette.WARN);
        draw.text(assets.small, "PROBLEM DURING TESTING", x + 14, y + h - 12, Palette.WARN);
        draw.text(assets.font, hangar.event.title, x + 14, y + h - 28, Palette.TEXT);
        draw.wrap(assets.small, hangar.event.text, x + 14, y + h - 50, w - 28, Palette.TEXT);

        float bw = 190, bh = 58, by = y + 52;
        option(x + 14, by, bw, bh, "FIX IT", hangar.event.fixLabel, 0);
        option(x + w - 14 - bw, by, bw, bh, "TAKE THE RISK", hangar.event.riskLabel, 1);
        String how = hangar.players.size() == 1 ? "Left or right to choose, action to decide."
                : "Each player: left or right to vote, action to lock in. A tie means fix it.";
        draw.wrap(assets.small, how, x + 14, y + 30, w - 28, Palette.DIM);
    }

    private void option(float x, float y, float w, float h, String title, String text, int value) {
        draw.rect(x, y, w, h, Palette.DARK);
        draw.frame(x, y, w, h, 1, value == 0 ? Palette.GOOD : Palette.BAD);
        draw.text(assets.small, title, x + 8, y + h - 8, value == 0 ? Palette.GOOD : Palette.BAD);
        draw.wrap(assets.small, text, x + 8, y + h - 22, w - 16, Palette.TEXT);
        int i = 0;
        for (Player p : hangar.players) {
            if (hangar.vote[p.index] != value) continue;
            float px = x + 8 + i * 16;
            if (hangar.locked[p.index]) draw.rect(px, y + 5, 10, 10, p.color);
            else draw.frame(px, y + 5, 10, 10, 2, p.color);
            i++;
        }
    }
}
