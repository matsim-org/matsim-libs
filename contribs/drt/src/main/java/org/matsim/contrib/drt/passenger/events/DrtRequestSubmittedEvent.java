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
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.data.Request;

/**
 * @author michalm
 */
public class DrtRequestSubmittedEvent extends Event {
	public static final String EVENT_TYPE = "DrtRequest submitted";

	public static final String ATTRIBUTE_MODE = "mode";
	public static final String ATTRIBUTE_REQUEST = "request";
	public static final String ATTRIBUTE_PERSON = "person";
	public static final String ATTRIBUTE_FROM_LINK = "fromLink";
	public static final String ATTRIBUTE_TO_LINK = "toLink";
	public static final String ATTRIBUTE_UNSHARED_RIDE_TIME = "unsharedRideTime";
	public static final String ATTRIBUTE_UNSHARED_RIDE_DISTANCE = "unsharedRideDistance";

	private final String mode;
	private final Id<Request> requestId;
	private final Id<Person> personId;
	private final Id<Link> fromLinkId;
	private final Id<Link> toLinkId;
	private final double unsharedRideTime;
	private final double unsharedRideDistance;

	public DrtRequestSubmittedEvent(double time, String mode, Id<Request> requestId, Id<Person> personId,
			Id<Link> fromLinkId, Id<Link> toLinkId, double unsharedRideTime, double unsharedRideDistance) {
		super(time);
		this.mode = mode;
		this.requestId = requestId;
		this.personId = personId;
		this.fromLinkId = fromLinkId;
		this.toLinkId = toLinkId;
		this.unsharedRideTime = unsharedRideTime;
		this.unsharedRideDistance = unsharedRideDistance;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public String getMode() {
		return mode;
	}

	/**
	 * the ID of the initial request submitted
	 */
	public Id<Request> getRequestId() {
		return requestId;
	}

	/**
	 * the Person Id that submitted the request
	 */
	public Id<Person> getPersonId() {
		return personId;
	}

	/**
	 * the request's origin
	 */
	public Id<Link> getFromLinkId() {
		return fromLinkId;
	}

	/**
	 * the request's destination
	 */
	public Id<Link> getToLinkId() {
		return toLinkId;
	}

	/**
	 * the estimated traveltime it would take to ride without any detours
	 */
	public double getUnsharedRideTime() {
		return unsharedRideTime;
	}

	/**
	 * the estimated distance it would take to ride without any detours
	 */
	public double getUnsharedRideDistance() {
		return unsharedRideDistance;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_MODE, mode);
		attr.put(ATTRIBUTE_REQUEST, requestId + "");
		attr.put(ATTRIBUTE_PERSON, personId + "");
		attr.put(ATTRIBUTE_FROM_LINK, fromLinkId + "");
		attr.put(ATTRIBUTE_TO_LINK, toLinkId + "");
		attr.put(ATTRIBUTE_UNSHARED_RIDE_TIME, unsharedRideTime + "");
		attr.put(ATTRIBUTE_UNSHARED_RIDE_DISTANCE, unsharedRideDistance + "");
		return attr;
	}
}
