package org.matsim.contrib.shifts.schedule;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.shifts.operationFacilities.OperationFacility;
import org.matsim.contrib.shifts.shift.ShiftBreak;

/**
 * @author nkuehnel
 */
public class ShiftBreakTaskImpl extends DrtStopTask implements ShiftBreakTask {

    private final ShiftBreak shiftBreak;
    private final OperationFacility facility;

    public ShiftBreakTaskImpl(double beginTime, double endTime, Link link, ShiftBreak shiftBreak, OperationFacility facility) {
        super(beginTime, endTime, link);
        this.shiftBreak = shiftBreak;
        this.facility = facility;
    }

    @Override
    public ShiftBreak getShiftBreak() {
        return shiftBreak;
    }

    @Override
    public OperationFacility getFacility() {
        return facility;
    }
}
