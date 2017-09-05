/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package utils;

import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.ActivityFacility;

public class BuildTrees {
	
	private final static Logger log = Logger.getLogger(BuildTrees.class);
	
	public QuadTree<ActivityFacility> createActivitiesTree(String activityType, Scenario scenario) {
		QuadTree<ActivityFacility> facQuadTree;
		
		if (activityType.equals("all")) {
			facQuadTree = this.builFacQuadTree(
					activityType, scenario.getActivityFacilities().getFacilities());	
		}
		else {
			facQuadTree = this.builFacQuadTree(
				activityType, ((MutableScenario)scenario).getActivityFacilities().getFacilitiesForActivityType(activityType));	
		}
		return facQuadTree;
	}
	
	public QuadTree<ActivityFacility> createActivitiesTree(String [] activityTypes, String mainType, Scenario scenario) {
		Map<Id<ActivityFacility>, ActivityFacility> facilities_of_type = new TreeMap<Id<ActivityFacility>, ActivityFacility>();
			
		for (String activityType : activityTypes) {
			facilities_of_type.putAll(((MutableScenario)scenario).getActivityFacilities().getFacilitiesForActivityType(activityType));
		}
		return this.builFacQuadTree(mainType, facilities_of_type);
	}
	
	private QuadTree<ActivityFacility> builFacQuadTree(String type, Map<Id<ActivityFacility>, ? extends ActivityFacility> facilities_of_type) {
		Gbl.startMeasurement();
		log.info(" building " + type + " facility quad tree");
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;

		for (final ActivityFacility f : facilities_of_type.values()) {
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
		QuadTree<ActivityFacility> quadtree = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);
		for (final ActivityFacility f : facilities_of_type.values()) {
			quadtree.put(f.getCoord().getX(),f.getCoord().getY(),f);
		}
		log.info("Quadtree size: " + quadtree.size());
		return quadtree;
	}
}
