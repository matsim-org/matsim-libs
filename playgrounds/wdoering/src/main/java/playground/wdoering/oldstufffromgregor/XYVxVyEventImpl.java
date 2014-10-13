/* *********************************************************************** *
 * project: org.matsim.*
 * XYZEvent.java
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
package playground.wdoering.oldstufffromgregor;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;

import com.vividsolutions.jts.geom.Coordinate;

public class XYVxVyEventImpl extends Event implements XYVxVyEvent {

	public static final String EVENT_TYPE = "XYVxVyEvent";
	public static final String ATTRIBUTE_X = "x";
	public static final String ATTRIBUTE_Y = "y";
	public static final String ATTRIBUTE_VX = "vx";
	public static final String ATTRIBUTE_VY = "vy";

	private final Coordinate c;
	private final double x;
	private final double y;
	private final double vx;
	private final double vy;

	public XYVxVyEventImpl(Id<Person> id, double x, double y, double vx, double vy, double time) {
		super(time);
		this.personId = id;
		this.c = new Coordinate(x, y);
		this.x = x;
		this.y = y;
		this.vx = vx;
		this.vy = vy;
	}

	public XYVxVyEventImpl(Id<Person> id, Coordinate c, double vx, double vy, double time) {
		super(time);
		this.personId = id;
		this.c = c;
		this.x = c.x;
		this.y = c.y;
		this.vx = vx;
		this.vy = vy;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.core.events.EventImpl#getEventType()
	 */
	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_X, Double.toString(this.x));
		attr.put(ATTRIBUTE_Y, Double.toString(this.y));
		attr.put(ATTRIBUTE_VX, Double.toString(this.vx));

		attr.put(ATTRIBUTE_PERSON, this.personId.toString());
		attr.put(ATTRIBUTE_VY, Double.toString(this.vy));
		return attr;
	}

	@Override
	public double getX() {
		return this.x;
	}

	@Override
	public double getY() {
		return this.y;
	}


	@Override
	public Coordinate getCoordinate() {
		return this.c;
	}

	@Override
	public double getVX() {
		return this.vx;
	}

	@Override
	public double getVY() {
		return this.vy;
	}
	
	public static final String ATTRIBUTE_PERSON = "person";

	private final Id<Person> personId;


	@Override
	public Id getPersonId() {
		return this.personId;
	}

}
