/*
 * Copyright (C) 2022 MOIA GmbH - All Rights Reserved
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.drt.extension.operations.eshifts.dispatcher;

import org.matsim.contrib.drt.extension.operations.eshifts.fleet.EvShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.operations.eshifts.schedule.EDrtWaitForShiftTask;
import org.matsim.contrib.drt.extension.operations.shifts.config.ShiftsParams;
import org.matsim.contrib.drt.extension.operations.shifts.dispatcher.AssignShiftToVehicleLogic;
import org.matsim.contrib.drt.extension.operations.shifts.fleet.ShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.ev.fleet.Battery;

/**
 * @author nkuehnel
 */
public class EDrtAssignShiftToVehicleLogic implements AssignShiftToVehicleLogic {

	private final AssignShiftToVehicleLogic delegate;
	private final ShiftsParams drtShiftParams;

	public EDrtAssignShiftToVehicleLogic(AssignShiftToVehicleLogic delegate, ShiftsParams drtShiftParams) {
		this.delegate = delegate;
		this.drtShiftParams = drtShiftParams;
	}

	@Override
	public boolean canAssignVehicleToShift(ShiftDvrpVehicle vehicle, DrtShift shift) {

		// no, if charging
		if(vehicle.getSchedule().getStatus() == Schedule.ScheduleStatus.STARTED) {
			final Task currentTask = vehicle.getSchedule().getCurrentTask();
			if (currentTask instanceof EDrtWaitForShiftTask) {
				if (((EDrtWaitForShiftTask) currentTask).getChargingTask() != null) {
					if (currentTask.getEndTime() > shift.getStartTime()) {
						return false;
					}
				}
			}
		}

		// no, if below battery threshold
		if (vehicle instanceof EvShiftDvrpVehicle) {
			final Battery battery = ((EvShiftDvrpVehicle) vehicle).getElectricVehicle().getBattery();
			if (battery.getCharge() / battery.getCapacity() < drtShiftParams.shiftAssignmentBatteryThreshold) {
				return false;
			}
		}

		return delegate.canAssignVehicleToShift(vehicle, shift);
	}
}
