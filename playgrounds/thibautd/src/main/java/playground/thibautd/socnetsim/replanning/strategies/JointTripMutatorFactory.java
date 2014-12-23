/* *********************************************************************** *
 * project: org.matsim.*
 * CliqueJointTripMutatorFactory.java
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

import org.matsim.core.config.Config;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.ReplanningContext;
import playground.thibautd.socnetsim.controller.ControllerRegistry;
import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.replanning.*;
import playground.thibautd.socnetsim.replanning.modules.AbstractMultithreadedGenericStrategyModule;
import playground.thibautd.socnetsim.replanning.modules.JointPlanMergingModule;
import playground.thibautd.socnetsim.replanning.modules.JointTripInsertorAndRemoverAlgorithm;
import playground.thibautd.socnetsim.sharedvehicles.SharedVehicleUtils;
import playground.thibautd.socnetsim.sharedvehicles.VehicleRessources;
import playground.thibautd.socnetsim.sharedvehicles.replanning.AllocateVehicleToPlansInGroupPlanModule;

/**
 * @author thibautd
 */
public class JointTripMutatorFactory extends AbstractConfigurableSelectionStrategy {

	public JointTripMutatorFactory(
			GroupPlanStrategyFactoryRegistry factoryRegistry) {
		super(factoryRegistry);
		// TODO Auto-generated constructor stub
	}

	@Override
	public GroupPlanStrategy createStrategy(final ControllerRegistry registry) {
		final GroupPlanStrategy strategy = instantiateStrategy( registry );
		
		final Config config = registry.getScenario().getConfig();

		strategy.addStrategyModule(
				new JointPlanMergingModule(
					registry.getJointPlans().getFactory(),
					config.global().getNumberOfThreads(),
					// merge everything
					1.0 ) );

		strategy.addStrategyModule(
			new JointPlanBasedGroupStrategyModule(
					new AbstractMultithreadedGenericStrategyModule<JointPlan>( config.global() ) {
						@Override
						public GenericPlanAlgorithm<JointPlan> createAlgorithm(ReplanningContext replanningContext) {
							return new JointTripInsertorAndRemoverAlgorithm(
								registry.getScenario(),
								registry.getTripRouterFactory().get(),
								MatsimRandom.getLocalInstance(),
								true); // "iterative"
						}
						
						@Override
						protected String getName() {
							return "JointTripMutator";
						}
					}));

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createReRouteModule(
					config,
					registry.getPlanRoutingAlgorithmFactory(),
					registry.getTripRouterFactory() ) );

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createSynchronizerModule(
					config,
					registry.getTripRouterFactory()) );

		final VehicleRessources vehicles = 
					(VehicleRessources) registry.getScenario().getScenarioElement(
							VehicleRessources.ELEMENT_NAME );

		if ( vehicles != null ) {
			strategy.addStrategyModule(
					new AllocateVehicleToPlansInGroupPlanModule(
						registry.getScenario().getConfig().global().getNumberOfThreads(),
						vehicles,
						SharedVehicleUtils.DEFAULT_VEHICULAR_MODES,
						false,
						true)); // preserve allocation (ie just reallocate for modified plans)
		}

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createRecomposeJointPlansModule(
					config,
					registry.getJointPlans().getFactory(),
					registry.getPlanLinkIdentifier() ) );

		return strategy;
	}
}

