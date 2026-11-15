package com.missionlab.sim;

import com.missionlab.data.Catalog;
import com.missionlab.model.Design;
import com.missionlab.model.Mission;
import com.missionlab.model.Part;
import com.missionlab.model.Slot;

/**
 * La carga de la batería a lo largo de unas vueltas, para el gráfico de la mesa de estaciones.
 * Cada vuelta: parte al sol (los paneles cargan) y parte en la sombra de la Tierra (solo batería).
 * En un encargo de noche la cámara trabaja justo en la sombra; de día, al sol.
 */
public final class PowerProfile {
    /** Carga de la batería en cada muestra (0..1 de la capacidad). */
    public final double[] charge;
    /** true si en esa muestra el satélite está al sol. */
    public final boolean[] sunlit;
    /** true si en esa muestra la cámara está haciendo fotos. */
    public final boolean[] imaging;
    /** Límite seguro: por debajo, el satélite entra en modo seguro. */
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

    /** true si la batería nunca baja del límite seguro. */
    public boolean healthy() {
        return minimum >= floor - 1e-9;
    }

    /** Simula varias vueltas, con muestras cada minuto. Devuelve null si falta alguna pieza de energía. */
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
            // De noche: la cámara al empezar la sombra. De día: en mitad de la parte con sol.
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
