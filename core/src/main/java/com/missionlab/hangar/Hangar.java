package com.missionlab.hangar;

import com.missionlab.data.Catalog;
import com.missionlab.game.Controls;
import com.missionlab.game.Palette;
import com.missionlab.game.Session;
import com.missionlab.model.BuildEvent;
import com.missionlab.model.Part;
import com.missionlab.model.Role;
import com.missionlab.model.Slot;
import com.missionlab.sim.Budgets;
import com.missionlab.sim.Readiness;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Las reglas del hangar (moverse, coger, cargar entre dos, instalar, votar, lanzar), sin dibujar nada.
 * HangarScreen lee el teclado, llama a estos métodos y dibuja el resultado.
 * Para mover muebles o estantes, cambia las coordenadas de este archivo (mundo de 320 x 180 píxeles).
 */
public final class Hangar {
    /** Lo que el hangar le pide a la pantalla: mensajes y sonidos. */
    public interface Listener {
        void toast(String message);

        void sound(String name);
    }

    public static final float FLOOR_X = 4, FLOOR_Y = 14, FLOOR_W = 312, FLOOR_H = 128;
    public static final float REACH = 18;
    public static final float LAUNCH_HOLD = 2.5f;

    public static final float PAD_X = 262, PAD_Y = 24, PAD_W = 52, PAD_H = 80;
    public static final float BP_X = 92, BP_Y = 36, BP_W = 132, BP_H = 74;

    public final List<Player> players = new ArrayList<>();
    public final List<Item> items = new ArrayList<>();
    public final Map<Slot, BlueprintSlot> slots = new EnumMap<>(Slot.class);
    public final List<Station> stations = new ArrayList<>();

    /** Problema abierto ahora mismo (o null). Mientras está abierto nadie se mueve. */
    public BuildEvent event;
    /** Consola abierta ahora mismo (o null) y quién la maneja. Mientras está abierta nadie se mueve. */
    public Station console;
    public Player consoleUser;
    public int[] vote;
    public boolean[] locked;

    public float padTimer;
    public boolean launchRequested;

    private final Session session;
    private final Listener listener;
    private int installs;
    private int eventsTriggered;
    private boolean padWarned;

    public Hangar(Session session, Listener listener) {
        this.session = session;
        this.listener = listener;

        // Estantes de arriba
        shelf(Slot.INSTRUMENT, 12, 124);
        shelf(Slot.POWER, 74, 124);
        shelf(Slot.BATTERY, 136, 124);
        shelf(Slot.POINTING, 178, 124);
        // Estantes de abajo
        shelf(Slot.STRUCTURE, 12, 18);
        shelf(Slot.RADIO, 74, 18);

        // Huecos del plano
        slots.put(Slot.INSTRUMENT, new BlueprintSlot(Slot.INSTRUMENT, 98, 76));
        slots.put(Slot.POINTING, new BlueprintSlot(Slot.POINTING, 143, 76));
        slots.put(Slot.RADIO, new BlueprintSlot(Slot.RADIO, 188, 76));
        slots.put(Slot.POWER, new BlueprintSlot(Slot.POWER, 98, 44));
        slots.put(Slot.BATTERY, new BlueprintSlot(Slot.BATTERY, 143, 44));
        slots.put(Slot.STRUCTURE, new BlueprintSlot(Slot.STRUCTURE, 188, 44));

        stations.add(new Station(Station.Kind.ORBIT, 220, 120, 30, 20));
        stations.add(new Station(Station.Kind.ROCKET, 258, 120, 30, 20));
        stations.add(new Station(Station.Kind.GROUND, 140, 16, 32, 18));

        for (int i = 0; i < session.playerCount; i++) {
            Role role = session.roleOf(i);
            players.add(new Player(i, role, role == null ? Palette.GOOD : Palette.role(role),
                    Controls.PLAYERS[i], 30 + i * 14, 70));
        }

        // Si es un nuevo intento, el diseño anterior ya está montado en el plano.
        for (Slot s : Slot.values()) {
            Part p = session.design.get(s);
            if (p == null) continue;
            for (Item it : items) {
                if (it.part == p) place(it, slots.get(s));
            }
        }
    }

    private void shelf(Slot slot, float x, float y) {
        float step = Item.SIZE + 6;
        List<Part> parts = Catalog.partsFor(slot);
        for (int i = 0; i < parts.size(); i++) items.add(new Item(parts.get(i), x + i * step, y));
    }

    // ------------------------------------------------------------------ movimiento

    public boolean needsTwo(Item it) {
        return it.part.heavy && session.twoPersonLifts();
    }

    /** true si el jugador sujeta una pieza pesada y espera a que alguien le ayude. */
    public boolean waitingForHelp(Player p) {
        return p.holding != null && needsTwo(p.holding) && p.holding.carriers.size() < 2;
    }

