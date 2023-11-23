/*
 * Copyright (C) 2022 MOIA GmbH - All Rights Reserved
 *
 * You may use, distribute and modify this code under the terms
 * of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 */
package org.matsim.contrib.drt.extension.operations.shifts.dispatcher;

import org.matsim.contrib.drt.extension.operations.shifts.fleet.ShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;

/**
 * @author nkuehnel / MOIA
 */
public interface AssignShiftToVehicleLogic {

	boolean canAssignVehicleToShift(ShiftDvrpVehicle vehicle, DrtShift shift);

}
