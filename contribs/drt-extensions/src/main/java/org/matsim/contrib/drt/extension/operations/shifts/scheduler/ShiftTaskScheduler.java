package org.matsim.contrib.drt.extension.operations.shifts.scheduler;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.operations.shifts.fleet.ShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.schedule.DrtTaskType;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.util.LinkTimePair;

import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.DRIVE;

public interface ShiftTaskScheduler {
	DrtTaskType RELOCATE_VEHICLE_SHIFT_BREAK_TASK_TYPE = new DrtTaskType("RELOCATE_SHIFT_BREAK", DRIVE);
	DrtTaskType RELOCATE_VEHICLE_SHIFT_CHANGEOVER_TASK_TYPE = new DrtTaskType("RELOCATE_SHIFT_CHANGEOVER", DRIVE);

	void relocateForBreak(ShiftDvrpVehicle vehicle, OperationFacility breakFacility, DrtShift shift);

	void relocateForShiftChange(DvrpVehicle vehicle, Link link, DrtShift shift, OperationFacility breakFacility);

	void startShift(ShiftDvrpVehicle vehicle, double now, DrtShift shift);

	boolean updateShiftChange(ShiftDvrpVehicle vehicle, Link link, DrtShift shift,
							  LinkTimePair start, OperationFacility facility, Task lastTask);

	void planAssignedShift(ShiftDvrpVehicle vehicle, double timeStep, DrtShift shift);

	void cancelAssignedShift(ShiftDvrpVehicle vehicle, double timeStep, DrtShift shift);
}
