package org.matsim.contrib.drt.extension.operations.shifts.shift;

import org.matsim.core.gbl.Gbl;

/**
 * @author nkuehnel, fzwick / MOIA
 */
public class DefaultShiftBreakImpl implements DrtShiftBreak {


    private final double earliestBreakStartTime;
    private final double latestBreakEndTime;
    private final double duration;

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
}
