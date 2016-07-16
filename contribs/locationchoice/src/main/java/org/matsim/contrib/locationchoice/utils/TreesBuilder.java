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

package org.matsim.contrib.locationchoice.utils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOption;

public class TreesBuilder {

	private Network network = null;
	private static final Logger log = Logger.getLogger(TreesBuilder.class);
	private Set<String> flexibleTypes = new HashSet<String>();
	private final DestinationChoiceConfigGroup config;
	
	protected TreeMap<String, QuadTree<ActivityFacility>> quadTreesOfType = new TreeMap<String, QuadTree<ActivityFacility>>();
	protected TreeMap<String, ActivityFacilityImpl []> facilitiesOfType = new TreeMap<String, ActivityFacilityImpl []>();
	
	private ActTypeConverter converter = new ActTypeConverter(true);


	public TreesBuilder(Set<String> flexibleTypes, Network network, DestinationChoiceConfigGroup config) {
		this.flexibleTypes = flexibleTypes;
		this.network = network;
		this.config = config;
	}

	public TreesBuilder(Network network, DestinationChoiceConfigGroup config) {
		this.network = network;
		this.config = config;
		this.initFlexibleTypes();
	}

	private void initFlexibleTypes() {
		String types = config.getFlexibleTypes();

		if (!types.equals("null")) {
			log.info("Doing location choice for activity types: " + types);
			String[] entries = types.split(",", -1);
			for (int i = 0; i < entries.length; i++) {
				if (!entries[i].trim().equals("null")) {
					this.flexibleTypes.add(this.converter.convertType(entries[i].trim()));
				}
			}
		}
	}

	public void createTrees(ActivityFacilities facilities) {
		TreeMap<String, TreeMap<Id<ActivityFacility>, ActivityFacility>> treesForTypes = this.createTreesForTypes(facilities);
		this.createQuadTreesAndArrays(treesForTypes);
	}

	private TreeMap<String, TreeMap<Id<ActivityFacility>, ActivityFacility>> createTreesForTypes(ActivityFacilities facilities) {

		boolean regionalScenario = false;
		double radius = 0.0;
		Node centerNode = null;

		if (this.config.getCenterNode() != null && this.config.getRadius() != null) {
			regionalScenario = true;
			centerNode = this.network.getNodes().get(Id.create(config.getCenterNode(), Node.class));
			radius = config.getRadius();
			log.info("Building trees regional scenario");
		}
		else {
			log.info("Building trees complete scenario");
		}

		TreeMap<String, TreeMap<Id<ActivityFacility>, ActivityFacility>> trees = new TreeMap<>();
		// get all types of activities
		for (ActivityFacility f : facilities.getFacilities().values()) {
			Map<String, ? extends ActivityOption> facilityActOpts = f.getActivityOptions();

			// do not add facility if it is not in region of interest ------------------------
			if (regionalScenario && (CoordUtils.calcEuclideanDistance(f.getCoord(), centerNode.getCoord()) > radius)) {
				continue;
			}
			// -------------------------------------------------------------------------------

			Iterator<? extends ActivityOption> actOpt_it = facilityActOpts.values().iterator();
			while (actOpt_it.hasNext()) {
				ActivityOption actOpt = actOpt_it.next();

				// if flexibleTypes is empty we add all types to trees as potentially all types can be relocated
				// otherwise we add all types given by flexibleTypes
				if (this.flexibleTypes.size() == 0 ||  this.flexibleTypes.contains(this.converter.convertType(actOpt.getType()))) {
					if (!trees.containsKey(this.converter.convertType(actOpt.getType()))) {
						trees.put(this.converter.convertType(actOpt.getType()), new TreeMap<Id<ActivityFacility>, ActivityFacility>());
					}
					trees.get(this.converter.convertType(actOpt.getType())).put(f.getId(), f);
				}
			}
		}
		return trees;
	}

	private void createQuadTreesAndArrays(TreeMap<String, TreeMap<Id<ActivityFacility>, ActivityFacility>> trees) {
		Iterator<TreeMap<Id<ActivityFacility>, ActivityFacility>> tree_it = trees.values().iterator();
		Iterator<String> type_it = trees.keySet().iterator();

		while (tree_it.hasNext()) {
			TreeMap<Id<ActivityFacility>, ActivityFacility> tree_of_type = tree_it.next();
			String type = type_it.next();

			// do not construct tree for home and tta act
//			if (type.startsWith("h") || type.startsWith("tta")) continue;	// startsWith("h") also removed oder activity types such as "hotel". cdobler, nov'14
			if (type.equals("h") || type.equals("home") || type.startsWith("tta")) continue;

			this.quadTreesOfType.put(this.converter.convertType(type), this.buildFacQuadTree(this.converter.convertType(type), tree_of_type));
			this.facilitiesOfType.put(this.converter.convertType(type), tree_of_type.values().toArray(new ActivityFacilityImpl[tree_of_type.size()]));
		}
	}

	private QuadTree<ActivityFacility> buildFacQuadTree(String type, TreeMap<Id<ActivityFacility>,ActivityFacility> facilities_of_type) {
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
		log.info("    done");
		Gbl.printRoundTime();
		Gbl.printMemoryUsage();
		return quadtree;
	}

	public TreeMap<String, QuadTree<ActivityFacility>> getQuadTreesOfType() {
		return quadTreesOfType;
	}
	public TreeMap<String, ActivityFacilityImpl[]> getFacilitiesOfType() {
		return facilitiesOfType;
	}

	public ActTypeConverter getActTypeConverter() {
		return converter;
	}

	public void setActTypeConverter(ActTypeConverter converter) {
		this.converter = converter;
	}
}
