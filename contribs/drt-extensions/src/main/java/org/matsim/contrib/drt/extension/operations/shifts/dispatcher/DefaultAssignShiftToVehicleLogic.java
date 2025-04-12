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

import org.matsim.contrib.drt.extension.operations.shifts.config.ShiftsParams;
import org.matsim.contrib.drt.extension.operations.shifts.fleet.ShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;

import java.util.Iterator;

/**
 * @author nkuehnel / MOIA
 */
public class DefaultAssignShiftToVehicleLogic implements AssignShiftToVehicleLogic {

	private final ShiftsParams drtShiftParams;

	public DefaultAssignShiftToVehicleLogic(ShiftsParams drtShiftParams) {
		this.drtShiftParams = drtShiftParams;
	}

	@Override
	public boolean canAssignVehicleToShift(ShiftDvrpVehicle vehicle, DrtShift shift) {
		// cannot assign shift outside of service hours
		if (shift.getEndTime() > vehicle.getServiceEndTime()
				|| shift.getStartTime() < vehicle.getServiceBeginTime()) {
			return false;
		}

		// assign shift to inactive vehicle
		if (vehicle.getShifts().isEmpty()) {
			return true;
		}

		final Iterator<DrtShift> iterator = vehicle.getShifts().iterator();
		DrtShift previous = iterator.next();
		if (!iterator.hasNext()) {
			if (previous.getEndTime() + drtShiftParams.getChangeoverDuration() < shift.getStartTime()) {
				return true;
			}
		}

		while (iterator.hasNext()) {
			DrtShift next = iterator.next();
			if (shift.getEndTime() + drtShiftParams.getChangeoverDuration() < next.getStartTime()
					&& previous.getEndTime() + drtShiftParams.getChangeoverDuration() < shift.getStartTime()) {
				return true;
			}
			previous = next;
		}
		return false;
	}
}
