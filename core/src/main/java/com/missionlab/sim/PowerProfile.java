package com.missionlab.sim;

import com.missionlab.data.Catalog;
import com.missionlab.model.Design;
import com.missionlab.model.Mission;
import com.missionlab.model.Part;
import com.missionlab.model.Slot;

/**
 * Battery state of charge over a few orbits, for the ground station desk chart.
 * Each orbit: part in sunlight (the panels charge) and part in the Earth's shadow (battery only).
 * On a night mission the camera operates during eclipse; on a day mission, in sunlight.
 */
public final class PowerProfile {
    /** Battery state of charge at each sample (0..1 of capacity). */
    public final double[] charge;
    /** true if the satellite is in sunlight at that sample. */
    public final boolean[] sunlit;
    /** true if the camera is imaging at that sample. */
    public final boolean[] imaging;
    /** Safe limit: below it, the satellite enters safe mode. */
    public final double floor;
    public final double minimum;
    public final int orbits;

    private PowerProfile(double[] charge, boolean[] sunlit, boolean[] imaging, double floor, int orbits) {
        this.charge = charge;
        this.sunlit = sunlit;
        this.imaging = imaging;
        this.floor = floor;
        this.orbits = orbits;
        double min = 1;
        for (double c : charge) min = Math.min(min, c);
        this.minimum = min;
    }

    /** true if the battery never drops below the safe limit. */
    public boolean healthy() {
        return minimum >= floor - 1e-9;
    }

    /** Simulates several orbits, sampled every minute. Returns null if any power part is missing. */
    public static PowerProfile of(Design d, Mission m, int orbits) {
        Part battery = d.get(Slot.BATTERY), panels = d.get(Slot.POWER);
        if (battery == null || panels == null) return null;
        Budgets b = Budgets.of(d, m);
        Part camera = d.get(Slot.INSTRUMENT), pointing = d.get(Slot.POINTING);
        double camW = camera != null ? camera.powerW : 0;
        double steady = Catalog.BUS_POWER_W + (pointing != null ? pointing.powerW : 0) + b.radioAvgW;
        double capacity = battery.batteryWh;
        double floor = 1 - Catalog.BATTERY_DEPTH_OF_DISCHARGE;
        double period = b.periodMin, eclipse = b.eclipseMin;
        double imagingMin = m.imagingDuty * period;

        int n = (int) Math.ceil(period * orbits);
        double[] charge = new double[n];
        boolean[] sun = new boolean[n];
        boolean[] img = new boolean[n];
        double wh = capacity;
        for (int i = 0; i < n; i++) {
            double phase = i % period;
            boolean lit = phase < period - eclipse;
            // Night: the camera at the start of eclipse. Day: in the middle of the sunlit arc.
            boolean shooting = m.night
                    ? !lit && phase - (period - eclipse) < imagingMin
                    : lit && Math.abs(phase - (period - eclipse) / 2) < imagingMin / 2;
            double load = steady + (shooting ? camW : 0);
            double gen = lit ? panels.generatesW : 0;
            wh = Math.max(0, Math.min(capacity, wh + (gen - load) / 60.0));
            charge[i] = wh / capacity;
            sun[i] = lit;
            img[i] = shooting;
        }
        return new PowerProfile(charge, sun, img, floor, orbits);
    }
}
