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

package org.matsim.contrib.dvrp.passenger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.optimizer.Request;

/**
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class PassengerWaitingEvent extends AbstractPassengerRequestEvent {
	public static final String EVENT_TYPE = "passenger waiting";

	public PassengerWaitingEvent(double time, String mode, Id<Request> requestId, List<Id<Person>> personIds) {
		super(time, mode, requestId, personIds);
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public static PassengerWaitingEvent convert(GenericEvent event) {
		Map<String, String> attributes = event.getAttributes();
		double time = Double.parseDouble(attributes.get(ATTRIBUTE_TIME));
		String mode = Objects.requireNonNull(attributes.get(ATTRIBUTE_MODE));
		Id<Request> requestId = Id.create(attributes.get(ATTRIBUTE_REQUEST), Request.class);
		String[] personIdsAttribute = attributes.get(ATTRIBUTE_PERSON).split(",");
		List<Id<Person>> personIds = new ArrayList<>();
		for (String person : personIdsAttribute) {
			personIds.add(Id.create(person, Person.class));
		}
		return new PassengerWaitingEvent(time, mode, requestId, personIds);
	}
}
