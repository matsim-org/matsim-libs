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

package playground.michalm.drt.schedule;

import java.util.*;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.schedule.StayTaskImpl;

import playground.michalm.drt.data.NDrtRequest;

/**
 * @author michalm
 */
public class NDrtStopTask extends StayTaskImpl implements NDrtTask {
	private final Set<NDrtRequest> dropoffRequests = new HashSet<>();
	private final Set<NDrtRequest> pickupRequests = new HashSet<>();

	private double maxArrivalTime = Double.MAX_VALUE;// relating to max pass drive time (for dropoff requests)
	private double maxDepartureTime = Double.MAX_VALUE;// relating to pass max wait time (for pickup requests)

	public NDrtStopTask(double beginTime, double endTime, Link link) {
		super(beginTime, endTime, link);
	}

	@Override
	public NDrtTaskType getDrtTaskType() {
		return NDrtTaskType.STOP;
	}

	public Set<NDrtRequest> getDropoffRequests() {
		return Collections.unmodifiableSet(dropoffRequests);
	}

	public Set<NDrtRequest> getPickupRequests() {
		return Collections.unmodifiableSet(pickupRequests);
	}

	public void addDropoffRequest(NDrtRequest request) {
		dropoffRequests.add(request);

		double reqMaxArrivalTime = request.getEarliestStartTime() + 3600;// TODO temp
		if (reqMaxArrivalTime < maxArrivalTime) {
			maxArrivalTime = reqMaxArrivalTime;
		}
	}

	public void addPickupRequest(NDrtRequest request) {
		pickupRequests.add(request);

		double reqMaxDepartureTime = request.getEarliestStartTime() + 1200;// TODO temp
		if (reqMaxDepartureTime < maxDepartureTime) {
			maxDepartureTime = reqMaxDepartureTime;
		}
	}

	public double getMaxArrivalTime() {
		return maxArrivalTime;
	}

	public double getMaxDepartureTime() {
		return maxDepartureTime;
	}

	@Override
	protected String commonToString() {
		return "[" + getDrtTaskType().name() + "]" + super.commonToString();
	}
}
