/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                          *
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

import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup.Algotype;
import org.matsim.contrib.locationchoice.timegeography.RandomLocationMutator;
import org.matsim.contrib.locationchoice.timegeography.RecursiveLocationMutator;
import org.matsim.contrib.locationchoice.timegeography.SingleActLocationMutator;
import org.matsim.contrib.locationchoice.utils.ActTypeConverter;
import org.matsim.contrib.locationchoice.utils.ActivitiesHandler;
import org.matsim.contrib.locationchoice.utils.TreesBuilder;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;

import javax.inject.Provider;

public class DestinationChoice extends AbstractMultithreadedModule {

    private static final Logger log = Logger.getLogger(DestinationChoice.class);
	private final Provider<TripRouter> tripRouterProvider;

	private final List<PlanAlgorithm>  planAlgoInstances = new Vector<PlanAlgorithm>();
	private ActivitiesHandler defineFlexibleActivities;
	private ActTypeConverter actTypeConverter;

	protected TreeMap<String, QuadTree<ActivityFacility>> quadTreesOfType = new TreeMap<String, QuadTree<ActivityFacility>>();

	protected TreeMap<String, ActivityFacilityImpl []> facilitiesOfType = new TreeMap<String, ActivityFacilityImpl []>();
	private final Scenario scenario;

	public DestinationChoice(Provider<TripRouter> tripRouterProvider, Scenario scenario) {
		super(scenario.getConfig().global());
		this.tripRouterProvider = tripRouterProvider;
		if ( DestinationChoiceConfigGroup.Algotype.bestResponse.equals(((DestinationChoiceConfigGroup)scenario.getConfig().getModule("locationchoice")).getAlgorithm()) ) {
			throw new RuntimeException("best response location choice not supported as part of LocationChoice. " +
					"Use BestReplyLocationChoice instead, but be aware that as of now some Java coding is necessary to do that. kai, feb'13");
		}
		this.scenario = scenario;
		initLocal();
	}

	private void initLocal() {
		this.defineFlexibleActivities = new ActivitiesHandler((DestinationChoiceConfigGroup) this.scenario.getConfig().getModule("locationchoice"));
//		((NetworkImpl) this.scenario.getNetwork()).connect();

		this.actTypeConverter = this.defineFlexibleActivities.getConverter();
		this.initTrees(this.scenario.getActivityFacilities(), (DestinationChoiceConfigGroup) this.scenario.getConfig().getModule("locationchoice"));
	}

	/**
	 * Initialize the quadtrees of all available activity types
	 */
	private void initTrees(ActivityFacilities facilities, DestinationChoiceConfigGroup config) {
		log.info("Doing location choice for activities: " + defineFlexibleActivities.getFlexibleTypes().toString());
		TreesBuilder treesBuilder = new TreesBuilder(defineFlexibleActivities.getFlexibleTypes(), this.scenario.getNetwork(), config);
		treesBuilder.setActTypeConverter(actTypeConverter);
		treesBuilder.createTrees(facilities);
		this.facilitiesOfType = treesBuilder.getFacilitiesOfType();
		this.quadTreesOfType = treesBuilder.getQuadTreesOfType();
	}

	@Override
	protected final void beforeFinishReplanningHook() {
		Gbl.printMemoryUsage() ;
	}

	@Override
	protected final void afterFinishReplanningHook() {
		Algotype algorithm = ((DestinationChoiceConfigGroup)this.scenario.getConfig().getModule("locationchoice")).getAlgorithm();

		if ( DestinationChoiceConfigGroup.Algotype.localSearchRecursive.equals(algorithm) 
				|| DestinationChoiceConfigGroup.Algotype.localSearchSingleAct.equals(algorithm) ) {
			int unsuccessfull = 0;
            for (PlanAlgorithm plan_algo : this.planAlgoInstances) {
                if (Algotype.localSearchSingleAct.equals(algorithm)) {
                    unsuccessfull += ((SingleActLocationMutator) plan_algo).getNumberOfUnsuccessfull();
                    ((SingleActLocationMutator) plan_algo).resetUnsuccsessfull();
                } else if (Algotype.localSearchRecursive.equals(algorithm)) {
                    unsuccessfull += ((RecursiveLocationMutator) plan_algo).getNumberOfUnsuccessfull();
                    ((RecursiveLocationMutator) plan_algo).resetUnsuccsessfull();
                }
            }
			log.info("Number of unsuccessfull LC in this iteration: "+ unsuccessfull);
		}
		this.planAlgoInstances.clear();
	}

	@Override
	public final PlanAlgorithm getPlanAlgoInstance() {		
		Algotype algorithm = ((DestinationChoiceConfigGroup)this.scenario.getConfig().getModule("locationchoice")).getAlgorithm();
		switch ( algorithm ) {
		case random:
			this.planAlgoInstances.add(new RandomLocationMutator(this.scenario,  
					this.quadTreesOfType, this.facilitiesOfType, MatsimRandom.getLocalInstance()));
			break ;
		case localSearchRecursive:
			this.planAlgoInstances.add(new RecursiveLocationMutator(this.scenario, this.tripRouterProvider.get(),
					this.quadTreesOfType, this.facilitiesOfType, MatsimRandom.getLocalInstance()));
			break ;
		case localSearchSingleAct:
			this.planAlgoInstances.add(new SingleActLocationMutator(this.scenario, this.quadTreesOfType, 
					this.facilitiesOfType, MatsimRandom.getLocalInstance()));
			break ;
		case bestResponse:
			throw new RuntimeException("wrong class for this locachoice algo; aborting ...") ;
		}		
		return this.planAlgoInstances.get(this.planAlgoInstances.size()-1);
	}

}
