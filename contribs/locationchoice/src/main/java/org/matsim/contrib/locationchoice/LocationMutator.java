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
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;

public abstract class LocationMutator implements PlanAlgorithm {
	// yy not clear why we need this as abstract class: does not have abstract methods.  Could as well be a final class that is used in the other classes.  kai, mar'19

	private TreeMap<String, ? extends QuadTree<ActivityFacility>> quadTreesOfType;

	// avoid costly call of .toArray() within handlePlan() (System.arraycopy()!)
	private TreeMap<String, ActivityFacilityImpl []> facilitiesOfType;
	private ActivitiesHandler defineFlexibleActivities;
	private boolean locationChoiceBasedOnKnowledge = true;
	private final Random random;

	private final Scenario scenario;
	private final DestinationChoiceConfigGroup dccg;

	// ----------------------------------------------------------

	public LocationMutator(final Scenario scenario, Random random) {
		this.dccg = (DestinationChoiceConfigGroup) scenario.getConfig().getModule(DestinationChoiceConfigGroup.GROUP_NAME);
		this.defineFlexibleActivities = new ActivitiesHandler(this.dccg);
		this.quadTreesOfType = new TreeMap<>();
		this.facilitiesOfType = new TreeMap<>();
		this.scenario = scenario;
		this.random = random;
		this.initLocal();
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
	
	private void initLocal() {

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

	protected final TreeMap<String, ? extends QuadTree<ActivityFacility>> getQuadTreesOfType(){
		return quadTreesOfType;
	}

	protected final void setQuadTreesOfType( TreeMap<String, ? extends QuadTree<ActivityFacility>> quadTreesOfType ){
		this.quadTreesOfType = quadTreesOfType;
	}

	protected final TreeMap<String, ActivityFacilityImpl[]> getFacilitiesOfType(){
		return facilitiesOfType;
	}

	protected final void setFacilitiesOfType( TreeMap<String, ActivityFacilityImpl[]> facilitiesOfType ){
		this.facilitiesOfType = facilitiesOfType;
	}

	protected final ActivitiesHandler getDefineFlexibleActivities(){
		return defineFlexibleActivities;
	}

	protected final void setDefineFlexibleActivities( ActivitiesHandler defineFlexibleActivities ){
		this.defineFlexibleActivities = defineFlexibleActivities;
	}

	protected final boolean isLocationChoiceBasedOnKnowledge(){
		return locationChoiceBasedOnKnowledge;
	}

	protected final void setLocationChoiceBasedOnKnowledge( boolean locationChoiceBasedOnKnowledge ){
		this.locationChoiceBasedOnKnowledge = locationChoiceBasedOnKnowledge;
	}

	protected final Random getRandom(){
		return random;
	}

	protected final Scenario getScenario(){
		return scenario;
	}

	protected final DestinationChoiceConfigGroup getDccg(){
		return dccg;
	}
}
