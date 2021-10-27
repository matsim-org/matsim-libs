package org.matsim.contrib.drt.extension.eshifts.schedule;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.evrp.ChargingTask;
import org.matsim.contrib.evrp.ETask;
import org.matsim.contrib.drt.extension.shifts.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.shifts.schedule.ShiftChangeOverTask;

/**
 * @author nkuehnel / MOIA
 */
public class EDrtShiftChangeoverTaskImpl extends DrtStopTask implements ShiftChangeOverTask, ETask {

    private final double shiftEndTime;
    private final double consumedEnergy;
    private final ChargingTask chargingTask;
    private final OperationFacility facility;

    public EDrtShiftChangeoverTaskImpl(double beginTime, double endTime, Link link,
                                       double latestArrivalTime, double consumedEnergy,
                                       ChargingTask chargingTask, OperationFacility facility) {
        super(beginTime, endTime, link);
        this.shiftEndTime = latestArrivalTime;
        this.consumedEnergy = consumedEnergy;
        this.chargingTask = chargingTask;
        this.facility = facility;
    }

    @Override
    public double getShiftEndTime() {
        return shiftEndTime;
    }

    @Override
    public double getTotalEnergy() {
        return consumedEnergy;
    }

    public ChargingTask getChargingTask() {
        return chargingTask;
    }

    @Override
    public OperationFacility getFacility() {
        return facility;
    }
}
