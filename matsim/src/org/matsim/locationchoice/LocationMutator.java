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

package org.matsim.locationchoice;

import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.api.facilities.ActivityFacilities;
import org.matsim.core.api.facilities.ActivityFacility;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.config.groups.LocationChoiceConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.knowledges.Knowledges;
import org.matsim.locationchoice.utils.DefineFlexibleActivities;
import org.matsim.locationchoice.utils.QuadTreeRing;
import org.matsim.locationchoice.utils.TreesBuilder;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;


public abstract class LocationMutator extends AbstractPersonAlgorithm implements PlanAlgorithm {

	protected NetworkLayer network = null;
	protected Controler controler = null;	
	protected TreeMap<String, QuadTreeRing<ActivityFacility>> quadTreesOfType;
	
	// avoid costly call of .toArray() within handlePlan() (System.arraycopy()!)
	protected TreeMap<String, ActivityFacility []> facilitiesOfType;
	protected final LocationChoiceConfigGroup config;
	
	protected DefineFlexibleActivities defineFlexibleActivities;
	protected boolean locationChoiceBasedOnKnowledge = true;
	protected Knowledges knowledges = null;
			
	private static final Logger log = Logger.getLogger(LocationMutator.class);
	// ----------------------------------------------------------

	public LocationMutator(final NetworkLayer network, final Controler controler, final Knowledges kn) {
		this.knowledges = kn;
		this.defineFlexibleActivities = new DefineFlexibleActivities(this.knowledges);
		this.quadTreesOfType = new TreeMap<String, QuadTreeRing<ActivityFacility>>();
		this.facilitiesOfType = new TreeMap<String, ActivityFacility []>();
		this.config = Gbl.getConfig().locationchoice();
		this.initLocal(network, controler);		
	}
	
	
	public LocationMutator(final NetworkLayer network, final Controler controler, final Knowledges kn,
			TreeMap<String, QuadTreeRing<ActivityFacility>> quad_trees,
			TreeMap<String, ActivityFacility []> facilities_of_type) {
		this.knowledges = kn;
		this.defineFlexibleActivities = new DefineFlexibleActivities(this.knowledges);
		this.quadTreesOfType = quad_trees;
		this.facilitiesOfType = facilities_of_type;
		this.config = Gbl.getConfig().locationchoice();	
		this.network = network;
		this.controler = controler;
		if (this.defineFlexibleActivities.getFlexibleTypes().size() > 0) {
			locationChoiceBasedOnKnowledge = false;
		}
	}
	
	private void initLocal(final NetworkLayer network, Controler controler) {	
		
		if (this.defineFlexibleActivities.getFlexibleTypes().size() > 0) {
			locationChoiceBasedOnKnowledge = false;
		}	
		this.initTrees(controler.getFacilities());				
		this.network = network;
		this.controler = controler;
	}
		
	/*
	 * Initialize the quadtrees of all available activity types
	 */
	
	private void initTrees(ActivityFacilities facilities) {
		TreesBuilder treesBuilder = new TreesBuilder(this.network);
		treesBuilder.createTrees(facilities);
		this.facilitiesOfType = treesBuilder.getFacilitiesOfType();
		this.quadTreesOfType = treesBuilder.getQuadTreesOfType();
	}
	
	public abstract void handlePlan(final Plan plan);


	@Override
	public void run(final Person person) {
		final int nofPlans = person.getPlans().size();

		for (int planId = 0; planId < nofPlans; planId++) {
			final Plan plan = person.getPlans().get(planId);
			handlePlan(plan);
		}
	}

	public void run(final Plan plan) {	
		handlePlan(plan);
	}
	
	public Controler getControler() {
		return controler;
	}

	public void setControler(Controler controler) {
		this.controler = controler;
	}
		
	protected List<ActivityImpl>  defineMovablePrimaryActivities(final Plan plan) {				
		return this.defineFlexibleActivities.getMovablePrimaryActivities(plan);
	}
	
	protected void resetRoutes(final Plan plan) {
		// loop over all <leg>s, remove route-information
		// routing is done after location choice
		final List<?> actslegs = plan.getPlanElements();
		for (int j = 1; j < actslegs.size(); j=j+2) {
			final Leg leg = (Leg)actslegs.get(j);
			leg.setRoute(null);
		}
	}
}
