package com.missionlab.sim;

import com.missionlab.data.Catalog;
import com.missionlab.ds.DirectedWeightedGraph.Edge;
import com.missionlab.ds.LinkedQueue;
import com.missionlab.model.Design;
import com.missionlab.model.EventEffect;
import com.missionlab.model.Mission;
import com.missionlab.model.MissionRun;
import com.missionlab.model.Orbit;
import com.missionlab.model.Part;
import com.missionlab.model.Role;
import com.missionlab.model.Route;
import com.missionlab.model.Slot;
import com.missionlab.model.Transfer;
import com.missionlab.model.Waypoint;
import com.missionlab.sim.FlightEvent.Kind;
import com.missionlab.sim.FlightEvent.Status;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SplittableRandom;

/**
 * The heart of the game: turns a design into a result.
 * It knows nothing about graphics, so it can be tested with plain unit tests.
 * The same seed always gives the same result.
 *
 * It assumes Mission Control performs perfectly; what the team actually achieves in
 * operations is applied afterwards with SimResult.withOperations().
 *
 * It uses both of the game's data structures:
 * - it walks the chosen route through the flight GRAPH leg by leg (and asks Dijkstra whether there was a cheaper one);
 * - while simulating, it enqueues each event in a QUEUE in chronological order: the flight timeline.
 */
public final class Simulator {
    private Simulator() {}

    /** Hours between first-orbit events. Simplified for teaching. */
    private static final double PANELS_AFTER_H = 0.5;
    private static final double CONTACT_AFTER_H = 1.5;

