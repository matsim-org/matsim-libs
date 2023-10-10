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

package org.matsim.contrib.drt.extension.prebooking.dvrp;

import java.util.Map;
import java.util.Objects;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.AbstractPassengerRequestEvent;

/**
 * When using prebooking in DRT, it is sometimes difficult to correctly
 * attribute which request is being consumed when a customer enters a vehicle.
 * The PassengerEnteringEvent provides exactly this information.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class PassengerEnteringEvent extends AbstractPassengerRequestEvent {
	public static final String EVENT_TYPE = "passenger entering";

	public PassengerEnteringEvent(double time, String mode, Id<Request> requestId, Id<Person> personId) {
		super(time, mode, requestId, personId);
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public static PassengerEnteringEvent convert(GenericEvent event) {
		Map<String, String> attributes = event.getAttributes();
		double time = Double.parseDouble(attributes.get(ATTRIBUTE_TIME));
		String mode = Objects.requireNonNull(attributes.get(ATTRIBUTE_MODE));
		Id<Request> requestId = Id.create(attributes.get(ATTRIBUTE_REQUEST), Request.class);
		Id<Person> personId = Id.createPersonId(attributes.get(ATTRIBUTE_PERSON));
		return new PassengerEnteringEvent(time, mode, requestId, personId);
	}
}
