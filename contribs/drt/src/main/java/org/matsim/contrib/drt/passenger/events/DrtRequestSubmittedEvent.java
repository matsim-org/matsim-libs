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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.load.DvrpLoad;
import org.matsim.contrib.dvrp.load.DvrpLoadType;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerRequestSubmittedEvent;

import java.util.*;

/**
 * @author michalm
 */
public class DrtRequestSubmittedEvent extends PassengerRequestSubmittedEvent {
	public static final String EVENT_TYPE = "DrtRequest submitted";

	public static final String ATTRIBUTE_UNSHARED_RIDE_TIME = "unsharedRideTime";
	public static final String ATTRIBUTE_UNSHARED_RIDE_DISTANCE = "unsharedRideDistance";

	public static final String ATTRIBUTE_EARLIEST_DEPARTURE_TIME = "earliestDepartureTime";
	public static final String ATTRIBUTE_LATEST_PICKUP_TIME = "latestPickupTime";
	public static final String ATTRIBUTE_LATEST_DROPOFF_TIME = "latestDropoffTime";
	public static final String ATTRIBUTE_MAX_RIDE_DURATION = "maxRideDuration";

	private final double unsharedRideTime;
	private final double unsharedRideDistance;

	private final double earliestDepartureTime;
	private final double latestPickupTime;
	private final double latestDropoffTime;
	private final double maxRideDuration;

	public DrtRequestSubmittedEvent(double time, String mode, Id<Request> requestId, List<Id<Person>> personIds,
									Id<Link> fromLinkId, Id<Link> toLinkId, double unsharedRideTime, double unsharedRideDistance,
									double earliestDepartureTime, double latestPickupTime, double latestDropoffTime, double maxRideDuration,
									DvrpLoad load, String serializedDvrpLoad) {
		super(time, mode, requestId, personIds, fromLinkId, toLinkId, load, serializedDvrpLoad);
		this.unsharedRideTime = unsharedRideTime;
		this.unsharedRideDistance = unsharedRideDistance;
		this.earliestDepartureTime = earliestDepartureTime;
		this.latestPickupTime = latestPickupTime;
		this.latestDropoffTime = latestDropoffTime;
		this.maxRideDuration = maxRideDuration;
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

	public final double getEarliestDepartureTime() {
		return earliestDepartureTime;
	}

	public final double getLatestPickupTime() {
		return latestPickupTime;
	}

	public final double getLatestDropoffTime() {
		return latestDropoffTime;
	}

	public double getMaxRideDuration() {
		return maxRideDuration;
	}
	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_UNSHARED_RIDE_TIME, unsharedRideTime + "");
		attr.put(ATTRIBUTE_UNSHARED_RIDE_DISTANCE, unsharedRideDistance + "");
		attr.put(ATTRIBUTE_EARLIEST_DEPARTURE_TIME, earliestDepartureTime + "");
		attr.put(ATTRIBUTE_LATEST_PICKUP_TIME, latestPickupTime + "");
		attr.put(ATTRIBUTE_LATEST_DROPOFF_TIME, latestDropoffTime + "");
		attr.put(ATTRIBUTE_MAX_RIDE_DURATION, maxRideDuration + "");
		return attr;
	}

	public static DrtRequestSubmittedEvent convert(GenericEvent event) {
		Map<String, String> attributes = event.getAttributes();
		double time = Double.parseDouble(attributes.get(ATTRIBUTE_TIME));
		String mode = Objects.requireNonNull(attributes.get(ATTRIBUTE_MODE));
		Id<Request> requestId = Id.create(attributes.get(ATTRIBUTE_REQUEST), Request.class);
		String[] personIdsAttribute = attributes.get(ATTRIBUTE_PERSON).split(",");
		List<Id<Person>> personIds = new ArrayList<>();
		for (String person : personIdsAttribute) {
			personIds.add(Id.create(person, Person.class));
		}
		Id<Link> fromLinkId = Id.createLinkId(attributes.get(ATTRIBUTE_FROM_LINK));
		Id<Link> toLinkId = Id.createLinkId(attributes.get(ATTRIBUTE_TO_LINK));
		double unsharedRideTime = Double.parseDouble(attributes.get(ATTRIBUTE_UNSHARED_RIDE_TIME));
		double unsharedRideDistance = Double.parseDouble(attributes.get(ATTRIBUTE_UNSHARED_RIDE_DISTANCE));
		double earliestDepartureTime = Double.parseDouble(attributes.getOrDefault(ATTRIBUTE_EARLIEST_DEPARTURE_TIME, "NaN"));
		double latestPickupTime = Double.parseDouble(attributes.getOrDefault(ATTRIBUTE_LATEST_PICKUP_TIME, "NaN"));
		double latestDropoffTime = Double.parseDouble(attributes.getOrDefault(ATTRIBUTE_LATEST_DROPOFF_TIME, "NaN"));
		double maxRideDuration = Double.parseDouble(attributes.getOrDefault(ATTRIBUTE_MAX_RIDE_DURATION, "NaN"));
		String serializedLoad  = attributes.get(ATTRIBUTE_LOAD);
		return new DrtRequestSubmittedEvent(time, mode, requestId, personIds, fromLinkId, toLinkId, unsharedRideTime,
				unsharedRideDistance, earliestDepartureTime, latestPickupTime, latestDropoffTime, maxRideDuration,
			null, serializedLoad);
	}
}
