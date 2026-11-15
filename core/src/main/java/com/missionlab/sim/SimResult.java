package com.missionlab.sim;

import com.missionlab.ds.LinkedQueue;
import com.missionlab.model.Role;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Todo lo que las pantallas de lanzamiento, Mission Control y resultado necesitan saber.
 *
 * El simulador calcula el resultado suponiendo que en Mission Control todo se hace a la perfección.
 * Después de jugar las operaciones, withOperations() ajusta la foto y la nota con lo que el equipo
 * consiguió de verdad: el diseño pone el techo y las operaciones deciden cuánto se alcanza.
 */
public final class SimResult {
    public final boolean launched;
    /** Cuántas etapas se superaron (0 a 4). 4 = misión completa. */
    public final int stagesPassed;
    /** La etapa que falló, o null si no falló ninguna (o si no despegó). */
    public final Stage failedAt;
    public final PhotoQuality photo;
    public final Score score;
    public final SuccessTier tier;
    /** Causas ordenadas de mayor a menor impacto; las buenas noticias van al final. */
    public final List<Finding> findings;
    public final Map<Role, Boolean> goals;
    public final Budgets budgets;
    /** Riesgos aceptados que sí ocurrieron. */
    public final List<String> struck;
    /** Parte de lo posible que consiguió Mission Control (1 = perfecto, o si aún no se ha jugado). */
    public final double operationsShare;
    /** Los sucesos del vuelo en orden cronológico. Privada: se entrega una copia para consumirla. */
    private final LinkedQueue<FlightEvent> timeline;

    SimResult(boolean launched, int stagesPassed, Stage failedAt, PhotoQuality photo, Score score,
              List<Finding> findings, Map<Role, Boolean> goals, Budgets budgets, List<String> struck,
              LinkedQueue<FlightEvent> timeline) {
        this(launched, stagesPassed, failedAt, photo, score, findings, goals, budgets, struck, timeline, 1.0);
    }

    private SimResult(boolean launched, int stagesPassed, Stage failedAt, PhotoQuality photo, Score score,
                      List<Finding> findings, Map<Role, Boolean> goals, Budgets budgets, List<String> struck,
                      LinkedQueue<FlightEvent> timeline, double operationsShare) {
        this.launched = launched;
        this.stagesPassed = stagesPassed;
        this.failedAt = failedAt;
        this.photo = photo;
        this.score = score;
        List<Finding> sorted = new ArrayList<>(findings);
        sorted.sort((a, b) -> {
            if (a.good != b.good) return a.good ? 1 : -1;
            return Double.compare(b.impact, a.impact);
        });
        this.findings = Collections.unmodifiableList(sorted);
        this.goals = Collections.unmodifiableMap(new EnumMap<>(goals));
        this.budgets = budgets;
        this.struck = Collections.unmodifiableList(new ArrayList<>(struck));
        this.timeline = new LinkedQueue<>(timeline);
        this.operationsShare = operationsShare;
        this.tier = SuccessTier.of(launched && failedAt == null, photo, score.total());
    }

    /**
     * Una copia nueva de la línea de tiempo del vuelo (cola FIFO), lista para ir sacando sucesos con dequeue().
     * Es una copia para que la pantalla de lanzamiento pueda vaciarla sin cambiar el resultado.
     */
    public LinkedQueue<FlightEvent> timeline() {
        return new LinkedQueue<>(timeline);
    }

    public boolean stagePassed(Stage s) {
        return stagesPassed > s.ordinal();
    }

    /** true si el satélite llegó vivo a las operaciones científicas (hay algo que jugar en Mission Control). */
    public boolean reachedOperations() {
        return launched && failedAt == null;
    }

    /** Las causas principales para el informe (como máximo n). */
    public List<Finding> topFindings(int n) {
        return findings.subList(0, Math.min(n, findings.size()));
    }

    /**
     * El resultado después de Mission Control.
     *
     * @param share parte de los datos que el equipo consiguió bajar, comparada con lo que habría
     *              conseguido un control perfecto con el mismo satélite (0..1)
     * @param note  una explicación para el informe (o null)
     */
    public SimResult withOperations(double share, Finding note) {
        double s = Math.max(0, Math.min(1, share));
        PhotoQuality p = photo.withCompleteness(photo.completeness * s);
        Score sc = new Score(score.science * s, score.margins, score.budget, score.reliability);
        List<Finding> f = new ArrayList<>(findings);
        if (note != null) f.add(note);
        Map<Role, Boolean> g = new EnumMap<>(goals);
        g.put(Role.GROUND, stagesPassed >= 3 && p.completeness >= SuccessTier.FULL_COMPLETENESS && budgets.budgetReserve() >= 0.10);
        return new SimResult(launched, stagesPassed, failedAt, p, sc, f, g, budgets, struck, timeline, s);
    }
}