    public static SimResult run(MissionRun run, long seed) {
        Mission m = run.mission;
        Design d = run.design;
        Budgets b = Budgets.of(run);
        // SplittableRandom gives varied results even with consecutive seeds (0, 1, 2...).
        SplittableRandom rng = new SplittableRandom(seed);
        List<Finding> findings = new ArrayList<>();
        List<String> struck = new ArrayList<>();
        Timeline timeline = new Timeline();

        // ---------------------------------------------------- before launch: what the agency would cancel
        if (!d.isComplete()) {
            return notLaunched(b, "The satellite was not finished: missing " + String.join(", ", d.missing()) + ".",
                    "Fill every slot on the blueprint and pick an orbit, a launch route and a ground station.");
        }
        Route route = d.route;
        if (route.orbit() != d.orbit) {
            return notLaunched(b, "The launch route ends at the " + route.orbit().name + ", but the orbit map says " + d.orbit.name + ".",
                    "After changing the orbit, pick a route again at the rocket console.");
        }
        if (b.costK > m.budgetK) {
            return notLaunched(b, f("The mission cost $%.0fk but the budget was $%.0fk. The agency cancelled the launch.",
                            b.costK, m.budgetK),
                    "Cheaper parts, a cheaper route or a smaller ground network would cut the cost.");
        }
        if (b.months > b.deadlineMonths) {
            return notLaunched(b, f("The satellite was ready at month %.1f, but it had to be handed over by month %.1f. The rocket left without it.",
                            b.months, b.deadlineMonths),
                    "Parts are bought at the same time, so the slowest one sets the date. Fixing problems also costs time.");
        }
        addRouteNote(b, d, findings);

        // ---------------------------------------------------- stage 1: liftoff (first leg of the route)
        Transfer rocket = route.launchVehicle();
        if (b.volumeU > b.frameVolumeU || b.massKg > b.frameMaxKg) {
            findings.add(Finding.problem(f("The %s holds %.0fU and %.0f kg, but the satellite needed %.1fU and weighed %.1f kg. Launch vibration broke it apart.",
                    d.get(Slot.STRUCTURE).name, b.frameVolumeU, b.frameMaxKg, b.volumeU, b.massKg),
                    "Use a bigger frame, or smaller and lighter parts.", 100));
            timeline.add(0, Kind.LIFTOFF, Status.FAILED, "Liftoff: the frame broke apart", null);
            return failed(b, 0, Stage.LIFTOFF, findings, struck, run, timeline);
        }
        if (!rocket.carries(b.massKg)) {
            findings.add(Finding.problem(f("The satellite weighed %.1f kg but the %s can carry only %.0f kg. It never reached space.",
                    b.massKg, rocket.name, rocket.maxKg),
                    "Lighter parts, or a route with bigger vehicles.", 100));
            timeline.add(0, Kind.LIFTOFF, Status.FAILED, "Liftoff: too heavy for the " + rocket.name, null);
            return failed(b, 0, Stage.LIFTOFF, findings, struck, run, timeline);
        }
        timeline.add(0, Kind.LIFTOFF, Status.OK, "Liftoff: " + rocket.name, null);

        // ---------------------------------------------------- stage 2: walk the route's edges up to the orbit
        List<Edge<Waypoint, Transfer>> legs = route.legs();
        for (int i = 0; i < legs.size(); i++) {
            Transfer vehicle = legs.get(i).weight;
            Waypoint here = legs.get(i).from.value;
            Waypoint next = legs.get(i).to.value;
            if (i > 0 && !vehicle.carries(b.massKg)) {
                findings.add(Finding.problem(f("The %s can move only %.0f kg, but the satellite weighed %.1f kg. It was left behind at %s.",
                        vehicle.name, vehicle.maxKg, b.massKg, here.name),
                        "Every vehicle on the route must carry the satellite. Check the route at the rocket console.", 80));
                timeline.add(0, Kind.ORBIT, Status.FAILED, "Stuck at " + here.name + ": too heavy for the " + vehicle.name, here.orbit);
                return failed(b, 1, Stage.ORBIT, findings, struck, run, timeline);
            }
            if (i == legs.size() - 1) {
                timeline.add(vehicle.hours, Kind.ORBIT, Status.OK, "Orbit reached: " + next.name, next.orbit);
            } else {
                Transfer following = legs.get(i + 1).weight;
                timeline.add(vehicle.hours, Kind.TRANSFER, Status.OK,
                        "At " + next.name + ": the " + following.name + " takes over", next.orbit);
            }
        }

        // ---------------------------------------------------- in orbit, the panels deploy
        Part panels = d.get(Slot.POWER);
        boolean jam = panels.deployable && rng.nextDouble() > panels.reliability;
        if (strikes(run, EventEffect.PANEL_JAM, rng)) {
            jam = true;
            struck.add("The stiff hinge you flew without fixing jammed.");
        }
        double generated = b.generatedW * (jam ? 0.5 : 1.0);
        if (jam) {
            findings.add(Finding.problem("One solar panel jammed when it tried to open. Only half the power arrived.",
                    "Deployable panels give more power but their hinges can fail. Body-mounted cells never jam.", 0));
        }
        timeline.add(PANELS_AFTER_H, Kind.PANELS, jam ? Status.WARN : Status.OK,
                jam ? "A solar panel jammed: half power" : panels.deployable ? "Solar panels deployed" : "Body-mounted cells charging", null);

        // ---------------------------------------------------- stage 3: first contact
        if (strikes(run, EventEffect.NO_CONTACT, rng)) {
            struck.add("The loose antenna broke.");
            findings.add(Finding.problem("The antenna that came loose in testing broke during launch. Nobody ever heard from the satellite.",
                    "Fixing a problem costs budget now but can save the whole mission.", 60));
            timeline.add(CONTACT_AFTER_H, Kind.CONTACT, Status.FAILED, "Silence: the antenna broke", null);
            return failed(b, 2, Stage.CONTACT, findings, struck, run, timeline);
        }
        timeline.add(CONTACT_AFTER_H, Kind.CONTACT, Status.OK, "First radio contact. Hand over to Mission Control", null);

        // ---------------------------------------------------- stage 4: the science
        Part camera = d.get(Slot.INSTRUMENT);
        Part pointing = d.get(Slot.POINTING);
        Orbit orbit = d.orbit;
        double completeness = 1;

        // Power: if the panels fall short, the camera is switched off part of the time.
        double duty = generated >= b.drawW ? 1.0 : generated / b.drawW;
        if (duty < 1) {
            completeness *= duty;
            findings.add(Finding.problem(f("Your parts needed %.1f W on average but the panels gave only %.1f W. The camera had to switch off %d%% of the time.",
                    b.drawW, generated, Math.round((1 - duty) * 100)),
                    "Bigger solar panels, or parts that use less power.", Score.SCIENCE_MAX * (1 - duty)));
        }

        boolean lit = !m.night || camera.seesAtNight;
        if (!lit) {
            findings.add(Finding.problem("A normal camera sees reflected sunlight. At night the " + m.targetName + " was just darkness.",
                    "To find fires at night you need an instrument that sees heat (thermal infrared), like NASA's VIIRS.", Score.SCIENCE_MAX));
        }

        // Battery: in the Earth's shadow everything runs on the battery.
        if (b.batteryUsableWh < b.eclipseNeedWh) {
            double share = Math.max(0.2, b.batteryUsableWh / b.eclipseNeedWh);
            if (!m.night) share = Math.max(share, 0.7);
            completeness *= share;
            findings.add(Finding.problem(f("In Earth's shadow the satellite needed %.1f Wh but the battery could safely give only %.1f Wh. It ran flat and lost %d%% of the photos.",
                    b.eclipseNeedWh, b.batteryUsableWh, Math.round((1 - share) * 100)),
                    m.night ? "Night imaging runs on the battery: a bigger battery, or a camera and pointing that use less power."
                            : "A bigger battery keeps the satellite running through every eclipse.",
                    Score.SCIENCE_MAX * (1 - share)));
        }

        boolean onTarget = orbit.passesOver(m.targetLatitude);
        if (!onTarget) {
            findings.add(Finding.problem(f("Your orbit never goes past %.0f degrees, but %s is at %.0f degrees. The camera photographed the ocean instead.",
                    orbit.maxLatitude, m.targetName, Math.abs(m.targetLatitude)),
                    "Pick an orbit that passes over the target. Sun-synchronous orbits see the whole planet.", Score.SCIENCE_MAX));
        }

        // Pointing: a poorly pointed narrow-field camera misses the target.
        double pointingFactor = Math.max(0.3, Math.min(1.0, pointing.stability / camera.pointingNeed));
        if (pointingFactor < 1) {
            completeness *= pointingFactor;
            findings.add(Finding.problem(f("The %s sees a narrow strip, and the %s could not aim it precisely. %d%% of the shots missed the target.",
                    camera.name.toLowerCase(Locale.ROOT), pointing.name.toLowerCase(Locale.ROOT), Math.round((1 - pointingFactor) * 100)),
                    "Narrow cameras need reaction wheels and a star tracker to point.", Score.SCIENCE_MAX * (1 - pointingFactor)));
        }

        // Resolution: depends on the camera and the altitude.
        double gsd = b.gsdM;
        if (strikes(run, EventEffect.BLUR, rng)) {
            gsd *= 2;
            struck.add("The camera you flew out of focus stayed blurry.");
            findings.add(Finding.problem("The camera you flew out of focus never recovered: every pixel covers twice as much ground.",
                    "Refocusing it in testing would have been cheaper than losing the detail.", 20));
        }
        double resolution = Math.min(1, m.requiredGsdM / gsd);
        if (resolution < 1) {
            findings.add(Finding.problem(f("Each pixel covered %.0f m of ground, but the scientists needed %.0f m %s.",
                    gsd, m.requiredGsdM, m.resolutionReason),
                    "A sharper camera, or a lower orbit: the same camera sees more detail from closer.", Score.SCIENCE_MAX * (1 - resolution)));
        }

        // Data: what the radio and the stations can bring down.
        double produced = camera.dataGbPerDay * duty;
        if (produced > 0 && b.downlinkGbPerDay < produced) {
            double sent = b.downlinkGbPerDay / produced;
            completeness *= sent;
            findings.add(Finding.problem(f("The camera recorded %.1f GB a day but the radio and ground stations could send only %.1f GB. %d%% of the picture never reached Earth.",
                    produced, b.downlinkGbPerDay, Math.round((1 - sent) * 100)),
                    "A faster radio or more ground stations. Stations near the poles see polar satellites on almost every orbit.",
                    Score.SCIENCE_MAX * (1 - sent)));
        }
        if (strikes(run, EventEffect.RESETS, rng)) {
            completeness *= 0.5;
            struck.add("The software bug made the computer restart.");
            findings.add(Finding.problem("The software bug you flew with made the computer restart again and again. Half the data was lost.",
                    "Fixing the code would have cost far less than the lost data.", 25));
        }

        double lifeFactor = Math.min(1.0, orbit.lifetimeYears / m.missionYears);
        if (lifeFactor < 1) {
            findings.add(Finding.problem(f("The orbit is so low that air slowly dragged the satellite down. It fell back after %.1f of the %.0f years planned.",
                    orbit.lifetimeYears, m.missionYears),
                    "Higher orbits last longer.", Score.SCIENCE_MAX * (1 - lifeFactor)));
        }

        PhotoQuality photo = new PhotoQuality(true, resolution, clamp01(completeness), lit, onTarget, lifeFactor);
        double sciencePts = Score.SCIENCE_MAX * resolution * clamp01(completeness) * (lit ? 1 : 0) * (onTarget ? 1 : 0) * lifeFactor;

        addMarginNotes(b, findings);
        if (sciencePts >= 0.9 * Score.SCIENCE_MAX) {
            findings.add(Finding.good("Sharp, complete coverage of " + m.targetName + ". The parts worked together."));
        }
        if (lit && onTarget && resolution >= 1) {
            findings.add(Finding.good(f("Detail met the science requirement: %.0f m per pixel (%.0f m needed).", gsd, m.requiredGsdM)));
        }

        Score score = score(b, run, sciencePts);
        return new SimResult(true, 4, null, photo, score, findings,
                goals(b, run, photo, 4), b, struck, timeline.queue);
    }

