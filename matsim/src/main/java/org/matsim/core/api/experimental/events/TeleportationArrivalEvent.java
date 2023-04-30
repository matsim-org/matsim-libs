/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.core.api.experimental.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.events.HasPersonId;

/**
 * This is similar to the VehicleArrival and PersonArrival events.
 * It is used for scoring teleported legs.
 */
public final class TeleportationArrivalEvent extends Event implements HasPersonId {

	public static final String ATTRIBUTE_PERSON = "person";
	public static final String ATTRIBUTE_DISTANCE = "distance";
    public static final String ATTRIBUTE_MODE = "mode";

	public static final String EVENT_TYPE = "travelled";
    private final String mode;

    private final Id<Person> agentId;
    private final double distance;

    public TeleportationArrivalEvent(double time, Id<Person> agentId, double distance, String mode) {
        super(time);
        this.agentId = agentId;
        this.distance = distance;
		this.mode = mode;
	}

	public Id<Person> getPersonId() {
		return agentId;
	}

	public double getDistance() {
		return distance;
	}

	public String getMode() {
		return mode;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attributes = super.getAttributes();
		//        attributes.put(ATTRIBUTE_PERSON, agentId.toString()); // done in super-class
		attributes.put(ATTRIBUTE_DISTANCE, Double.toString(distance));
		attributes.put(ATTRIBUTE_MODE, mode);
        return attributes;
    }
}
