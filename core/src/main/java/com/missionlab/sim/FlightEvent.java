package com.missionlab.sim;

import com.missionlab.model.Orbit;

import java.util.Locale;

/**
 * An event of the Launch and Early Orbit Phase (LEOP), with the time
 * at which it happens. The simulator puts them in a queue (see SimResult.timeline()) in chronological order and the
 * launch screen dequeues them one by one. Imaging and data downlink are played later, in Mission Control.
 */
public final class FlightEvent {
    public enum Status { OK, WARN, FAILED }

    public enum Kind {
        CANCELLED(null),
        LIFTOFF(Stage.LIFTOFF),
        /** An intermediate leg of the route: one vehicle releases the satellite and another picks it up. */
        TRANSFER(null),
        ORBIT(Stage.ORBIT),
        PANELS(null),
        /** First radio contact: the launch phase ends and Mission Control begins. */
        CONTACT(Stage.CONTACT);

        /** The flight stage this event decides, or null if it is informational only. */
        public final Stage stage;

        Kind(Stage stage) {
            this.stage = stage;
        }
    }

    public final Kind kind;
    public final Status status;
    /** Hours since liftoff. */
    public final double hours;
    public final String text;
    /** Where the satellite is after this event (for drawing), or null if unchanged. */
    public final Orbit orbit;

    public FlightEvent(Kind kind, Status status, double hours, String text, Orbit orbit) {
        this.kind = kind;
        this.status = status;
        this.hours = hours;
        this.text = text;
        this.orbit = orbit;
    }

    public boolean failed() {
        return status == Status.FAILED;
    }

    /** Mission clock: "T-0", "T+1.5 h" or "Day 31". */
    public String clock() {
        if (kind == Kind.CANCELLED) return "--";
        if (hours <= 0) return "T-0";
        if (hours < 48) return String.format(Locale.ROOT, "T+%.1f h", hours);
        return String.format(Locale.ROOT, "Day %d", (int) (hours / 24) + 1);
    }

    @Override
    public String toString() {
        return clock() + " " + kind + " " + status + ": " + text;
    }
}
