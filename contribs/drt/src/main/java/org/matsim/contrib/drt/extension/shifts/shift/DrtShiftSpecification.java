package org.matsim.contrib.drt.extension.shifts.shift;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.contrib.drt.extension.shifts.operationFacilities.OperationFacility;

import java.util.Optional;

/**
 * @author nkuehnel / MOIA
 */
public interface DrtShiftSpecification extends Identifiable<DrtShift> {

	double getStartTime();

	double getEndTime();

	DrtShiftBreakSpecification getBreak();

	Optional<Id<OperationFacility>> getOperationFacilityId();
}
