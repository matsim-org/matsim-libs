/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.optimizer.Request;

import static org.matsim.api.core.v01.events.HasPersonId.ATTRIBUTE_PERSON;

/**
 * @author michalm
 */
public class PassengerRequestRejectedEvent extends AbstractPassengerRequestEvent {
	public static final String EVENT_TYPE = "PassengerRequest rejected";

	public static final String ATTRIBUTE_CAUSE = "cause";

	private final String cause;

	public PassengerRequestRejectedEvent(double time, String mode, Id<Request> requestId, List<Id<Person>> personIds,
			String cause) {
		super(time, mode, requestId, personIds);
		this.cause = cause;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public final String getCause() {
		return cause;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_CAUSE, cause);
		return attr;
	}

	public static PassengerRequestRejectedEvent convert(GenericEvent event) {
		Map<String, String> attributes = event.getAttributes();
		double time = Double.parseDouble(attributes.get(ATTRIBUTE_TIME));
		String mode = Objects.requireNonNull(attributes.get(ATTRIBUTE_MODE));
		Id<Request> requestId = Id.create(attributes.get(ATTRIBUTE_REQUEST), Request.class);
		String[] personIdsAttribute = attributes.get(ATTRIBUTE_PERSON).split(",");
		List<Id<Person>> personIds = new ArrayList<>();
		for (String person : personIdsAttribute) {
			personIds.add(Id.create(person, Person.class));
		}		String cause = Objects.requireNonNull(attributes.get(ATTRIBUTE_CAUSE));
		return new PassengerRequestRejectedEvent(time, mode, requestId, personIds, cause);
	}
}
