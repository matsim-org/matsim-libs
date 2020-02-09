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

public class DriveTask extends AbstractTask {
	private VrpPath path;

	public DriveTask(TaskType taskType, VrpPathWithTravelData path) {
		super(taskType, path.getDepartureTime(), path.getArrivalTime());
		this.path = path;
	}

	public final VrpPath getPath() {
		return path;
	}

	/**
	 * Vehicle changes its path. Just replaces the previous VrpPath with this one; this will work (if consistent) since
	 * (1) nobody (hopefully) caches a references to the original path, and (2) access to the link on the path is via
	 * index only (and not via an iterator or similar). <br/>
	 * <br/>
	 * Can be used for:
	 * <ul>
	 * <li>changing destination (while keeping the current task active)
	 * <li>stopping it as soon as possible (i.e. at the end of the current/next link)
	 * <li>random walk, roaming/cruising around
	 * <li>...
	 * </ul>
	 */
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
