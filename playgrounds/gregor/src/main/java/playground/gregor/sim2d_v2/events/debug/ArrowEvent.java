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
package playground.gregor.sim2d_v2.events.debug;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.Event;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * @author laemmel
 * 
 */
public class ArrowEvent implements Event {

	private final Coordinate from;
	private final Coordinate to;
	private final int type;
	private final Id persId;
	private final float r;
	private final float g;
	private final float b;

	public ArrowEvent(Id personId, Coordinate from, Coordinate to, float r, float g, float b, int type) {
		this.from = from;
		this.to = to;
		this.type = type;
		this.persId = personId;
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
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.core.api.experimental.events.Event#getTime()
	 */
	@Override
	public double getTime() {
		// TODO Auto-generated method stub
		return 0;
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

}
