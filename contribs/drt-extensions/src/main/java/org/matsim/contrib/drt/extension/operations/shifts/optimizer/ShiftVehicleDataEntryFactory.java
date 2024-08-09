package org.matsim.contrib.drt.extension.operations.shifts.optimizer;

import org.matsim.contrib.drt.extension.operations.shifts.fleet.ShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.OperationalStop;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.WaitForShiftTask;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftVehicleDataEntryFactory implements VehicleEntry.EntryFactory {

	private final VehicleEntry.EntryFactory entryFactory;

	private final boolean considerUpcomingShifts;


	public ShiftVehicleDataEntryFactory(VehicleEntry.EntryFactory delegate, boolean considerUpcomingShifts) {
		entryFactory = delegate;
        this.considerUpcomingShifts = considerUpcomingShifts;
    }

	@Override
	public VehicleEntry create(DvrpVehicle vehicle, double currentTime) {
		if (!isEligibleForRequestInsertion(vehicle, currentTime)) {
			return null;
		} else {
			return entryFactory.create(vehicle, currentTime);
		}
	}


	public boolean isEligibleForRequestInsertion(DvrpVehicle dvrpVehicle, double currentTime) {
		final DrtShift currentShift = ((ShiftDvrpVehicle) dvrpVehicle).getShifts().peek();

		// no shift assigned
		if (currentShift == null) {
            return false;
        }

		if(currentShift.isStarted()) {
			if(currentTime > currentShift.getEndTime()) {
				return false;
			}
			// do not insert into operational stops such as breaks
			return !(dvrpVehicle.getSchedule().getCurrentTask() instanceof OperationalStop);
		} else {
			// upcoming shift assigned but not started yet. Only consider vehicles already waiting for shift start
            return considerUpcomingShifts && dvrpVehicle.getSchedule().getCurrentTask() instanceof WaitForShiftTask;
		}
	}
}
