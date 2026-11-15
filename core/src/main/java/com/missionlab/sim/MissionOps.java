package com.missionlab.sim;

import com.missionlab.ds.LinkedQueue;
import com.missionlab.model.Design;
import com.missionlab.model.GroundStation;
import com.missionlab.model.Mission;
import com.missionlab.model.Part;
import com.missionlab.model.Slot;
import com.missionlab.sim.OrbitMechanics.State;
import com.missionlab.sim.OrbitMechanics.Track;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Mission Control: un día de operaciones en órbita, paso a paso, sin gráficos (así se puede probar).
 *
 * El satélite sigue su traza de verdad sobre la Tierra que gira. El equipo manda dos órdenes:
 *   IMAGE    fotografiar la zona en la próxima pasada útil (de noche o de día, según el encargo); la cámara
 *            barre la zona durante toda la pasada, así que una orden que llega tarde cubre solo una parte;
 *   DOWNLINK bajar los datos guardados en el próximo contacto con una estación.
 *
 * Las órdenes entran en una COLA FIFO (la secuencia de órdenes guardada a bordo, como en los satélites reales):
 * el satélite ejecuta la primera cuando se dan sus condiciones; las demás esperan su turno.
 * La batería se carga al sol y se gasta en la sombra; si baja del límite seguro, el satélite entra en
 * modo seguro y no obedece hasta recargarse.
 *
 * Para puntuar de forma justa, un "piloto automático perfecto" juega el mismo día con el mismo satélite:
 * la nota de las operaciones es lo que bajó el equipo comparado con lo que bajó el piloto perfecto.
 */
public final class MissionOps {
    public enum Command { IMAGE, DOWNLINK }

    /** Una ventana de oportunidad: pasada útil sobre el objetivo o contacto con una estación. */
    public static final class Window {
        public enum Type { TARGET, CONTACT }

        public final Type type;
        public final double start, end;
        /** Nombre de la antena (solo contactos). */
        public final String site;

        Window(Type type, double start, double end, String site) {
            this.type = type;
            this.start = start;
            this.end = end;
            this.site = site;
        }

        public boolean contains(double t) {
            return t >= start && t < end;
        }
    }

    /** Horas de operaciones que se juegan. */
    public static final double DURATION_S = 30 * 3600;
    public static final int MAX_QUEUE = 6;
    private static final double STEP_S = 10;

    private final Mission mission;
    private final Track track;
    private final GroundStation station;
    private final double coverageRadius;
    /** Tiempo de la traza en el que empiezan las operaciones (un poco antes de la primera pasada útil). */
    private final double epoch;
    private final List<Window> windows;

    private final double busW, pointingW, cameraW, radioW, panelsW;
    private final double capacityWh, floorWh;
    private final double imageGb, rateGbPerS;

    private final LinkedQueue<Command> uplink = new LinkedQueue<>();
    private final List<String> log = new ArrayList<>();

    private double t;
    private State sat;
    private double socWh;
    private boolean safeMode, imaging, downlinking;
    private double coverage, storedGb, receivedGb, imagingThisPassS;
    private Window currentTarget, currentContact;

    /**
     * @param powerFactor 1 normal, 0.5 si un panel se atascó al desplegarse
     */
    public MissionOps(Design d, Mission m, double powerFactor) {
        this.mission = m;
        this.station = d.station;
        this.track = new Track(d.orbit, m.sunDeclinationDeg, 0);
        this.coverageRadius = OrbitMechanics.coverageRadiusDeg(d.orbit);
        Part cam = d.get(Slot.INSTRUMENT), radio = d.get(Slot.RADIO), battery = d.get(Slot.BATTERY);
        this.busW = com.missionlab.data.Catalog.BUS_POWER_W;
        this.pointingW = d.get(Slot.POINTING).powerW;
        this.cameraW = cam.powerW;
        this.radioW = radio.powerW;
        this.panelsW = d.get(Slot.POWER).generatesW * powerFactor;
        this.capacityWh = battery.batteryWh;
        this.floorWh = capacityWh * (1 - com.missionlab.data.Catalog.BATTERY_DEPTH_OF_DISCHARGE);
        this.imageGb = cam.dataGbPerDay;
        this.rateGbPerS = radio.downlinkMbps / 8000.0;
        this.epoch = findEpoch();
        this.windows = Collections.unmodifiableList(scanWindows());
        this.socWh = capacityWh;
        this.sat = track.at(epoch);
        say("Mission Control online. " + (targetPasses() == 0
                ? "This orbit never gives a useful pass over " + m.targetName + "."
                : "Queue IMAGE before the pass over " + m.targetName + ", then DOWNLINK at a ground station."));
    }

