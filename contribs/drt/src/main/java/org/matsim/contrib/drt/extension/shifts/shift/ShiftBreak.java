package org.matsim.contrib.drt.extension.shifts.shift;

/**
 * @author nkuehnel, fzwick
 */
public interface ShiftBreak {

    double getEarliestBreakStartTime();

    double getLatestBreakEndTime();

    double getDuration();

    void schedule(double latestArrivalTime);

    boolean isScheduled();

    double getScheduledLatestArrival();

    void reset();
}
