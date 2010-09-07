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

import org.matsim.api.core.v01.Id;

import com.vividsolutions.jts.geom.Coordinate;

public class XYZEvent {
	
	private final Coordinate c;
	final double time;
	private final double azimuth;
	private Id id;

	public XYZEvent(Id id, Coordinate c, double azimuth, double time) {
		this.id = id;
		this.c = c;
		this.azimuth = azimuth;
		this.time = time;
	}

	public Coordinate getC() {
		return c;
	}

	public double getAzimuth() {
		return azimuth;
	}
	
	public double getTime() {
		return this.time;
	}

	public Id getId() {
		return this.id;
	}
	

}
