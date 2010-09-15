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

package playground.anhorni.locationchoice.preprocess.facilities;

import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.locationchoice.LocationMutator;
import org.matsim.locationchoice.utils.QuadTreeRing;


public class FacilityQuadTreeBuilder {

	private static final Logger log = Logger.getLogger(LocationMutator.class);

	public QuadTreeRing<ActivityFacility> buildFacilityQuadTree(String type, List<ActivityFacility> facilities) {

		TreeMap<Id, ActivityFacility> treeMap = new TreeMap<Id, ActivityFacility>();
		// get all types of activities
		for (ActivityFacility f : facilities) {
			if (!treeMap.containsKey(f.getId())) {
				treeMap.put(f.getId(), f);
			}
		}
		return this.builFacQuadTree(type, treeMap);
	}


	public QuadTreeRing<ActivityFacility> buildFacilityQuadTree(String type, ActivityFacilitiesImpl facilities) {
		TreeMap<Id, ActivityFacility> treeMap = new TreeMap<Id, ActivityFacility>();
		// get all types of activities
		for (ActivityFacility f : facilities.getFacilitiesForActivityType(type).values()) {
			if (!treeMap.containsKey(f.getId())) {
				treeMap.put(f.getId(), f);
			}
		}
		return this.builFacQuadTree(type, treeMap);
	}


	public QuadTreeRing<ActivityFacility> builFacQuadTree(String type, TreeMap<Id, ActivityFacility> treeMap) {
		Gbl.startMeasurement();
		log.info(" building " + type + " facility quad tree");
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;

		for (final ActivityFacility f : treeMap.values()) {
			if (f.getCoord().getX() < minx) { minx = f.getCoord().getX(); }
			if (f.getCoord().getY() < miny) { miny = f.getCoord().getY(); }
			if (f.getCoord().getX() > maxx) { maxx = f.getCoord().getX(); }
			if (f.getCoord().getY() > maxy) { maxy = f.getCoord().getY(); }
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		System.out.println("        xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
		QuadTreeRing<ActivityFacility> quadtree = new QuadTreeRing<ActivityFacility>(minx, miny, maxx, maxy);
		for (final ActivityFacility f : treeMap.values()) {
			quadtree.put(f.getCoord().getX(),f.getCoord().getY(),f);
		}
		log.info("Number of facilities: " + quadtree.size());
		Gbl.printRoundTime();
		return quadtree;
	}
}
