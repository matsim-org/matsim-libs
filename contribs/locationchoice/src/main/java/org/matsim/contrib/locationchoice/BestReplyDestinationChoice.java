/* *********************************************************************** *
 * project: org.matsim.*
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

import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.locationchoice.bestresponse.BestResponseLocationMutator;
import org.matsim.contrib.locationchoice.bestresponse.DestinationSampler;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.locationchoice.utils.QuadTreeRing;
import org.matsim.contrib.locationchoice.utils.TreesBuilder;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.groups.LocationChoiceConfigGroup;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.utils.objectattributes.ObjectAttributes;

public class BestReplyDestinationChoice extends AbstractMultithreadedModule {

	/**
	 * yyyy It is unclear to me why we need this as a collection and not just as a variable.  kai, dec'12
	 */
	private final List<PlanAlgorithm>  planAlgoInstances = new Vector<PlanAlgorithm>();

	private static final Logger log = Logger.getLogger(BestReplyDestinationChoice.class);
	private ObjectAttributes personsMaxEpsUnscaled;
	private DestinationSampler sampler;
	protected TreeMap<String, QuadTreeRing<ActivityFacility>> quadTreesOfType = new TreeMap<String, QuadTreeRing<ActivityFacility>>();
	protected TreeMap<String, ActivityFacilityImpl []> facilitiesOfType = new TreeMap<String, ActivityFacilityImpl []>();
	private final Scenario scenario;
	private DestinationChoiceBestResponseContext lcContext;
	private HashSet<String> flexibleTypes;
	public static double useScaleEpsilonFromConfig = -99.0;

	public BestReplyDestinationChoice(DestinationChoiceBestResponseContext lcContext, ObjectAttributes personsMaxDCScoreUnscaled) {
		super(lcContext.getScenario().getConfig().global());
		if ( !LocationChoiceConfigGroup.Algotype.bestResponse.equals(lcContext.getScenario().getConfig().locationchoice().getAlgorithm())) {
			throw new RuntimeException("wrong class for selected location choice algorithm type; aborting ...") ;
		}
		this.lcContext = lcContext ;
		this.scenario = lcContext.getScenario() ;
		this.personsMaxEpsUnscaled = personsMaxDCScoreUnscaled;
		initLocal();
		
	}

	private void initLocal() {
		this.flexibleTypes = this.lcContext.getFlexibleTypes() ;		
		((NetworkImpl) this.scenario.getNetwork()).connect(); // ???	
		this.initTrees(((ScenarioImpl) this.scenario).getActivityFacilities(), this.scenario.getConfig().locationchoice());	
		this.sampler = new DestinationSampler(
				this.lcContext.getPersonsKValues(), 
				this.lcContext.getFacilitiesKValues(), 
				this.scenario.getConfig().locationchoice());
	}

	/**
	 * Initialize the quadtrees of all available activity types
	 */
	private void initTrees(ActivityFacilities facilities, LocationChoiceConfigGroup config) {
		log.info("Doing location choice for activities: " + this.flexibleTypes.toString());
		TreesBuilder treesBuilder = new TreesBuilder(this.flexibleTypes, this.scenario.getNetwork(), config);
		treesBuilder.setActTypeConverter(this.lcContext.getConverter());
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
		this.planAlgoInstances.clear();
	}

	@Override
	public final PlanAlgorithm getPlanAlgoInstance() {		
		// this one corresponds to the "frozen epsilon" paper(s)
		// the random number generators are re-seeded anyway in the dc module. So we do not need a MatsimRandom instance here
		this.planAlgoInstances.add(new BestResponseLocationMutator(this.quadTreesOfType, this.facilitiesOfType, this.personsMaxEpsUnscaled, 
				this.lcContext, this.sampler, this.getReplanningContext()));
		return this.planAlgoInstances.get(this.planAlgoInstances.size()-1);
	}
}
