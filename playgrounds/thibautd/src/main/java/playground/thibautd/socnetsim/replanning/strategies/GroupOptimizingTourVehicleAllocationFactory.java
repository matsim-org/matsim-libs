/* *********************************************************************** *
 * project: org.matsim.*
 * GroupOptimizingTourVehicleAllocationFactory.java
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

import org.matsim.core.router.CompositeStageActivityTypes;
import playground.thibautd.socnetsim.controller.ControllerRegistry;
import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.replanning.GroupPlanStrategy;
import playground.thibautd.socnetsim.replanning.GroupPlanStrategyFactoryRegistry;
import playground.thibautd.socnetsim.replanning.GroupPlanStrategyFactoryUtils;
import playground.thibautd.socnetsim.sharedvehicles.SharedVehicleUtils;
import playground.thibautd.socnetsim.sharedvehicles.VehicleRessources;
import playground.thibautd.socnetsim.sharedvehicles.replanning.OptimizeVehicleAllocationAtTourLevelModule;

/**
 * @author thibautd
 */
public class GroupOptimizingTourVehicleAllocationFactory extends AbstractConfigurableSelectionStrategy {

	public GroupOptimizingTourVehicleAllocationFactory(
			GroupPlanStrategyFactoryRegistry factoryRegistry) {
		super(factoryRegistry);
	}

	@Override
	public GroupPlanStrategy createStrategy(final ControllerRegistry registry) {
		final GroupPlanStrategy strategy = instantiateStrategy( registry );

		final CompositeStageActivityTypes stageActs = new CompositeStageActivityTypes();
		stageActs.addActivityTypes( registry.getTripRouterFactory().get().getStageActivityTypes() );
		stageActs.addActivityTypes( JointActingTypes.JOINT_STAGE_ACTS );
		strategy.addStrategyModule(
				//new AllocateVehicleToPlansInGroupPlanModule(
				new OptimizeVehicleAllocationAtTourLevelModule(
						registry.getScenario().getConfig().global().getNumberOfThreads(),
						stageActs,
						(VehicleRessources) registry.getScenario().getScenarioElement(
							VehicleRessources.ELEMENT_NAME ),
						SharedVehicleUtils.DEFAULT_VEHICULAR_MODES,
						true));

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

		return strategy;

	}
}

