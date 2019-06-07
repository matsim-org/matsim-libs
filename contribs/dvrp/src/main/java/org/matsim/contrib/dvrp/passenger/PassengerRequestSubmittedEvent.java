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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.optimizer.Request;

/**
 * @author michalm
 */
public class PassengerRequestSubmittedEvent extends AbstractPassengerRequestEvent {
	public static final String EVENT_TYPE = "PassengersRequest scheduled";

	public static final String ATTRIBUTE_FROM_LINK = "fromLink";
	public static final String ATTRIBUTE_TO_LINK = "toLink";

	private final Id<Link> fromLinkId;
	private final Id<Link> toLinkId;

	public PassengerRequestSubmittedEvent(double time, String mode, Id<Request> requestId, Id<Person> personId,
			Id<Link> fromLinkId, Id<Link> toLinkId) {
		super(time, mode, requestId, personId);
		this.fromLinkId = fromLinkId;
		this.toLinkId = toLinkId;
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
		return attr;
	}
}
