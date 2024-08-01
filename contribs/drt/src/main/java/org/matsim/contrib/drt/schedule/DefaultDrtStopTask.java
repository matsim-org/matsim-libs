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

import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.STOP;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.schedule.DefaultStayTask;

import com.google.common.base.MoreObjects;

/**
 * A task representing stopping at a bus stop with at least one or more passengers being picked up or dropped off.
 * <p>
 * Note that we can have both dropoff requests and pickup requests for the same stop.  kai, nov'18
 *
 * @author michalm
 */
public class DefaultDrtStopTask extends DefaultStayTask implements DrtStopTask {
	public static final DrtTaskType TYPE = new DrtTaskType(STOP);

	private final Map<Id<Request>, AcceptedDrtRequest> dropoffRequests = new LinkedHashMap<>();
	private final Map<Id<Request>, AcceptedDrtRequest> pickupRequests = new LinkedHashMap<>();

	public DefaultDrtStopTask(double beginTime, double endTime, Link link) {
		super(TYPE, beginTime, endTime, link);
	}

	/**
	 * @return requests associated with passengers being dropped off at this stop
	 */
	@Override
	public Map<Id<Request>, AcceptedDrtRequest> getDropoffRequests() {
		return Collections.unmodifiableMap(dropoffRequests);
	}

	/**
	 * @return requests associated with passengers being picked up at this stop
	 */
	@Override
	public Map<Id<Request>, AcceptedDrtRequest> getPickupRequests() {
		return Collections.unmodifiableMap(pickupRequests);
	}

	@Override
	public void addDropoffRequest(AcceptedDrtRequest request) {
		dropoffRequests.put(request.getId(), request);
	}

	@Override
	public void addPickupRequest(AcceptedDrtRequest request) {
		pickupRequests.put(request.getId(), request);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("dropoffRequests", dropoffRequests)
				.add("pickupRequests", pickupRequests)
				.add("super", super.toString())
				.toString();
	}
	
	@Override
	public void removePickupRequest(Id<Request> requestId) {
		pickupRequests.remove(requestId);
	}
	
	@Override
	public void removeDropoffRequest(Id<Request> requestId) {
		dropoffRequests.remove(requestId);
	}
}
