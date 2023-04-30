package org.matsim.contrib.drt.extension.operations.eshifts.schedule;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.evrp.ChargingTask;
import org.matsim.contrib.evrp.ETask;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftBreakTask;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftBreakTaskImpl;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftBreak;

/**
 * @author nkuehnel / MOIA
 */
public class EDrtShiftBreakTaskImpl extends ShiftBreakTaskImpl implements ShiftBreakTask, ETask {

    private final double consumedEnergy;
    private final ChargingTask chargingTask;

    public EDrtShiftBreakTaskImpl(double beginTime, double endTime, Link link, DrtShiftBreak shiftBreak,
                                  double consumedEnergy, ChargingTask chargingTask, OperationFacility facility) {
        super(beginTime, endTime, link, shiftBreak, facility);
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
