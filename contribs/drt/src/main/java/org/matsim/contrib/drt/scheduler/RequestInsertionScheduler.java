/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.scheduler;

import org.matsim.contrib.drt.optimizer.insertion.InsertionWithDetourData;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch;

/**
 * @author Michal Maciejewski (michalm)
 */
public interface RequestInsertionScheduler {
	class PickupDropoffTaskPair {
		public final DrtStopTask pickupTask;
		public final DrtStopTask dropoffTask;

		public PickupDropoffTaskPair(DrtStopTask pickupTask, DrtStopTask dropoffTask) {
			this.pickupTask = pickupTask;
			this.dropoffTask = dropoffTask;
		}
	}

	PickupDropoffTaskPair scheduleRequest(DrtRequest request,
			InsertionWithDetourData<OneToManyPathSearch.PathData> insertion);
}