    // ---------------------------------------------------------------- route (flight graph)

    /**
     * The cheapest route to the chosen orbit for the satellite's actual mass (Dijkstra over the flight graph),
     * or null if no route can carry that mass.
     */
    static Route cheapestRoute(Design d, Budgets b) {
        return Catalog.FLIGHTS.cheapestRoute(d.orbit, b.massKg);
    }

    /** true if the team flew the cheapest possible route to its orbit. */
    static boolean tookCheapestRoute(Design d, Budgets b) {
        Route best = cheapestRoute(d, b);
        return best != null && b.launchCostK <= best.costFor(b.massKg) + 0.01;
    }

    /** If there was a cheaper route that could carry the satellite, it is explained in the report. */
    private static void addRouteNote(Budgets b, Design d, List<Finding> findings) {
        Route best = cheapestRoute(d, b);
        if (best == null || tookCheapestRoute(d, b)) return;
        double saved = b.launchCostK - best.costFor(b.massKg);
        findings.add(Finding.problem(f("Getting to the %s cost $%.0fk. For a %.1f kg satellite the cheapest route was %s at $%.0fk.",
                        d.orbit.name, b.launchCostK, b.massKg, best.label(), best.costFor(b.massKg)),
                "The rocket console offers the cheapest route first. A pricier route can still be worth it for its hand-over date.",
                Math.min(Score.BUDGET_MAX, Score.BUDGET_MAX * saved / b.budgetK / 0.10)));
    }

