package org.matsim.contrib.drt.extension.operations.eshifts.schedule;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.WaitForShiftTask;
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
                                double consumedEnergy, OperationFacility facility,
                                ChargingTask chargingTask) {
        super(beginTime, endTime, link, facility);
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
