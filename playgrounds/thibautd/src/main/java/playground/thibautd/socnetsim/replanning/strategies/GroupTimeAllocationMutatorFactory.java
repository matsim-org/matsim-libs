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

import javax.inject.Provider;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.CompositeStageActivityTypes;
import org.matsim.core.router.TripRouter;
import org.matsim.population.algorithms.PlanAlgorithm;

import com.google.inject.Inject;

import playground.thibautd.router.PlanRoutingAlgorithmFactory;
import playground.thibautd.router.replanning.BlackListedTimeAllocationMutator;
import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.framework.population.JointPlans;
import playground.thibautd.socnetsim.framework.replanning.GroupPlanStrategy;
import playground.thibautd.socnetsim.replanning.GroupPlanStrategyFactoryUtils;
import playground.thibautd.socnetsim.framework.replanning.IndividualBasedGroupStrategyModule;
import playground.thibautd.socnetsim.framework.replanning.modules.PlanLinkIdentifier;
import playground.thibautd.socnetsim.framework.replanning.modules.PlanLinkIdentifier.Strong;

public class GroupTimeAllocationMutatorFactory extends AbstractConfigurableSelectionStrategy {
	private static final Logger log =
		Logger.getLogger(GroupTimeAllocationMutatorFactory.class);

	private final Scenario sc;
	private final PlanRoutingAlgorithmFactory planRoutingAlgorithmFactory;
	private final Provider<TripRouter> tripRouterFactory;
	private final PlanLinkIdentifier planLinkIdentifier;

	// TODO make configurable again
	private final double maxTemp = 1;

	@Inject
	public GroupTimeAllocationMutatorFactory( final Scenario sc , final PlanRoutingAlgorithmFactory planRoutingAlgorithmFactory , final Provider<TripRouter> tripRouterFactory ,
			@Strong final PlanLinkIdentifier planLinkIdentifier ) {
		this.sc = sc;
		this.planRoutingAlgorithmFactory = planRoutingAlgorithmFactory;
		this.tripRouterFactory = tripRouterFactory;
		this.planLinkIdentifier = planLinkIdentifier;
	}


	@Override
	public GroupPlanStrategy get() {
		final Config config = sc.getConfig();
		final GroupPlanStrategy strategy = instantiateStrategy( config );
		final PlanRoutingAlgorithmFactory planRouterFactory = planRoutingAlgorithmFactory;

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
					((JointPlans) sc.getScenarioElement( JointPlans.ELEMENT_NAME  )).getFactory(),
					planLinkIdentifier ) );

		return strategy;
	}
}

