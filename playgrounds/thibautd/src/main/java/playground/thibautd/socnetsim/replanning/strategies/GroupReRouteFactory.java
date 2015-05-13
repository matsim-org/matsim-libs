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

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.router.TripRouter;

import playground.thibautd.socnetsim.framework.PlanRoutingAlgorithmFactory;
import playground.thibautd.socnetsim.framework.population.JointPlans;
import playground.thibautd.socnetsim.framework.replanning.GroupPlanStrategy;
import playground.thibautd.socnetsim.framework.replanning.strategies.AbstractConfigurableSelectionStrategy;
import playground.thibautd.socnetsim.replanning.GroupPlanStrategyFactoryUtils;
import playground.thibautd.socnetsim.framework.replanning.modules.PlanLinkIdentifier;
import playground.thibautd.socnetsim.framework.replanning.modules.PlanLinkIdentifier.Strong;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * @author thibautd
 */
public class GroupReRouteFactory extends AbstractConfigurableSelectionStrategy {
	private final Scenario sc;
	private final PlanRoutingAlgorithmFactory routingAlgorithmFactory;
	private final Provider<TripRouter> tripRouterFactory;
	private final PlanLinkIdentifier planLinkIdentifier;


	@Inject
	public GroupReRouteFactory( Scenario sc , PlanRoutingAlgorithmFactory routingAlgorithmFactory , Provider<TripRouter> tripRouterFactory ,
			@Strong PlanLinkIdentifier planLinkIdentifier ) {
		this.sc = sc;
		this.routingAlgorithmFactory = routingAlgorithmFactory;
		this.tripRouterFactory = tripRouterFactory;
		this.planLinkIdentifier = planLinkIdentifier;
	}


	@Override
	public GroupPlanStrategy get() {
		final GroupPlanStrategy strategy = instantiateStrategy( sc.getConfig() );
	
		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createReRouteModule(
					sc.getConfig(),
					routingAlgorithmFactory,
					tripRouterFactory ) );

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createSynchronizerModule(
					sc.getConfig(),
					tripRouterFactory ) );
		
		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createRecomposeJointPlansModule(
					sc.getConfig(),
					((JointPlans) sc.getScenarioElement( JointPlans.ELEMENT_NAME ) ).getFactory(),
					planLinkIdentifier ));

		return strategy;
	}
}

