package org.matsim.dsim.simulation.net;

class StuckTimer {

    private final double stuckThreshold;

    private double timerStarted = Double.POSITIVE_INFINITY;

    StuckTimer(double stuckThreshold) {
        this.stuckThreshold = stuckThreshold;
    }

    void start(double now) {
        if (Double.isInfinite(timerStarted)) {
            timerStarted = now;
        }
    }

    void reset() {
        timerStarted = Double.POSITIVE_INFINITY;
    }

    boolean isStuck(double now) {
        return now - timerStarted >= stuckThreshold;
    }
}
