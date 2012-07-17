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

package org.matsim.core.events;

import org.matsim.api.core.v01.Id;

import java.util.Map;

public class TravelledEventImpl extends EventImpl implements TravelledEvent {


	public static final String ATTRIBUTE_PERSON = "person";
	public static final String ATTRIBUT_DISTANCE = "distance";

	public static final String EVENT_TYPE = "travelled";

    private Id agentId;
    private double distance;

    public TravelledEventImpl(double time, Id agentId, double distance) {
        super(time);
        this.agentId = agentId;
        this.distance = distance;
    }

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }

    @Override
    public Map<String, String> getAttributes() {
        Map<String, String> attributes = super.getAttributes();
        attributes.put(ATTRIBUTE_PERSON, agentId.toString());
        attributes.put(ATTRIBUT_DISTANCE, Double.toString(distance));
        return attributes;
    }

    @Override
    public Id getPersonId() {
        return agentId;
    }

    @Override
    public double getDistance() {
        return distance;
    }

}
