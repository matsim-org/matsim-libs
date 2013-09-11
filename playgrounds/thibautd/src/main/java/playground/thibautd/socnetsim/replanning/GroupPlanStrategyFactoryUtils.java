/* *********************************************************************** *
 * project: org.matsim.*
 * GroupPlanStrategyFactoryUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.socnetsim.replanning;

import org.matsim.core.config.Config;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.TripRouterFactoryInternal;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.population.algorithms.TripsToLegsAlgorithm;

import playground.thibautd.router.PlanRoutingAlgorithmFactory;
import playground.thibautd.socnetsim.population.JointPlanFactory;
import playground.thibautd.socnetsim.replanning.grouping.GroupPlans;
import playground.thibautd.socnetsim.replanning.modules.RecomposeJointPlanAlgorithm.PlanLinkIdentifier;
import playground.thibautd.socnetsim.replanning.modules.RecomposeJointPlanModule;
import playground.thibautd.socnetsim.replanning.modules.SynchronizeCoTravelerPlansModule;
import playground.thibautd.socnetsim.replanning.selectors.IncompatiblePlansIdentifierFactory;
import playground.thibautd.socnetsim.replanning.selectors.RandomGroupLevelSelector;

/**
 * @author thibautd
 */
public class GroupPlanStrategyFactoryUtils {

	public static GroupPlanStrategy createRandomSelectingStrategy(
			final IncompatiblePlansIdentifierFactory fact) {
		return new GroupPlanStrategy(
				new RandomGroupLevelSelector(
					MatsimRandom.getLocalInstance(),
					fact) );
	}

	// /////////////////////////////////////////////////////////////////////////
	// standard modules
	// /////////////////////////////////////////////////////////////////////////
	public static GenericStrategyModule<GroupPlans> createTripsToLegsModule(
			final Config config,
			final TripRouterFactoryInternal tripRouterFactory) {
		return new IndividualBasedGroupStrategyModule(
				new AbstractMultithreadedModule( config.global() ) {
					@Override
					public PlanAlgorithm getPlanAlgoInstance() {
						return new TripsToLegsAlgorithm( tripRouterFactory.instantiateAndConfigureTripRouter() );
					}
				});
	}

	public static GenericStrategyModule<GroupPlans> createSynchronizerModule(
			final Config config,
			final TripRouterFactoryInternal tripRouterFactory) {
		return new JointPlanBasedGroupStrategyModule(
				new SynchronizeCoTravelerPlansModule(
					config.global().getNumberOfThreads(),
					tripRouterFactory.instantiateAndConfigureTripRouter().getStageActivityTypes() ) );
	}

	public static GenericStrategyModule<GroupPlans> createReRouteModule(
			final Config config,
			final PlanRoutingAlgorithmFactory planRouterFactory,
			final TripRouterFactoryInternal tripRouterFactory) {
		return new IndividualBasedGroupStrategyModule(
				new AbstractMultithreadedModule( config.global() ) {
					@Override
					public PlanAlgorithm getPlanAlgoInstance() {
						return planRouterFactory.createPlanRoutingAlgorithm( tripRouterFactory.instantiateAndConfigureTripRouter() );
					}
				});
	}

	public static GenericStrategyModule<GroupPlans> createRecomposeJointPlansModule(
			final Config config,
			final JointPlanFactory jpFactory,
			final PlanLinkIdentifier linkIdentifier) {
		return new RecomposeJointPlanModule(
				config.global().getNumberOfThreads(),
				jpFactory,
				linkIdentifier );
	}
}