    // ------------------------------------------------------------------ órdenes

    /** Mete una orden en la cola. Devuelve false si la cola está llena. */
    public boolean send(Command c) {
        if (uplink.size() >= MAX_QUEUE) {
            say("Uplink queue full.");
            return false;
        }
        uplink.enqueue(c);
        say("Queued " + c + (uplink.size() > 1 ? " (" + uplink.size() + " in line)" : "") + ".");
        return true;
    }

    public LinkedQueue<Command> queue() {
        return new LinkedQueue<>(uplink);
    }

    // ------------------------------------------------------------------ avanzar el tiempo

    /** Avanza la simulación dtSeconds segundos de misión. */
    public void step(double dtSeconds) {
        double left = dtSeconds;
        while (left > 0 && !finished()) {
            double dt = Math.min(STEP_S, left);
            tick(dt);
            left -= dt;
        }
    }

    private void tick(double dt) {
        t += dt;
        sat = track.at(epoch + t);
        Window target = windowAt(Window.Type.TARGET);
        Window contact = windowAt(Window.Type.CONTACT);

        // Entradas y salidas de las ventanas
        if (target != currentTarget) {
            if (currentTarget != null && imaging) {
                imaging = false;
                say(String.format(Locale.ROOT, "Pass over %s ended. Target covered: %d%%.", mission.targetName, Math.round(coverage * 100)));
            }
            if (target != null) say("Pass over " + mission.targetName + " begins!");
            currentTarget = target;
            imagingThisPassS = 0;
        }
        if (contact != currentContact) {
            if (currentContact != null && downlinking) say("Lost signal from " + currentContact.site + ".");
            downlinking = false;
            if (contact != null) say("Signal acquired: " + contact.site + ".");
            currentContact = contact;
        }

        runQueue();

        // Energía
        boolean sending = downlinking && storedGb > 0;
        double gen = sat.sunlit ? panelsW : 0;
        double load = busW + pointingW + (imaging ? cameraW : 0) + (sending ? radioW : 0);
        socWh = Math.min(capacityWh, socWh + (gen - load) * dt / 3600.0);
        if (!safeMode && socWh <= floorWh) {
            safeMode = true;
            imaging = false;
            downlinking = false;
            say("SAFE MODE: battery at its safe limit. Waiting for sunlight.");
        } else if (safeMode && socWh >= floorWh + 0.5 * (capacityWh - floorWh)) {
            safeMode = false;
            say("Battery recovered. Back to normal operations.");
        }
        socWh = Math.max(0, socWh);

        // Fotos y datos
        if (imaging) {
            // La cámara barre la zona durante toda la pasada: cubrirla entera lleva la pasada entera.
            double passS = Math.max(60, currentTarget.end - currentTarget.start);
            double add = Math.min(dt / passS, 1 - coverage);
            coverage += add;
            storedGb += add * imageGb;
            imagingThisPassS += dt;
            if (coverage >= 0.999) {
                coverage = 1;
                imaging = false;
                say("Target fully covered! " + String.format(Locale.ROOT, "%.1f GB stored on board.", storedGb));
            }
        }
        if (downlinking && storedGb > 0) {
            double sent = Math.min(storedGb, rateGbPerS * dt);
            storedGb -= sent;
            receivedGb += sent;
            if (storedGb <= 1e-9) {
                storedGb = 0;
                say(String.format(Locale.ROOT, "Downlink complete: %d%% of the picture is on the ground.", Math.round(received() * 100)));
            }
        }
    }

