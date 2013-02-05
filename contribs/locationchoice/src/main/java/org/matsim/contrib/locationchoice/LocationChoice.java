/* *********************************************************************** *
 * project: org.matsim.*
 * LocationChoice.java
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.locationchoice.bestresponse.BestResponseLocationMutator;
import org.matsim.contrib.locationchoice.bestresponse.DestinationSampler;
import org.matsim.contrib.locationchoice.bestresponse.preprocess.ComputeKValsAndMaxEpsilon;
import org.matsim.contrib.locationchoice.bestresponse.scoring.ScaleEpsilon;
import org.matsim.contrib.locationchoice.random.RandomLocationMutator;
import org.matsim.contrib.locationchoice.timegeography.RecursiveLocationMutator;
import org.matsim.contrib.locationchoice.timegeography.SingleActLocationMutator;
import org.matsim.contrib.locationchoice.utils.ActTypeConverter;
import org.matsim.contrib.locationchoice.utils.ActivitiesHandler;
import org.matsim.contrib.locationchoice.utils.QuadTreeRing;
import org.matsim.contrib.locationchoice.utils.TreesBuilder;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.groups.LocationChoiceConfigGroup;
import org.matsim.core.config.groups.LocationChoiceConfigGroup.Algotype;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.utils.objectattributes.ObjectAttributes;

public class LocationChoice extends AbstractMultithreadedModule {

	/**
	 * yyyy It is unclear to me why we need this as a collection and not just as a variable.  kai, dec'12
	 */
	private final List<PlanAlgorithm>  planAlgoInstances = new Vector<PlanAlgorithm>();

	private static final Logger log = Logger.getLogger(LocationChoice.class);
	private ObjectAttributes personsMaxEpsUnscaled;
