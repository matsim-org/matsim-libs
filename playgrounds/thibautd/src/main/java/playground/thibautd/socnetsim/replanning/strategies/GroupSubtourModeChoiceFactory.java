/* *********************************************************************** *
 * project: org.matsim.*
 * GroupSubtourModeChoiceFactory.java
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

import org.matsim.core.replanning.modules.SubtourModeChoice;

import playground.thibautd.socnetsim.controller.ControllerRegistry;
import playground.thibautd.socnetsim.replanning.GroupPlanStrategy;
import playground.thibautd.socnetsim.replanning.GroupPlanStrategyFactory;
import playground.thibautd.socnetsim.replanning.GroupPlanStrategyFactoryUtils;
import playground.thibautd.socnetsim.replanning.IndividualBasedGroupStrategyModule;
import playground.thibautd.socnetsim.sharedvehicles.SharedVehicleUtils;
import playground.thibautd.socnetsim.sharedvehicles.VehicleRessources;
import playground.thibautd.socnetsim.sharedvehicles.replanning.AllocateVehicleToPlansInGroupPlanModule;

/**
 * @author thibautd
 */
public class GroupSubtourModeChoiceFactory implements GroupPlanStrategyFactory {

	@Override
	public GroupPlanStrategy createStrategy(final ControllerRegistry registry) {
		final GroupPlanStrategy strategy =
				GroupPlanStrategyFactoryUtils.createRandomSelectingStrategy(
					registry.getIncompatiblePlansIdentifierFactory());

		// Why the hell did I put this here???
		//strategy.addStrategyModule(
		//		createReRouteModule(
		//			registry.getScenario().getConfig(),
		//			registry.getPlanRoutingAlgorithmFactory(),
		//			registry.getTripRouterFactory() ) );

		strategy.addStrategyModule(
				new IndividualBasedGroupStrategyModule(
					new SubtourModeChoice(
						registry.getScenario().getConfig() ) ) );

		// TODO: add an option to enable or disable this part?
		final VehicleRessources vehicles =
				(VehicleRessources) registry.getScenario().getScenarioElement(
			VehicleRessources.ELEMENT_NAME );
		if ( vehicles != null ) {
			strategy.addStrategyModule(
				new AllocateVehicleToPlansInGroupPlanModule(
						registry.getScenario().getConfig().global().getNumberOfThreads(),
						(VehicleRessources) registry.getScenario().getScenarioElement(
							VehicleRessources.ELEMENT_NAME ),
						SharedVehicleUtils.DEFAULT_VEHICULAR_MODES,
						true,
						true));
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
					registry.getPlanLinkIdentifier()));

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createSynchronizerModule(
					registry.getScenario().getConfig(),
					registry.getTripRouterFactory()) );

		return strategy;

	}
}

