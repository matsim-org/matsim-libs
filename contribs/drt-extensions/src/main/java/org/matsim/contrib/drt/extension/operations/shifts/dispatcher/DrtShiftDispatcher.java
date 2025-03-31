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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.extension.operations.shifts.fleet.ShiftDvrpVehicle;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.extension.operations.shifts.schedule.ShiftBreakTask;

/**
 * @author nkuehnel, fzwick / MOIA
 */
public interface DrtShiftDispatcher {

    void initialize();

    record ShiftEntry(DrtShift shift, ShiftDvrpVehicle vehicle){}

    void dispatch(double timeStep);

    void endShift(ShiftDvrpVehicle vehicle, Id<Link> id, Id<OperationFacility> operationFacilityId);

    void endBreak(ShiftDvrpVehicle vehicle, ShiftBreakTask task);

    void startBreak(ShiftDvrpVehicle vehicle, Id<Link> linkId);
}
