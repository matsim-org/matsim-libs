package org.matsim.contrib.drt.extension.operations.shifts.shift;

/**
 * @author nkuehnel / MOIA
 */
public interface DrtShiftBreakSpecification {

	double getEarliestBreakStartTime();

	double getLatestBreakEndTime();

	double getDuration();

}
