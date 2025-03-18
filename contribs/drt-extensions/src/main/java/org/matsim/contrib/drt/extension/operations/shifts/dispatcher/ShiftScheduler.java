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

import com.google.common.collect.ImmutableMap;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShift;
import org.matsim.contrib.drt.extension.operations.shifts.shift.DrtShiftsSpecification;

import jakarta.inject.Provider;
import org.matsim.contrib.dvrp.fleet.Fleet;

import java.util.List;

/**
 * @author nkuehnel / MOIA
 */
public interface ShiftScheduler extends Provider<DrtShiftsSpecification> {

    List<DrtShift> schedule(double time, Fleet fleet);
    ImmutableMap<Id<DrtShift>, DrtShift> initialSchedule();

}
