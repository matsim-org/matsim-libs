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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.locationchoice.bestresponse.BestResponseLocationMutator;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.locationchoice.bestresponse.DestinationSampler;
import org.matsim.contrib.locationchoice.utils.QuadTreeRing;
import org.matsim.contrib.locationchoice.utils.TreesBuilder;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.groups.LocationChoiceConfigGroup;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.BackwardFastMultiNodeDijkstra;
import org.matsim.core.router.MultiNodeDijkstra;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.priorityqueue.HasIndex;
import org.matsim.core.router.util.BackwardsFastMultiNodeDijkstraFactory;
import org.matsim.core.router.util.FastMultiNodeDijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.utils.objectattributes.ObjectAttributes;

import java.util.*;
import java.util.Map.Entry;

public class BestReplyDestinationChoice extends AbstractMultithreadedModule {

    private static final Logger log = Logger.getLogger(BestReplyDestinationChoice.class);

    private final List<PlanAlgorithm>  planAlgoInstances = new Vector<PlanAlgorithm>();
	private ObjectAttributes personsMaxEpsUnscaled;
	private DestinationSampler sampler;
	protected TreeMap<String, QuadTreeRing<ActivityFacilityWithIndex>> quadTreesOfType = new TreeMap<String, QuadTreeRing<ActivityFacilityWithIndex>>();
	protected TreeMap<String, ActivityFacilityImpl []> facilitiesOfType = new TreeMap<String, ActivityFacilityImpl []>();
	private final Scenario scenario;
	private DestinationChoiceBestResponseContext lcContext;
	private HashSet<String> flexibleTypes;
	private LeastCostPathCalculatorFactory forwardMultiNodeDijsktaFactory;
	private LeastCostPathCalculatorFactory backwardMultiNodeDijsktaFactory;
	
	public static double useScaleEpsilonFromConfig = -99.0;

	public BestReplyDestinationChoice(DestinationChoiceBestResponseContext lcContext, ObjectAttributes personsMaxDCScoreUnscaled) {
		super(lcContext.getScenario().getConfig().global());
		if ( !LocationChoiceConfigGroup.Algotype.bestResponse.equals(lcContext.getScenario().getConfig().locationchoice().getAlgorithm())) {
			throw new RuntimeException("wrong class for selected location choice algorithm type; aborting ...") ;
		}
		this.lcContext = lcContext;
		this.scenario = lcContext.getScenario();
		this.personsMaxEpsUnscaled = personsMaxDCScoreUnscaled;
		this.forwardMultiNodeDijsktaFactory = new FastMultiNodeDijkstraFactory(true);
		this.backwardMultiNodeDijsktaFactory = new BackwardsFastMultiNodeDijkstraFactory(true);
		
		initLocal();
	}

	private void initLocal() {
		this.flexibleTypes = this.lcContext.getFlexibleTypes() ;		
		((NetworkImpl) this.scenario.getNetwork()).connect();
		this.initTrees(this.scenario.getActivityFacilities(), this.scenario.getConfig().locationchoice());
		this.sampler = new DestinationSampler(
				this.lcContext.getPersonsKValuesArray(), 
				this.lcContext.getFacilitiesKValuesArray(), 
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
//		this.quadTreesOfType = treesBuilder.getQuadTreesOfType();
		
		/*
		 * Create a copy of the treesBuilder.getQuadTreesOfType() outcome where the
		 * ActivityFacility objects are replaced by ActivityFacilityWithIndex objects. 
		 */
		Map<Id, ActivityFacilityWithIndex> map = new HashMap<Id, ActivityFacilityWithIndex>();
		for (ActivityFacility activityFacility : facilities.getFacilities().values()) {
			int index = this.lcContext.getFacilityIndex(activityFacility.getId());
			map.put(activityFacility.getId(), new ActivityFacilityWithIndex(activityFacility, index));
		}
		
		TreeMap<String, QuadTreeRing<ActivityFacility>> quadTree = treesBuilder.getQuadTreesOfType();
		for (Entry<String, QuadTreeRing<ActivityFacility>> entry : quadTree.entrySet()) {
			String key = entry.getKey();
			 QuadTreeRing<ActivityFacility> value = entry.getValue();
			 
			 double minX = value.getMinEasting();
			 double maxX = value.getMaxEasting();
			 double minY = value.getMinNorthing();
			 double maxY = value.getMaxNorthing();
			 QuadTreeRing<ActivityFacilityWithIndex> quadTreeRing = new QuadTreeRing<ActivityFacilityWithIndex>(minX, minY, maxX, maxY);
			 for (ActivityFacility activityFacility : value.values()) {
				 quadTreeRing.put(activityFacility.getCoord().getX(), activityFacility.getCoord().getY(), map.get(activityFacility.getId()));
			 }
			 
			 this.quadTreesOfType.put(key, quadTreeRing);
		}
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
		
		ReplanningContext replanningContext = this.getReplanningContext();
		
		MultiNodeDijkstra forwardMultiNodeDijkstra = (MultiNodeDijkstra) this.forwardMultiNodeDijsktaFactory.createPathCalculator(this.scenario.getNetwork(), 
				replanningContext.getTravelDisutility(), this.getReplanningContext().getTravelTime());

		BackwardFastMultiNodeDijkstra backwardMultiNodeDijkstra = (BackwardFastMultiNodeDijkstra) this.backwardMultiNodeDijsktaFactory.createPathCalculator(
				this.scenario.getNetwork(), replanningContext.getTravelDisutility(), this.getReplanningContext().getTravelTime());
		
		// this one corresponds to the "frozen epsilon" paper(s)
		// the random number generators are re-seeded anyway in the dc module. So we do not need a MatsimRandom instance here

		TripRouter tripRouter = replanningContext.getTripRouter();
		ScoringFunctionFactory scoringFunctionFactory = replanningContext.getScoringFunctionFactory();
		int iteration = replanningContext.getIteration();
		
		this.planAlgoInstances.add(new BestResponseLocationMutator(this.quadTreesOfType, this.facilitiesOfType, this.personsMaxEpsUnscaled, 
				this.lcContext, this.sampler, tripRouter, forwardMultiNodeDijkstra, backwardMultiNodeDijkstra, scoringFunctionFactory, iteration));
		return this.planAlgoInstances.get(this.planAlgoInstances.size()-1);
	}
	

	public static final class ActivityFacilityWithIndex implements ActivityFacility, HasIndex {

		private final ActivityFacility activityFacility;
		private final int index;
		
		public ActivityFacilityWithIndex(ActivityFacility activityFacility, int index) {
			this.activityFacility = activityFacility;
			this.index = index;
		}
		
		@Override
		public Id getLinkId() {
			return this.activityFacility.getLinkId();
		}

		@Override
		public Coord getCoord() {
			return this.activityFacility.getCoord();
		}

		@Override
		public Id getId() {
			return this.activityFacility.getId();
		}

		@Override
		public Map<String, Object> getCustomAttributes() {
			return this.activityFacility.getCustomAttributes();
		}

		@Override
		public int getArrayIndex() {
			return this.index;
		}

		@Override
		public Map<String, ActivityOption> getActivityOptions() {
			return this.activityFacility.getActivityOptions();
		}

		@Override
		public void addActivityOption(ActivityOption option) {
			this.activityFacility.addActivityOption(option);
		}
	}
}
