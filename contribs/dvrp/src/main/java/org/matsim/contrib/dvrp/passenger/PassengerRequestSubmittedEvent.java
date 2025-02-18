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

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.load.DvrpLoad;
import org.matsim.contrib.dvrp.load.DvrpLoadType;
import org.matsim.contrib.dvrp.optimizer.Request;

/**
 * @author michalm
 */
public class PassengerRequestSubmittedEvent extends AbstractPassengerRequestEvent {
	public static final String EVENT_TYPE = "PassengerRequest submitted";

	public static final String ATTRIBUTE_FROM_LINK = "fromLink";
	public static final String ATTRIBUTE_TO_LINK = "toLink";
	public static final String ATTRIBUTE_LOAD = "load";

	private final Id<Link> fromLinkId;
	private final Id<Link> toLinkId;
	private final DvrpLoad load;
	private final String serializedLoad;

	public PassengerRequestSubmittedEvent(double time, String mode, Id<Request> requestId, List<Id<Person>> personIds,
			Id<Link> fromLinkId, Id<Link> toLinkId, DvrpLoad load, String serializedDvrpLoad) {
		super(time, mode, requestId, personIds);
		this.fromLinkId = fromLinkId;
		this.toLinkId = toLinkId;
		this.load = load;
		this.serializedLoad = serializedDvrpLoad;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	/**
	 * @return id of the request's origin link
	 */
	public final Id<Link> getFromLinkId() {
		return fromLinkId;
	}

	/**
	 * @return id of the request's destination link
	 */
	public final Id<Link> getToLinkId() {
		return toLinkId;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_FROM_LINK, fromLinkId + "");
		attr.put(ATTRIBUTE_TO_LINK, toLinkId + "");
		attr.put(ATTRIBUTE_LOAD, serializedLoad);
		return attr;
	}

	public static PassengerRequestSubmittedEvent convert(GenericEvent event) {
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
		String serializedLoad  = attributes.get(ATTRIBUTE_LOAD);
		return new PassengerRequestSubmittedEvent(time, mode, requestId, personIds, fromLinkId, toLinkId, null, serializedLoad);
	}

	public DvrpLoad getLoad() {
		return load;
	}

	public String getSerializedLoad() {
		return serializedLoad;
	}
}
