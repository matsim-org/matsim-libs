/*
 * Copyright (C) 2022 MOIA GmbH - All Rights Reserved
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.drt.extension.eshifts.dispatcher;

import org.matsim.contrib.drt.extension.eshifts.fleet.EvShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.eshifts.schedule.EDrtWaitForShiftStayTask;
import org.matsim.contrib.drt.extension.shifts.config.DrtShiftParams;
import org.matsim.contrib.drt.extension.shifts.dispatcher.AssignShiftToVehicleLogic;
import org.matsim.contrib.drt.extension.shifts.fleet.ShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.shifts.shift.DrtShift;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.ev.fleet.Battery;

/**
 * @author nkuehnel
 */
public class EDrtAssignShiftToVehicleLogic implements AssignShiftToVehicleLogic {

	private final AssignShiftToVehicleLogic delegate;
	private final DrtShiftParams drtShiftParams;

	public EDrtAssignShiftToVehicleLogic(AssignShiftToVehicleLogic delegate, DrtShiftParams drtShiftParams) {
		this.delegate = delegate;
		this.drtShiftParams = drtShiftParams;
	}


	@Override
	public boolean canAssignVehicleToShift(ShiftDvrpVehicle vehicle, DrtShift shift) {

		// no, if charging
		if(vehicle.getSchedule().getStatus() == Schedule.ScheduleStatus.STARTED) {
			final Task currentTask = vehicle.getSchedule().getCurrentTask();
			if (currentTask instanceof EDrtWaitForShiftStayTask) {
				if (((EDrtWaitForShiftStayTask) currentTask).getChargingTask() != null) {
					if (currentTask.getEndTime() > shift.getStartTime()) {
						return false;
					}
				}
			}
		}

		// no, if below battery threshold
		if (vehicle instanceof EvShiftDvrpVehicle) {
			final Battery battery = ((EvShiftDvrpVehicle) vehicle).getElectricVehicle().getBattery();
			if (battery.getSoc() / battery.getCapacity() < drtShiftParams.getShiftAssignmentBatteryThreshold()) {
				return false;
			}
		}

		return delegate.canAssignVehicleToShift(vehicle, shift);
	}
}
