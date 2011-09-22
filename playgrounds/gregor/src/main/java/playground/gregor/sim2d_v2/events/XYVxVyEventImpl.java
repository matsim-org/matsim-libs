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
package playground.gregor.sim2d_v2.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.events.PersonEventImpl;

import com.vividsolutions.jts.geom.Coordinate;

public class XYVxVyEventImpl extends PersonEventImpl implements XYVxVyEvent {

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

	public XYVxVyEventImpl(Id id, double x, double y, double vx, double vy, double time) {
		super(time, id);
		this.c = new Coordinate(x, y);
		this.x = x;
		this.y = y;
		this.vx = vx;
		this.vy = vy;
	}

	public XYVxVyEventImpl(Id id, Coordinate c, double vx, double vy, double time) {
		super(time, id);
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
		attr.put(ATTRIBUTE_VY, Double.toString(this.vy));
		return attr;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see playground.gregor.sim2d.events.XYZAzimuthEvent#getX()
	 */
	@Override
	public double getX() {
		return this.x;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see playground.gregor.sim2d.events.XYZAzimuthEvent#getY()
	 */
	@Override
	public double getY() {
		return this.y;
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see playground.gregor.sim2d.events.XYZAzimuthEvent#getCoordinate()
	 */
	@Override
	public Coordinate getCoordinate() {
		return this.c;
	}

	/*
	 * (non-Javadoc)
	 * @see playground.gregor.sim2d_v2.events.XYZAzimuthEvent#getVX()
	 */
	@Override
	public double getVX() {
		return this.vx;
	}

	/*
	 * (non-Javadoc)
	 * @see playground.gregor.sim2d_v2.events.XYZAzimuthEvent#getVY()
	 */
	@Override
	public double getVY() {
		return this.vy;
	}

}
