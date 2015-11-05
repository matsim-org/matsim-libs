/* *********************************************************************** *
 * project: org.matsim.*
 * LocationMutator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.contrib.locationchoice;

import java.util.Random;
import java.util.TreeMap;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.locationchoice.utils.ActivitiesHandler;
import org.matsim.contrib.locationchoice.utils.TreesBuilder;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.population.algorithms.PlanAlgorithm;

public abstract class LocationMutator implements PlanAlgorithm {

	protected TreeMap<String, ? extends QuadTree<ActivityFacility>> quadTreesOfType;

	// avoid costly call of .toArray() within handlePlan() (System.arraycopy()!)
	protected TreeMap<String, ActivityFacilityImpl []> facilitiesOfType;
	protected ActivitiesHandler defineFlexibleActivities;
	protected boolean locationChoiceBasedOnKnowledge = true;
	protected final Random random;

	protected final Scenario scenario;
	protected final DestinationChoiceConfigGroup dccg;

	// ----------------------------------------------------------

	public LocationMutator(final Scenario scenario, Random random) {
		this.dccg = (DestinationChoiceConfigGroup) scenario.getConfig().getModule(DestinationChoiceConfigGroup.GROUP_NAME);
		this.defineFlexibleActivities = new ActivitiesHandler(this.dccg);
		this.quadTreesOfType = new TreeMap<String, QuadTree<ActivityFacility>>();
		this.facilitiesOfType = new TreeMap<String, ActivityFacilityImpl []>();
		this.scenario = scenario;
		this.random = random;
		this.initLocal(scenario.getNetwork());
	}

	public LocationMutator(Scenario scenario, TreeMap<String, ? extends QuadTree<ActivityFacility>> quad_trees,
			TreeMap<String, ActivityFacilityImpl []> facilities_of_type, Random random) {
		this.dccg = (DestinationChoiceConfigGroup) scenario.getConfig().getModule(DestinationChoiceConfigGroup.GROUP_NAME);
		this.defineFlexibleActivities = new ActivitiesHandler(this.dccg);
		this.quadTreesOfType = quad_trees;
		this.facilitiesOfType = facilities_of_type;
		if (this.defineFlexibleActivities.getFlexibleTypes().size() > 0) {
			locationChoiceBasedOnKnowledge = false;
		}
		this.scenario = scenario;
		this.random = random;
	}
	
	private void initLocal(final Network network) {

		if (this.defineFlexibleActivities.getFlexibleTypes().size() > 0) {
			locationChoiceBasedOnKnowledge = false;
		}
		this.initTrees(scenario.getActivityFacilities());
	}

	/**
	 * Initialize the quadtrees of all available activity types
	 */
	private void initTrees(ActivityFacilities facilities) {
		TreesBuilder treesBuilder = new TreesBuilder(this.scenario.getNetwork(), (DestinationChoiceConfigGroup) this.scenario.getConfig().getModule(DestinationChoiceConfigGroup.GROUP_NAME));
		treesBuilder.createTrees(facilities);
		this.facilitiesOfType = treesBuilder.getFacilitiesOfType();
		this.quadTreesOfType = treesBuilder.getQuadTreesOfType();
	}

}