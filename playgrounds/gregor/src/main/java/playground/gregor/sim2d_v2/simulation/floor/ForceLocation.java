/* *********************************************************************** *
 * project: org.matsim.*
 * ForceLocation.java
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
package playground.gregor.sim2d_v2.simulation.floor;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * @author laemmel
 * 
 */
public class ForceLocation {

	private Force f = null;
	private final Coordinate c;

	private EnvironmentDistances ed;

	public ForceLocation(EnvironmentDistances ed) {
		this.ed = ed;
		this.c = ed.getLocation();
	}

	public ForceLocation(Force f, Coordinate c) {
		this.f = f;
		this.c = c;
	}

	public Coordinate getLocation() {
		return this.c;
	}

	public Force getForce() {
		return this.f;
	}

	public EnvironmentDistances getEnvironmentDistances() {
		return this.ed;
	}

	public void setForce(Force f) {
		this.f = f;
		this.ed = null;
	}

}
