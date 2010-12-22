/* *********************************************************************** *
 * project: org.matsim.*
 * EnvironmentDistances.java
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

import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * @author laemmel
 * 
 */
public class EnvironmentDistances {

	private final Coordinate location;
	private final List<Coordinate> objects = new ArrayList<Coordinate>();

	public EnvironmentDistances(Coordinate location) {
		this.location = location;
	}

	public void addEnvironmentDistanceLocation(Coordinate obj) {
		this.objects.add(obj);
	}

	public Coordinate getLocation() {
		return this.location;
	}

	public List<Coordinate> getObjects() {
		return this.objects;
	}

}
