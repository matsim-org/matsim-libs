package org.matsim.contrib.drt.extension.operations.shifts.schedule;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.common.util.reservation.ReservationManager;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.drt.schedule.DrtTaskType;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.schedule.DefaultStayTask;
import org.matsim.contrib.evrp.ChargingTask;
import org.matsim.contrib.evrp.ETask;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.STOP;

/**
 * Unified implementation of WaitForShiftTask that can optionally handle electric vehicles.
 * This implementation replaces both the standard and electric-specific implementations.
 *
 * @author nkuehnel / MOIA
 */
public class WaitForShiftTask extends DefaultStayTask implements DrtStopTask, OperationalStop, ETask {

	public static final DrtTaskType TYPE = new DrtTaskType("WAIT_FOR_SHIFT", STOP);

    private final Id<OperationFacility> facilityId;
    private final Id<ReservationManager.Reservation> reservation;
    
    // Optional charging fields - null for non-electric vehicles
    private ChargingTask chargingTask;
    private double consumedEnergy = 0;

    /**
     * Constructor for creating a standard wait for shift task.
     */
    public WaitForShiftTask(double beginTime, double endTime, Link link, Id<OperationFacility> facilityId,
                            Id<ReservationManager.Reservation> reservation) {
        this(beginTime, endTime, link, facilityId, reservation, null, 0);
    }
    
    /**
     * Constructor for creating a wait for shift task with electric capabilities.
     */
    public WaitForShiftTask(double beginTime, double endTime, Link link, Id<OperationFacility> facilityId,
                          Id<ReservationManager.Reservation> reservation,
                          ChargingTask chargingTask, double consumedEnergy) {
        super(TYPE, beginTime, endTime, link);
        this.facilityId = facilityId;
        this.reservation = reservation;
        this.chargingTask = chargingTask;
        this.consumedEnergy = consumedEnergy;
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

    @Override
    public double calcLatestArrivalTime() {
        return Double.MAX_VALUE;
    }

    @Override
    public double calcEarliestArrivalTime() {
        return 0;
    }

    @Override
    public double calcEarliestDepartureTime() {
        return 0;
    }

    @Override
    public double calcLatestDepartureTime() {
        return Double.MAX_VALUE;
    }

    @Override
    public Id<OperationFacility> getFacilityId() {
        return facilityId;
    }

    @Override
    public Optional<Id<ReservationManager.Reservation>> getReservationId() {
        return Optional.ofNullable(reservation);
    }
    
    /**
     * @return The charging task if this wait task includes charging, empty otherwise
     */
    public Optional<ChargingTask> getChargingTask() {
        return Optional.ofNullable(chargingTask);
    }
    
    /**
     * Adds charging capability to this wait task
     * 
     * @param chargingTask The charging task to add
     * @return true if charging was added successfully, false otherwise
     */
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