    /** Ejecuta la primera orden de la cola si se dan sus condiciones (FIFO: las demás esperan). */
    private void runQueue() {
        while (!uplink.isEmpty() && !safeMode) {
            Command head = uplink.peek();
            if (head == Command.IMAGE) {
                if (coverage >= 0.999) {
                    uplink.dequeue();
                    say("IMAGE skipped: the target is already covered.");
                    continue;
                }
                if (currentTarget == null || imaging) return;
                uplink.dequeue();
                imaging = true;
                say("IMAGE: camera on over " + mission.targetName + ".");
            } else {
                if (currentContact == null || downlinking) return;
                uplink.dequeue();
                downlinking = true;
                say(storedGb > 0 ? "DOWNLINK: sending to " + currentContact.site + "." : "DOWNLINK: nothing stored yet, the pass is wasted.");
            }
        }
    }

    // ------------------------------------------------------------------ piloto automático perfecto

    /**
     * Lo que bajaría un control perfecto con este satélite en el mismo día: fotografiar en cuanto se puede
     * y transmitir en cada contacto con datos guardados.
     */
    public static double autopilotReceived(Design d, Mission m, double powerFactor) {
        MissionOps ops = new MissionOps(d, m, powerFactor);
        while (!ops.finished()) {
            if (ops.uplink.isEmpty()) {
                if (ops.currentTarget != null && ops.coverage < 0.999 && !ops.imaging) ops.uplink.enqueue(Command.IMAGE);
                else if (ops.currentContact != null && !ops.downlinking && ops.storedGb > 0) ops.uplink.enqueue(Command.DOWNLINK);
            }
            ops.step(STEP_S);
        }
        return ops.received();
    }

    /**
     * La nota de las operaciones (0..1): lo que bajó el equipo comparado con el piloto perfecto.
     * Si ni el piloto perfecto podía bajar nada (por ejemplo, la órbita no pasa por el objetivo), vale 1.
     */
    public double shareOf(double autopilotReceived) {
        if (autopilotReceived <= 1e-6) return 1;
        return Math.min(1, received() / autopilotReceived);
    }

    // ------------------------------------------------------------------ consultas para la pantalla

    public boolean finished() {
        return t >= DURATION_S || (coverage >= 0.999 && storedGb <= 1e-9 && receivedGb > 0);
    }

    /** Termina las operaciones ahora (el equipo pulsa "terminar"). */
    public void endNow() {
        if (!finished()) say("Operations ended by Mission Control.");
        t = Math.max(t, DURATION_S);
    }

    /** Parte de la foto recibida en tierra (0..1). */
    public double received() {
        return imageGb > 0 ? Math.min(1, receivedGb / imageGb) : 0;
    }

    public double coverage() { return coverage; }
    public double storedGb() { return storedGb; }
    public double receivedGb() { return receivedGb; }
    public double imageGb() { return imageGb; }
    public double elapsedS() { return t; }
    public double trackTime() { return epoch + t; }
    public State satellite() { return sat; }
    public boolean safeMode() { return safeMode; }
    public boolean imaging() { return imaging; }
    public boolean downlinking() { return downlinking && storedGb > 0; }
    public Window currentTarget() { return currentTarget; }
    public Window currentContact() { return currentContact; }
    public double batteryFraction() { return capacityWh > 0 ? socWh / capacityWh : 0; }
    /** Por debajo de esta carga entra en modo seguro (0..1). */
    public double batteryFloor() { return capacityWh > 0 ? floorWh / capacityWh : 0; }
    public double coverageRadiusDeg() { return coverageRadius; }
    public Track track() { return track; }
    public GroundStation station() { return station; }
    public List<Window> windows() { return windows; }
    public List<String> log() { return Collections.unmodifiableList(log); }

