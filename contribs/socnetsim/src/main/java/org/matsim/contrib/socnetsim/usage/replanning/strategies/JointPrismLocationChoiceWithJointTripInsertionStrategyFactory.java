/* *********************************************************************** *
 * project: org.matsim.*
 * JointPrismLocationChoiceStrategyFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetsim.usage.replanning.strategies;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.router.TripRouter;

import org.matsim.contrib.socnetsim.framework.PlanRoutingAlgorithmFactory;
import org.matsim.contrib.socnetsim.framework.cliques.config.JointTripInsertorConfigGroup;
import org.matsim.contrib.socnetsim.framework.population.JointPlan;
import org.matsim.contrib.socnetsim.framework.population.JointPlans;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.contrib.socnetsim.framework.replanning.GenericPlanAlgorithm;
import org.matsim.contrib.socnetsim.framework.replanning.GroupPlanStrategy;
import org.matsim.contrib.socnetsim.usage.replanning.GroupPlanStrategyFactoryUtils;
import org.matsim.contrib.socnetsim.framework.replanning.JointPlanBasedGroupStrategyModule;
import org.matsim.contrib.socnetsim.framework.replanning.modules.AbstractMultithreadedGenericStrategyModule;
import org.matsim.contrib.socnetsim.jointtrips.replanning.modules.JointTripInsertorAlgorithm;
import org.matsim.contrib.socnetsim.framework.replanning.modules.PlanLinkIdentifier;
import org.matsim.contrib.socnetsim.framework.replanning.modules.PlanLinkIdentifier.Strong;
import org.matsim.contrib.socnetsim.jointactivities.replanning.modules.prismiclocationchoice.PrismicLocationChoiceModule;
import org.matsim.contrib.socnetsim.sharedvehicles.VehicleRessources;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * @author thibautd
 */
public class JointPrismLocationChoiceWithJointTripInsertionStrategyFactory extends AbstractConfigurableSelectionStrategy {

	private final Scenario sc;
	private final PlanRoutingAlgorithmFactory planRoutingAlgorithmFactory;
	private final Provider<TripRouter> tripRouterFactory;
	private final PlanLinkIdentifier planLinkIdentifier;
	private javax.inject.Provider<TripRouter> tripRouterProvider;

	@Inject
	public JointPrismLocationChoiceWithJointTripInsertionStrategyFactory(Scenario sc, PlanRoutingAlgorithmFactory planRoutingAlgorithmFactory,
																		 Provider<TripRouter> tripRouterFactory, @Strong PlanLinkIdentifier planLinkIdentifier, javax.inject.Provider<TripRouter> tripRouterProvider) {
		this.sc = sc;
		this.planRoutingAlgorithmFactory = planRoutingAlgorithmFactory;
		this.tripRouterFactory = tripRouterFactory;
		this.planLinkIdentifier = planLinkIdentifier;
		this.tripRouterProvider = tripRouterProvider;
	}


	@Override
	public GroupPlanStrategy get() {
		final GroupPlanStrategy strategy = instantiateStrategy( sc.getConfig() );

		strategy.addStrategyModule(
				new PrismicLocationChoiceModule(
					sc, tripRouterProvider) );

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createJointTripAwareTourModeUnifierModule(
						sc.getConfig(),
						tripRouterFactory) );
		
		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createRecomposeJointPlansModule(
					sc.getConfig(),
					((JointPlans) sc.getScenarioElement( JointPlans.ELEMENT_NAME  )).getFactory(),
					planLinkIdentifier ) );
		
		strategy.addStrategyModule(
			new JointPlanBasedGroupStrategyModule(
					new AbstractMultithreadedGenericStrategyModule<JointPlan>( sc.getConfig().global() ) {
						@Override
						public GenericPlanAlgorithm<JointPlan> createAlgorithm(ReplanningContext replanningContext) {
							return new JointTripInsertorAlgorithm(
								MatsimRandom.getLocalInstance(),
								(SocialNetwork) sc.getScenarioElement(  SocialNetwork.ELEMENT_NAME ),
								(JointTripInsertorConfigGroup) sc.getConfig().getModule( JointTripInsertorConfigGroup.GROUP_NAME ),
								tripRouterFactory.get());
						}
						
						@Override
						protected String getName() {
							return "JointTripMutator";
						}
					}));

		// TODO: add an option to enable or disable this part?
		final VehicleRessources vehicles =
				(VehicleRessources) sc.getScenarioElement(
					VehicleRessources.ELEMENT_NAME );
		if ( vehicles != null ) {
			strategy.addStrategyModule(
					GroupPlanStrategyFactoryUtils.createVehicleAllocationModule(
						sc.getConfig(),
						vehicles ) );
		}

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createReRouteModule(
					sc.getConfig(),
					planRoutingAlgorithmFactory,
					tripRouterFactory ) );

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createSynchronizerModule(
					sc.getConfig(),
					tripRouterFactory) );

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createRecomposeJointPlansModule(
					sc.getConfig(),
					((JointPlans) sc.getScenarioElement( JointPlans.ELEMENT_NAME  )).getFactory(),
					planLinkIdentifier ) );

		return strategy;
	}
}

