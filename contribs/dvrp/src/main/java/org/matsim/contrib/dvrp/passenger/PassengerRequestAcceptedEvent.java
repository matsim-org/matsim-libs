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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.core.api.internal.HasPersonId;

import java.util.Map;

/**
 * @author michalm
 */
public class PassengerRequestAcceptedEvent extends Event implements HasPersonId {
	public static final String EVENT_TYPE = "PassengersRequest accepted";

	public static final String ATTRIBUTE_MODE = "mode";
	public static final String ATTRIBUTE_REQUEST = "request";


	private final String mode;
	private final Id<Request> requestId;
	private final Id<Person> personId;


	public PassengerRequestAcceptedEvent(double time, String mode, Id<Request> requestId, Id<Person> personId) {
		super(time);
		this.mode = mode;
		this.requestId = requestId;
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

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_MODE, mode);
		attr.put(ATTRIBUTE_REQUEST, requestId + "");
		return attr;
	}

	@Override
	public Id<Person> getPersonId() {
        return personId;
	}
}