    public int targetPasses() {
        int n = 0;
        for (Window w : windows) if (w.type == Window.Type.TARGET) n++;
        return n;
    }

    /** La próxima ventana de este tipo que todavía no ha terminado, o null. */
    public Window next(Window.Type type) {
        for (Window w : windows) if (w.type == type && w.end > t) return w;
        return null;
    }

    /** Potencia que se gasta ahora mismo (para la telemetría). */
    public double loadW() {
        return busW + pointingW + (imaging ? cameraW : 0) + (downlinking() ? radioW : 0);
    }

    public double generationW() {
        return sat.sunlit ? panelsW : 0;
    }

    // ------------------------------------------------------------------ ventanas

    private static final java.util.Map<String, Integer> PASSES_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    /** Pasadas útiles sobre el objetivo en un día (con la luz que pide el encargo). Para el mapa de órbitas. */
    public static int usefulPassesPerDay(com.missionlab.model.Orbit orbit, Mission m) {
        return PASSES_CACHE.computeIfAbsent(orbit.id + "|" + m.id, k -> {
            Track tr = new Track(orbit, m.sunDeclinationDeg, 0);
            int n = 0;
            boolean in = false;
            for (double s = 0; s < 86400; s += 20) {
                State st = tr.at(s);
                boolean useful = m.inRegion(st.lat, st.lon) && (m.night
                        ? OrbitMechanics.sunElevationDeg(st.lat, st.lon, s, m.sunDeclinationDeg) < -6
                        : OrbitMechanics.sunElevationDeg(st.lat, st.lon, s, m.sunDeclinationDeg) > 5);
                if (useful && !in) n++;
                in = useful;
            }
            return n;
        });
    }

    private Window windowAt(Window.Type type) {
        for (Window w : windows) if (w.type == type && w.contains(t)) return w;
        return null;
    }

    /** true si desde aquí la foto vale: dentro de la zona y con la luz que pide el encargo. */
    private boolean usefulAt(State s, double trackT) {
        if (!mission.inRegion(s.lat, s.lon)) return false;
        double sun = OrbitMechanics.sunElevationDeg(s.lat, s.lon, trackT, mission.sunDeclinationDeg);
        return mission.night ? sun < -6 : sun > 5;
    }

    /** Empieza 20 minutos antes de la primera pasada útil, para que dé tiempo a mandar la orden. */
    private double findEpoch() {
        for (double s = 0; s < 2 * 86400; s += 20) {
            if (usefulAt(track.at(s), s)) return Math.max(0, s - 20 * 60);
        }
        return 0;
    }

    private List<Window> scanWindows() {
        List<Window> out = new ArrayList<>();
        double tStart = -1, cStart = -1;
        String cSite = null;
        double step = 20;
        for (double s = 0; s <= DURATION_S; s += step) {
            State st = track.at(epoch + s);
            boolean useful = usefulAt(st, epoch + s);
            GroundStation.Site site = OrbitMechanics.siteInView(station, st.lat, st.lon, coverageRadius);
            if (useful && tStart < 0) tStart = s;
            if (!useful && tStart >= 0) {
                out.add(new Window(Window.Type.TARGET, tStart, s, null));
                tStart = -1;
            }
            if (site != null && cStart < 0) {
                cStart = s;
                cSite = site.name;
            }
            if (site == null && cStart >= 0) {
                out.add(new Window(Window.Type.CONTACT, cStart, s, cSite));
                cStart = -1;
            }
        }
        if (tStart >= 0) out.add(new Window(Window.Type.TARGET, tStart, DURATION_S, null));
        if (cStart >= 0) out.add(new Window(Window.Type.CONTACT, cStart, DURATION_S, cSite));
        out.sort((a, b) -> Double.compare(a.start, b.start));
        return out;
    }

    private void say(String line) {
        int h = (int) (t / 3600), mi = (int) ((t % 3600) / 60);
        log.add(String.format(Locale.ROOT, "%02d:%02d %s", h, mi, line));
    }
}
