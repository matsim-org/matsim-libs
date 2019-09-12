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

package org.matsim.contrib.drt.passenger.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerRequestSubmittedEvent;

/**
 * @author michalm
 */
public class DrtRequestSubmittedEvent extends PassengerRequestSubmittedEvent {
	public static final String EVENT_TYPE = "DrtRequest submitted";

	public static final String ATTRIBUTE_UNSHARED_RIDE_TIME = "unsharedRideTime";
	public static final String ATTRIBUTE_UNSHARED_RIDE_DISTANCE = "unsharedRideDistance";

	private final double unsharedRideTime;
	private final double unsharedRideDistance;

	public DrtRequestSubmittedEvent(double time, String mode, Id<Request> requestId, Id<Person> personId,
			Id<Link> fromLinkId, Id<Link> toLinkId, double unsharedRideTime, double unsharedRideDistance) {
		super(time, mode, requestId, personId, fromLinkId, toLinkId);
		this.unsharedRideTime = unsharedRideTime;
		this.unsharedRideDistance = unsharedRideDistance;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	/**
	 * @return estimated travel time it would take to ride without any detours
	 */
	public final double getUnsharedRideTime() {
		return unsharedRideTime;
	}

	/**
	 * @return estimated distance it would take to ride without any detours
	 */
	public final double getUnsharedRideDistance() {
		return unsharedRideDistance;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_UNSHARED_RIDE_TIME, unsharedRideTime + "");
		attr.put(ATTRIBUTE_UNSHARED_RIDE_DISTANCE, unsharedRideDistance + "");
		return attr;
	}
}
