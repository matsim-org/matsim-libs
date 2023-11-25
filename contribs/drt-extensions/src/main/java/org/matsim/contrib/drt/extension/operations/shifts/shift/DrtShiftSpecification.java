package org.matsim.contrib.drt.extension.operations.shifts.shift;

import java.util.Optional;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;

/**
 * @author nkuehnel / MOIA
 */
public interface DrtShiftSpecification extends Identifiable<DrtShift> {

  double getStartTime();

  double getEndTime();

  Optional<DrtShiftBreakSpecification> getBreak();

  Optional<Id<OperationFacility>> getOperationFacilityId();
}