    public void move(Player p, float dx, float dy, float delta) {
        if (event != null || console != null || waitingForHelp(p)) return;
        if (dx != 0 && dy != 0) {
            dx *= 0.7071f;
            dy *= 0.7071f;
        }
        float speed = Player.SPEED * (p.holding != null && needsTwo(p.holding) ? 0.7f : 1f);
        p.x = clamp(p.x + dx * speed * delta, FLOOR_X, FLOOR_X + FLOOR_W - Player.W);
        p.y = clamp(p.y + dy * speed * delta, FLOOR_Y, FLOOR_Y + FLOOR_H - Player.H);
        if (dx != 0) p.facingLeft = dx < 0;
        if (dx != 0 || dy != 0) p.walkTime += delta;
    }

    /** Las piezas cargadas siguen a quien las lleva. */
    public void updateCarried() {
        for (Item it : items) {
            if (it.carriers.isEmpty()) continue;
            if (!needsTwo(it)) {
                Player p = it.carriers.get(0);
                it.x = p.cx() - Item.SIZE / 2;
                it.y = p.y + Player.H - 1;
            } else if (it.carriers.size() >= 2) {
                Player a = it.carriers.get(0), b = it.carriers.get(1);
                if (a.distanceTo(b.cx(), b.cy()) > 34) {
                    it.releaseAll();
                    listener.sound("deny");
                    listener.toast("You dropped the " + it.part.name + ". Stay close when carrying together.");
                    continue;
                }
                it.x = (a.cx() + b.cx()) / 2 - Item.SIZE / 2;
                it.y = (a.y + b.y) / 2 + Player.H - 1;
            }
        }
    }

    // ------------------------------------------------------------------ acción

    /** El jugador pulsó su tecla de acción. */
    public void act(Player p) {
        if (event != null) return;
        if (console != null) {
            if (p == consoleUser) closeConsole();
            return;
        }
        Item held = p.holding;
        if (held != null) {
            if (waitingForHelp(p)) {
                held.carriers.remove(p);
                p.holding = null;
                listener.sound("click");
                return;
            }
            BlueprintSlot target = nearestSlot(held);
            if (target != null) {
                if (target.slot != held.part.slot) {
                    listener.sound("deny");
                    listener.toast("That slot is for the " + target.slot.label.toLowerCase() + ". The "
                            + held.part.name + " goes in " + held.part.slot.label.toLowerCase() + ".");
                    return;
                }
                install(held, target);
                return;
            }
            boolean nearHome = dist(held.cx(), held.cy(), held.homeX + Item.SIZE / 2, held.homeY + Item.SIZE / 2) < REACH;
            held.releaseAll();
            if (nearHome) held.sendHome();
            listener.sound("place");
            return;
        }
        Item it = itemNear(p);
        if (it != null) {
            grab(p, it);
            return;
        }
        Station st = stationNear(p);
        if (st != null) use(p, st);
    }

    private void grab(Player p, Item it) {
        if (it.carriers.size() == 1 && needsTwo(it)) {
            it.carriers.add(p);
            p.holding = it;
            listener.sound("pickup");
            listener.toast("Lifted together! Carry the " + it.part.name + " to the blueprint.");
            return;
        }
        if (!it.carriers.isEmpty()) return;
        Role owner = it.part.slot.owner;
        if (!session.canUse(p.index, owner)) {
            listener.sound("deny");
            listener.toast("Only the " + owner.label + " can take parts from this shelf. Anyone can help lift heavy ones.");
            return;
        }
        if (it.installedIn != null) uninstall(it);
        it.carriers.add(p);
        p.holding = it;
        listener.sound("pickup");
        if (needsTwo(it)) listener.toast("Heavy! Another player must press their action key next to it to help lift.");
    }

    private void install(Item it, BlueprintSlot s) {
        place(it, s);
        listener.sound("place");
        installs++;
        if ((installs == 2 || installs == 5) && eventsTriggered < 2) startEvent();
    }

    private void place(Item it, BlueprintSlot s) {
        if (s.installed != null && s.installed != it) {
            Item old = s.installed;
            old.installedIn = null;
            old.sendHome();
        }
        it.releaseAll();
        session.design.install(it.part);
        s.installed = it;
        it.installedIn = s;
        it.x = s.cx() - Item.SIZE / 2;
        it.y = s.cy() - Item.SIZE / 2;
    }

    private void uninstall(Item it) {
        BlueprintSlot s = it.installedIn;
        session.design.remove(s.slot);
        s.installed = null;
        it.installedIn = null;
    }

    private void use(Player p, Station st) {
        if (!session.canUse(p.index, st.kind.owner)) {
            listener.sound("deny");
            listener.toast("The " + st.kind.label.toLowerCase() + " belongs to the " + st.kind.owner.label + ".");
            return;
        }
        console = st;
        consoleUser = p;
        listener.sound("click");
    }

