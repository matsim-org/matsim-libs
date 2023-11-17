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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class is designed for inheritance without overriding.
 * It is okay to add new attributes and getters, but overriding the inherited ones should not take place.
 * To enforce that, please make sure that all attributes are 'private final' and getters are 'final'.
 *
 * @author Michal Maciejewski (michalm)
 */
public abstract class AbstractPassengerRequestEvent extends Event {
	public static final String ATTRIBUTE_MODE = "mode";
	public static final String ATTRIBUTE_REQUEST = "request";
	public static final String ATTRIBUTE_PERSON = "person";

	private final String mode;
	private final Id<Request> requestId;
	private final List<Id<Person>> personIds;

	protected AbstractPassengerRequestEvent(double time, String mode, Id<Request> requestId, List<Id<Person>> personIds) {
		super(time);
		this.mode = mode;
		this.requestId = requestId;
		this.personIds = personIds;
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
	 * @return ids of the passengers (persons)
	 */
	public final List<Id<Person>> getPersonIds() {
		return List.copyOf(personIds);
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_MODE, mode);
		attr.put(ATTRIBUTE_REQUEST, requestId + "");
		attr.put(ATTRIBUTE_PERSON, personIds.stream().map(Object::toString).collect(Collectors.joining(",")));
		return attr;
	}
}
