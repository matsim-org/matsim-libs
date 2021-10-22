package org.matsim.contrib.drt.extension.eshifts.charging;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleLookup;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.ev.discharging.AuxDischargingHandler;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.evrp.OperatingVehicleProvider;
import org.matsim.contrib.drt.extension.shifts.schedule.OperationalStop;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftOperatingVehicleProvider implements AuxDischargingHandler.VehicleProvider {

    private final DvrpVehicleLookup dvrpVehicleLookup;
    private final AuxDischargingHandler.VehicleProvider delegate;

    @Inject
    public ShiftOperatingVehicleProvider(DvrpVehicleLookup dvrpVehicleLookup) {
        this.dvrpVehicleLookup = dvrpVehicleLookup;
        delegate = new OperatingVehicleProvider(dvrpVehicleLookup);
    }

    @Override
    public ElectricVehicle getVehicle(ActivityStartEvent event) {
        //assumes driverId == vehicleId
        DvrpVehicle vehicle = dvrpVehicleLookup.lookupVehicle(Id.create(event.getPersonId(), DvrpVehicle.class));
        if(vehicle != null) {
            if (vehicle.getSchedule().getStatus() == Schedule.ScheduleStatus.STARTED && vehicle.getSchedule().getStatus()
                    != Schedule.ScheduleStatus.COMPLETED) {
                if (vehicle.getSchedule().getCurrentTask() instanceof OperationalStop) {
                    return null;
                }
            }
        }
        return delegate.getVehicle(event);
    }
}
