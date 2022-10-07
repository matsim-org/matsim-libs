package org.matsim.contrib.drt.extension.operations.eshifts.optimizer;

import com.google.inject.Provider;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.WaitForShiftStayTask;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.drt.extension.edrt.optimizer.EDrtVehicleDataEntryFactory;
import org.matsim.contrib.drt.extension.operations.shifts.fleet.ShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftEDrtVehicleDataEntryFactory implements VehicleEntry.EntryFactory {

    private final EDrtVehicleDataEntryFactory entryFactory;

    public ShiftEDrtVehicleDataEntryFactory(DrtConfigGroup drtCfg, double minSoc) {
        entryFactory = new EDrtVehicleDataEntryFactory(drtCfg, minSoc);
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
        return currentShift != null && !(currentTime > currentShift.getEndTime())
				&& !(dvrpVehicle.getSchedule().getCurrentTask() instanceof WaitForShiftStayTask)
				&& (currentShift.isStarted() && !currentShift.isEnded());
    }

    public static class ShiftEDrtVehicleDataEntryFactoryProvider implements Provider<ShiftEDrtVehicleDataEntryFactory> {
		private final DrtConfigGroup drtCfg;
		private final double minimumRelativeSoc;


        public ShiftEDrtVehicleDataEntryFactoryProvider(DrtConfigGroup drtCfg, double minimumRelativeSoc) {
			this.drtCfg = drtCfg;
            this.minimumRelativeSoc = minimumRelativeSoc;
        }

        @Override
        public ShiftEDrtVehicleDataEntryFactory get() {
            return new ShiftEDrtVehicleDataEntryFactory(drtCfg, minimumRelativeSoc);
        }
    }
}
