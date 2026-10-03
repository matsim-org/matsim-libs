package org.matsim.contrib.drt.extension.operations.shifts.schedule;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.common.util.reservation.ReservationManager;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.evrp.ChargingTask;

import java.util.Optional;

/**
 * An {@link OperationalStop} that takes place at a physical {@link OperationFacility}: the vehicle reserves the
 * facility for the duration of the stop and may charge there. Shift breaks, shift changeovers and waiting for a shift
 * are facility stops; an operational stop that happens wherever the vehicle currently is on the network (e.g. an
 * incident hold) is not and implements only the bare {@link OperationalStop} marker.
 *
 * @author nkuehnel / MOIA
 */
public interface FacilityStop extends OperationalStop {
    Id<OperationFacility> getFacilityId();

    Optional<Id<ReservationManager.Reservation>> getReservationId();

    /**
     * @return The charging task if this stop includes charging, empty otherwise
     */
    Optional<ChargingTask> getChargingTask();

    /**
     * Adds charging capability to this stop.
     *
     * @param chargingTask The charging task to add
     * @return true if charging was added successfully, false otherwise
     */
    boolean addCharging(ChargingTask chargingTask);
}