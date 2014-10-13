/* *********************************************************************** *
 * project: org.matsim.*
 * ArrowEvent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

/**
 * @author laemmel
 * 
 */
public class ArrowEvent extends Event {
	
	public static final String ATTRIBUTE_FROM_X = "fromX";
	public static final String ATTRIBUTE_FROM_Y = "fromY";
	public static final String ATTRIBUTE_TO_X = "toX";
	public static final String ATTRIBUTE_TO_Y = "toY";
	public static final String ATTRIBUTE_LINE_SEG_TYPE = "lineSegType";
	
	public static final String EVENT_TYPE = "line_seg";
	
	private final Coordinate from;
	private final Coordinate to;
	private final int type;
	private final Id<Person> persId;
	private final float r;
	private final float g;
	private final float b;

	public ArrowEvent(Id<Person> personId, Coordinate from, Coordinate to, float r, float g, float b, int type, double time) {
		super(time);
		this.persId = personId;
		this.from = from;
		this.to = to;
		this.type = type;
		this.r = r;
		this.g = g;
		this.b = b;
	}

	@Override
	public String toString() {
		return "type:" + this.type + " person:" + this.persId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.core.api.experimental.events.Event#getAttributes()
	 */
	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> map = super.getAttributes();
		map.put(ATTRIBUTE_FROM_X, ""+this.from.x);
		map.put(ATTRIBUTE_FROM_Y, ""+this.from.y);
		map.put(ATTRIBUTE_TO_X, ""+this.to.x);
		map.put(ATTRIBUTE_TO_Y, ""+this.to.y);
		map.put(ATTRIBUTE_LINE_SEG_TYPE, ""+this.type);
		map.put(ATTRIBUTE_PERSON, this.persId.toString());
		return map;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.core.api.experimental.events.Event#getTime()
	 */
	@Override
	public double getTime() {
		return super.getTime();
	}

	/**
	 * @return the persId
	 */
	public Id getPersId() {
		return this.persId;
	}

	/**
	 * @return the r
	 */
	public float getR() {
		return this.r;
	}

	/**
	 * @return the g
	 */
	public float getG() {
		return this.g;
	}

	/**
	 * @return the b
	 */
	public float getB() {
		return this.b;
	}

	/**
	 * @return the type
	 */
	public int getType() {
		return this.type;
	}

	/**
	 * @return the from
	 */
	public Coordinate getFrom() {
		return this.from;
	}

	/**
	 * @return the to
	 */
	public Coordinate getTo() {
		return this.to;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}
	
	public static final String ATTRIBUTE_PERSON = "person";

}

