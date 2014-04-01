/* *********************************************************************** *
 * project: org.matsim.*
 * RandomJointLocationChoiceStrategy.java
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

import playground.thibautd.socnetsim.controller.ControllerRegistry;
import playground.thibautd.socnetsim.replanning.GroupPlanStrategy;
import playground.thibautd.socnetsim.replanning.GroupPlanStrategyFactoryRegistry;
import playground.thibautd.socnetsim.replanning.GroupPlanStrategyFactoryUtils;
import playground.thibautd.socnetsim.replanning.modules.randomlocationchoice.RandomJointLocationChoiceModule;
import playground.thibautd.socnetsim.sharedvehicles.VehicleRessources;

/**
 * @author thibautd
 */
public class RandomJointLocationChoiceStrategyFactory extends AbstractConfigurableSelectionStrategy {

	public RandomJointLocationChoiceStrategyFactory(
			GroupPlanStrategyFactoryRegistry factoryRegistry) {
		super(factoryRegistry);
	}

	@Override
	public GroupPlanStrategy createStrategy( final ControllerRegistry registry ) {
		final GroupPlanStrategy strategy = instantiateStrategy( registry );

		strategy.addStrategyModule(
				new RandomJointLocationChoiceModule(
					registry.getScenario() ) );

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createJointTripAwareTourModeUnifierModule(
					registry.getScenario().getConfig(),
					registry.getTripRouterFactory() ) );

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
				GroupPlanStrategyFactoryUtils.createRecomposeJointPlansModule(
					registry.getScenario().getConfig(),
					registry.getJointPlans().getFactory(),
					registry.getPlanLinkIdentifier() ) );

		return strategy;
	}
}

