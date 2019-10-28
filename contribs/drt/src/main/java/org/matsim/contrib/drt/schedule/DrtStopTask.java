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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.schedule.StayTaskImpl;

/**
 * A task representing stopping at a bus stop with at least one or more passengers being picked up or dropped off.
 * <p>
 * Note that we can have both dropoff requests and pickup requests for the same stop.  kai, nov'18
 *
 * @author michalm
 */
public class DrtStopTask extends StayTaskImpl implements DrtTask {
	private final Map<Id<Request>, DrtRequest> dropoffRequests = new LinkedHashMap<>();
	private final Map<Id<Request>, DrtRequest> pickupRequests = new LinkedHashMap<>();

	public DrtStopTask(double beginTime, double endTime, Link link) {
		super(beginTime, endTime, link);
	}

	@Override
	public DrtTaskType getDrtTaskType() {
		return DrtTaskType.STOP;
	}

	/**
	 * @return requests associated with passengers being dropped off at this stop
	 */
	public Map<Id<Request>, DrtRequest> getDropoffRequests() {
		return Collections.unmodifiableMap(dropoffRequests);
	}

	/**
	 * @return requests associated with passengers being picked up at this stop
	 */
	public Map<Id<Request>, DrtRequest> getPickupRequests() {
		return Collections.unmodifiableMap(pickupRequests);
	}

	public void addDropoffRequest(DrtRequest request) {
		dropoffRequests.put(request.getId(), request);
	}

	public void addPickupRequest(DrtRequest request) {
		pickupRequests.put(request.getId(), request);
	}

	@Override
	protected String commonToString() {
		return "[" + getDrtTaskType().name() + "]" + super.commonToString();
	}
}
