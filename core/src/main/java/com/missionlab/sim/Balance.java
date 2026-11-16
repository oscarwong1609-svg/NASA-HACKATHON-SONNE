package com.missionlab.sim;

import com.missionlab.data.Catalog;
import com.missionlab.model.Design;
import com.missionlab.model.GroundStation;
import com.missionlab.model.Mission;
import com.missionlab.model.MissionRun;
import com.missionlab.model.Orbit;
import com.missionlab.model.Part;
import com.missionlab.model.Route;
import com.missionlab.model.Slot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Tests EVERY possible design for each mission and checks that the game is fair.
 * Run it after changing numbers in Catalog: ./gradlew :core:balance
 *
 * Rules (the same for both missions):
 * 1. At least 25% of designs pass the flight readiness review (can be launched).
 * 2. The design with the cheapest part in every slot can be launched (no guessing needed to lift off).
 * 3. At least 10 designs achieve full success, with at least 4 distinct combinations
 *    of orbit, radio and station (there are several good answers, not just one).
 * 4. Minimum success is reachable with less than 70% of the budget, and full success with less than 95%.
 * 5. No design reaches 100.
 * 6. Every part is useful: in some mission there is a design scoring 60 or more that uses it.
 */
public final class Balance {
    private static final int SEEDS = 40;

    public static void main(String[] args) {
        boolean allPass = true;
        List<Design> designs = allDesigns();
        Map<Part, Double> bestWithPart = new java.util.HashMap<>();
        for (Mission m : Catalog.MISSIONS) {
            Report r = evaluate(m, designs);
            System.out.println(r);
            allPass &= r.passes();
            for (Entry e : r.entries) {
                for (Part p : e.design.parts()) bestWithPart.merge(p, e.score, Math::max);
            }
        }
        System.out.println("\nBest score using each part (rule 6: 60 or more):");
        for (Part p : Catalog.PARTS) {
            double best = bestWithPart.getOrDefault(p, 0.0);
            System.out.printf(Locale.ROOT, "  %-14s %5.1f%s%n", p.id, best, best < 60 ? "   <-- unused" : "");
            if (best < 60) allPass = false;
        }
        System.out.println(allPass ? "\nBALANCE OK" : "\nBALANCE NEEDS WORK");
    }

    /** Evaluates every design for a mission. */
    public static Report evaluate(Mission m, List<Design> designs) {
        Report r = new Report(m);
        for (Design d : designs) {
            Readiness review = Readiness.of(d, m);
            double total = 0;
            Map<SuccessTier, Integer> tiers = new EnumMap<>(SuccessTier.class);
            for (int s = 0; s < SEEDS; s++) {
                SimResult res = Simulator.run(new MissionRun(m, d), s);
                total += res.score.total();
                tiers.merge(res.tier, 1, Integer::sum);
            }
            SuccessTier usual = tiers.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();
            r.entries.add(new Entry(d, total / SEEDS, review.go(), usual, review.budgets.costK));
        }
        r.entries.sort((a, b) -> Double.compare(b.score, a.score));
        return r;
    }

    /** Every combination: one part per slot, every orbit with every graph route, every station network. */
    public static List<Design> allDesigns() {
        List<Design> out = new ArrayList<>();
        for (Part ins : Catalog.partsFor(Slot.INSTRUMENT))
            for (Part pow : Catalog.partsFor(Slot.POWER))
                for (Part bat : Catalog.partsFor(Slot.BATTERY))
                    for (Part pnt : Catalog.partsFor(Slot.POINTING))
                        for (Part rad : Catalog.partsFor(Slot.RADIO))
                            for (Part frm : Catalog.partsFor(Slot.STRUCTURE))
                                for (Orbit o : Catalog.ORBITS)
                                    for (Route r : Catalog.FLIGHTS.routesTo(o))
                                        for (GroundStation g : Catalog.STATIONS) {
                                            Design d = new Design();
                                            d.install(ins); d.install(pow); d.install(bat);
                                            d.install(pnt); d.install(rad); d.install(frm);
                                            d.orbit = o; d.route = r; d.station = g;
                                            out.add(d);
                                        }
        return out;
    }

    /** The cheapest part in each slot, on the cheapest orbit that passes over the target. */
    public static Design cheapestDesign(Mission m) {
        Design d = new Design();
        for (Slot s : Slot.values()) d.install(Catalog.partsFor(s).stream().min(Comparator.comparingDouble(p -> p.costK)).get());
        d.station = Catalog.STATIONS.stream().min(Comparator.comparingDouble(g -> g.costKPerYear)).get();
        Design best = null;
        double bestCost = Double.MAX_VALUE;
        for (Orbit o : Catalog.ORBITS) {
            if (!o.passesOver(m.targetLatitude)) continue;
            Design c = d.copy();
            c.orbit = o;
            c.route = Catalog.FLIGHTS.cheapestRoute(o, Budgets.of(c, m).massKg);
            if (c.route == null) continue;
            double cost = Budgets.of(c, m).costK;
            if (cost < bestCost) {
                bestCost = cost;
                best = c;
            }
        }
        return best;
    }

