/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.schedule;

import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.DRIVE;

import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.DefaultDriveTask;

/**
 * @author michalm
 */
public class DrtDriveTask extends DefaultDriveTask {
	public static final DrtTaskType TYPE = new DrtTaskType(DRIVE);

	public DrtDriveTask(VrpPathWithTravelData path, DrtTaskType taskType) {
		super(taskType, path);
	}
}
