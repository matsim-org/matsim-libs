package org.matsim.contrib.drt.extension.operations.shifts.schedule;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.common.util.reservation.ReservationManager;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.evrp.ChargingTask;

import java.util.Optional;

/**
 * @author nkuehnel / MOIA
 */
public interface OperationalStop extends Task {
    Id<OperationFacility> getFacilityId();

    Optional<Id<ReservationManager.Reservation>> getReservationId();


    /**
     * @return The charging task if this changeover includes charging, empty otherwise
     */
    Optional<ChargingTask> getChargingTask();

    /**
     * Adds charging capability to this changeover task
     *
     * @param chargingTask The charging task to add
     * @return true if charging was added successfully, false otherwise
     */
    boolean addCharging(ChargingTask chargingTask);

}
