package com.missionlab.sim;

import com.missionlab.data.Catalog;
import com.missionlab.model.Design;
import com.missionlab.model.Mission;
import com.missionlab.model.MissionRun;
import com.missionlab.model.Orbit;
import com.missionlab.model.Part;
import com.missionlab.model.Slot;

/**
 * Las cuentas del diseño, como las llevan los equipos de NASA: peso, espacio, energía, batería, datos,
 * presupuesto y tiempo. Las barras del hangar, la revisión de vuelo (Readiness), el simulador y la
 * pantalla final usan exactamente estas cuentas, así nunca se contradicen.
 *
 * Reglas que hacen el juego justo y realista:
 * - Energía con ciclo de trabajo: la cámara solo gasta mientras hace fotos y la radio solo mientras transmite.
 * - Calendario en paralelo: las piezas se compran a la vez, así que manda la que más tarda en llegar
 *   (más los meses fijos de montaje y pruebas).
 */
public final class Budgets {
    // ---------------------------------------------------------------- peso y espacio
    /** Peso total, con estructura y vela de frenado si hace falta. */
    public final double massKg;
    /** Peso máximo que admite la estructura elegida (CubeSat Design Specification). 0 si no hay. */
    public final double frameMaxKg;
    /** Lo máximo que lleva la ruta de lanzamiento (el vehículo más pequeño del camino). 0 si no hay ruta. */
    public final double launchCapacityKg;
    /** Espacio usado dentro de la estructura, en U (1U = un cubo de 10 cm). */
    public final double volumeU;
    public final double frameVolumeU;
    /** true si la órbita aguantaría demasiado tiempo y hace falta una vela de frenado (regla de los 5 años). */
    public final boolean needsDragSail;

    // ---------------------------------------------------------------- órbita (de OrbitMechanics)
    /** La órbita con la que se hacen las cuentas (la elegida, o una de referencia si aún no hay). */
    public final Orbit orbitUsed;
    public final double periodMin;
    public final double eclipseMin;
    public final double sunlitFraction;
    public final double contactMinPerDay;

    // ---------------------------------------------------------------- energía
    /** Consumo medio por vuelta. */
    public final double drawW;
    public final double busW, pointingW, cameraAvgW, radioAvgW;
    /** Energía media generada por vuelta (paneles × parte del tiempo al sol). */
    public final double generatedW;
    /** Energía que hay que sacar de la batería en la parte oscura de cada vuelta. */
    public final double eclipseNeedWh;
    /** Energía de la batería que se puede usar sin dañarla (profundidad de descarga). */
    public final double batteryUsableWh;

    // ---------------------------------------------------------------- datos
    public final double dataGbPerDay;
    public final double downlinkGbPerDay;

    // ---------------------------------------------------------------- presupuesto
    public final double partsCostK;
    /** Ordenador, placa de energía y pruebas de vibración y vacío: lo paga cualquier satélite. */
    public final double busAndTestingK;
    public final double launchCostK;
    public final double dragSailK;
    public final double groundCostK;
    public final double extraCostK;
    public final double costK;
    public final double budgetK;

    // ---------------------------------------------------------------- tiempo
    /** Meses hasta tener el satélite terminado: pieza más lenta + montaje y pruebas + retrasos. */
    public final double months;
    /** La pieza que manda en el calendario (o null). */
    public final Part slowestPart;
    /** Mes límite para entregarlo: el lanzamiento menos lo que pide la ruta. */
    public final double deadlineMonths;
    public final double monthsToLaunch;

    // ---------------------------------------------------------------- ciencia
    /** Resolución en el suelo de la cámara desde la órbita (m por píxel). 0 si falta algo. */
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

        // Energía: cada pieza gasta solo el tiempo que está encendida.
        double contactFraction = contactMinPerDay / 1440.0;
        this.busW = Catalog.BUS_POWER_W;
        this.pointingW = pointing != null ? pointing.powerW : 0;
        this.cameraAvgW = camera != null ? camera.powerW * m.imagingDuty : 0;
        this.radioAvgW = radio != null ? radio.powerW * contactFraction : 0;
        this.drawW = busW + pointingW + cameraAvgW + radioAvgW;
        this.generatedW = panels != null ? panels.generatesW * sunlitFraction : 0;
        // En la sombra siempre siguen encendidos el ordenador y la orientación.
        // En un encargo de noche, además, la cámara trabaja justo cuando no hay sol.
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

    // ---------------------------------------------------------------- márgenes (0.2 = sobra un 20%)

    /** El límite de peso que manda: el menor entre la estructura y la ruta. 0 si no hay ninguno. */
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

    /** Parte del presupuesto que sobra (reserva). */
    public double budgetReserve() {
        return (budgetK - costK) / budgetK;
    }

    /** Meses de holgura antes de la fecha de entrega. */
    public double scheduleMarginMonths() {
        return deadlineMonths - months;
    }

    /** Parte de los datos de cada día que llegan a Tierra (0..1). */
    public double downlinkShare() {
        if (dataGbPerDay <= 0) return 1;
        return Math.min(1, downlinkGbPerDay / dataGbPerDay);
    }

    /** 1 si la cámara ve tanto detalle como piden los científicos; menos si ve peor. */
    public double resolutionFactor() {
        return gsdM > 0 ? Math.min(1, requiredGsdM / gsdM) : 0;
    }

    // ---------------------------------------------------------------- barras del hangar: 1.0 = justo en el límite

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
