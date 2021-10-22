package org.matsim.contrib.drt.extension.shifts.shift;

import org.matsim.api.core.v01.Identifiable;

/**
 * @author nkuehnel / MOIA
 */
public interface DrtShiftBreakSpecification {

	double getEarliestBreakStartTime();

	double getLatestBreakEndTime();

	double getDuration();

}
