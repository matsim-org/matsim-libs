package org.matsim.contrib.drt.extension.operations.shifts.schedule;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.common.util.reservation.ReservationManager;
import org.matsim.contrib.drt.extension.operations.remoteoperations.schedule.IncidentHoldTask;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftBreak;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

/**
 * @author nkuehnel / MOIA
 */
public interface DrtOperationsTaskFactory extends DrtTaskFactory {

    ShiftBreakTask createShiftBreakTask(DvrpVehicle vehicle, double beginTime, double endTime, Link link,
                                        DrtShiftBreak shiftBreak, Id<OperationFacility> facilityId,
                                        Id<ReservationManager.Reservation> reservationId);

    ShiftChangeOverTask createShiftChangeoverTask(DvrpVehicle vehicle, double beginTime, double endTime,
                                                  Link link, DrtShift shift, Id<OperationFacility> facilityId,
                                                  Id<ReservationManager.Reservation> reservationId);

    WaitForShiftTask createWaitForShiftStayTask(DvrpVehicle vehicle, double beginTime, double endTime, Link link,
                                                Id<OperationFacility> facilityId,
                                                Id<ReservationManager.Reservation> reservationId);

    /**
     * Creates a remote-guidance incident hold task (vehicle held in place until an operator processes the incident).
     * A held vehicle is stationary, so only time-dependent auxiliary energy accrues over the hold (no drive energy);
     * for an electric vehicle the factory sets that auxiliary consumption, for a non-electric vehicle it is zero.
     */
    IncidentHoldTask createIncidentHoldTask(DvrpVehicle vehicle, double beginTime, double endTime, Link link);
}
