/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
 * *********************************************************************** */

package org.matsim.contrib.taxi.schedule;

import static org.matsim.contrib.taxi.schedule.TaxiTaskBaseType.EMPTY_DRIVE;

import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.DefaultDriveTask;

import com.google.common.base.Preconditions;

public class TaxiEmptyDriveTask extends DefaultDriveTask {
	public static final TaxiTaskType TYPE = new TaxiTaskType(EMPTY_DRIVE);

	public TaxiEmptyDriveTask(VrpPathWithTravelData path, TaxiTaskType taskType) {
		super(taskType, path);
		Preconditions.checkArgument(taskType.baseType().get() == EMPTY_DRIVE);
	}
}
