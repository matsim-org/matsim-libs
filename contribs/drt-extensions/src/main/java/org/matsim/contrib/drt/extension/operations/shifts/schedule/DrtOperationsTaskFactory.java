package org.matsim.contrib.drt.extension.operations.shifts.schedule;

import com.google.common.base.Verify;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.common.util.reservation.ReservationManager;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilities;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilityReservationManager;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacilityUtils;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftBreak;
import org.matsim.contrib.drt.schedule.*;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.DefaultStayTask;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Task factory that creates operations-related tasks (shifts, breaks, etc.) using unified task implementations
 * that support both standard and electric vehicles. This implementation creates tasks without 
 * charging capabilities initially, which can be added later by a scheduler or other components.
 *
 * @author nkuehnel / MOIA
 */
public class DrtOperationsTaskFactory implements ShiftDrtTaskFactory {

    private final DrtTaskFactory delegate;
    private final OperationFacilities operationFacilities;
    private final OperationFacilityReservationManager reservationManager;

    public DrtOperationsTaskFactory(DrtTaskFactory delegate, OperationFacilities operationFacilities,
                                 OperationFacilityReservationManager reservationManager) {
        this.delegate = delegate;
        this.operationFacilities = operationFacilities;
        this.reservationManager = reservationManager;
    }

    @Override
    public DrtDriveTask createDriveTask(DvrpVehicle vehicle, VrpPathWithTravelData path, DrtTaskType drtTaskType) {
        return delegate.createDriveTask(vehicle, path, drtTaskType);
    }

    @Override
    public DrtStopTask createStopTask(DvrpVehicle vehicle, double beginTime, double endTime, Link link) {
        return delegate.createStopTask(vehicle, beginTime, endTime, link);
    }

    @Override
    public DrtStayTask createStayTask(DvrpVehicle vehicle, double beginTime, double endTime, Link link) {
        return delegate.createStayTask(vehicle, beginTime, endTime, link);
    }

    @Override
    public ShiftBreakTask createShiftBreakTask(DvrpVehicle vehicle, double beginTime, double endTime,
                                           Link link, DrtShiftBreak shiftBreak, Id<OperationFacility> facilityId,
                                           Id<ReservationManager.Reservation> reservationId) {
        // Create a standard shift break task without charging capabilities
        return new ShiftBreakTaskImpl(beginTime, endTime, link, shiftBreak, facilityId, reservationId);
    }

    @Override
    public ShiftChangeOverTask createShiftChangeoverTask(DvrpVehicle vehicle, double beginTime, double endTime,
                                                     Link link, DrtShift shift, Id<OperationFacility> facilityId,
                                                     Id<ReservationManager.Reservation> reservationId) {
        // Create a standard shift changeover task without charging capabilities
        return new ShiftChangeoverTaskImpl(beginTime, endTime, link, shift, facilityId, reservationId);
    }

    @Override
    public WaitForShiftTask createWaitForShiftStayTask(DvrpVehicle vehicle, double beginTime, double endTime,
                                                   Link link, Id<OperationFacility> facilityId,
                                                   Id<ReservationManager.Reservation> reservationId) {
        // Create a standard wait for shift task without charging capabilities
        return new WaitForShiftTask(beginTime, endTime, link, facilityId, reservationId);
    }

    public DefaultStayTask createInitialTask(DvrpVehicle vehicle, double beginTime, double endTime, Link link) {
        try {
            OperationFacility facility = OperationFacilityUtils.getFacilityForLink(operationFacilities, vehicle.getStartLink().getId())
                    .orElseThrow((Supplier<Throwable>) () -> new RuntimeException("Vehicles must start at an operation facility!"));
            Optional<ReservationManager.ReservationInfo<OperationFacility, DvrpVehicle>> reservation =
                    reservationManager.addReservation(facility, vehicle, beginTime, endTime);

            Verify.verify(reservation.isPresent(), "Could not register %s vehicle at facility %s during start up!", vehicle.getId(), facility.getId());

            facility.register(vehicle.getId());
            return createWaitForShiftStayTask(vehicle, vehicle.getServiceBeginTime(), vehicle.getServiceEndTime(),
                    vehicle.getStartLink(), facility.getId(), reservation.get().reservationId());

        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}