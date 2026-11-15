package com.missionlab.sim;

import com.missionlab.model.Design;
import com.missionlab.model.Mission;
import com.missionlab.model.MissionRun;
import com.missionlab.model.Part;
import com.missionlab.model.Slot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * La Revisión de Preparación para el Vuelo (Flight Readiness Review), como en NASA: antes de lanzar,
 * el director de vuelo pregunta a cada sistema "¿GO o NO-GO?".
 *
 * - NO-GO: el lanzamiento no se permite (no hay dinero, no llega a tiempo, no cabe, pesa demasiado).
 * - WARN:  se puede lanzar, pero la misión saldrá peor (poca energía, pocos datos, foto borrosa...).
 * - GO:    todo en orden.
 * - PENDING: todavía falta elegir algo para poder comprobarlo.
 *
 * La plataforma de lanzamiento, la consola del cohete y el panel de objetivos del hangar usan esta lista,
 * así el equipo siempre sabe qué falta y cómo arreglarlo.
 */
public final class Readiness {
    public enum Status { GO, WARN, NO_GO, PENDING }

    /** Una pregunta del director de vuelo. */
    public static final class Check {
        public final String label;
        public final Status status;
        /** El dato que justifica la respuesta, por ejemplo "$812k of $1050k". */
        public final String detail;
        /** Qué hacer si no está en GO (o null). */
        public final String fix;

        Check(String label, Status status, String detail, String fix) {
            this.label = label;
            this.status = status;
            this.detail = detail;
            this.fix = fix;
        }

        @Override
        public String toString() {
            return label + " " + status + ": " + detail;
        }
    }

    public final List<Check> checks;
    public final Budgets budgets;
    private final Design design;
    private final Mission mission;

    private Readiness(List<Check> checks, Budgets budgets, Design design, Mission mission) {
        this.checks = Collections.unmodifiableList(checks);
        this.budgets = budgets;
        this.design = design;
        this.mission = mission;
    }

    public static Readiness of(MissionRun run) {
        return build(run.design, run.mission, Budgets.of(run), run.risksTaken());
    }

    public static Readiness of(Design d, Mission m) {
        return build(d, m, Budgets.of(d, m), 0);
    }

    // ------------------------------------------------------------------ resultado

    /** true si el lanzamiento está permitido: nada en NO-GO ni pendiente. */
    public boolean go() {
        for (Check c : checks) if (c.status == Status.NO_GO || c.status == Status.PENDING) return false;
        return true;
    }

    public int count(Status s) {
        int n = 0;
        for (Check c : checks) if (c.status == s) n++;
        return n;
    }

    /** La primera comprobación que impide lanzar, o null. */
    public Check firstBlocker() {
        for (Check c : checks) if (c.status == Status.NO_GO) return c;
        for (Check c : checks) if (c.status == Status.PENDING) return c;
        return null;
    }

    public Check firstWarning() {
        for (Check c : checks) if (c.status == Status.WARN) return c;
        return null;
    }

    /** El siguiente paso para el equipo, en una frase. Lo enseña el panel de objetivos del hangar. */
    public String nextStep() {
        for (Slot s : Slot.values()) {
            if (design.get(s) == null) {
                return "Fill the " + s.label.toUpperCase(Locale.ROOT) + " slot on the blueprint with a part from the " + s.owner.label + "'s shelf.";
            }
        }
        if (design.orbit == null) return "Launch lead: at the orbit map, pick an orbit that passes over " + mission.targetName + ".";
        if (design.route == null) return "Launch lead: at the rocket console, pick how to get to the " + design.orbit.name + ".";
        if (design.station == null) return "Ground control: at the ground station desk, pick who receives the data.";
        Check b = firstBlocker();
        if (b != null) return "NO-GO on " + b.label + ": " + b.fix;
        Check w = firstWarning();
        if (w != null) return "Ready to fly (WARN on " + w.label + "). Improve it, or the whole team stands on the launch pad.";
        return "All systems GO! The whole team stands on the launch pad to launch.";
    }

    // ------------------------------------------------------------------ las preguntas

