package com.missionlab.sim;

import com.missionlab.data.Catalog;
import com.missionlab.model.Design;
import com.missionlab.model.Mission;
import com.missionlab.model.MissionRun;
import com.missionlab.model.Orbit;
import com.missionlab.model.Part;
import com.missionlab.model.Slot;

/**
 * The design budgets, kept the way NASA teams keep them: mass, volume, power, battery, data,
 * money and schedule. The hangar bars, the flight readiness review (Readiness), the simulator and the
 * final screen all use exactly these figures, so they never contradict each other.
 *
 * Rules that keep the game fair and realistic:
 * - Power with duty cycles: the camera only draws power while imaging and the radio only while transmitting.
 * - Parallel schedule: parts are ordered together, so the slowest one to arrive sets the date
 *   (plus the fixed months of integration and testing).
 */
public final class Budgets {
    // ---------------------------------------------------------------- mass and volume
    /** Total mass, including structure and drag sail if needed. */
    public final double massKg;
    /** Maximum mass the chosen structure allows (CubeSat Design Specification). 0 if none. */
    public final double frameMaxKg;
    /** The most the launch route can carry (the smallest vehicle on the path). 0 if there is no route. */
    public final double launchCapacityKg;
    /** Volume used inside the structure, in U (1U = a 10 cm cube). */
    public final double volumeU;
    public final double frameVolumeU;
    /** true if the orbit would last too long and a drag sail is needed (5-year rule). */
    public final boolean needsDragSail;

    // ---------------------------------------------------------------- orbit (from OrbitMechanics)
    /** The orbit used for the budgets (the chosen one, or a reference orbit if none yet). */
    public final Orbit orbitUsed;
    public final double periodMin;
    public final double eclipseMin;
    public final double sunlitFraction;
    public final double contactMinPerDay;

    // ---------------------------------------------------------------- power
    /** Average power draw per orbit. */
    public final double drawW;
    public final double busW, pointingW, cameraAvgW, radioAvgW;
    /** Average power generated per orbit (panels × fraction of time in sunlight). */
    public final double generatedW;
    /** Energy that must come from the battery during the dark part of each orbit. */
    public final double eclipseNeedWh;
    /** Battery energy usable without damaging it (depth of discharge). */
    public final double batteryUsableWh;

    // ---------------------------------------------------------------- data
    public final double dataGbPerDay;
    public final double downlinkGbPerDay;

    // ---------------------------------------------------------------- budget
    public final double partsCostK;
    /** Flight computer, power board, and vibration and thermal-vacuum testing: every satellite pays for these. */
    public final double busAndTestingK;
    public final double launchCostK;
    public final double dragSailK;
    public final double groundCostK;
    public final double extraCostK;
    public final double costK;
    public final double budgetK;

    // ---------------------------------------------------------------- schedule
    /** Months until the satellite is finished: slowest part + integration and testing + delays. */
    public final double months;
    /** The part that drives the schedule (or null). */
    public final Part slowestPart;
    /** Delivery deadline month: the launch date minus what the route requires. */
    public final double deadlineMonths;
    public final double monthsToLaunch;

    // ---------------------------------------------------------------- science
    /** Ground resolution of the camera from the orbit (m per pixel). 0 if something is missing. */
    public final double gsdM;
    public final double requiredGsdM;

