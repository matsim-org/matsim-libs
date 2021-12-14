package org.matsim.contrib.drt.extension.shifts.schedule;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.drt.extension.shifts.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.shifts.shift.DrtShiftBreak;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftBreakTaskImpl extends DrtStopTask implements ShiftBreakTask {

    private final DrtShiftBreak shiftBreak;
    private final OperationFacility facility;

    public ShiftBreakTaskImpl(double beginTime, double endTime, Link link, DrtShiftBreak shiftBreak, OperationFacility facility) {
        super(beginTime, endTime, link);
        this.shiftBreak = shiftBreak;
        this.facility = facility;
    }

    @Override
    public DrtShiftBreak getShiftBreak() {
        return shiftBreak;
    }

    @Override
    public OperationFacility getFacility() {
        return facility;
    }
}
