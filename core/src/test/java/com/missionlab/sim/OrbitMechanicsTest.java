package com.missionlab.sim;

import com.missionlab.data.Catalog;
import com.missionlab.model.Orbit;
import com.missionlab.sim.OrbitMechanics.Track;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** La física de órbitas contra números conocidos. */
class OrbitMechanicsTest {

    @Test
    void periodsMatchRealSatellites() {
        // La ISS da una vuelta en unos 92-93 minutos.
        assertEquals(92.8, OrbitMechanics.periodMinutes(Catalog.ISS_ORBIT), 0.5);
        assertEquals(7.66, OrbitMechanics.speedKmS(Catalog.ISS_ORBIT), 0.05);
        assertTrue(OrbitMechanics.periodMinutes(Catalog.SSO_800) > OrbitMechanics.periodMinutes(Catalog.SSO_550));
    }

    @Test
    void higherOrbitsSeeFarther() {
        assertTrue(OrbitMechanics.coverageRadiusDeg(Catalog.SSO_800) > OrbitMechanics.coverageRadiusDeg(Catalog.SSO_550));
        assertTrue(OrbitMechanics.coverageRadiusDeg(Catalog.SSO_550) > OrbitMechanics.coverageRadiusDeg(Catalog.ISS_ORBIT));
    }

    @Test
    void analyticEclipseMatchesTheSimulatedTrack() {
        for (Orbit o : Catalog.ORBITS) {
            for (double decl : new double[] {8, 20}) {
                Track t = new Track(o, decl, 0);
                int dark = 0, n = 0;
                for (double s = 0; s < 86400; s += 10, n++) if (!t.at(s).sunlit) dark++;
                double numeric = (double) dark / n * OrbitMechanics.periodMinutes(o);
                assertEquals(OrbitMechanics.eclipseMinutes(o, decl), numeric, 1.0, o.id);
            }
        }
    }

    @Test
    void groundTrackStaysWithinTheInclination() {
        for (Orbit o : Catalog.ORBITS) {
            Track t = new Track(o, 0, 0);
            double maxLat = 0;
            for (double s = 0; s < 86400; s += 30) maxLat = Math.max(maxLat, Math.abs(t.at(s).lat));
            assertEquals(o.maxLatitude, maxLat, 0.5, o.id);
        }
    }

    @Test
    void polarStationsHearPolarSatellitesMost() {
        double uni = OrbitMechanics.contactMinutesPerDay(Catalog.SSO_550, Catalog.station("gs_uni"));
        double polar = OrbitMechanics.contactMinutesPerDay(Catalog.SSO_550, Catalog.station("gs_polar"));
        assertTrue(polar > 3 * uni, "polar " + polar + " vs university " + uni);
        // Desde la ISS (51.6 grados) Svalbard y Fairbanks no la oyen.
        assertEquals(0, OrbitMechanics.contactMinutesPerDay(Catalog.ISS_ORBIT, Catalog.station("gs_polar")), 1e-9);
        assertTrue(OrbitMechanics.contactPassesPerDay(Catalog.SSO_550, Catalog.station("gs_polar")) >= 10);
    }

    @Test
    void sunIsOverheadAtTheSubsolarPoint() {
        double lon = OrbitMechanics.subsolarLongitude(3600);
        assertEquals(90, OrbitMechanics.sunElevationDeg(20, lon, 3600, 20), 1e-6);
        assertTrue(OrbitMechanics.sunElevationDeg(0, OrbitMechanics.wrapLon(lon + 180), 3600, 0) < -80);
        // En verano (declinación +20) el Sol no se pone a 75 grados norte.
        for (double t = 0; t < 86400; t += 1800) assertTrue(OrbitMechanics.sunElevationDeg(75, -40, t, 20) > 0);
    }

    @Test
    void longitudesWrap() {
        assertEquals(-170, OrbitMechanics.wrapLon(190), 1e-9);
        assertEquals(170, OrbitMechanics.wrapLon(-190), 1e-9);
        assertEquals(180, OrbitMechanics.wrapLon(180), 1e-9);
    }

    @Test
    void earthMaskKnowsLandFromSea() {
        assertTrue(com.missionlab.data.EarthMask.landAt(-8, -62));   // Amazonas
        assertTrue(com.missionlab.data.EarthMask.landAt(72, -40));   // Groenlandia
        assertTrue(!com.missionlab.data.EarthMask.landAt(0, -140));  // Pacífico
    }
}
