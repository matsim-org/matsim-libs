/* *********************************************************************** *
 * project: org.matsim.*
 * JointPrismLocationChoiceStrategyFactory.java
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

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.router.TripRouter;

import playground.thibautd.socnetsim.framework.PlanRoutingAlgorithmFactory;
import playground.thibautd.socnetsim.framework.cliques.config.JointTripInsertorConfigGroup;
import playground.thibautd.socnetsim.framework.population.JointPlan;
import playground.thibautd.socnetsim.framework.population.JointPlans;
import playground.thibautd.socnetsim.framework.population.SocialNetwork;
import playground.thibautd.socnetsim.framework.replanning.GenericPlanAlgorithm;
import playground.thibautd.socnetsim.framework.replanning.GroupPlanStrategy;
import playground.thibautd.socnetsim.replanning.GroupPlanStrategyFactoryUtils;
import playground.thibautd.socnetsim.framework.replanning.JointPlanBasedGroupStrategyModule;
import playground.thibautd.socnetsim.framework.replanning.modules.AbstractMultithreadedGenericStrategyModule;
import playground.thibautd.socnetsim.replanning.modules.JointTripInsertorAlgorithm;
import playground.thibautd.socnetsim.framework.replanning.modules.PlanLinkIdentifier;
import playground.thibautd.socnetsim.framework.replanning.modules.PlanLinkIdentifier.Strong;
import playground.thibautd.socnetsim.replanning.modules.prismiclocationchoice.PrismicLocationChoiceModule;
import playground.thibautd.socnetsim.sharedvehicles.VehicleRessources;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * @author thibautd
 */
public class JointPrismLocationChoiceWithJointTripInsertionStrategyFactory extends AbstractConfigurableSelectionStrategy {

	private final Scenario sc;
	private final PlanRoutingAlgorithmFactory planRoutingAlgorithmFactory;
	private final Provider<TripRouter> tripRouterFactory;
	private final PlanLinkIdentifier planLinkIdentifier;

	@Inject
	public JointPrismLocationChoiceWithJointTripInsertionStrategyFactory( Scenario sc , PlanRoutingAlgorithmFactory planRoutingAlgorithmFactory ,
			Provider<TripRouter> tripRouterFactory , @Strong PlanLinkIdentifier planLinkIdentifier ) {
		this.sc = sc;
		this.planRoutingAlgorithmFactory = planRoutingAlgorithmFactory;
		this.tripRouterFactory = tripRouterFactory;
		this.planLinkIdentifier = planLinkIdentifier;
	}


	@Override
	public GroupPlanStrategy get() {
		final GroupPlanStrategy strategy = instantiateStrategy( sc.getConfig() );

		strategy.addStrategyModule(
				new PrismicLocationChoiceModule(
					sc ) );

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createJointTripAwareTourModeUnifierModule(
					sc.getConfig(),
					tripRouterFactory ) );
		
		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createRecomposeJointPlansModule(
					sc.getConfig(),
					((JointPlans) sc.getScenarioElement( JointPlans.ELEMENT_NAME  )).getFactory(),
					planLinkIdentifier ) );
		
		strategy.addStrategyModule(
			new JointPlanBasedGroupStrategyModule(
					new AbstractMultithreadedGenericStrategyModule<JointPlan>( sc.getConfig().global() ) {
						@Override
						public GenericPlanAlgorithm<JointPlan> createAlgorithm(ReplanningContext replanningContext) {
							return new JointTripInsertorAlgorithm(
								MatsimRandom.getLocalInstance(),
								(SocialNetwork) sc.getScenarioElement(  SocialNetwork.ELEMENT_NAME ),
								(JointTripInsertorConfigGroup) sc.getConfig().getModule( JointTripInsertorConfigGroup.GROUP_NAME ),
								tripRouterFactory.get());
						}
						
						@Override
						protected String getName() {
							return "JointTripMutator";
						}
					}));

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createSynchronizerModule(
					sc.getConfig(),
					tripRouterFactory) );

		// TODO: add an option to enable or disable this part?
		final VehicleRessources vehicles =
				(VehicleRessources) sc.getScenarioElement(
					VehicleRessources.ELEMENT_NAME );
		if ( vehicles != null ) {
			strategy.addStrategyModule(
					GroupPlanStrategyFactoryUtils.createVehicleAllocationModule(
						sc.getConfig(),
						vehicles ) );
		}

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createReRouteModule(
					sc.getConfig(),
					planRoutingAlgorithmFactory,
					tripRouterFactory ) );

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createSynchronizerModule(
					sc.getConfig(),
					tripRouterFactory) );

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createRecomposeJointPlansModule(
					sc.getConfig(),
					((JointPlans) sc.getScenarioElement( JointPlans.ELEMENT_NAME  )).getFactory(),
					planLinkIdentifier ) );

		return strategy;
	}
}

