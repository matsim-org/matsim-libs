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

package org.matsim.contrib.drt.tasks;

import org.matsim.contrib.dvrp.schedule.Task;

public interface DrtTask extends Task {
	public static enum DrtTaskType {
		DRIVE_EMPTY, // drive empty might be needed later.
		STAY, // not directly related to any customer (although may be related to serving a customer; e.g. a pickup
				// drive)
		PICKUP, DRIVE_WITH_PASSENGERS, DROPOFF;// serving n customers (DrtTaskWithRequests)
	}

	DrtTaskType getDrtTaskType();
}
