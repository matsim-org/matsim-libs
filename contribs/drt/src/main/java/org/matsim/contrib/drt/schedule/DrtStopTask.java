/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

import java.util.*;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.data.DrtRequest;
import org.matsim.contrib.dvrp.schedule.StayTaskImpl;

/**
 * @author michalm
 */
public class DrtStopTask extends StayTaskImpl implements DrtTask {
	private final Set<DrtRequest> dropoffRequests = new HashSet<>();
	private final Set<DrtRequest> pickupRequests = new HashSet<>();

	public DrtStopTask(double beginTime, double endTime, Link link) {
		super(beginTime, endTime, link);
	}

	@Override
	public DrtTaskType getDrtTaskType() {
		return DrtTaskType.STOP;
	}
	
	/**
	 * one stop task my have multiple dropoffs.  I speculate that this may mean, e.g., drop off multiple passengers at same stop. kai, nov'18
	 */
	public Set<DrtRequest> getDropoffRequests() {
		return Collections.unmodifiableSet(dropoffRequests);
	}
	
	/**
	 * one task my have multiple pickups (?).  I speculate this this may mean, e.g., pick up multiple passengers at same stop.
	 * Note that we can have both dropoff requests and pickup requests for the same stop.  kai, nov'18
	 */
	public Set<DrtRequest> getPickupRequests() {
		return Collections.unmodifiableSet(pickupRequests);
	}

	public void addDropoffRequest(DrtRequest request) {
		dropoffRequests.add(request);
	}

	public void addPickupRequest(DrtRequest request) {
		pickupRequests.add(request);
	}

	@Override
	protected String commonToString() {
		return "[" + getDrtTaskType().name() + "]" + super.commonToString();
	}
}
