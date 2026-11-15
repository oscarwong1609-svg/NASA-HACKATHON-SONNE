package com.missionlab.game;

import com.missionlab.data.Catalog;
import com.missionlab.ds.LinkedQueue;
import com.missionlab.model.BuildEvent;
import com.missionlab.model.Design;
import com.missionlab.model.Mission;
import com.missionlab.model.MissionRun;
import com.missionlab.model.Role;
import com.missionlab.sim.SimResult;
import com.missionlab.sim.SuccessTier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** El estado de la partida que pasa de una pantalla a otra. */
public final class Session {
    public int playerCount = 1;
    /** Empieza por la Mission 1. */
    public Mission mission = Catalog.MISSIONS.get(0);
    public Design design = new Design();
    public MissionRun run;
    public SimResult result;
    public long seed;
    public int attempt;
    public final Random rng = new Random();
    /**
     * La campaña de pruebas de este intento: una cola FIFO con los problemas en el orden en que se van a probar.
     * Cada problema sale una sola vez, en el orden programado.
     */
    private final LinkedQueue<BuildEvent> testCampaign = new LinkedQueue<>();
    /** Progreso de la campaña: el mejor nivel y la mejor nota de cada encargo en esta sesión. */
    private final Map<String, SuccessTier> bestTier = new HashMap<>();
    private final Map<String, Integer> bestScore = new HashMap<>();

    /** Empieza un encargo desde cero (plano vacío). */
    public void newMission() {
        design = new Design();
        attempt = 0;
        newRun();
    }

    /** Nuevo intento con el mismo diseño: se borran las decisiones y vuelven a salir problemas. */
    public void newRun() {
        run = new MissionRun(mission, design);
        scheduleTests();
        result = null;
        seed = rng.nextLong();
        attempt++;
    }

    /** Guarda el resultado final de un intento si mejora lo conseguido en ese encargo. */
    public void record(Mission m, SimResult r) {
        bestTier.merge(m.id, r.tier, (a, b) -> b.ordinal() > a.ordinal() ? b : a);
        bestScore.merge(m.id, r.score.total(), Math::max);
    }

    public SuccessTier bestTier(Mission m) {
        return bestTier.getOrDefault(m.id, SuccessTier.NONE);
    }

    /** La mejor nota en este encargo, o -1 si todavía no se ha jugado. */
    public int bestScore(Mission m) {
        return bestScore.getOrDefault(m.id, -1);
    }

    /** Papel del jugador, o null si juega solo (en solitario todo está abierto). */
    public Role roleOf(int playerIndex) {
        if (playerCount <= 1) return null;
        return Role.values()[playerIndex];
    }

    /** Un papel está ocupado si algún jugador lo tiene. Los estantes de papeles libres los usa cualquiera. */
    public boolean claimed(Role r) {
        return playerCount > 1 && r.ordinal() < playerCount;
    }

    public boolean canUse(int playerIndex, Role owner) {
        return !claimed(owner) || roleOf(playerIndex) == owner;
    }

    /** Las piezas pesadas solo necesitan dos personas si hay al menos dos jugadores. */
    public boolean twoPersonLifts() {
        return playerCount >= 2;
    }

    /**
     * Programa la campaña de pruebas: el orden se sortea al empezar cada intento (para que no salga
     * siempre lo mismo) y a partir de ahí es fijo: es una cola.
     */
    private void scheduleTests() {
        testCampaign.clear();
        List<BuildEvent> order = new ArrayList<>(Catalog.EVENTS);
        Collections.shuffle(order, rng);
        for (BuildEvent e : order) testCampaign.enqueue(e);
    }

    /**
     * La siguiente prueba de la campaña que tenga sentido con el diseño actual, o null si no queda ninguna.
     * Si la prueba del frente necesita una pieza que todavía no está montada (por ejemplo, la bisagra de los
     * paneles plegables), vuelve al final de la cola y se prueba la siguiente. Se da como mucho una vuelta.
     */
    public BuildEvent nextEvent() {
        for (int i = 0, n = testCampaign.size(); i < n; i++) {
            BuildEvent e = testCampaign.dequeue();
            if (e.canHappen(design)) return e;
            testCampaign.enqueue(e); // todavía no se puede probar: al final de la cola
        }
        return null;
    }
}
