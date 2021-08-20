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
package org.matsim.contrib.socnetsim.usage.replanning.strategies;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.contrib.socnetsim.framework.PlanRoutingAlgorithmFactory;
import org.matsim.contrib.socnetsim.framework.population.JointPlan;
import org.matsim.contrib.socnetsim.framework.population.JointPlans;
import org.matsim.contrib.socnetsim.framework.replanning.GenericPlanAlgorithm;
import org.matsim.contrib.socnetsim.framework.replanning.GroupPlanStrategy;
import org.matsim.contrib.socnetsim.usage.replanning.GroupPlanStrategyFactoryUtils;
import org.matsim.contrib.socnetsim.framework.replanning.JointPlanBasedGroupStrategyModule;
import org.matsim.contrib.socnetsim.framework.replanning.modules.AbstractMultithreadedGenericStrategyModule;
import org.matsim.contrib.socnetsim.framework.replanning.modules.JointPlanMergingModule;
import org.matsim.contrib.socnetsim.jointtrips.replanning.modules.JointTripInsertorAndRemoverAlgorithm;
import org.matsim.contrib.socnetsim.framework.replanning.modules.PlanLinkIdentifier;
import org.matsim.contrib.socnetsim.framework.replanning.modules.PlanLinkIdentifier.Strong;
import org.matsim.contrib.socnetsim.sharedvehicles.SharedVehicleUtils;
import org.matsim.contrib.socnetsim.sharedvehicles.VehicleRessources;
import org.matsim.contrib.socnetsim.sharedvehicles.replanning.AllocateVehicleToPlansInGroupPlanModule;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * @author thibautd
 */
public class JointTripMutatorFactory extends AbstractConfigurableSelectionStrategy {

	private final Scenario sc;
	private final PlanRoutingAlgorithmFactory planRoutingAlgorithmFactory;
	private final Provider<TripRouter> tripRouterFactory;
	private final PlanLinkIdentifier planLinkIdentifier;
	private final MainModeIdentifier mainModeIdentifier;
	private final TimeInterpretation timeInterpretation;


	@Inject
	JointTripMutatorFactory( Scenario sc , PlanRoutingAlgorithmFactory planRoutingAlgorithmFactory , Provider<TripRouter> tripRouterFactory ,
			@Strong PlanLinkIdentifier planLinkIdentifier, MainModeIdentifier mainModeIdentifier, TimeInterpretation timeInterpretation ) {
		this.sc = sc;
		this.planRoutingAlgorithmFactory = planRoutingAlgorithmFactory;
		this.tripRouterFactory = tripRouterFactory;
		this.planLinkIdentifier = planLinkIdentifier;
		this.mainModeIdentifier = mainModeIdentifier;
		this.timeInterpretation = timeInterpretation;
	}



	@Override
	public GroupPlanStrategy get() {
		final GroupPlanStrategy strategy = instantiateStrategy( sc.getConfig() );
		
		final Config config = sc.getConfig();

		strategy.addStrategyModule(
				new JointPlanMergingModule(
					((JointPlans) sc.getScenarioElement( JointPlans.ELEMENT_NAME  )).getFactory(),
					config.global().getNumberOfThreads(),
					// merge everything
					1.0 ) );

		strategy.addStrategyModule(
			new JointPlanBasedGroupStrategyModule(
					new AbstractMultithreadedGenericStrategyModule<JointPlan>( config.global() ) {
						@Override
						public GenericPlanAlgorithm<JointPlan> createAlgorithm(ReplanningContext replanningContext) {
							return new JointTripInsertorAndRemoverAlgorithm(
								sc,
								MatsimRandom.getLocalInstance(),
								true,  // "iterative"
								mainModeIdentifier);
						}
						
						@Override
						protected String getName() {
							return "JointTripMutator";
						}
					}));

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createReRouteModule(
						config,
						planRoutingAlgorithmFactory,
						tripRouterFactory) );

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createSynchronizerModule(
					config,
					tripRouterFactory, timeInterpretation) );

		final VehicleRessources vehicles = 
					(VehicleRessources) sc.getScenarioElement(
							VehicleRessources.ELEMENT_NAME );

		if ( vehicles != null ) {
			strategy.addStrategyModule(
					new AllocateVehicleToPlansInGroupPlanModule(
						sc.getConfig().global().getNumberOfThreads(),
						vehicles,
						SharedVehicleUtils.DEFAULT_VEHICULAR_MODES,
						false,
						true)); // preserve allocation (ie just reallocate for modified plans)
		}

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createRecomposeJointPlansModule(
					config,
					((JointPlans) sc.getScenarioElement( JointPlans.ELEMENT_NAME  )).getFactory(),
					planLinkIdentifier ) );

		return strategy;
	}
}

