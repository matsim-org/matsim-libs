/* *********************************************************************** *
 * project: org.matsim.*
 * GroupPlanStrategyFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.trafficmonitoring.DepartureDelayAverageCalculator;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.population.algorithms.TripsToLegsAlgorithm;

import playground.thibautd.cliquessim.replanning.modules.jointtimeallocationmutator.JointTimeAllocationMutatorModule;
import playground.thibautd.cliquessim.replanning.modules.jointtimemodechooser.JointTimeModeChooserAlgorithm;
import playground.thibautd.cliquessim.replanning.modules.jointtripinsertor.JointTripInsertorAndRemoverAlgorithm;
import playground.thibautd.socnetsim.controller.ControllerRegistry;
import playground.thibautd.socnetsim.replanning.modules.JointPlanMergingModule;
import playground.thibautd.socnetsim.replanning.modules.SplitJointPlansBasedOnJointTripsModule;
import playground.thibautd.socnetsim.replanning.selectors.LogitSumSelector;
import playground.thibautd.socnetsim.replanning.selectors.RandomGroupLevelSelector;
import playground.thibautd.socnetsim.router.JointPlanRouter;

/**
 * Provides factory methods to create standard strategies.
 * @author thibautd
 */
public class GroupPlanStrategyFactory {
	private GroupPlanStrategyFactory() {}

	// /////////////////////////////////////////////////////////////////////////
	// strategies
	// /////////////////////////////////////////////////////////////////////////
	public static GroupPlanStrategy createReRoute(
			final Config config,
			final TripRouterFactory tripRouterFactory) {
		final GroupPlanStrategy strategy = createRandomSelectingStrategy();
	
		strategy.addStrategyModule( createReRouteModule( config , tripRouterFactory ) );

		return strategy;
	}

	public static GroupPlanStrategy createTimeAllocationMutator(
			final Config config,
			final TripRouterFactory tripRouterFactory) {
		final GroupPlanStrategy strategy = createRandomSelectingStrategy();

		// XXX trips to legs does not keeps co-traveler information
		//strategy.addStrategyModule( createTripsToLegsModule( config , tripRouterFactory ) ) ;

		strategy.addStrategyModule(
				new JointPlanBasedGroupStrategyModule(
							new JointTimeAllocationMutatorModule(
								config,
								tripRouterFactory)));

		strategy.addStrategyModule( createReRouteModule( config , tripRouterFactory ) );

		return strategy;
	}

	public static GroupPlanStrategy createCliqueJointTripMutator(
			final ControllerRegistry registry) {
		final GroupPlanStrategy strategy = createRandomSelectingStrategy();
		final Config config = registry.getScenario().getConfig();

		strategy.addStrategyModule(
				new JointPlanMergingModule(
					config.global().getNumberOfThreads(),
					// merge everything
					1.0 ) );

		strategy.addStrategyModule(
				new JointPlanBasedGroupStrategyModule(
					new AbstractMultithreadedModule( config.global() ) {
						@Override
						public PlanAlgorithm getPlanAlgoInstance() {
							return new JointTripInsertorAndRemoverAlgorithm(
								config,
								registry.getTripRouterFactory().createTripRouter(),
								MatsimRandom.getLocalInstance(),
								true); // "iterative"
						}
					}));

		// split immediately after insertion/removal,
		// to make optimisation easier.
		strategy.addStrategyModule(
				new SplitJointPlansBasedOnJointTripsModule(
					config.global().getNumberOfThreads() ) );

		strategy.addStrategyModule(
				createReRouteModule(
					config,
					registry.getTripRouterFactory() ) );

		final DepartureDelayAverageCalculator delay =
			new DepartureDelayAverageCalculator(
				registry.getScenario().getNetwork(),
				registry.getScenario().getConfig().travelTimeCalculator().getTraveltimeBinSize());

		strategy.addStrategyModule(
				new JointPlanBasedGroupStrategyModule(
					new AbstractMultithreadedModule(
							registry.getScenario().getConfig().global() ) {
						@Override
						public PlanAlgorithm getPlanAlgoInstance() {
							return new JointTimeModeChooserAlgorithm(
								MatsimRandom.getLocalInstance(),
								null,
								delay,
								registry.getScenario(),
								registry.getScoringFunctionFactory(),
								registry.getTravelTime(),
								registry.getLeastCostPathCalculatorFactory(),
								registry.getTripRouterFactory() );
						}
					}));

		return strategy;
	}

	public static GroupPlanStrategy createSelectExpBeta(final Config config) {
		return new GroupPlanStrategy(
				new LogitSumSelector(
					MatsimRandom.getLocalInstance(),
					config.planCalcScore().getBrainExpBeta()) );
	}

	public static GroupPlanStrategy createSubtourModeChoice(
			final Config config,
			final TripRouterFactory tripRouterFactory) {
		final GroupPlanStrategy strategy = createRandomSelectingStrategy();

		strategy.addStrategyModule( createReRouteModule( config , tripRouterFactory ) );

		strategy.addStrategyModule(
				new IndividualBasedGroupStrategyModule(
					new SubtourModeChoice( config ) ) );

		strategy.addStrategyModule( createReRouteModule( config , tripRouterFactory ) );

		return strategy;
	}

	// /////////////////////////////////////////////////////////////////////////
	// standard modules
	// /////////////////////////////////////////////////////////////////////////
	public static GroupStrategyModule createTripsToLegsModule(
			final Config config,
			final TripRouterFactory tripRouterFactory) {
		return new IndividualBasedGroupStrategyModule(
				new AbstractMultithreadedModule( config.global() ) {
					@Override
					public PlanAlgorithm getPlanAlgoInstance() {
						return new TripsToLegsAlgorithm( tripRouterFactory.createTripRouter() );
					}
				});
	}

	public static GroupStrategyModule createReRouteModule(
			final Config config,
			final TripRouterFactory tripRouterFactory) {
		return new IndividualBasedGroupStrategyModule(
				new AbstractMultithreadedModule( config.global() ) {
					@Override
					public PlanAlgorithm getPlanAlgoInstance() {
						return new JointPlanRouter( tripRouterFactory.createTripRouter() );
					}
				});
	}


	private static GroupPlanStrategy createRandomSelectingStrategy() {
		return new GroupPlanStrategy(
				new RandomGroupLevelSelector(
					MatsimRandom.getLocalInstance() ) );
	}
}

