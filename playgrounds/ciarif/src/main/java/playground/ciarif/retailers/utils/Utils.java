/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.ciarif.retailers.utils;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.world.World;

public abstract class Utils {


	private final static double EPSILON = 0.0001;
	public static final void moveFacility(ActivityFacilityImpl f, Link link, World world) {
		double [] vector = new double[2];
		vector[0] = link.getToNode().getCoord().getY()-link.getFromNode().getCoord().getY();
		vector[1] = -(link.getToNode().getCoord().getX()-link.getFromNode().getCoord().getX());
//		double length = Math.sqrt(vector[0]*vector[0]+vector[1]*vector[1]);
//		System.out.println("length = " + length);
		Coord coord = new CoordImpl(link.getCoord().getX()+vector[0]*EPSILON,link.getCoord().getY()+vector[1]*EPSILON);
		f.setCoord(coord);

		throw new RuntimeException("World / Layer no longer exists.");
//		Link oldL = ((NetworkImpl) world.getLayer(NetworkImpl.LAYER_TYPE)).getLinks().get(f.getLinkId());
//		if (oldL != null) {
			// world.removeMapping(f, (LinkImpl) oldL);
//		}
		// world.addMapping(f, (LinkImpl) link);

	}

	// BAD CODE STYLE but keep that anyway for the moment
	private static QuadTree<Person> personQuadTree = null;
	private static QuadTree<ActivityFacility> facilityQuadTree = null;

	public static final void setPersonQuadTree(QuadTree<Person> personQuadTree) {
		Utils.personQuadTree = personQuadTree;
	}

	public static final QuadTree<Person> getPersonQuadTree() {
		return Utils.personQuadTree;
	}

	public static final void setFacilityQuadTree(QuadTree<ActivityFacility> facilityQuadTree) {
		Utils.facilityQuadTree  = facilityQuadTree;
	}

	public static final QuadTree<ActivityFacility> getFacilityQuadTree() {
		return Utils.facilityQuadTree;
	}
}
