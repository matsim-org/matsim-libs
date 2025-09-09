package org.matsim.contrib.drt.extension.operations.shifts.schedule;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.common.util.reservation.ReservationManager;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftBreak;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.drt.schedule.DefaultDrtStopTask;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.drt.schedule.DrtTaskType;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.schedule.DefaultStayTask;

import java.util.Map;
import java.util.Optional;

import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.STOP;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftBreakTaskImpl extends DefaultStayTask implements ShiftBreakTask {

	public static final DrtTaskType TYPE = new DrtTaskType("SHIFT_BREAK", STOP);

	private final DrtShiftBreak shiftBreak;

	private final Id<OperationFacility> facilityId;
	private final Id<ReservationManager.Reservation> reservationId;

	private final DrtStopTask delegate;

	public ShiftBreakTaskImpl(double beginTime, double endTime, Link link, DrtShiftBreak shiftBreak,
							  Id<OperationFacility> facilityId,
							  Id<ReservationManager.Reservation> reservationId) {
		super(TYPE, beginTime, endTime, link);
        this.facilityId = facilityId;
        this.reservationId = reservationId;
        this.delegate = new DefaultDrtStopTask(beginTime, endTime, link);
		this.shiftBreak = shiftBreak;
    }

    @Override
    public DrtShiftBreak getShiftBreak() {
        return shiftBreak;
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

	@Override
	public Id<OperationFacility> getFacilityId() {
		return facilityId;
	}

	@Override
	public Optional<Id<ReservationManager.Reservation>> getReservationId() {
		return Optional.ofNullable(reservationId);
	}
}
