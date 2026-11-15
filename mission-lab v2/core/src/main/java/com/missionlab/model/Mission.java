package com.missionlab.model;

import java.util.List;

/**
 * Un encargo científico: la pregunta, la zona, el dinero, la fecha del cohete y qué cuenta como éxito.
 * Los dos encargos del juego tienen exactamente la misma estructura; solo cambian los números.
 */
public final class Mission {
    public final String id;
    /** Orden en la campaña: Mission 1, Mission 2... */
    public final int number;
    public final String title;
    public final String brief;
    public final String targetName;
    /** Punto que se marca en los mapas (por ejemplo, el glaciar Jakobshavn). */
    public final double targetLatitude;
    public final double targetLongitude;
    /** Zona que hay que fotografiar (grados). Una pasada por encima de esta caja cuenta como pasada útil. */
    public final double regionLatMin, regionLatMax, regionLonMin, regionLonMax;
    /** true = el objetivo solo se ve de noche (por ejemplo, incendios). */
    public final boolean night;
    public final double budgetK;
    /** Meses hasta que sale el cohete. */
    public final double monthsToLaunch;
    public final double missionYears;
    /** Resolución en el suelo que piden los científicos (metros por píxel). Más fino no da más puntos. */
    public final double requiredGsdM;
    /** Para qué hace falta esa resolución, en palabras. */
    public final String resolutionReason;
    /** Declinación del Sol en la estación del encargo (grados): decide día, noche y eclipses. */
    public final double sunDeclinationDeg;
    /** Parte del tiempo que la cámara está haciendo fotos (0..1). */
    public final double imagingDuty;
    /** Archivo en assets/photos/. Si no existe, el juego dibuja una imagen provisional. */
    public final String photoFile;
    /** Texto de crédito de la foto real de NASA (rellenar al guardar la imagen). */
    public final String photoCredit;
    /** Misión real de NASA parecida, para la tarjeta final. */
    public final String similarMission;
    /** Datos reales para la sesión informativa y la pantalla final. */
    public final List<String> facts;

    private Mission(Builder b) {
        id = b.id; number = b.number; title = b.title; brief = b.brief; targetName = b.targetName;
        targetLatitude = b.targetLat; targetLongitude = b.targetLon;
        regionLatMin = b.latMin; regionLatMax = b.latMax; regionLonMin = b.lonMin; regionLonMax = b.lonMax;
        night = b.night; budgetK = b.budgetK; monthsToLaunch = b.monthsToLaunch; missionYears = b.missionYears;
        requiredGsdM = b.requiredGsdM; resolutionReason = b.resolutionReason; sunDeclinationDeg = b.sunDeclinationDeg;
        imagingDuty = b.imagingDuty; photoFile = b.photoFile; photoCredit = b.photoCredit;
        similarMission = b.similarMission; facts = List.copyOf(b.facts);
    }

    public static Builder builder(String id, int number, String title) {
        return new Builder(id, number, title);
    }

    /** true si el punto está dentro de la zona del encargo. */
    public boolean inRegion(double lat, double lon) {
        return lat >= regionLatMin && lat <= regionLatMax && lon >= regionLonMin && lon <= regionLonMax;
    }

    @Override
    public String toString() {
        return title;
    }

    public static final class Builder {
        private final String id;
        private final int number;
        private final String title;
        private String brief = "", targetName = "", resolutionReason = "", photoFile = "", photoCredit = "", similarMission = "";
        private double targetLat, targetLon, latMin, latMax, lonMin, lonMax;
        private boolean night;
        private double budgetK, monthsToLaunch, missionYears, requiredGsdM, sunDeclinationDeg, imagingDuty = 0.2;
        private List<String> facts = List.of();

        private Builder(String id, int number, String title) {
            this.id = id; this.number = number; this.title = title;
        }

        public Builder brief(String text) { brief = text; return this; }
        public Builder target(String name, double lat, double lon) { targetName = name; targetLat = lat; targetLon = lon; return this; }
        public Builder region(double latMin, double latMax, double lonMin, double lonMax) {
            this.latMin = latMin; this.latMax = latMax; this.lonMin = lonMin; this.lonMax = lonMax; return this;
        }
        public Builder atNight() { night = true; return this; }
        public Builder budget(double k) { budgetK = k; return this; }
        public Builder launchInMonths(double m) { monthsToLaunch = m; return this; }
        public Builder lasts(double years) { missionYears = years; return this; }
        public Builder resolution(double gsdM, String why) { requiredGsdM = gsdM; resolutionReason = why; return this; }
        public Builder season(double sunDeclinationDeg) { this.sunDeclinationDeg = sunDeclinationDeg; return this; }
        public Builder imagingDuty(double duty) { imagingDuty = duty; return this; }
        public Builder photo(String file, String credit) { photoFile = file; photoCredit = credit; return this; }
        public Builder similarTo(String realMission) { similarMission = realMission; return this; }
        public Builder facts(String... lines) { facts = List.of(lines); return this; }
        public Mission build() { return new Mission(this); }
    }
}
