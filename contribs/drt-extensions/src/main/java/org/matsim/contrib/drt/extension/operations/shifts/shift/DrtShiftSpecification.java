package org.matsim.contrib.drt.extension.operations.shifts.shift;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

import java.util.Optional;

/**
 * @author nkuehnel / MOIA
 */
public interface DrtShiftSpecification extends Identifiable<DrtShift> {

	/**
	 * The shift's start time
	 */
	double getStartTime();

	/**
	 * The shift's end time.
	 */
	double getEndTime();

	/**
	 * Returns an optional break during the shift.
	 */
	Optional<DrtShiftBreakSpecification> getBreak();

	/**
	 * Indicates whether the shift should start and end at a specific operation facility
	 */
	Optional<Id<OperationFacility>> getOperationFacilityId();

	/**
	 * Indicates whether the shift is already predefined to operate an a given vehicle
	 */
    Optional<Id<DvrpVehicle>> getDesignatedVehicleId();

	/**
	 * Type may be used to distinguish various specifications of the shift, e.g., for specifically trained drivers/operators
	 */
	Optional<String> getShiftType();
}
