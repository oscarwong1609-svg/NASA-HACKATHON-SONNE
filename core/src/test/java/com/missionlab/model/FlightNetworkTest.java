package com.missionlab.model;

import com.missionlab.data.Catalog;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** El grafo de vuelo de Catalog: qué ruta es la más barata según el peso del satélite. */
class FlightNetworkTest {
    private static final FlightNetwork F = Catalog.FLIGHTS;

    @Test
    void graphHasALaunchSiteAndOneNodePerOrbit() {
        assertEquals(1 + Catalog.ORBITS.size(), F.graph().nodeCount());
        assertEquals(5, F.graph().edgeCount());
        assertTrue(F.launchSite().onGround());
    }

    @Test
    void nasaGivesSmallSatellitesAFreeRideToTheStation() {
        assertEquals("csli", F.cheapestRoute(Catalog.ISS_ORBIT, 4).id());
        assertEquals(0, F.cheapestRoute(Catalog.ISS_ORBIT, 4).costFor(4), 1e-9);
        // Por encima de 6 kg la NASA ya no lo lleva gratis: hay que pagar el despliegue.
        assertEquals("iss_deploy", F.cheapestRoute(Catalog.ISS_ORBIT, 7).id());
    }

    @Test
    void cheapestRouteToTheHighOrbitChangesWithMass() {
        // Ligero: viaje compartido + remolcador (90 + 45/kg) gana al viaje alto directo (130 + 30/kg).
        Route light = F.cheapestRoute(Catalog.SSO_800, 2);
        assertEquals("ride_sso+tug", light.id());
        assertEquals(180, light.costFor(2), 1e-9);
        assertEquals("Launch site > 550 km > 800 km", light.stops());
        assertEquals("ride_sso_high", F.cheapestRoute(Catalog.SSO_800, 4).id());
    }

    @Test
    void consoleListsFeasibleRoutesFirstCheapestFirst() {
        List<Route> r = F.routesFor(Catalog.ISS_ORBIT, 7);
        assertEquals(2, r.size());
        assertEquals("iss_deploy", r.get(0).id());
        assertFalse(r.get(1).carries(7));
    }

    @Test
    void nextRouteStartsWithDijkstraAndCycles() {
        Route first = F.nextRoute(null, Catalog.SSO_800, 2);
        assertEquals(F.cheapestRoute(Catalog.SSO_800, 2), first);
        Route second = F.nextRoute(first, Catalog.SSO_800, 2);
        assertEquals("ride_sso_high", second.id());
        assertEquals(first, F.nextRoute(second, Catalog.SSO_800, 2));
        assertEquals(second, F.stepRoute(first, Catalog.SSO_800, 2, -1));
    }

    @Test
    void routeCapacityIsItsSmallestVehicle() {
        Route tug = Catalog.route(Catalog.SSO_800, "ride_sso", "tug");
        assertEquals(Math.min(Catalog.RIDE_SSO.maxKg, Catalog.SPACE_TUG.maxKg), tug.capacityKg(), 1e-9);
        assertEquals(2, tug.legs().size());
        assertEquals(Catalog.RIDE_SSO, tug.launchVehicle());
        assertEquals(Catalog.SSO_800, tug.orbit());
        assertEquals(Catalog.RIDE_SSO.deliveryMonths, tug.deliveryMonths(), 1e-9);
    }

    @Test
    void unknownRoutesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> Catalog.route(Catalog.ISS_ORBIT, "tug"));
    }
}
