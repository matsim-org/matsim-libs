package org.matsim.contrib.drt.extension.operations.shifts.shift;

import org.matsim.core.gbl.Gbl;

/**
 * @author nkuehnel, fzwick / MOIA
 */
public class DefaultShiftBreakImpl implements DrtShiftBreak {

    private final static double UNSCHEDULED_ARRIVAL_TIME = Double.NaN;

    private final double earliestBreakStartTime;
    private final double latestBreakEndTime;
    private final double duration;

    private double latestArrivalTime = UNSCHEDULED_ARRIVAL_TIME;

    public DefaultShiftBreakImpl(double earliestBreakStartTime, double latestBreakEndTime, double duration) {
        Gbl.assertIf(latestBreakEndTime - earliestBreakStartTime >= duration);
        if ((earliestBreakStartTime % 1) != 0 || (latestBreakEndTime % 1) != 0 || (duration % 1) != 0) {
            throw new RuntimeException("Cannot use fractions of seconds!");
        }
        this.earliestBreakStartTime = earliestBreakStartTime;
        this.latestBreakEndTime = latestBreakEndTime;
        this.duration = duration;
    }

    @Override
    public double getEarliestBreakStartTime() {
        return earliestBreakStartTime;
    }

    @Override
    public double getLatestBreakEndTime() {
        return latestBreakEndTime;
    }

    @Override
    public double getDuration() {
        return duration;
    }

    @Override
    public void schedule(double latestArrivalTime) {
        this.latestArrivalTime = latestArrivalTime;
    }

    @Override
    public boolean isScheduled() {
        return !Double.isNaN(latestArrivalTime);
    }

    @Override
    public double getScheduledLatestArrival() {
        return latestArrivalTime;
    }
}
