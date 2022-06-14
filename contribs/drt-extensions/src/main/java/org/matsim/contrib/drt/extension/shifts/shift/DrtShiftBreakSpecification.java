package org.matsim.contrib.drt.extension.shifts.shift;

/**
 * @author nkuehnel / MOIA
 */
public interface DrtShiftBreakSpecification {

	double getEarliestBreakStartTime();

	double getLatestBreakEndTime();

	double getDuration();

}
