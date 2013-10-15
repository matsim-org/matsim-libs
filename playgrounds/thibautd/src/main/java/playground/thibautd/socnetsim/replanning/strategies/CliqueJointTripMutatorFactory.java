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
import org.matsim.core.trafficmonitoring.DepartureDelayAverageCalculator;

import playground.thibautd.socnetsim.cliques.replanning.modules.jointtimemodechooser.JointTimeModeChooserAlgorithm;
import playground.thibautd.socnetsim.cliques.replanning.modules.jointtripinsertor.JointTripInsertorAndRemoverAlgorithm;
import playground.thibautd.socnetsim.controller.ControllerRegistry;
import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.replanning.DefaultPlanLinkIdentifier;
import playground.thibautd.socnetsim.replanning.GenericPlanAlgorithm;
import playground.thibautd.socnetsim.replanning.GroupPlanStrategy;
import playground.thibautd.socnetsim.replanning.GroupPlanStrategyFactory;
import playground.thibautd.socnetsim.replanning.GroupPlanStrategyFactoryUtils;
import playground.thibautd.socnetsim.replanning.JointPlanBasedGroupStrategyModule;
import playground.thibautd.socnetsim.replanning.modules.AbstractMultithreadedGenericStrategyModule;
import playground.thibautd.socnetsim.replanning.modules.JointPlanMergingModule;
import playground.thibautd.socnetsim.sharedvehicles.SharedVehicleUtils;
import playground.thibautd.socnetsim.sharedvehicles.VehicleRessources;
import playground.thibautd.socnetsim.sharedvehicles.replanning.AllocateVehicleToPlansInGroupPlanModule;

/**
 * @author thibautd
 */
public class CliqueJointTripMutatorFactory implements GroupPlanStrategyFactory {
	private final boolean optimize;

	public  CliqueJointTripMutatorFactory(final boolean optimize) {
		this.optimize = optimize;
	}

	@Override
	public GroupPlanStrategy createStrategy(final ControllerRegistry registry) {
		final GroupPlanStrategy strategy =
				GroupPlanStrategyFactoryUtils.createRandomSelectingStrategy(
					registry.getIncompatiblePlansIdentifierFactory());
		
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
						public GenericPlanAlgorithm<JointPlan> createAlgorithm() {
							return new JointTripInsertorAndRemoverAlgorithm(
								config,
								registry.getTripRouterFactory().instantiateAndConfigureTripRouter(),
								MatsimRandom.getLocalInstance(),
								true); // "iterative"
						}
					}));

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createReRouteModule(
					config,
					registry.getPlanRoutingAlgorithmFactory(),
					registry.getTripRouterFactory() ) );

		if (optimize) {
			final DepartureDelayAverageCalculator delay =
				new DepartureDelayAverageCalculator(
					registry.getScenario().getNetwork(),
					registry.getScenario().getConfig().travelTimeCalculator().getTraveltimeBinSize());

			// split immediately after insertion/removal,
			// to make optimisation easier.
			strategy.addStrategyModule(
					GroupPlanStrategyFactoryUtils.createRecomposeJointPlansModule(
						config,
						registry.getJointPlans().getFactory(),
						// XXX use the default, to allow stupid simulations
						// without joint plans. This is rather ugly.
						new DefaultPlanLinkIdentifier() ) );

			strategy.addStrategyModule(
					new JointPlanBasedGroupStrategyModule(
						new AbstractMultithreadedGenericStrategyModule<JointPlan>(
								registry.getScenario().getConfig().global() ) {
							@Override
							public GenericPlanAlgorithm<JointPlan> createAlgorithm() {
								return new JointTimeModeChooserAlgorithm(
									MatsimRandom.getLocalInstance(),
									null,
									delay,
									registry.getScenario(),
									registry.getScoringFunctionFactory(),
									registry.getTravelTime().getLinkTravelTimes(),
									registry.getLeastCostPathCalculatorFactory(),
									registry.getTripRouterFactory() );
							}
						}));
		}
		else {
			strategy.addStrategyModule(
					GroupPlanStrategyFactoryUtils.createSynchronizerModule(
						config,
						registry.getTripRouterFactory()) );
		}

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

