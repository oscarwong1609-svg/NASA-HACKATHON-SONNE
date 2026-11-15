package com.missionlab.model;

/**
 * Una pieza del satélite. Todas tienen peso, consumo, precio, plazo de entrega y fiabilidad.
 * Los demás campos solo importan para su tipo de hueco (por ejemplo, batteryWh solo para baterías).
 * Los números están simplificados para enseñar, pero en el orden de magnitud de las piezas reales de CubeSat.
 */
public final class Part {
    public final String id;
    public final String name;
    public final String description;
    public final Slot slot;

    public final double massKg;
    /**
     * Consumo en vatios MIENTRAS TRABAJA. La cámara solo consume cuando hace fotos y la radio solo cuando
     * transmite; la orientación consume siempre. Budgets reparte cada consumo según el tiempo que está encendido.
     */
    public final double powerW;
    /** Precio en miles de dólares. */
    public final double costK;
    /** Plazo de entrega en meses. Las piezas se compran a la vez: manda la que más tarda. */
    public final double leadMonths;
    /** Probabilidad de funcionar (0..1). Afecta la puntuación de fiabilidad. */
    public final double reliability;
    /** Si es true, en partidas de 2+ jugadores hacen falta dos personas para cargarla. */
    public final boolean heavy;
    /** Espacio que ocupa dentro de la estructura, en U (1U = un cubo de 10 cm). Los paneles van por fuera. */
    public final double volumeU;

    // Instrumento
    /**
     * Campo de visión de un píxel, en microrradianes. Resolución en el suelo (m) = altura (km) × IFOV / 1000.
     * Por eso la misma cámara ve más detalle desde una órbita más baja.
     */
    public final double ifovMicroRad;
    /** Datos de la foto del objetivo de cada día, en GB. */
    public final double dataGbPerDay;
    /** Puntería que necesita (0..1). Las cámaras de campo estrecho necesitan apuntar muy bien. */
    public final double pointingNeed;
    public final boolean seesAtNight;
    // Paneles solares
    /** Vatios mientras les da el sol. */
    public final double generatesW;
    /** Paneles que se despliegan con bisagras (pueden atascarse). */
    public final boolean deployable;
    // Batería
    public final double batteryWh;
    // Orientación
    public final double stability;
    // Radio
    public final double downlinkMbps;
    // Estructura
    /** Masa total máxima del satélite con esta estructura (CubeSat Design Specification: 2 kg por U). */
    public final double frameMaxKg;
    /** Espacio que ofrece la estructura, en U. */
    public final double frameVolumeU;

    private Part(Builder b) {
        id = b.id; name = b.name; description = b.description; slot = b.slot;
        massKg = b.massKg; powerW = b.powerW; costK = b.costK; leadMonths = b.leadMonths;
        reliability = b.reliability; heavy = b.heavy; volumeU = b.volumeU;
        ifovMicroRad = b.ifovMicroRad; dataGbPerDay = b.dataGbPerDay; pointingNeed = b.pointingNeed;
        seesAtNight = b.seesAtNight; generatesW = b.generatesW; deployable = b.deployable; batteryWh = b.batteryWh;
        stability = b.stability; downlinkMbps = b.downlinkMbps; frameMaxKg = b.frameMaxKg; frameVolumeU = b.frameVolumeU;
    }

    public static Builder of(String id, Slot slot, String name) {
        return new Builder(id, slot, name);
    }

    /** Resolución en el suelo (metros por píxel) desde esta altura. Solo para instrumentos. */
    public double groundResolutionM(double altitudeKm) {
        return altitudeKm * ifovMicroRad / 1000.0;
    }

    @Override
    public String toString() {
        return name;
    }

    public static final class Builder {
        private final String id;
        private final Slot slot;
        private final String name;
        private String description = "";
        private double massKg, powerW, costK, leadMonths = 1, reliability = 0.97, volumeU;
        private boolean heavy;
        private double ifovMicroRad, dataGbPerDay, pointingNeed;
        private boolean seesAtNight, deployable;
        private double generatesW, batteryWh, stability, downlinkMbps, frameMaxKg, frameVolumeU;

        private Builder(String id, Slot slot, String name) {
            this.id = id; this.slot = slot; this.name = name;
        }

        public Builder text(String d) { description = d; return this; }
        public Builder mass(double kg) { massKg = kg; return this; }
        public Builder power(double w) { powerW = w; return this; }
        public Builder cost(double k) { costK = k; return this; }
        public Builder lead(double months) { leadMonths = months; return this; }
        public Builder reliability(double r) { reliability = r; return this; }
        public Builder heavy() { heavy = true; return this; }
        public Builder volume(double u) { volumeU = u; return this; }
        public Builder camera(double ifovMicroRad, double gbPerDay, double needsPointing, boolean night) {
            this.ifovMicroRad = ifovMicroRad; dataGbPerDay = gbPerDay; pointingNeed = needsPointing; seesAtNight = night;
            return this;
        }
        public Builder generates(double w) { generatesW = w; return this; }
        public Builder deployable() { deployable = true; return this; }
        public Builder stores(double wh) { batteryWh = wh; return this; }
        public Builder stability(double s) { stability = s; return this; }
        public Builder downlink(double mbps) { downlinkMbps = mbps; return this; }
        /** Una estructura de "units" U: CubeSat Design Specification, hasta 2 kg por U. */
        public Builder frame(double units) { frameVolumeU = units; frameMaxKg = 2.0 * units; return this; }
        public Part build() { return new Part(this); }
    }
}
