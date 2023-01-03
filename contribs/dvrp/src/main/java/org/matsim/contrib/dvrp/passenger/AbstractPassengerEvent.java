/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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
import org.matsim.api.core.v01.events.HasPersonId;

/**
 * @author Michal Maciejewski (michalm)
 */
public abstract class AbstractPassengerEvent extends Event implements HasPersonId {
	public static final String ATTRIBUTE_MODE = "mode";
	public static final String ATTRIBUTE_REQUEST = "request";
	public static final String ATTRIBUTE_VEHICLE = "vehicle";

	private final String mode;
	private final Id<Request> requestId;
	private final Id<Person> personId;
	private final Id<DvrpVehicle> vehicleId;

	public AbstractPassengerEvent(double time, String mode, Id<Request> requestId, Id<Person> personId,
			Id<DvrpVehicle> vehicleId) {
		super(time);
		this.mode = mode;
		this.requestId = requestId;
		this.personId = personId;
		this.vehicleId = vehicleId;
	}

	public final String getMode() {
		return mode;
	}

	/**
	 * @return id of the request
	 */
	public final Id<Request> getRequestId() {
		return requestId;
	}

	/**
	 * @return id of the passenger (person)
	 */
	@Override
	public final Id<Person> getPersonId() {
		return personId;
	}

	public Id<DvrpVehicle> getVehicleId() {
		return vehicleId;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_MODE, mode);
		attr.put(ATTRIBUTE_REQUEST, requestId + "");
		attr.put(ATTRIBUTE_VEHICLE, vehicleId + "");
		return attr;
	}
}
