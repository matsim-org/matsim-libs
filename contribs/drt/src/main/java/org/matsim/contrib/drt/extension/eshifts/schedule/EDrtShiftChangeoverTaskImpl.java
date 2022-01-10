package org.matsim.contrib.drt.extension.eshifts.schedule;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.shifts.shift.DrtShift;
import org.matsim.contrib.drt.schedule.DefaultDrtStopTask;
import org.matsim.contrib.evrp.ChargingTask;
import org.matsim.contrib.evrp.ETask;
import org.matsim.contrib.drt.extension.shifts.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.shifts.schedule.ShiftChangeOverTask;

/**
 * @author nkuehnel / MOIA
 */
public class EDrtShiftChangeoverTaskImpl extends DefaultDrtStopTask implements ShiftChangeOverTask, ETask {

    private final DrtShift shift;
    private final double consumedEnergy;
    private final ChargingTask chargingTask;
    private final OperationFacility facility;

    public EDrtShiftChangeoverTaskImpl(double beginTime, double endTime, Link link,
                                       DrtShift shift, double consumedEnergy,
                                       ChargingTask chargingTask, OperationFacility facility) {
        super(beginTime, endTime, link);
        this.shift = shift;
        this.consumedEnergy = consumedEnergy;
        this.chargingTask = chargingTask;
        this.facility = facility;
    }

    @Override
    public DrtShift getShift() {
        return shift;
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
