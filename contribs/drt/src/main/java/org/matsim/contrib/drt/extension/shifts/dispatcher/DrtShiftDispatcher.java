package org.matsim.contrib.drt.extension.shifts.dispatcher;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.shifts.fleet.ShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.shifts.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.shifts.schedule.ShiftBreakTask;
import org.matsim.contrib.drt.extension.shifts.shift.DrtShift;

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

    OperationFacility decideOnBreak(ShiftEntry activeShift);

    void endShift(ShiftDvrpVehicle vehicle, Id<Link> id);

    void endBreak(ShiftDvrpVehicle vehicle, ShiftBreakTask task);

    void startBreak(ShiftDvrpVehicle vehicle, Id<Link> linkId);
}
