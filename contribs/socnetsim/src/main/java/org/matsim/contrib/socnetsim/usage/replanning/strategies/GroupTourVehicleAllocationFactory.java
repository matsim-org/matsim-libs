/* *********************************************************************** *
 * project: org.matsim.*
 * GroupTourVehicleAllocationFactory.java
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
package org.matsim.contrib.socnetsim.usage.replanning.strategies;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.router.TripRouter;

import org.matsim.contrib.socnetsim.framework.PlanRoutingAlgorithmFactory;
import org.matsim.contrib.socnetsim.framework.population.JointPlans;
import org.matsim.contrib.socnetsim.framework.replanning.GroupPlanStrategy;
import org.matsim.contrib.socnetsim.usage.replanning.GroupPlanStrategyFactoryUtils;
import org.matsim.contrib.socnetsim.framework.replanning.IndividualBasedGroupStrategyModule;
import org.matsim.contrib.socnetsim.framework.replanning.modules.PlanLinkIdentifier;
import org.matsim.contrib.socnetsim.framework.replanning.modules.PlanLinkIdentifier.Strong;
import org.matsim.contrib.socnetsim.sharedvehicles.VehicleRessources;
import org.matsim.contrib.socnetsim.sharedvehicles.replanning.AllocateVehicleToSubtourModule;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * @author thibautd
 */
public class GroupTourVehicleAllocationFactory extends AbstractConfigurableSelectionStrategy {

	private final Scenario sc;
	private final PlanRoutingAlgorithmFactory planRoutingAlgorithmFactory;
	private final Provider<TripRouter> tripRouterFactory;
	private final PlanLinkIdentifier planLinkIdentifier;

	@Inject
	public GroupTourVehicleAllocationFactory( final Scenario sc , final PlanRoutingAlgorithmFactory planRoutingAlgorithmFactory , final Provider<TripRouter> tripRouterFactory ,
			@Strong final PlanLinkIdentifier planLinkIdentifier ) {
		this.sc = sc;
		this.planRoutingAlgorithmFactory = planRoutingAlgorithmFactory;
		this.tripRouterFactory = tripRouterFactory;
		this.planLinkIdentifier = planLinkIdentifier;
	}


	@Override
	public GroupPlanStrategy get() {
		final GroupPlanStrategy strategy = instantiateStrategy( sc.getConfig() );

		strategy.addStrategyModule(
				new IndividualBasedGroupStrategyModule(
					new AllocateVehicleToSubtourModule(
						sc.getConfig().global().getNumberOfThreads(),
						TransportMode.car,
						(VehicleRessources) sc.getScenarioElement(
							VehicleRessources.ELEMENT_NAME ), tripRouterFactory) ) );

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createReRouteModule(
						sc.getConfig(),
						planRoutingAlgorithmFactory,
						tripRouterFactory) );
		
		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createRecomposeJointPlansModule(
					sc.getConfig(),
					((JointPlans) sc.getScenarioElement( JointPlans.ELEMENT_NAME  )).getFactory(),
					planLinkIdentifier));

		return strategy;
	}
}

