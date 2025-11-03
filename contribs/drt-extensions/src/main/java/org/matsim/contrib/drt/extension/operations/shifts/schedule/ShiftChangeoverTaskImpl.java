package org.matsim.contrib.drt.extension.operations.shifts.schedule;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.common.util.reservation.ReservationManager;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
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
 * Unified implementation of ShiftChangeOverTask that can optionally handle electric vehicles.
 * This implementation replaces both the standard and electric-specific implementations.
 *
 * @author nkuehnel / MOIA
 */
public class ShiftChangeoverTaskImpl extends DefaultStayTask implements ShiftChangeOverTask, ETask {

	public static final DrtTaskType TYPE = new DrtTaskType("SHIFT_CHANGEOVER", STOP);

	private final DrtShift shift;
	private final Id<OperationFacility> facilityId;
	private final Id<ReservationManager.Reservation> reservationId;

	private final DrtStopTask delegate;
	
	// Optional charging fields - null for non-electric vehicles
	private ChargingTask chargingTask;
	private double consumedEnergy = 0;

	/**
	 * Constructor for creating a standard shift changeover task.
	 */
	public ShiftChangeoverTaskImpl(double beginTime, double endTime, Link link, DrtShift shift,
								   Id<OperationFacility> facilityId, Id<ReservationManager.Reservation> reservationId) {
		this(beginTime, endTime, link, shift, facilityId, reservationId, null, 0);
	}
	
	/**
	 * Constructor for creating a shift changeover task with electric capabilities.
	 */
	public ShiftChangeoverTaskImpl(double beginTime, double endTime, Link link, DrtShift shift,
								   Id<OperationFacility> facilityId, Id<ReservationManager.Reservation> reservationId,
								   ChargingTask chargingTask, double consumedEnergy) {
		super(TYPE, beginTime, endTime, link);
        this.delegate = new DefaultDrtStopTask(beginTime, endTime, link);
		this.shift = shift;
        this.facilityId = facilityId;
		this.reservationId = reservationId;
		this.chargingTask = chargingTask;
		this.consumedEnergy = consumedEnergy;
	}

	@Override
	public DrtShift getShift() {
		return shift;
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
	
	@Override
	public Optional<ChargingTask> getChargingTask() {
		return Optional.ofNullable(chargingTask);
	}
	
	@Override
	public boolean addCharging(ChargingTask chargingTask) {
		// Only allow adding charging if task is planned and no charging exists
		if (this.getStatus() == TaskStatus.PLANNED && this.chargingTask == null) {
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