//	private ObjectAttributes facilitiesKValues = new ObjectAttributes();
//	private ObjectAttributes personsKValues = new ObjectAttributes();
	
	private static int facKVals = 0 ;
	private static int persKVals = 1 ;
	private static int maxEpsUnsc = 2 ;

	private ScaleEpsilon scaleEpsilon;
	private ActivitiesHandler defineFlexibleActivities;
	private ActTypeConverter actTypeConverter;
	private DestinationSampler sampler;

	protected TreeMap<String, QuadTreeRing<ActivityFacility>> quadTreesOfType = new TreeMap<String, QuadTreeRing<ActivityFacility>>();

	protected TreeMap<String, ActivityFacilityImpl []> facilitiesOfType = new TreeMap<String, ActivityFacilityImpl []>();
	private final Scenario scenario;

	public LocationChoice(Scenario scenario) {
		super(scenario.getConfig().global());
		if ( LocationChoiceConfigGroup.Algotype.bestResponse.equals(scenario.getConfig().locationchoice().getAlgorithm()) ) {
			throw new RuntimeException("best response location choice not supported as part of LocationChoice. " +
					"Use BestReplyLocationChoice instead, but be aware that as of now some Java coding is necessary to do that. kai, feb'13") ;
			// yyyyyy the best reply code pieces can be removed from this here.  kai, feb'13
		}
		this.scenario = scenario;
		initLocal();
	}

	private void initLocal() {
		this.defineFlexibleActivities = new ActivitiesHandler(this.scenario.getConfig().locationchoice());
		((NetworkImpl) this.scenario.getNetwork()).connect();

		this.actTypeConverter = this.defineFlexibleActivities.getConverter();
		this.initTrees(((ScenarioImpl) this.scenario).getActivityFacilities(), this.scenario.getConfig().locationchoice());

		if ( LocationChoiceConfigGroup.Algotype.bestResponse.equals(this.scenario.getConfig().locationchoice().getAlgorithm())) {
			this.scaleEpsilon = this.defineFlexibleActivities.createScaleEpsilon();
			List<ObjectAttributes> epsilons = this.readOrCreateEpsilons(Long.parseLong(this.scenario.getConfig().locationchoice().getRandomSeed()));
//			this.sampler = new DestinationSampler(this.personsKValues, this.facilitiesKValues, this.scenario.getConfig().locationchoice());
			this.sampler = new DestinationSampler(epsilons.get(persKVals), epsilons.get(facKVals), this.scenario.getConfig().locationchoice());
			this.personsMaxEpsUnscaled = epsilons.get(maxEpsUnsc) ;
		}
	}

	private List<ObjectAttributes> readOrCreateEpsilons(long seed) {
//		this.personsMaxEpsUnscaled = new ObjectAttributes();

		// check if object attributes files are available, other wise do preprocessing
		String maxEpsValues = this.scenario.getConfig().locationchoice().getMaxEpsFile();

		if (!maxEpsValues.equals("null")) {
//			ObjectAttributesXmlReader attributesReader = new ObjectAttributesXmlReader(this.personsMaxEpsUnscaled);
//			try {
//				attributesReader.parse(maxEpsValues);
//			} catch  (UncheckedIOException e) {  // reading was not successful
//				this.computeEpsilons(seed);
//			}
			throw new RuntimeException("yyyyyy I cannot see how the reading from file obtains the person and facility values.  " +
			"Since I cannot see this, I am also not able to modify the code accordingly.  kai, jan'13" ) ;
		}
		else {
			return this.computeEpsilons(seed);
		}
	}

	private List<ObjectAttributes> computeEpsilons(long seed) {
		ComputeKValsAndMaxEpsilon computer = new ComputeKValsAndMaxEpsilon(
				seed, this.scenario, this.scaleEpsilon, 
				this.actTypeConverter, defineFlexibleActivities.getFlexibleTypes());

		computer.run();
		
		// the reason for the following somewhat strange construct is that I want to _return_ the result, rather than
		// having it as a side effect. kai, feb'13
		List<ObjectAttributes> epsilons = new ArrayList<ObjectAttributes>(3) ;
		for ( int ii=0 ; ii<3 ; ii++ ) {
			if ( ii==persKVals ) {
				epsilons.add( computer.getPersonsKValues() ) ;
			} else if ( ii==facKVals ) {
				epsilons.add( computer.getFacilitiesKValues() ) ;
			} else if ( ii==maxEpsUnsc ) {
				epsilons.add( computer.getPersonsMaxEpsUnscaled() ) ;
			}
		}
		

//		this.personsMaxEpsUnscaled = computer.getPersonsMaxEpsUnscaled();
//		this.personsKValues = computer.getPersonsKValues();
//		this.facilitiesKValues = computer.getFacilitiesKValues();
		
//		System.err.println( "ff:\n" + this.facilitiesKValues.toString() ) ;
//		System.err.println( "pp:\n" + this.personsKValues.toString() ) ;
//		System.err.println( "max:\n" + this.personsMaxEpsUnscaled.toString() ) ;
		System.err.println( "ff:\n" + epsilons.get(facKVals).toString() ) ;
		System.err.println( "pp:\n" + epsilons.get(persKVals).toString() ) ;
		System.err.println( "max:\n" + epsilons.get(maxEpsUnsc).toString() ) ;
		
		return epsilons ;
		
	}


	/**
	 * Initialize the quadtrees of all available activity types
	 */
	private void initTrees(ActivityFacilities facilities, LocationChoiceConfigGroup config) {
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
		Algotype algorithm = this.scenario.getConfig().locationchoice().getAlgorithm();

		if ( LocationChoiceConfigGroup.Algotype.localSearchRecursive.equals(algorithm) 
				|| LocationChoiceConfigGroup.Algotype.localSearchSingleAct.equals(algorithm) ) {
			int unsuccessfull = 0;
			Iterator<PlanAlgorithm> planAlgo_it = this.planAlgoInstances.iterator();
			while (planAlgo_it.hasNext()) {
				PlanAlgorithm plan_algo = planAlgo_it.next();

				if ( LocationChoiceConfigGroup.Algotype.localSearchSingleAct.equals(algorithm) ) {
					unsuccessfull += ((SingleActLocationMutator)plan_algo).getNumberOfUnsuccessfull();
					((SingleActLocationMutator)plan_algo).resetUnsuccsessfull();
				}
				else if ( LocationChoiceConfigGroup.Algotype.localSearchRecursive.equals(algorithm) ) {
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
		Algotype algorithm = this.scenario.getConfig().locationchoice().getAlgorithm();
		switch ( algorithm ) {
		case random:
			this.planAlgoInstances.add(new RandomLocationMutator(this.scenario,  
					this.quadTreesOfType, this.facilitiesOfType, MatsimRandom.getLocalInstance()));
			break ;
		case bestResponse:
			// this one corresponds to the "frozen epsilon" paper(s)
			// the random number generators are re-seeded anyway in the dc module. So we do not need a MatsimRandom instance here
			this.planAlgoInstances.add(new BestResponseLocationMutator(this.scenario,   
					this.quadTreesOfType, this.facilitiesOfType, this.personsMaxEpsUnscaled, 
					this.scaleEpsilon, this.actTypeConverter, this.sampler, this.getReplanningContext()));
			// yyyy the k-values are carried into that class only via the sampler.  However, the sampler does not return them.  Thus there
			// needs to be another channel to get them in there.  Presumably via the scoring function, but I don't think that it will
			// do that without further measures somewhere else.  kai, feb'13
			break ;
		case localSearchRecursive:
			this.planAlgoInstances.add(new RecursiveLocationMutator(this.scenario, this.getReplanningContext().getTripRouterFactory().createTripRouter(),  
					this.quadTreesOfType, this.facilitiesOfType, MatsimRandom.getLocalInstance()));
			break ;
		case localSearchSingleAct:
			this.planAlgoInstances.add(new SingleActLocationMutator(this.scenario, this.quadTreesOfType, 
					this.facilitiesOfType, MatsimRandom.getLocalInstance()));
			break ;
		}		
		return this.planAlgoInstances.get(this.planAlgoInstances.size()-1);
	}

}
