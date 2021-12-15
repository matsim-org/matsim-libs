package org.matsim.contrib.drt.extension.shifts.schedule;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.shifts.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.schedule.DrtTaskType;
import org.matsim.contrib.dvrp.schedule.DefaultStayTask;

import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.STAY;

/**
 * @author nkuehnel / MOIA
 */
public class WaitForShiftStayTask extends DefaultStayTask implements OperationalStop {

	public static final DrtTaskType TYPE = new DrtTaskType("WAIT_FOR_SHIFT", STAY);

	private final OperationFacility facility;

    public WaitForShiftStayTask(double beginTime, double endTime, Link link, OperationFacility facility) {
        super(TYPE, beginTime, endTime, link);
        this.facility = facility;
    }

    @Override
    public OperationFacility getFacility() {
        return facility;
    }

}
