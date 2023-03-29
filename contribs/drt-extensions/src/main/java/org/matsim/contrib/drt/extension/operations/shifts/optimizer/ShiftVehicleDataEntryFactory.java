package org.matsim.contrib.drt.extension.operations.shifts.optimizer;

import com.google.inject.Provider;
import org.matsim.contrib.drt.extension.operations.shifts.fleet.ShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.OperationalStop;
import org.matsim.contrib.drt.optimizer.VehicleDataEntryFactoryImpl;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.core.config.Config;

import javax.inject.Inject;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftVehicleDataEntryFactory implements VehicleEntry.EntryFactory {

	private final VehicleEntry.EntryFactory entryFactory;


	public ShiftVehicleDataEntryFactory(VehicleEntry.EntryFactory delegate) {
		entryFactory = delegate;
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
		if(currentShift == null ||
				currentTime > currentShift.getEndTime() ||
				!currentShift.isStarted() ||
				currentShift.isEnded()) {
			return false;
		}
		return !(dvrpVehicle.getSchedule().getCurrentTask() instanceof OperationalStop);
	}
}