    private Budgets(Design d, Mission m, double extraCostK, double extraMonths) {
        Orbit orbit = d.orbit != null ? d.orbit : Catalog.REFERENCE_ORBIT;
        this.orbitUsed = orbit;
        this.periodMin = OrbitMechanics.periodMinutes(orbit);
        this.eclipseMin = OrbitMechanics.eclipseMinutes(orbit, m.sunDeclinationDeg);
        this.sunlitFraction = 1 - eclipseMin / periodMin;
        this.contactMinPerDay = d.station != null ? OrbitMechanics.contactMinutesPerDay(orbit, d.station) : 0;
        this.needsDragSail = d.orbit != null && d.orbit.lifetimeYears > m.missionYears + Catalog.DEBRIS_RULE_YEARS;

        double mass = needsDragSail ? Catalog.DRAG_SAIL_KG : 0, volume = Catalog.BUS_VOLUME_U, cost = 0, lead = 0;
        Part slowest = null;
        for (Part p : d.parts()) {
            mass += p.massKg;
            cost += p.costK;
            if (p.slot != Slot.STRUCTURE) volume += p.volumeU;
            if (p.leadMonths > lead) {
                lead = p.leadMonths;
                slowest = p;
            }
        }
        Part frame = d.get(Slot.STRUCTURE), camera = d.get(Slot.INSTRUMENT), radio = d.get(Slot.RADIO);
        Part pointing = d.get(Slot.POINTING), panels = d.get(Slot.POWER), battery = d.get(Slot.BATTERY);

        this.massKg = mass;
        this.frameMaxKg = frame != null ? frame.frameMaxKg : 0;
        this.launchCapacityKg = d.route != null ? d.route.capacityKg() : 0;
        this.volumeU = volume;
        this.frameVolumeU = frame != null ? frame.frameVolumeU : 0;

        // Power: each part only draws power for the time it is switched on.
        double contactFraction = contactMinPerDay / 1440.0;
        this.busW = Catalog.BUS_POWER_W;
        this.pointingW = pointing != null ? pointing.powerW : 0;
        this.cameraAvgW = camera != null ? camera.powerW * m.imagingDuty : 0;
        this.radioAvgW = radio != null ? radio.powerW * contactFraction : 0;
        this.drawW = busW + pointingW + cameraAvgW + radioAvgW;
        this.generatedW = panels != null ? panels.generatesW * sunlitFraction : 0;
        // In eclipse the flight computer and attitude control always stay on.
        // On a night mission, the camera also operates exactly when there is no sunlight.
        double imagingMinPerOrbit = m.imagingDuty * periodMin;
        double nightCameraWh = m.night && camera != null ? camera.powerW * Math.min(imagingMinPerOrbit, eclipseMin) / 60.0 : 0;
        this.eclipseNeedWh = (busW + pointingW) * eclipseMin / 60.0 + nightCameraWh;
        this.batteryUsableWh = battery != null ? battery.batteryWh * Catalog.BATTERY_DEPTH_OF_DISCHARGE : 0;

        this.dataGbPerDay = camera != null ? camera.dataGbPerDay : 0;
        this.downlinkGbPerDay = radio != null ? radio.downlinkMbps * contactMinPerDay * 60.0 / 8000.0 : 0;

        this.partsCostK = cost;
        this.busAndTestingK = Catalog.BUS_AND_TESTING_K;
        this.launchCostK = d.route != null ? d.route.costFor(mass) : 0;
        this.dragSailK = needsDragSail ? Catalog.DRAG_SAIL_K : 0;
        this.groundCostK = d.station != null ? d.station.costKPerYear * m.missionYears : 0;
        this.extraCostK = extraCostK;
        this.costK = partsCostK + busAndTestingK + launchCostK + dragSailK + groundCostK + extraCostK;
        this.budgetK = m.budgetK;

        this.slowestPart = slowest;
        this.months = Catalog.INTEGRATION_MONTHS + lead + extraMonths;
        this.monthsToLaunch = m.monthsToLaunch;
        this.deadlineMonths = m.monthsToLaunch - (d.route != null ? d.route.deliveryMonths() : 0);

        this.gsdM = camera != null ? camera.groundResolutionM(orbit.altitudeKm) : 0;
        this.requiredGsdM = m.requiredGsdM;
    }

    public static Budgets of(MissionRun run) {
        return new Budgets(run.design, run.mission, run.extraCostK(), run.extraMonths());
    }

    public static Budgets of(Design d, Mission m) {
        return new Budgets(d, m, 0, 0);
    }

    // ---------------------------------------------------------------- margins (0.2 = 20% to spare)

    /** The binding mass limit: the lower of structure and route. 0 if there is none. */
    public double massLimitKg() {
        if (frameMaxKg > 0 && launchCapacityKg > 0) return Math.min(frameMaxKg, launchCapacityKg);
        return Math.max(frameMaxKg, launchCapacityKg);
    }

    public double massMargin() {
        double cap = massLimitKg();
        return cap > 0 ? (cap - massKg) / cap : 0;
    }

    public double volumeMargin() {
        return frameVolumeU > 0 ? (frameVolumeU - volumeU) / frameVolumeU : 0;
    }

    public double powerMargin() {
        return drawW > 0 ? (generatedW - drawW) / drawW : 0;
    }

    public double batteryMargin() {
        return eclipseNeedWh > 0 ? (batteryUsableWh - eclipseNeedWh) / eclipseNeedWh : 0;
    }

    public double dataMargin() {
        return dataGbPerDay > 0 ? (downlinkGbPerDay - dataGbPerDay) / dataGbPerDay : 0;
    }

    /** Fraction of the budget left over (reserve). */
    public double budgetReserve() {
        return (budgetK - costK) / budgetK;
    }

    /** Months of slack before the delivery date. */
    public double scheduleMarginMonths() {
        return deadlineMonths - months;
    }

    /** Fraction of each day's data that reaches the ground (0..1). */
    public double downlinkShare() {
        if (dataGbPerDay <= 0) return 1;
        return Math.min(1, downlinkGbPerDay / dataGbPerDay);
    }

    /** 1 if the camera sees as much detail as the scientists ask for; less if it sees worse. */
    public double resolutionFactor() {
        return gsdM > 0 ? Math.min(1, requiredGsdM / gsdM) : 0;
    }

    // ---------------------------------------------------------------- hangar bars: 1.0 = right at the limit

    public double massRatio() {
        double cap = massLimitKg();
        return ratio(massKg, cap > 0 ? cap : 12);
    }

    public double volumeRatio() {
        return ratio(volumeU, frameVolumeU > 0 ? frameVolumeU : 6);
    }

    public double powerRatio() {
        return ratio(drawW, generatedW);
    }

    public double batteryRatio() {
        return ratio(eclipseNeedWh, batteryUsableWh);
    }

    public double dataRatio() {
        return ratio(dataGbPerDay, downlinkGbPerDay);
    }

    public double budgetRatio() {
        return ratio(costK, budgetK);
    }

    public double timeRatio() {
        return ratio(months, deadlineMonths);
    }

    private static double ratio(double used, double available) {
        if (used <= 0) return 0;
        if (available <= 0) return 9.99;
        return used / available;
    }
}
