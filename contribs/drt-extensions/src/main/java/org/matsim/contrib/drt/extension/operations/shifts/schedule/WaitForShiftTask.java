package org.matsim.contrib.drt.extension.operations.shifts.schedule;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.drt.schedule.DrtTaskType;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.schedule.DefaultStayTask;

import java.util.Collections;
import java.util.Map;

import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.STOP;

/**
 * @author nkuehnel / MOIA
 */
public class WaitForShiftTask extends DefaultStayTask implements DrtStopTask, OperationalStop {

	public static final DrtTaskType TYPE = new DrtTaskType("WAIT_FOR_SHIFT", STOP);

	private final OperationFacility facility;

    public WaitForShiftTask(double beginTime, double endTime, Link link, OperationFacility facility) {
        super(TYPE, beginTime, endTime, link);
        this.facility = facility;
    }

    @Override
    public OperationFacility getFacility() {
        return facility;
    }

    @Override
    public Map<Id<Request>, AcceptedDrtRequest> getDropoffRequests() {
        return Collections.emptyMap();
    }

    @Override
    public Map<Id<Request>, AcceptedDrtRequest> getPickupRequests() {
        return Collections.emptyMap();
    }

    @Override
    public void addDropoffRequest(AcceptedDrtRequest request) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void addPickupRequest(AcceptedDrtRequest request) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void removePickupRequest(Id<Request> requestId) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public void removeDropoffRequest(Id<Request> requestId) {
        throw new RuntimeException("Not supported");
    }
}
