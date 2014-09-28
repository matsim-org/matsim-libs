/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityOption;

import playground.johannes.gsv.misc.QuadTree;

/**
 * @author johannes
 * 
 */
public class FacilityData {

	private static FacilityData instance;

	/**
	 * @deprecated
	 */
	public static synchronized FacilityData getInstance(ActivityFacilities facilities, Random random) {
		if (instance == null)
			instance = new FacilityData(facilities, random);

		return instance;
	}

	private Map<String, QuadTree<ActivityFacility>> quadTrees = new HashMap<>();

	private final Map<String, List<ActivityFacility>> facilitiesMap;

	private final Random random;

	public FacilityData(ActivityFacilities facilities, Random random) {
		System.out.println("Loading random facilities...");
		this.random = random;
		facilitiesMap = new HashMap<String, List<ActivityFacility>>();

		for (ActivityFacility facility : facilities.getFacilities().values()) {
			for (ActivityOption option : facility.getActivityOptions().values()) {
				List<ActivityFacility> list = facilitiesMap.get(option.getType());

				if (list == null) {
					list = new LinkedList<ActivityFacility>();
					facilitiesMap.put(option.getType(), list);
				}

				// int cap = (int) option.getCapacity();
				// cap = Math.max(1, cap/10);
				// for(int i = 0; i < cap; i++) {
				list.add(facility);
				// }
			}
		}

		for (Entry<String, List<ActivityFacility>> entry : facilitiesMap.entrySet()) {
			entry.setValue(new ArrayList<ActivityFacility>(entry.getValue()));
		}

		System.out.println("Done.");
	}

	public ActivityFacility randomFacility(String type) {
		List<ActivityFacility> list = facilitiesMap.get(type);
		if (list != null) {
			return list.get(random.nextInt(list.size()));
		} else {
			return null;
		}
	}

	public List<ActivityFacility> getFacilities(String type) {
		return facilitiesMap.get(type);
	}

	public ActivityFacility getClosest(Coord coord, String type) {
		initQuadTree(type);

		QuadTree<ActivityFacility> quadTree = quadTrees.get(type);
		return quadTree.get(coord.getX(), coord.getY());
	}

	public QuadTree<ActivityFacility> getQuadTree(String type) {
		initQuadTree(type);
		return quadTrees.get(type);
	}

	private void initQuadTree(String type) {
		// if(quadTrees == null) {
		// quadTrees = new HashMap<>();

		// for(Entry<String, List<ActivityFacility>> entry :
		// facilitiesMap.entrySet()) {

		QuadTree<ActivityFacility> quadtree = quadTrees.get(type);
		if (quadtree == null) {
			List<ActivityFacility> facilities = facilitiesMap.get(type);
			double minx = Double.MAX_VALUE;
			double miny = Double.MAX_VALUE;
			double maxx = 0;
			double maxy = 0;

			for (ActivityFacility fac : facilities) {
				minx = Math.min(minx, fac.getCoord().getX());
				miny = Math.min(miny, fac.getCoord().getY());
				maxx = Math.max(maxx, fac.getCoord().getX());
				maxy = Math.max(maxy, fac.getCoord().getY());
			}

			quadtree = new QuadTree<>(minx, miny, maxx, maxy);

			for (ActivityFacility fac : facilities) {
				quadtree.put(fac.getCoord().getX(), fac.getCoord().getY(), fac);
			}

			quadTrees.put(type, quadtree);
		}
	}
	// }
}