    // ---------------------------------------------------------------- helpers

    private static SimResult notLaunched(Budgets b, String text, String tip) {
        List<Finding> f = new ArrayList<>();
        f.add(Finding.problem(text, tip, 100));
        Map<Role, Boolean> goals = new EnumMap<>(Role.class);
        for (Role r : Role.values()) goals.put(r, false);
        Timeline timeline = new Timeline();
        timeline.add(0, Kind.CANCELLED, Status.FAILED, "Launch cancelled", null);
        return new SimResult(false, 0, null, PhotoQuality.none(), Score.zero(), f, goals, b, new ArrayList<>(), timeline.queue);
    }

    private static SimResult failed(Budgets b, int passed, Stage at, List<Finding> findings, List<String> struck,
                                    MissionRun run, Timeline timeline) {
        // If it never reached space there are no points; if it failed later, the design still counts for something.
        Score score = passed == 0 ? Score.zero() : score(b, run, 0);
        return new SimResult(true, passed, at, PhotoQuality.none(), score, findings,
                goals(b, run, PhotoQuality.none(), passed), b, struck, timeline.queue);
    }

    private static boolean strikes(MissionRun run, EventEffect effect, SplittableRandom rng) {
        for (MissionRun.Decision dec : run.decisions) {
            if (!dec.fixed && dec.event.effect == effect && rng.nextDouble() < dec.event.chance) return true;
        }
        return false;
    }

