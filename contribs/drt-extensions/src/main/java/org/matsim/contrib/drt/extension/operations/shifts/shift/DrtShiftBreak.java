package org.matsim.contrib.drt.extension.operations.shifts.shift;

/**
 * @author nkuehnel, fzwick / MOIA
 */
public interface DrtShiftBreak {

    double getEarliestBreakStartTime();

    double getLatestBreakEndTime();

    double getDuration();

    void schedule(double latestArrivalTime);

    boolean isScheduled();

    double getScheduledLatestArrival();
}
