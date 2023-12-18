/*
 * Copyright (C) 2022 MOIA GmbH - All Rights Reserved
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.drt.extension.operations.eshifts.dispatcher;

import org.matsim.contrib.drt.extension.operations.eshifts.schedule.EDrtWaitForShiftStayTask;
import org.matsim.contrib.drt.extension.operations.shifts.dispatcher.DrtShiftDispatcher;
import org.matsim.contrib.drt.extension.operations.shifts.dispatcher.ShiftStartLogic;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;

/**
 * @author nkuehnel / MOIA
 */
public class EDrtShiftStartLogic implements ShiftStartLogic {

	private final ShiftStartLogic delegate;

	public EDrtShiftStartLogic(ShiftStartLogic delegate) {
		this.delegate = delegate;
	}

	@Override
	public boolean shiftStarts(DrtShiftDispatcher.ShiftEntry shiftEntry) {
		Schedule schedule = shiftEntry.vehicle().getSchedule();
		Task currentTask = schedule.getCurrentTask();
		if (currentTask instanceof EDrtWaitForShiftStayTask) {
			//check whether vehicle still needs to complete charging task
			if(((EDrtWaitForShiftStayTask) currentTask).getChargingTask() == null) {
				return delegate.shiftStarts(shiftEntry);
			}
		}
		return false;
	}
}
