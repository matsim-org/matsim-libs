package org.matsim.contrib.drt.extension.operations.shifts.schedule;

import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.STOP;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.drt.schedule.DefaultDrtStopTask;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.drt.schedule.DrtTaskType;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.schedule.DefaultStayTask;

/**
 * A task representing stopping and waiting for a new shift.
 * @author nkuehnel / MOIA
 */
public class ShiftChangeoverTaskImpl extends DefaultStayTask implements ShiftChangeOverTask {

	public static final DrtTaskType TYPE = new DrtTaskType("SHIFT_CHANGEOVER", STOP);

	private final DrtShift shift;
	private final OperationFacility facility;

	private final DrtStopTask delegate;

	public ShiftChangeoverTaskImpl(double beginTime, double endTime, Link link, DrtShift shift, OperationFacility facility) {
		super(TYPE, beginTime, endTime, link);
		this.delegate = new DefaultDrtStopTask(beginTime, endTime, link);
		this.shift = shift;
		this.facility = facility;
	}

	@Override
	public DrtShift getShift() {
		return shift;
	}

	@Override
	public OperationFacility getFacility() {
		return facility;
	}

	@Override
	public Map<Id<Request>, AcceptedDrtRequest> getDropoffRequests() {
		return delegate.getDropoffRequests();
	}

	@Override
	public Map<Id<Request>, AcceptedDrtRequest> getPickupRequests() {
		return delegate.getPickupRequests();
	}

	@Override
	public void addDropoffRequest(AcceptedDrtRequest request) {
		delegate.addDropoffRequest(request);
	}

	@Override
	public void addPickupRequest(AcceptedDrtRequest request) {
		delegate.addPickupRequest(request);
	}

	@Override
	public void removePickupRequest(Id<Request> requestId) {
		delegate.removePickupRequest(requestId);
	}

	@Override
	public void removeDropoffRequest(Id<Request> requestId) {
		delegate.removeDropoffRequest(requestId);
	}
}

