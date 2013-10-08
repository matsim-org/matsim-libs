/* *********************************************************************** *
 * project: org.matsim.*
 * VXYEvent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.christoph.mobsim.flexiblecells.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;

public class VXYEvent extends Event {

	public static final String EVENT_TYPE = "VXYEvent";
	public static final String ATTRIBUTE_X = "x";
	public static final String ATTRIBUTE_Y = "y";
	public static final String ATTRIBUTE_V = "v";
	
	public static final String ATTRIBUTE_PERSON = "person";

	private final double v;
	private final double x;
	private final double y;
	private final Id personId;

	public VXYEvent(Id id, double v, double x, double y, double time) {
		super(time);
		this.personId = id;
		this.v = v;
		this.x = x;
		this.y = y;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_PERSON, this.personId.toString());
		attr.put(ATTRIBUTE_V, Double.toString(this.v));
		attr.put(ATTRIBUTE_X, Double.toString(this.x));
		attr.put(ATTRIBUTE_Y, Double.toString(this.y));

		return attr;
	}
	
	public double getV() {
		return this.v;
	}
	
	public double getX() {
		return this.x;
	}

	public double getY() {
		return this.y;
	}


	public Id getPersonId() {
		return this.personId;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}
	
}