    // ---------------------------------------------------------------- report

    public static final class Entry {
        public final Design design;
        public final double score;
        public final boolean go;
        public final SuccessTier tier;
        public final double costK;

        Entry(Design design, double score, boolean go, SuccessTier tier, double costK) {
            this.design = design;
            this.score = score;
            this.go = go;
            this.tier = tier;
            this.costK = costK;
        }

        @Override
        public String toString() {
            Design d = design;
            return String.format(Locale.ROOT, "%5.1f %-9s $%4.0fk  %s | %s | %s | %s | %s | %s | %s | %s | %s", score, tier.medal, costK,
                    d.get(Slot.INSTRUMENT).id, d.get(Slot.POWER).id, d.get(Slot.BATTERY).id,
                    d.get(Slot.POINTING).id, d.get(Slot.RADIO).id, d.get(Slot.STRUCTURE).id,
                    d.orbit.id, d.route.id(), d.station.id);
        }
    }

    public static final class Report {
        public final Mission mission;
        public final List<Entry> entries = new ArrayList<>();

        Report(Mission mission) {
            this.mission = mission;
        }

        public long launchable() {
            return entries.stream().filter(e -> e.go).count();
        }

        public long atLeast(SuccessTier t) {
            return entries.stream().filter(e -> e.go && e.tier.atLeast(t)).count();
        }

        /** Distinct orbit + radio + station combinations among the full-success designs. */
        public int fullSuccessFamilies() {
            Set<String> fam = new HashSet<>();
            for (Entry e : entries) {
                if (e.go && e.tier.atLeast(SuccessTier.FULL)) fam.add(e.design.orbit.id + e.design.get(Slot.RADIO).id + e.design.station.id);
            }
            return fam.size();
        }

        /** The lowest cost at which this tier is achieved (as a fraction of the budget), or 9.99. */
        public double cheapestShare(SuccessTier t) {
            return entries.stream().filter(e -> e.go && e.tier.atLeast(t)).mapToDouble(e -> e.costK / mission.budgetK).min().orElse(9.99);
        }

        public boolean cheapestLaunches() {
            Design c = cheapestDesign(mission);
            return c != null && Readiness.of(c, mission).go();
        }

        public double best() {
            return entries.isEmpty() ? 0 : entries.get(0).score;
        }

        public boolean passes() {
            return launchable() >= 0.25 * entries.size() && cheapestLaunches()
                    && atLeast(SuccessTier.FULL) >= 10 && fullSuccessFamilies() >= 4
                    && cheapestShare(SuccessTier.MINIMUM) <= 0.70 && cheapestShare(SuccessTier.FULL) <= 0.95
                    && best() < 99.5;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            int n = entries.size();
            sb.append(String.format(Locale.ROOT, "%n=== Mission %d: %s (budget $%.0fk, %.0f months) ===%n",
                    mission.number, mission.title, mission.budgetK, mission.monthsToLaunch));
            sb.append(String.format(Locale.ROOT, "  %d designs | pass flight review %d (%.0f%%) | minimum+ %d | full+ %d (%d families) | exemplary %d%n",
                    n, launchable(), 100.0 * launchable() / n, atLeast(SuccessTier.MINIMUM), atLeast(SuccessTier.FULL),
                    fullSuccessFamilies(), atLeast(SuccessTier.EXEMPLARY)));
            Design c = cheapestDesign(mission);
            Readiness cr = c == null ? null : Readiness.of(c, mission);
            sb.append(String.format(Locale.ROOT, "  cheapest parts: %s%s | cheapest minimum success %.0f%% of budget | cheapest full success %.0f%% | best %.1f%n",
                    cr != null && cr.go() ? "can launch" : "CANNOT launch",
                    cr != null && !cr.go() ? " (" + cr.firstBlocker() + ")" : "",
                    100 * cheapestShare(SuccessTier.MINIMUM), 100 * cheapestShare(SuccessTier.FULL), best()));
            for (int i = 0; i < Math.min(10, n); i++) sb.append("  ").append(entries.get(i)).append('\n');
            sb.append(passes() ? "  -> rules OK" : "  -> RULES NOT MET");
            return sb.toString();
        }
    }
}