    private static Readiness build(Design d, Mission m, Budgets b, int risks) {
        List<Check> out = new ArrayList<>();
        Part camera = d.get(Slot.INSTRUMENT);

        // 1. Montaje
        List<String> missing = d.missing();
        int parts = d.parts().size();
        out.add(missing.isEmpty()
                ? new Check("ASSEMBLY", Status.GO, "All 6 parts installed, orbit, route and station chosen", null)
                : new Check("ASSEMBLY", Status.PENDING, parts + " of 6 parts installed. Missing: " + String.join(", ", missing),
                "Fill every slot on the blueprint and use the three consoles."));

        // 2. Presupuesto
        String spent = f("$%.0fk of $%.0fk (%s reserve)", b.costK, b.budgetK, pct(b.budgetReserve()));
        if (b.costK > b.budgetK) {
            out.add(new Check("BUDGET", Status.NO_GO, spent, f("Cut $%.0fk: cheaper parts, a cheaper route or ground network.", b.costK - b.budgetK)));
        } else if (b.budgetReserve() < 0.10) {
            out.add(new Check("BUDGET", Status.WARN, spent, "NASA projects keep at least 10% in reserve for surprises."));
        } else {
            out.add(new Check("BUDGET", Status.GO, spent, null));
        }

        // 3. Calendario
        String time = f("Ready at month %.1f, must be delivered by month %.1f", b.months, b.deadlineMonths);
        String slow = b.slowestPart != null ? " The " + b.slowestPart.name + " takes " + f("%.0f", b.slowestPart.leadMonths) + " months to arrive." : "";
        if (d.route == null && b.slowestPart == null) {
            out.add(new Check("SCHEDULE", Status.PENDING, "No parts yet", "Install parts and pick a route."));
        } else if (b.months > b.deadlineMonths) {
            out.add(new Check("SCHEDULE", Status.NO_GO, time, f("%.1f months late.", b.months - b.deadlineMonths) + slow
                    + " A route with a later hand-over date also helps."));
        } else if (b.scheduleMarginMonths() < 1) {
            out.add(new Check("SCHEDULE", Status.WARN, time, "Less than 1 month to spare: one test problem could make you late." + slow));
        } else {
            out.add(new Check("SCHEDULE", Status.GO, time, null));
        }

        // 4. Estructura: espacio y peso
        Part frame = d.get(Slot.STRUCTURE);
        if (frame == null) {
            out.add(new Check("STRUCTURE", Status.PENDING, f("Parts need %.1fU of space", b.volumeU), "Install a frame."));
        } else {
            String fit = f("%.1fU of %.0fU used, %.1f of %.0f kg", b.volumeU, b.frameVolumeU, b.massKg, b.frameMaxKg);
            if (b.volumeU > b.frameVolumeU) {
                out.add(new Check("STRUCTURE", Status.NO_GO, fit, "The parts do not fit. Use a bigger frame or smaller parts."));
            } else if (b.massKg > b.frameMaxKg) {
                out.add(new Check("STRUCTURE", Status.NO_GO, fit, "Too heavy for this frame (2 kg per U). Use a bigger frame or lighter parts."));
            } else {
                out.add(new Check("STRUCTURE", Status.GO, fit, null));
            }
        }

        // 5. Vehículo de lanzamiento
        if (d.route == null) {
            out.add(new Check("VEHICLE", Status.PENDING, f("Satellite weighs %.1f kg", b.massKg), "Pick a launch route at the rocket console."));
        } else if (d.orbit != null && d.route.orbit() != d.orbit) {
            out.add(new Check("VEHICLE", Status.NO_GO, "The route goes to the " + d.route.orbit().name,
                    "Pick a route again: the orbit changed."));
        } else if (!d.route.carries(b.massKg)) {
            out.add(new Check("VEHICLE", Status.NO_GO, f("%.1f kg, but the route carries up to %.0f kg", b.massKg, d.route.capacityKg()),
                    "Pick a route with bigger vehicles, or make the satellite lighter."));
        } else {
            out.add(new Check("VEHICLE", Status.GO, f("%s: %.1f of %.0f kg, $%.0fk", d.route.label(), b.massKg, d.route.capacityKg(), b.launchCostK), null));
        }

        // 6. Energía
        if (d.get(Slot.POWER) == null) {
            out.add(new Check("POWER", Status.PENDING, f("Parts use %.1f W on average", b.drawW), "Install solar panels."));
        } else {
            String pw = f("Uses %.1f W, panels make %.1f W on average (%s margin)", b.drawW, b.generatedW, pct(b.powerMargin()));
            if (b.generatedW < b.drawW) {
                out.add(new Check("POWER", Status.WARN, pw, "Not enough power: the camera will be switched off part of the time. Bigger panels."));
            } else if (b.powerMargin() < 0.10) {
                out.add(new Check("POWER", Status.WARN, pw, "Keep at least 10% spare power in case a panel underperforms."));
            } else {
                out.add(new Check("POWER", Status.GO, pw, null));
            }
        }

        // 7. Batería
        if (d.get(Slot.BATTERY) == null) {
            out.add(new Check("BATTERY", Status.PENDING, f("Needs %.1f Wh in Earth's shadow", b.eclipseNeedWh), "Install a battery."));
        } else {
            String bt = f("Needs %.1f Wh in %.0f min of shadow, can give %.1f Wh", b.eclipseNeedWh, b.eclipseMin, b.batteryUsableWh);
            if (b.batteryUsableWh < b.eclipseNeedWh) {
                out.add(new Check("BATTERY", Status.WARN, bt, m.night
                        ? "At night the camera runs on the battery. Bigger battery or less power-hungry parts."
                        : "The satellite will run flat in every eclipse. Bigger battery."));
            } else {
                out.add(new Check("BATTERY", Status.GO, bt, null));
            }
        }

        // 8. Enlace de datos
        if (camera == null || d.get(Slot.RADIO) == null || d.station == null) {
            out.add(new Check("DATA LINK", Status.PENDING, "Needs a camera, a radio and a ground station", "Pick all three."));
        } else {
            String dl = f("Camera makes %.1f GB/day, link sends %.1f GB/day (%.0f min of contact)", b.dataGbPerDay, b.downlinkGbPerDay, b.contactMinPerDay);
            if (b.downlinkGbPerDay < b.dataGbPerDay) {
                out.add(new Check("DATA LINK", Status.WARN, dl, f("Only %s of each photo would reach Earth. Faster radio or more ground stations.", pct(b.downlinkShare()))));
            } else {
                out.add(new Check("DATA LINK", Status.GO, dl, null));
            }
        }

        // 9. Cobertura del objetivo
        if (d.orbit == null) {
            out.add(new Check("COVERAGE", Status.PENDING, mission(m), "Pick an orbit."));
        } else if (!d.orbit.passesOver(m.targetLatitude)) {
            out.add(new Check("COVERAGE", Status.WARN, f("Orbit reaches %.0f deg, %s is at %.0f deg", d.orbit.maxLatitude, m.targetName, Math.abs(m.targetLatitude)),
                    "This orbit never flies over the target. Pick one that reaches higher latitudes."));
        } else {
            out.add(new Check("COVERAGE", Status.GO, f("Orbit reaches %.0f deg, %s is at %.0f deg", d.orbit.maxLatitude, m.targetName, Math.abs(m.targetLatitude)), null));
        }

        // 10. Luz: de día cualquier cámara; de noche hace falta infrarrojo térmico
        if (camera == null) {
            out.add(new Check("LIGHTING", Status.PENDING, m.night ? "Target must be seen at night" : "Target is photographed in daylight", "Install an instrument."));
        } else if (m.night && !camera.seesAtNight) {
            out.add(new Check("LIGHTING", Status.WARN, "The " + camera.name + " sees reflected sunlight only",
                    "Fires must be found at night: you need an instrument that sees heat."));
        } else {
            out.add(new Check("LIGHTING", Status.GO, m.night ? "Sees heat at night" : "Daylight photos", null));
        }

        // 11. Resolución
        if (camera == null) {
            out.add(new Check("RESOLUTION", Status.PENDING, f("Scientists need %.0f m per pixel", m.requiredGsdM), "Install an instrument."));
        } else {
            String rs = f("%.0f m per pixel from %.0f km, %.0f m needed", b.gsdM, b.orbitUsed.altitudeKm, m.requiredGsdM);
            if (b.gsdM > m.requiredGsdM) {
                out.add(new Check("RESOLUTION", Status.WARN, rs, "Too blurry for the science: a sharper camera or a lower orbit."));
            } else {
                out.add(new Check("RESOLUTION", Status.GO, rs, null));
            }
        }

        // 12. Vida en órbita
        if (d.orbit == null) {
            out.add(new Check("LIFETIME", Status.PENDING, f("Mission lasts %.0f year%s", m.missionYears, m.missionYears == 1 ? "" : "s"), "Pick an orbit."));
        } else if (d.orbit.lifetimeYears < m.missionYears) {
            out.add(new Check("LIFETIME", Status.WARN, f("Orbit decays in %.1f years, mission needs %.0f", d.orbit.lifetimeYears, m.missionYears),
                    "Air drag will pull it down too soon. Pick a higher orbit."));
        } else {
            out.add(new Check("LIFETIME", Status.GO, f("Orbit lasts %.0f years%s", Math.min(99, d.orbit.lifetimeYears),
                    b.needsDragSail ? "; drag sail added for the 5-year debris rule" : ""), null));
        }

        // 13. Riesgos aceptados en las pruebas
        if (risks > 0) {
            out.add(new Check("RISKS", Status.WARN, risks + " test problem" + (risks == 1 ? "" : "s") + " flying unfixed",
                    "Each unfixed problem may strike in orbit."));
        } else {
            out.add(new Check("RISKS", Status.GO, "No unfixed test problems", null));
        }
        return new Readiness(out, b, d, m);
    }

    private static String mission(Mission m) {
        return f("%s is at %.0f deg %s", m.targetName, Math.abs(m.targetLatitude), m.targetLatitude >= 0 ? "N" : "S");
    }

    static String pct(double v) {
        return Math.round(v * 100) + "%";
    }

    private static String f(String fmt, Object... args) {
        return String.format(Locale.ROOT, fmt, args);
    }
}
