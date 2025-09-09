package org.matsim.contrib.drt.extension.operations.eshifts.schedule;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.common.util.reservation.ReservationManager;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.WaitForShiftTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.evrp.ChargingTask;
import org.matsim.contrib.evrp.ETask;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;

/**
 * @author nkuehnel / MOIA
 */
public class EDrtWaitForShiftTask extends WaitForShiftTask implements ETask {

    private final double consumedEnergy;

    private final ChargingTask chargingTask;

    public EDrtWaitForShiftTask(double beginTime, double endTime, Link link,
                                double consumedEnergy, Id<OperationFacility> facilityId,
                                Id<ReservationManager.Reservation> reservationId,
                                ChargingTask chargingTask) {
        super(beginTime, endTime, link, facilityId, reservationId);
        this.consumedEnergy = consumedEnergy;
        this.chargingTask = chargingTask;
    }

    @Override
    public double getTotalEnergy() {
        return consumedEnergy;
    }

    public ChargingTask getChargingTask() {
        return chargingTask;
    }

}
