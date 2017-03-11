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

import java.util.*;

import org.matsim.contrib.drt.DrtRequest;
import org.matsim.contrib.dvrp.schedule.StayTaskImpl;

public class DrtPickupTask extends StayTaskImpl implements DrtTaskWithRequests {
	private final Set<DrtRequest> requests;

	public DrtPickupTask(double beginTime, double endTime, Set<DrtRequest> requests) {
		super(beginTime, endTime, requests.iterator().next().getFromLink());

		this.requests = new HashSet<>(requests);
		for (DrtRequest req : this.requests) {
			req.setPickupTask(this);
		}
	}

	@Override
	public DrtTaskType getDrtTaskType() {
		return DrtTaskType.PICKUP;
	}

	@Override
	public Set<DrtRequest> getRequests() {
		return Collections.unmodifiableSet(requests);
	}

	@Override
	protected String commonToString() {
		return "[" + getDrtTaskType().name() + "]" + super.commonToString();
	}
}
