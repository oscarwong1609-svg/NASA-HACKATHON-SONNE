package com.missionlab.sim;

import com.missionlab.model.Design;

/** Lets tests in other packages use SimulatorTest's reference designs. */
public final class SimulatorTestAccess {
    private SimulatorTestAccess() {}

    public static Design goodGreenland() {
        return SimulatorTest.goodGreenland();
    }

    public static Design goodAmazon() {
        return SimulatorTest.goodAmazon();
    }
}
