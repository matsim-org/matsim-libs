package org.matsim.contrib.drt.extension.operations.shifts.shift;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

import java.util.Optional;

/**
 * @author nkuehnel, fzwick / MOIA
 */
public interface DrtShift extends Identifiable<DrtShift>, Comparable<DrtShift> {

	double getStartTime();

	double getEndTime();

	boolean isStarted();

	boolean isEnded();

	void start();

	void end();

	Optional<Id<OperationFacility>> getOperationFacilityId();

	Optional<DrtShiftBreak> getBreak();

	Optional<Id<DvrpVehicle>> getDesignatedVehicleId();
}
