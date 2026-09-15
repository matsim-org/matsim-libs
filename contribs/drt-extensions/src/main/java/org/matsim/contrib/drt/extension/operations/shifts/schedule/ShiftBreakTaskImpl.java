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
import org.matsim.contrib.evrp.ChargingTask;
import org.matsim.contrib.evrp.ETask;

import java.util.Map;
import java.util.Optional;

import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.STOP;

/**
 * Unified implementation of ShiftBreakTask that can optionally handle electric vehicles.
 * This implementation replaces both the standard and electric-specific implementations.
 *
 * @author nkuehnel / MOIA
 */
public class ShiftBreakTaskImpl extends DefaultStayTask implements ShiftBreakTask, ETask {

	public static final DrtTaskType TYPE = new DrtTaskType("SHIFT_BREAK", STOP);

	private final DrtShiftBreak shiftBreak;

	private final Id<OperationFacility> facilityId;
	private final Id<ReservationManager.Reservation> reservationId;

	private final DrtStopTask delegate;
	
	// Optional charging fields - null for non-electric vehicles
	private ChargingTask chargingTask;
	private double consumedEnergy = 0;

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

	public double calcEarliestArrivalTime() {
		return shiftBreak.getEarliestBreakStartTime();
	}

	@Override
	public double calcLatestArrivalTime() {
		return shiftBreak.getLatestBreakEndTime() - shiftBreak.getDuration();
	}

	@Override
	public double calcLatestDepartureTime() {
		return shiftBreak.getLatestBreakEndTime();
	}

	@Override
	public double calcEarliestDepartureTime() {
		return shiftBreak.getEarliestBreakStartTime() + shiftBreak.getDuration();
	}

	@Override
	public Id<OperationFacility> getFacilityId() {
		return facilityId;
	}

	@Override
	public Optional<Id<ReservationManager.Reservation>> getReservationId() {
		return Optional.ofNullable(reservationId);
	}
	
	@Override
	public Optional<ChargingTask> getChargingTask() {
		return Optional.ofNullable(chargingTask);
	}
	
	@Override
	public boolean addCharging(ChargingTask chargingTask) {
	    // Only allow adding charging if task is planned and no charging exists
	    if (this.getStatus() != TaskStatus.PERFORMED && this.chargingTask == null) {
	        this.chargingTask = chargingTask;
	        this.consumedEnergy = chargingTask.getTotalEnergy();
	        return true;
	    }
	    return false;
	}
	
	/**
	 * Removes charging capability from this task if it's still in the planned state.
	 * 
	 * @return true if charging was removed successfully, false otherwise
	 */
	public boolean removeCharging() {
	    if (this.getStatus() == TaskStatus.PLANNED && chargingTask != null) {
	        this.chargingTask = null;
	        this.consumedEnergy = 0;
	        return true;
	    }
	    return false;
	}
	
	@Override
	public double getTotalEnergy() {
	    return consumedEnergy;
	}

}