    // ------------------------------------------------------------------ consolas

    /** Izquierda/derecha dentro de una consola abierta: cambia la opción. Solo quien la abrió. */
    public void consoleInput(Player p, int dir) {
        if (console == null || p != consoleUser || dir == 0) return;
        double mass = Budgets.of(session.design, session.mission).massKg;
        String name = console.cycle(session.design, mass, dir);
        if (name == null) {
            listener.sound("deny");
            listener.toast("Pick an orbit on the orbit map first. The rocket console plans the route to it.");
            return;
        }
        listener.sound("click");
    }

    public void closeConsole() {
        if (console == null) return;
        Object chosen = console.current(session.design);
        if (chosen != null) listener.toast(console.kind.label + ": " + chosen + ".");
        console = null;
        consoleUser = null;
        listener.sound("place");
    }

    /** La revisión de vuelo del diseño actual (con los costes y meses de los problemas ya decididos). */
    public Readiness readiness() {
        return Readiness.of(session.run);
    }

    // ------------------------------------------------------------------ qué hay cerca

    public Item itemNear(Player p) {
        Item best = null;
        float bestD = REACH;
        for (Item it : items) {
            boolean free = it.carriers.isEmpty() || (needsTwo(it) && it.carriers.size() == 1 && !it.carriers.contains(p));
            if (!free) continue;
            float d = p.distanceTo(it.cx(), it.cy());
            if (d < bestD) {
                bestD = d;
                best = it;
            }
        }
        return best;
    }

    public Station stationNear(Player p) {
        Station best = null;
        float bestD = REACH + 6;
        for (Station s : stations) {
            float d = p.distanceTo(s.cx(), s.cy());
            if (d < bestD) {
                bestD = d;
                best = s;
            }
        }
        return best;
    }

    public BlueprintSlot nearestSlot(Item it) {
        BlueprintSlot best = null;
        float bestD = 22;
        for (BlueprintSlot s : slots.values()) {
            float d = dist(it.cx(), it.cy(), s.cx(), s.cy());
            if (d < bestD) {
                bestD = d;
                best = s;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ problemas y votos

    private void startEvent() {
        BuildEvent e = session.nextEvent();
        if (e == null) return;
        event = e;
        eventsTriggered++;
        vote = new int[players.size()];
        locked = new boolean[players.size()];
        listener.sound("alert");
    }

    /** dir = -1 elige ARREGLAR, 1 elige ARRIESGAR. */
    public void vote(Player p, int dir) {
        if (event == null || locked[p.index] || dir == 0) return;
        vote[p.index] = dir < 0 ? 0 : 1;
        listener.sound("click");
    }

    public void lockVote(Player p) {
        if (event == null) return;
        locked[p.index] = true;
        for (boolean l : locked) if (!l) return;
        int fix = 0, risk = 0;
        for (int v : vote) {
            if (v == 0) fix++;
            else risk++;
        }
        boolean fixed = fix >= risk; // empate = arreglar
        session.run.decide(event, fixed);
        listener.toast(fixed ? "Decision: " + event.fixLabel : "Decision: " + event.riskLabel);
        listener.sound(fixed ? "place" : "deny");
        event = null;
    }

    // ------------------------------------------------------------------ lanzamiento

    public boolean onPad(Player p) {
        return p.cx() > PAD_X && p.cx() < PAD_X + PAD_W && p.cy() > PAD_Y && p.cy() < PAD_Y + PAD_H;
    }

    public int playersOnPad() {
        int n = 0;
        for (Player p : players) if (onPad(p)) n++;
        return n;
    }

    /** El lanzamiento empieza cuando TODO el equipo se queda en la plataforma unos segundos. */
    public void updatePad(float delta) {
        if (event != null || console != null || playersOnPad() < players.size()) {
            padTimer = 0;
            padWarned = false;
            return;
        }
        if (!session.design.isComplete()) {
            if (!padWarned) {
                listener.sound("deny");
                listener.toast("Not ready to launch. Missing: " + String.join(", ", session.design.missing()) + ".");
                padWarned = true;
            }
            padTimer = 0;
            return;
        }
        // Revisión de vuelo: con un solo NO-GO, el director de vuelo no deja lanzar.
        Readiness review = readiness();
        if (!review.go()) {
            if (!padWarned) {
                Readiness.Check b = review.firstBlocker();
                listener.sound("deny");
                listener.toast("Flight review says NO-GO on " + b.label + ". " + b.fix + " Check the rocket console.");
                padWarned = true;
            }
            padTimer = 0;
            return;
        }
        padTimer += delta;
        if (padTimer >= LAUNCH_HOLD) launchRequested = true;
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static float dist(float ax, float ay, float bx, float by) {
        float dx = ax - bx, dy = ay - by;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
}
