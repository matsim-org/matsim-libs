/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityInGroupLocationChoiceFactory.java
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

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.router.TripRouter;

import playground.thibautd.router.PlanRoutingAlgorithmFactory;
import playground.thibautd.socnetsim.GroupReplanningConfigGroup;
import playground.thibautd.socnetsim.population.JointPlans;
import playground.thibautd.socnetsim.replanning.GroupPlanStrategy;
import playground.thibautd.socnetsim.replanning.GroupPlanStrategyFactoryUtils;
import playground.thibautd.socnetsim.replanning.modules.MutateActivityLocationsToLocationsOfOthersModule;
import playground.thibautd.socnetsim.replanning.modules.PlanLinkIdentifier;
import playground.thibautd.socnetsim.replanning.modules.PlanLinkIdentifier.Strong;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * @author thibautd
 */
public class ActivityInGroupLocationChoiceFactory extends AbstractConfigurableSelectionStrategy {


	private final Scenario sc;
	private final PlanRoutingAlgorithmFactory planRoutingAlgorithmFactory;
	private final Provider<TripRouter> tripRouterFactory;
	private final PlanLinkIdentifier planLinkIdentifier;

	@Inject
	public ActivityInGroupLocationChoiceFactory(
			Scenario sc ,
			PlanRoutingAlgorithmFactory planRoutingAlgorithmFactory ,
			Provider<TripRouter> tripRouterFactory ,
			@Strong PlanLinkIdentifier planLinkIdentifier ) {
		this.sc = sc;
		this.planRoutingAlgorithmFactory = planRoutingAlgorithmFactory;
		this.tripRouterFactory = tripRouterFactory;
		this.planLinkIdentifier = planLinkIdentifier;
	}

	@Override
	public GroupPlanStrategy get() {
		final GroupPlanStrategy strategy = instantiateStrategy( sc.getConfig() );

		final GroupReplanningConfigGroup configGroup = (GroupReplanningConfigGroup)
				sc.getConfig().getModule(
						GroupReplanningConfigGroup.GROUP_NAME );
		strategy.addStrategyModule(
				new MutateActivityLocationsToLocationsOfOthersModule(
					sc.getConfig().global().getNumberOfThreads(),
					sc.getPopulation(),
					configGroup.getLocationChoiceActivityType() ) );

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createJointTripAwareTourModeUnifierModule(
					sc.getConfig(),
					tripRouterFactory ) );

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createReRouteModule(
					sc.getConfig(),
					planRoutingAlgorithmFactory,
					tripRouterFactory ) );
		

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createRecomposeJointPlansModule(
					sc.getConfig(),
					((JointPlans) sc.getScenarioElement( JointPlans.ELEMENT_NAME  )).getFactory(),
					planLinkIdentifier ));

		return strategy;
	}
}

