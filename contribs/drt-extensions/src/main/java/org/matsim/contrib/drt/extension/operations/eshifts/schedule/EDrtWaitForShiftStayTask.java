package org.matsim.contrib.drt.extension.operations.eshifts.schedule;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.evrp.ChargingTask;
import org.matsim.contrib.evrp.ETask;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.WaitForShiftStayTask;

/**
 * @author nkuehnel / MOIA
 */
public class EDrtWaitForShiftStayTask extends WaitForShiftStayTask implements ETask {

    private final double consumedEnergy;

    private final ChargingTask chargingTask;

    public EDrtWaitForShiftStayTask(double beginTime, double endTime, Link link,
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
