/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.dvrp.passenger;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.core.api.internal.HasPersonId;

/**
 * @author michalm
 */
public final class PassengerRequestScheduledEvent extends Event implements HasPersonId {
	public static final String EVENT_TYPE = "PassengersRequest scheduled";

	public static final String ATTRIBUTE_MODE = "mode";
	public static final String ATTRIBUTE_REQUEST = "request";
	public static final String ATTRIBUTE_VEHICLE = "vehicle";
	public static final String ATTRIBUTE_PICKUP_TIME = "pickupTime";
	public static final String ATTRIBUTE_DROPOFF_TIME = "dropoffTime";

	private final String mode;
	private final Id<Request> requestId;
	private final Id<DvrpVehicle> vehicleId;
	private final double pickupTime;
	private final double dropoffTime;
	private final Id<Person> personId;

	/**
	 * An event processed upon request submission.
	 */
	public PassengerRequestScheduledEvent(double time, String mode, Id<Request> requestId, Id<DvrpVehicle> vehicleId,
			double pickupTime, double dropoffTime, Id<Person> personId) {
		super(time);
		this.mode = mode;
		this.requestId = requestId;
		this.vehicleId = vehicleId;
		this.pickupTime = pickupTime;
		this.dropoffTime = dropoffTime;
		this.personId = personId;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public String getMode() {
		return mode;
	}

	/**
	 * the ID of the request
	 */
	public Id<Request> getRequestId() {
		return requestId;
	}

	/**
	 * the vehicle scheduled to the request
	 */
	public Id<DvrpVehicle> getVehicleId() {
		return vehicleId;
	}

	/**
	 * the <b>estimated</b> pickup time of the request
	 */
	public double getPickupTime() {
		return pickupTime;
	}

	/**
	 * the <b>estimated</b> arrival time of the request
	 */
	public double getDropoffTime() {
		return dropoffTime;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_MODE, mode);
		attr.put(ATTRIBUTE_REQUEST, requestId + "");
		attr.put(ATTRIBUTE_VEHICLE, vehicleId + "");
		attr.put(ATTRIBUTE_PICKUP_TIME, pickupTime + "");
		attr.put(ATTRIBUTE_DROPOFF_TIME, dropoffTime + "");
		return attr;
	}

	@Override
	public Id<Person> getPersonId() {
		return personId;
	}
}
