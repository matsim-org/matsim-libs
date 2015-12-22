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
package playground.thibautd.socnetsimusages.traveltimeequity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.socnetsim.framework.PlanRoutingAlgorithmFactory;
import org.matsim.contrib.socnetsim.framework.population.JointPlans;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.contrib.socnetsim.framework.replanning.GenericPlanAlgorithm;
import org.matsim.contrib.socnetsim.framework.replanning.GroupPlanStrategy;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.GroupPlans;
import org.matsim.contrib.socnetsim.framework.replanning.modules.AbstractMultithreadedGenericStrategyModule;
import org.matsim.contrib.socnetsim.framework.replanning.modules.PlanLinkIdentifier;
import org.matsim.contrib.socnetsim.framework.replanning.modules.PlanLinkIdentifier.Strong;
import org.matsim.contrib.socnetsim.jointactivities.replanning.modules.prismiclocationchoice.PrismicLocationChoiceAlgorithm;
import org.matsim.contrib.socnetsim.jointactivities.replanning.modules.prismiclocationchoice.PrismicLocationChoiceConfigGroup;
import org.matsim.contrib.socnetsim.jointtrips.population.JointActingTypes;
import org.matsim.contrib.socnetsim.sharedvehicles.VehicleRessources;
import org.matsim.contrib.socnetsim.usage.replanning.GroupPlanStrategyFactoryUtils;
import org.matsim.contrib.socnetsim.usage.replanning.strategies.AbstractConfigurableSelectionStrategy;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.router.CompositeStageActivityTypes;
import org.matsim.core.router.TripRouter;

/**
 * @author thibautd
 */
public class JointPrismMinStandardDeviationLocationChoiceStrategyFactory extends AbstractConfigurableSelectionStrategy {

	private final Scenario sc;
	private final PlanRoutingAlgorithmFactory planRoutingAlgorithmFactory;
	private final Provider<TripRouter> tripRouterFactory;
	private final PlanLinkIdentifier planLinkIdentifier;

	@Inject
	public JointPrismMinStandardDeviationLocationChoiceStrategyFactory(
			final Scenario sc,
			final PlanRoutingAlgorithmFactory planRoutingAlgorithmFactory,
			final Provider<TripRouter> tripRouterFactory,
			@Strong final PlanLinkIdentifier planLinkIdentifier) {
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
					sc, tripRouterFactory) );

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createJointTripAwareTourModeUnifierModule(
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
				GroupPlanStrategyFactoryUtils.createRecomposeJointPlansModule(
					sc.getConfig(),
					((JointPlans) sc.getScenarioElement( JointPlans.ELEMENT_NAME  )).getFactory(),
					planLinkIdentifier ) );

		return strategy;
	}

	private static class PrismicLocationChoiceModule  extends AbstractMultithreadedGenericStrategyModule<GroupPlans> {
		private final Scenario scenario;

		private javax.inject.Provider<TripRouter> tripRouterProvider;

		public PrismicLocationChoiceModule(final Scenario sc, javax.inject.Provider<TripRouter> tripRouterProvider) {
			super( sc.getConfig().global() );
			this.scenario = sc;
			this.tripRouterProvider = tripRouterProvider;
		}

		@Override
		public GenericPlanAlgorithm<GroupPlans> createAlgorithm(final ReplanningContext replanningContext) {
			return new PrismicLocationChoiceAlgorithm(
					(PrismicLocationChoiceConfigGroup) scenario.getConfig().getModule( PrismicLocationChoiceConfigGroup.GROUP_NAME ),
					new DistanceStandardDeviationLocationChooser(),
					scenario.getActivityFacilities(),
					(SocialNetwork) scenario.getScenarioElement( SocialNetwork.ELEMENT_NAME ),
					new CompositeStageActivityTypes(
							tripRouterProvider.get().getStageActivityTypes(),
							JointActingTypes.JOINT_STAGE_ACTS ) );
		}
	}
}

