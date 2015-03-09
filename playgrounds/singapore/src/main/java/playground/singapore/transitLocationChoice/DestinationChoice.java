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

package playground.singapore.transitLocationChoice;

import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.locationchoice.random.RandomLocationMutator;
import org.matsim.contrib.locationchoice.timegeography.SingleActLocationMutator;
import org.matsim.contrib.locationchoice.utils.QuadTreeRing;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup.Algotype;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.population.algorithms.PlanAlgorithm;

public class DestinationChoice extends AbstractMultithreadedModule {

	/**
	 * yyyy It is unclear to me why we need this as a collection and not just as a variable.  kai, dec'12
	 */
	private final List<PlanAlgorithm>  planAlgoInstances = new Vector<PlanAlgorithm>();
	private static final Logger log = Logger.getLogger(DestinationChoice.class);
	private ActivitiesHandler defineFlexibleActivities;
	private ActTypeConverter actTypeConverter;

	protected TreeMap<String, QuadTreeRing<ActivityFacility>> quadTreesOfType = new TreeMap<String, QuadTreeRing<ActivityFacility>>();

	protected TreeMap<String, ActivityFacilityImpl []> facilitiesOfType = new TreeMap<String, ActivityFacilityImpl []>();
	private final Scenario scenario;

	public DestinationChoice(Scenario scenario) {
		super(scenario.getConfig().global());
		if ( DestinationChoiceConfigGroup.Algotype.bestResponse.equals(
				((DestinationChoiceConfigGroup)scenario.getConfig().getModule("locationchoice")).getAlgorithm())) {
			throw new RuntimeException("best response location choice not supported as part of LocationChoice. " +
					"Use BestReplyLocationChoice instead, but be aware that as of now some Java coding is necessary to do that. kai, feb'13") ;
			// yyyyyy the best reply code pieces can be removed from this here.  kai, feb'13
			// done. ah feb'13
		}
		this.scenario = scenario;
		initLocal();
	}

	private void initLocal() {
		this.defineFlexibleActivities = new ActivitiesHandler((DestinationChoiceConfigGroup) this.scenario.getConfig().getModule("locationchoice"));
		((NetworkImpl) this.scenario.getNetwork()).connect();

		this.actTypeConverter = this.defineFlexibleActivities.getConverter();
		this.initTrees(((ScenarioImpl) this.scenario).getActivityFacilities(), (DestinationChoiceConfigGroup) this.scenario.getConfig().getModule("locationchoice"));
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
			Iterator<PlanAlgorithm> planAlgo_it = this.planAlgoInstances.iterator();
			while (planAlgo_it.hasNext()) {
				PlanAlgorithm plan_algo = planAlgo_it.next();

				if ( DestinationChoiceConfigGroup.Algotype.localSearchSingleAct.equals(algorithm) ) {
					unsuccessfull += ((SingleActLocationMutator)plan_algo).getNumberOfUnsuccessfull();
					((SingleActLocationMutator)plan_algo).resetUnsuccsessfull();
				}
				else if ( DestinationChoiceConfigGroup.Algotype.localSearchRecursive.equals(algorithm) ) {
					unsuccessfull += ((RecursiveLocationMutator)plan_algo).getNumberOfUnsuccessfull();
					((RecursiveLocationMutator)plan_algo).resetUnsuccsessfull();
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
			this.planAlgoInstances.add(new RecursiveLocationMutator(this.scenario, this.getReplanningContext().getTripRouter(),  
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
