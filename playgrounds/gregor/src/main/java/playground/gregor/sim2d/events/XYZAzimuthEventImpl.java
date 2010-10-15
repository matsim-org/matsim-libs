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
package playground.gregor.sim2d.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.events.PersonEventImpl;

import com.vividsolutions.jts.geom.Coordinate;

public class XYZAzimuthEventImpl extends PersonEventImpl implements XYZAzimuthEvent {

	public static final String EVENT_TYPE = "XYZAzimuth";
	public static final String ATTRIBUTE_X = "x";
	public static final String ATTRIBUTE_Y = "y";
	public static final String ATTRIBUTE_Z = "z";
	public static final String ATTRIBUTE_AZIMUTH = "azimuth";

	private final Coordinate c;
	private final double azimuth;
	private final double x;
	private final double y;
	private final double z;

	public XYZAzimuthEventImpl(Id id, double x, double y, double z, double azimuth, double time) {
		super(time, id);
		this.c = new Coordinate(x, y, z);
		this.x = x;
		this.y = y;
		this.z = z;
		this.azimuth = azimuth;
	}

	public XYZAzimuthEventImpl(Id id, Coordinate c, double azimuth, double time) {
		super(time, id);
		this.c = c;
		this.x = c.x;
		this.y = c.y;
		this.z = c.z;
		this.azimuth = azimuth;
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
		attr.put(ATTRIBUTE_Z, Double.toString(this.z));
		attr.put(ATTRIBUTE_AZIMUTH, Double.toString(this.azimuth));
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
	 * @see playground.gregor.sim2d.events.XYZAzimuthEvent#getZ()
	 */
	@Override
	public double getZ() {
		return this.z;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see playground.gregor.sim2d.events.XYZAzimuthEvent#getAzimuth()
	 */
	@Override
	public double getAzimuth() {
		return this.azimuth;
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

}
