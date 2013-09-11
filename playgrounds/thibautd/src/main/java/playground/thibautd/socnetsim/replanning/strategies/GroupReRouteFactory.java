/* *********************************************************************** *
 * project: org.matsim.*
 * GroupReRouteFactory.java
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

import playground.thibautd.socnetsim.controller.ControllerRegistry;
import playground.thibautd.socnetsim.replanning.GroupPlanStrategy;
import playground.thibautd.socnetsim.replanning.GroupPlanStrategyFactory;

import static playground.thibautd.socnetsim.replanning.GroupPlanStrategyFactoryUtils.*;
/**
 * @author thibautd
 */
public class GroupReRouteFactory implements GroupPlanStrategyFactory {
	@Override
	public GroupPlanStrategy createStrategy(
			final ControllerRegistry registry) {
		final GroupPlanStrategy strategy =
				createRandomSelectingStrategy(
					registry.getIncompatiblePlansIdentifierFactory());
	
		strategy.addStrategyModule(
				createReRouteModule(
					registry.getScenario().getConfig(),
					registry.getPlanRoutingAlgorithmFactory(),
					registry.getTripRouterFactory() ) );

		strategy.addStrategyModule(
				createRecomposeJointPlansModule(
					registry.getScenario().getConfig(),
					registry.getJointPlans().getFactory(),
					registry.getPlanLinkIdentifier()));

		strategy.addStrategyModule(
				createSynchronizerModule(
					registry.getScenario().getConfig(),
					registry.getTripRouterFactory() ) );

		return strategy;
	}
}

