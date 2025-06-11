/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2025 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** *
 */
package org.matsim.contrib.drt.extension.operations.shifts.dispatcher;

import com.google.common.base.Verify;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.WaitForShiftTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;

/**
 * @author nkuehnel / MOIA
 */
public class DefaultShiftStartLogic implements ShiftStartLogic {
	@Override
	public boolean shiftStarts(DrtShiftDispatcher.ShiftEntry peek) {
		// old shift hasn't ended yet
		if (!peek.shift().equals(peek.vehicle().getShifts().peek())) {
			return false;
		}
		Schedule schedule = peek.vehicle().getSchedule();

		// only active vehicles
		if (schedule.getStatus() != Schedule.ScheduleStatus.STARTED) {
			return false;
		}

		// current task is WaitForShiftTask
		Task currentTask = schedule.getCurrentTask();
		if(currentTask instanceof WaitForShiftTask) {
			//check if optional location requirement is met
			if(peek.shift().getOperationFacilityId().isPresent()) {
				Id<OperationFacility> operationFacilityId = peek.shift().getOperationFacilityId().get();
				Verify.verify((operationFacilityId.equals(((WaitForShiftTask) currentTask).getFacility().getId())),
						"Vehicle and shift start locations do not match.");
			}
			return true;
		} else {
			return false;
		}
	}
}
