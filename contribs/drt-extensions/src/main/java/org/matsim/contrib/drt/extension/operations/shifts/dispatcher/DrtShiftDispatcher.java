package org.matsim.contrib.drt.extension.operations.shifts.dispatcher;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.operations.shifts.fleet.ShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftBreakTask;

/**
 * @author nkuehnel, fzwick / MOIA
 */
public interface DrtShiftDispatcher {

	final class ShiftEntry {
		public final DrtShift shift;
		public final ShiftDvrpVehicle vehicle;

		public ShiftEntry(DrtShift shift, ShiftDvrpVehicle vehicle) {
			this.shift = shift;
			this.vehicle = vehicle;
		}
	}

    void dispatch(double timeStep);

    void endShift(ShiftDvrpVehicle vehicle, Id<Link> id, Id<OperationFacility> operationFacilityId);

    void endBreak(ShiftDvrpVehicle vehicle, ShiftBreakTask task);

    void startBreak(ShiftDvrpVehicle vehicle, Id<Link> linkId);
}
