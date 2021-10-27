package org.matsim.contrib.drt.extension.shifts.schedule;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.drt.extension.shifts.operationFacilities.OperationFacility;

/**
 * @author nkuehnel / MOIA
 */
public class WaitForShiftStayTask extends DrtStayTask implements OperationalStop {

    private final OperationFacility facility;
    private DrtStopTask stopTask;

    public WaitForShiftStayTask(double beginTime, double endTime, Link link, OperationFacility facility) {
        super(beginTime, endTime, link);
        this.facility = facility;
    }

    @Override
    public OperationFacility getFacility() {
        return facility;
    }

}
