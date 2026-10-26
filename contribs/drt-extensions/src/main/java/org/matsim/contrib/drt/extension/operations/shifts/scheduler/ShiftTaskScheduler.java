package org.matsim.contrib.drt.extension.operations.shifts.scheduler;

import org.matsim.contrib.common.util.reservation.ReservationManager;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.operations.shifts.dispatcher.DrtShiftDispatcher;
import org.matsim.contrib.drt.extension.operations.shifts.fleet.ShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.OperationalStop;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.schedule.DrtTaskType;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.Task;

import java.util.Optional;

import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.DRIVE;

public interface ShiftTaskScheduler {
	DrtTaskType RELOCATE_VEHICLE_SHIFT_BREAK_TASK_TYPE = new DrtTaskType("RELOCATE_SHIFT_BREAK", DRIVE);
	DrtTaskType RELOCATE_VEHICLE_SHIFT_CHANGEOVER_TASK_TYPE = new DrtTaskType("RELOCATE_SHIFT_CHANGEOVER", DRIVE);

	boolean updateWaitingVehicleWithCharging(ShiftDvrpVehicle vehicle, double now);

	record OperationalStopUpdate(OperationalStop old, OperationalStop updated){}


	void startShift(ShiftDvrpVehicle vehicle, double now, DrtShift shift);

	/**
	 * Reshapes the schedule tail to route the vehicle to the reserved facility and end the shift there.
	 *
	 * @param changeoverStartTime the time the changeover begins at the hub (clamped to the arrival time): pass
	 *                            {@code shift.getEndTime()} for a regular end-of-shift reschedule, or {@code now} to end
	 *                            the shift early. {@code shift.getEndTime()} is never mutated either way.
	 */
	boolean updateShiftChange(ShiftDvrpVehicle vehicle, VrpPathWithTravelData vrpPath, DrtShift shift,
							  ReservationManager.ReservationInfo<OperationFacility, DvrpVehicle> reservation,
							  Task lastTask, double changeoverStartTime);

	boolean updateShiftBreak(DrtShiftDispatcher.ShiftEntry activeShift, double now);

	void cancelAssignedShift(ShiftDvrpVehicle vehicle, double timeStep, DrtShift shift);
}
