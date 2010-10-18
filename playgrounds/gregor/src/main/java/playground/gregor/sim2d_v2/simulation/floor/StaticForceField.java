/* *********************************************************************** *
 * project: org.matsim.*
 * StaticForceField.java
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
package playground.gregor.sim2d_v2.simulation.floor;

import java.util.Collection;

import org.matsim.core.utils.collections.QuadTree;

import playground.gregor.sim2d.simulation.Force;

import com.vividsolutions.jts.geom.Coordinate;

public class StaticForceField {

	private final QuadTree<Force> forceQuad;

	public StaticForceField(QuadTree<Force> forceQuad) {
		this.forceQuad = forceQuad;
	}

	public Force getForceWithin(Coordinate location, double range) {
		if (this.forceQuad.get(location.x, location.y, range).size() > 0) {
			return this.forceQuad.get(location.x, location.y);
		}
		return null;
	}

	public Collection<Force> getForcesWithin(Coordinate location, double range) {
		return this.forceQuad.get(location.x, location.y, range);
	}

	public Collection<Force> getForces() {
		return this.forceQuad.values();
	}

	public void addForce(Force force) {
		this.forceQuad.put(force.getXCoord(), force.getYCoord(), force);
	}
}