    static Score score(Budgets b, MissionRun run, double sciencePts) {
        double quarter = Score.MARGIN_MAX / 4.0;
        double margins = quarter * Score.marginPoints(b.massMargin())
                + quarter * Score.marginPoints(b.powerMargin())
                + quarter * Score.marginPoints(b.batteryMargin())
                + quarter * Math.max(0, Math.min(1, b.scheduleMarginMonths()));
        double budget = Score.BUDGET_MAX * Math.max(0, Math.min(1, b.budgetReserve() / 0.10));
        // Probability that EVERY part works: each extra fragile part lowers the figure.
        double rel = 1;
        for (Part p : run.design.parts()) rel *= p.reliability;
        double reliability = Math.max(0, Score.RELIABILITY_MAX * rel - 3 * run.risksTaken());
        return new Score(sciencePts, margins, budget, reliability);
    }

    private static void addMarginNotes(Budgets b, List<Finding> findings) {
        if (b.massMargin() >= 0 && b.massMargin() < 0.1) {
            findings.add(Finding.problem(f("Only %.0f%% spare weight. Real teams keep 10 to 30%% because parts always grow heavier.", b.massMargin() * 100),
                    "Leave some spare weight: a bigger frame or a route that carries more.", 3));
        }
        if (b.powerMargin() >= 0 && b.powerMargin() < 0.1) {
            findings.add(Finding.problem(f("Only %.0f%% spare power. One weak panel and the camera would switch off.", b.powerMargin() * 100),
                    "Keep at least 10% spare power.", 4));
        }
        if (b.budgetReserve() >= 0 && b.budgetReserve() < 0.1) {
            findings.add(Finding.problem(f("Only %.0f%% of the budget left in reserve. NASA projects keep a budget reserve for surprises.", b.budgetReserve() * 100),
                    "Cheaper parts or a cheaper route would rebuild the reserve.", 4));
        }
    }

    private static Map<Role, Boolean> goals(Budgets b, MissionRun run, PhotoQuality photo, int passed) {
        Mission m = run.mission;
        Design d = run.design;
        Map<Role, Boolean> g = new EnumMap<>(Role.class);
        g.put(Role.SCIENTIST, passed == 4 && photo.lit && photo.onTarget && photo.resolution >= 1);
        g.put(Role.ENGINEER, b.massMargin() >= 0.10 && b.powerMargin() >= 0.10 && b.batteryMargin() >= 0.10);
        // Launch lead: an orbit that passes over the target, and the cheapest path through the graph to it.
        g.put(Role.LAUNCH, passed >= 1 && d.orbit.passesOver(m.targetLatitude) && tookCheapestRoute(d, b));
        g.put(Role.GROUND, passed >= 3 && photo.completeness >= SuccessTier.FULL_COMPLETENESS && b.budgetReserve() >= 0.10);
        return g;
    }

    private static double clamp01(double v) {
        return Math.max(0, Math.min(1, v));
    }

    private static String f(String fmt, Object... args) {
        return String.format(Locale.ROOT, fmt, args);
    }

    /**
     * Helper for writing the timeline: keeps the mission clock and enqueues each event at the back.
     * Since the clock only moves forward, the queue is always in chronological order.
     */
    private static final class Timeline {
        final LinkedQueue<FlightEvent> queue = new LinkedQueue<>();
        private double clock;

        /** Advances the clock by "afterHours" hours and enqueues the event at that time. */
        void add(double afterHours, Kind kind, Status status, String text, Orbit orbit) {
            clock += afterHours;
            queue.enqueue(new FlightEvent(kind, status, clock, text, orbit));
        }
    }
}
