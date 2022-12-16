package org.matsim.contrib.drt.extension.operations.shifts.optimizer;

import org.matsim.contrib.drt.extension.operations.shifts.fleet.ShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.OperationalStop;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.optimizer.SlackTimeCalculator;
import org.matsim.contrib.drt.optimizer.VehicleDataEntryFactoryImpl;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftVehicleDataEntryFactory implements VehicleEntry.EntryFactory {

	private final VehicleDataEntryFactoryImpl entryFactory;


	public ShiftVehicleDataEntryFactory(DrtConfigGroup drtCfg, SlackTimeCalculator slackTimeCalculator) {
		entryFactory = new VehicleDataEntryFactoryImpl(drtCfg, slackTimeCalculator);
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
