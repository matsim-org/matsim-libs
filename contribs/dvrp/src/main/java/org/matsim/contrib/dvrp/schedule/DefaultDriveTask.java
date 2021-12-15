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

package org.matsim.contrib.dvrp.schedule;

import org.matsim.contrib.dvrp.path.DivertedVrpPath;
import org.matsim.contrib.dvrp.path.VrpPath;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;

import com.google.common.base.MoreObjects;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DefaultDriveTask extends AbstractTask implements DriveTask {
	private VrpPath path;

	public DefaultDriveTask(TaskType taskType, VrpPathWithTravelData path) {
		super(taskType, path.getDepartureTime(), path.getArrivalTime());
		this.path = path;
	}

	@Override
	public final VrpPath getPath() {
		return path;
	}

	@Override
	public final void pathDiverted(DivertedVrpPath divertedPath, double newEndTime) {
		// can only divert an ongoing task
		if (getStatus() != TaskStatus.STARTED) {
			throw new IllegalStateException();
		}

		path = divertedPath;
		setEndTime(newEndTime);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("super", super.toString()).add("path", path).toString();
	}
}
