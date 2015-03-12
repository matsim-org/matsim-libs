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
package playground.thibautd.socnetsim.replanning.strategies;

import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.ReplanningContext;
import playground.thibautd.socnetsim.cliques.config.JointTripInsertorConfigGroup;
import playground.thibautd.socnetsim.controller.ControllerRegistry;
import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.population.SocialNetwork;
import playground.thibautd.socnetsim.replanning.*;
import playground.thibautd.socnetsim.replanning.modules.AbstractMultithreadedGenericStrategyModule;
import playground.thibautd.socnetsim.replanning.modules.JointTripInsertorAlgorithm;
import playground.thibautd.socnetsim.replanning.modules.prismiclocationchoice.PrismicLocationChoiceModule;
import playground.thibautd.socnetsim.sharedvehicles.VehicleRessources;

/**
 * @author thibautd
 */
public class JointPrismLocationChoiceWithJointTripInsertionStrategyFactory extends AbstractConfigurableSelectionStrategy {

	public JointPrismLocationChoiceWithJointTripInsertionStrategyFactory(
			final GroupPlanStrategyFactoryRegistry factoryRegistry) {
		super(factoryRegistry);
	}

	@Override
	public GroupPlanStrategy createStrategy( final ControllerRegistry registry ) {
		final GroupPlanStrategy strategy = instantiateStrategy( registry );

		strategy.addStrategyModule(
				new PrismicLocationChoiceModule(
					registry.getScenario() ) );

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createJointTripAwareTourModeUnifierModule(
					registry.getScenario().getConfig(),
					registry.getTripRouterFactory() ) );
		
		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createRecomposeJointPlansModule(
					registry.getScenario().getConfig(),
					registry.getJointPlans().getFactory(),
					registry.getPlanLinkIdentifier() ) );
		
		strategy.addStrategyModule(
			new JointPlanBasedGroupStrategyModule(
					new AbstractMultithreadedGenericStrategyModule<JointPlan>( registry.getScenario().getConfig().global() ) {
						@Override
						public GenericPlanAlgorithm<JointPlan> createAlgorithm(ReplanningContext replanningContext) {
							return new JointTripInsertorAlgorithm(
								MatsimRandom.getLocalInstance(),
								(SocialNetwork) registry.getScenario().getScenarioElement(  SocialNetwork.ELEMENT_NAME ),
								(JointTripInsertorConfigGroup) registry.getScenario().getConfig().getModule( JointTripInsertorConfigGroup.GROUP_NAME ),
								registry.getTripRouterFactory().get());
						}
						
						@Override
						protected String getName() {
							return "JointTripMutator";
						}
					}));

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createSynchronizerModule(
					registry.getScenario().getConfig(),
					registry.getTripRouterFactory()) );

		// TODO: add an option to enable or disable this part?
		final VehicleRessources vehicles =
				(VehicleRessources) registry.getScenario().getScenarioElement(
					VehicleRessources.ELEMENT_NAME );
		if ( vehicles != null ) {
			strategy.addStrategyModule(
					GroupPlanStrategyFactoryUtils.createVehicleAllocationModule(
						registry.getScenario().getConfig(),
						vehicles ) );
		}

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createReRouteModule(
					registry.getScenario().getConfig(),
					registry.getPlanRoutingAlgorithmFactory(),
					registry.getTripRouterFactory() ) );

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createSynchronizerModule(
					registry.getScenario().getConfig(),
					registry.getTripRouterFactory()) );

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createRecomposeJointPlansModule(
					registry.getScenario().getConfig(),
					registry.getJointPlans().getFactory(),
					registry.getPlanLinkIdentifier() ) );

		return strategy;
	}
}

