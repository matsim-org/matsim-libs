package org.matsim.contrib.drt.extension.operations.shifts.schedule;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.common.util.reservation.ReservationManager;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.dvrp.schedule.Task;

import java.util.Optional;

/**
 * @author nkuehnel / MOIA
 */
public interface OperationalStop extends Task {
    Id<OperationFacility> getFacilityId();

    Optional<Id<ReservationManager.Reservation>> getReservationId();

}
