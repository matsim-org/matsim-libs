package org.matsim.contrib.drt.extension.shifts.optimizer;

import com.google.inject.Provider;
import org.matsim.contrib.drt.extension.shifts.schedule.OperationalStop;
import org.matsim.contrib.drt.optimizer.VehicleDataEntryFactoryImpl;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.drt.extension.shifts.fleet.ShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.shifts.shift.DrtShift;
import org.matsim.core.config.Config;

import javax.inject.Inject;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftVehicleDataEntryFactory implements VehicleEntry.EntryFactory {

	private final VehicleDataEntryFactoryImpl entryFactory;


	public ShiftVehicleDataEntryFactory(DrtConfigGroup drtCfg) {
		entryFactory = new VehicleDataEntryFactoryImpl(drtCfg);
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

	public static class ShiftVehicleDataEntryFactoryProvider implements Provider<ShiftVehicleDataEntryFactory> {

		@Inject
		private Config config;

		public ShiftVehicleDataEntryFactoryProvider() {
		}

		@Override
		public ShiftVehicleDataEntryFactory get() {
			return new ShiftVehicleDataEntryFactory(DrtConfigGroup.getSingleModeDrtConfig(config));
		}
	}
}
