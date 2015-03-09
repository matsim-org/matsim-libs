/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.anhorni.csestimation;

import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.ActivityFacility;

public class Utils {
	
	private final static Logger log = Logger.getLogger(Utils.class);
	
	public static QuadTree<Location> buildLocationQuadTree(TreeMap<Id<Location>, ? extends Location> shops) {
		Gbl.startMeasurement();
		System.out.println("      building loc quad tree...");
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		for (Location loc : shops.values()) {
			if (loc.getCoord().getX() < minx) { minx = loc.getCoord().getX(); }
			if (loc.getCoord().getY() < miny) { miny = loc.getCoord().getY(); }
			if (loc.getCoord().getX() > maxx) { maxx = loc.getCoord().getX(); }
			if (loc.getCoord().getY() > maxy) { maxy = loc.getCoord().getY(); }
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		System.out.println("        xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
		QuadTree<Location> locQuadTree = new QuadTree<Location>(minx, miny, maxx, maxy);
		for (Location loc : shops.values()) {
			locQuadTree.put(loc.getCoord().getX(), loc.getCoord().getY(), loc);
		}
		Gbl.printRoundTime();
		log.info("Created tree with " + locQuadTree.size() + " locations");
		return locQuadTree;
	}
	
	public static QuadTree<ActivityFacility> buildLocationQuadTreeFacilities(TreeMap<Id<ActivityFacility>, ? extends ActivityFacility> shops) {
		Gbl.startMeasurement();
		System.out.println("      building loc quad tree...");
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		for (ActivityFacility loc : shops.values()) {
			if (loc.getCoord().getX() < minx) { minx = loc.getCoord().getX(); }
			if (loc.getCoord().getY() < miny) { miny = loc.getCoord().getY(); }
			if (loc.getCoord().getX() > maxx) { maxx = loc.getCoord().getX(); }
			if (loc.getCoord().getY() > maxy) { maxy = loc.getCoord().getY(); }
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		System.out.println("        xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
		QuadTree<ActivityFacility> facQuadTree = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);
		for (ActivityFacility f : shops.values()) {
			facQuadTree.put(f.getCoord().getX(), f.getCoord().getY(), f);
		}
		Gbl.printRoundTime();
		log.info("Created tree with " + facQuadTree.size() + " locations");
		return facQuadTree;
	}
}
