package org.matsim.contrib.drt.extension.shifts.shift;

import org.matsim.api.core.v01.Identifiable;

/**
 * @author nkuehnel / MOIA
 */
public interface DrtShiftSpecification extends Identifiable<DrtShift> {

	double getStartTime();

	double getEndTime();

	DrtShiftBreakSpecification getBreak();
}
