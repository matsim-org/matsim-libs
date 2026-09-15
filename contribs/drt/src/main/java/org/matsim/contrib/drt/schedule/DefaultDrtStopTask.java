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
import java.util.stream.DoubleStream;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.schedule.DefaultStayTask;

import com.google.common.base.MoreObjects;

/**
 * A task representing stopping at a bus stop where one or more passengers may be picked up or dropped off.
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

	@Override
	public double calcLatestArrivalTime() {
		return getMaxTimeConstraint(
					getDropoffRequests().values()
							.stream()
							.mapToDouble(request -> request.getLatestArrivalTime() - request.getDropoffDuration()),
					getBeginTime()
		);
	}

	@Override
	public double calcLatestDepartureTime() {
		return getMaxTimeConstraint(
					getPickupRequests().values()
							.stream()
							.mapToDouble(AcceptedDrtRequest::getLatestStartTime),
					getEndTime()
		);
	}

	@Override
	public double calcEarliestArrivalTime() {
		return getPickupRequests().values()
					.stream()
					.mapToDouble(AcceptedDrtRequest::getEarliestStartTime)
					.min()
					.orElse(0);
	}

	@Override
	public double calcEarliestDepartureTime() {
		// no restriction on earliest departure time in default implementation
		return Double.NEGATIVE_INFINITY;
	}

	private double getMaxTimeConstraint(DoubleStream latestAllowedTimes, double scheduledTime) {
		//XXX if task is already delayed beyond one or more of latestTimes, use scheduledTime as maxTime constraint
		//thus we can still add a new request to the already scheduled stops (as no further delays are incurred)
		//but we cannot add a new stop before the delayed task
		return Math.max(latestAllowedTimes.min().orElse(Double.MAX_VALUE), scheduledTime);
	}
}
