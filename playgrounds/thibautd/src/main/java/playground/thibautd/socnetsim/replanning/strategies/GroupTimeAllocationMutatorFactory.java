/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.thibautd.socnetsim.replanning.strategies;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.CompositeStageActivityTypes;
import org.matsim.core.router.TripRouter;
import org.matsim.population.algorithms.PlanAlgorithm;
import playground.thibautd.router.PlanRoutingAlgorithmFactory;
import playground.thibautd.router.replanning.BlackListedTimeAllocationMutator;
import playground.thibautd.socnetsim.controller.ControllerRegistry;
import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.replanning.GroupPlanStrategy;
import playground.thibautd.socnetsim.replanning.GroupPlanStrategyFactoryRegistry;
import playground.thibautd.socnetsim.replanning.GroupPlanStrategyFactoryUtils;
import playground.thibautd.socnetsim.replanning.IndividualBasedGroupStrategyModule;

import javax.inject.Provider;

public class GroupTimeAllocationMutatorFactory extends AbstractConfigurableSelectionStrategy {
	private static final Logger log =
		Logger.getLogger(GroupTimeAllocationMutatorFactory.class);

	private final double maxTemp;

	public  GroupTimeAllocationMutatorFactory(
			final GroupPlanStrategyFactoryRegistry factoryRegistry,
			final double maxTemp) {
		super( factoryRegistry );
		this.maxTemp = maxTemp;
	}

	@Override
	public GroupPlanStrategy createStrategy(
			final ControllerRegistry registry) {
		final GroupPlanStrategy strategy = instantiateStrategy( registry );
		final Config config = registry.getScenario().getConfig();
		final Provider<TripRouter> tripRouterFactory = registry.getTripRouterFactory();
		final PlanRoutingAlgorithmFactory planRouterFactory = registry.getPlanRoutingAlgorithmFactory();

		strategy.addStrategyModule(
				new IndividualBasedGroupStrategyModule(
					new AbstractMultithreadedModule( config.global().getNumberOfThreads() ) {
						@Override
						public PlanAlgorithm getPlanAlgoInstance() {
							final CompositeStageActivityTypes blackList = new CompositeStageActivityTypes();
							blackList.addActivityTypes( tripRouterFactory.get().getStageActivityTypes() );
							blackList.addActivityTypes( JointActingTypes.JOINT_STAGE_ACTS );

							final int iteration = getReplanningContext().getIteration();
							final int firstIteration = config.controler().getFirstIteration();
							final double nIters = config.controler().getLastIteration() - firstIteration;
							final double minTemp = 1;
							final double startMin = (2 / 3.) * nIters;
							final double progress = (iteration - firstIteration) / startMin;
							final double temp = minTemp + Math.max(1 - progress , 0) * (maxTemp - minTemp);
							log.debug( "temperature in iteration "+iteration+": "+temp );
							final BlackListedTimeAllocationMutator algo =
									new BlackListedTimeAllocationMutator(
										blackList,
										config.timeAllocationMutator().getMutationRange() * temp,
										MatsimRandom.getLocalInstance() );
							return algo;
						}
					}));

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createReRouteModule(
						config,
						planRouterFactory,
						tripRouterFactory ) );

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createSynchronizerModule(
					config,
					tripRouterFactory) );

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createRecomposeJointPlansModule(
					config,
					registry.getJointPlans().getFactory(),
					registry.getPlanLinkIdentifier() ) );

		return strategy;
	}
}

