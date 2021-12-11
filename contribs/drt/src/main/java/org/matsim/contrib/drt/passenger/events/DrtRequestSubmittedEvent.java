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
import java.util.Objects;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.GenericEvent;
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
	
	public static final String ATTRIBUTE_LATEST_PICKUP_TIME = "latestPickupTime";
	public static final String ATTRIBUTE_LATEST_DROPOFF_TIME = "latestDropoffTime";

	private final double unsharedRideTime;
	private final double unsharedRideDistance;

	private final double latestPickupTime;
	private final double latestDropoffTime;

	public DrtRequestSubmittedEvent(double time, String mode, Id<Request> requestId, Id<Person> personId,
			Id<Link> fromLinkId, Id<Link> toLinkId, double unsharedRideTime, double unsharedRideDistance,
			double latestPickupTime, double latestDropoffTime) {
		super(time, mode, requestId, personId, fromLinkId, toLinkId);
		this.unsharedRideTime = unsharedRideTime;
		this.unsharedRideDistance = unsharedRideDistance;
		this.latestPickupTime = latestPickupTime;
		this.latestDropoffTime = latestDropoffTime;
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
	
	public final double getLatestPickupTime() {
		return latestPickupTime;
	}
	
	public final double getLatestDropoffTime() {
		return latestDropoffTime;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_UNSHARED_RIDE_TIME, unsharedRideTime + "");
		attr.put(ATTRIBUTE_UNSHARED_RIDE_DISTANCE, unsharedRideDistance + "");
		attr.put(ATTRIBUTE_LATEST_PICKUP_TIME, latestPickupTime + "");
		attr.put(ATTRIBUTE_LATEST_DROPOFF_TIME, latestDropoffTime + "");
		return attr;
	}

	public static DrtRequestSubmittedEvent convert(GenericEvent event) {
		Map<String, String> attributes = event.getAttributes();
		double time = Double.parseDouble(attributes.get(ATTRIBUTE_TIME));
		String mode = Objects.requireNonNull(attributes.get(ATTRIBUTE_MODE));
		Id<Request> requestId = Id.create(attributes.get(ATTRIBUTE_REQUEST), Request.class);
		Id<Person> personId = Id.createPersonId(attributes.get(ATTRIBUTE_PERSON));
		Id<Link> fromLinkId = Id.createLinkId(attributes.get(ATTRIBUTE_FROM_LINK));
		Id<Link> toLinkId = Id.createLinkId(attributes.get(ATTRIBUTE_TO_LINK));
		double unsharedRideTime = Double.parseDouble(attributes.get(ATTRIBUTE_UNSHARED_RIDE_TIME));
		double unsharedRideDistance = Double.parseDouble(attributes.get(ATTRIBUTE_UNSHARED_RIDE_DISTANCE));
		double latestPickupTime = Double.parseDouble(attributes.get(ATTRIBUTE_LATEST_PICKUP_TIME));
		double latestDropoffTime = Double.parseDouble(attributes.get(ATTRIBUTE_LATEST_DROPOFF_TIME));
		return new DrtRequestSubmittedEvent(time, mode, requestId, personId, fromLinkId, toLinkId, unsharedRideTime,
				unsharedRideDistance, latestPickupTime, latestDropoffTime);
	}
}
