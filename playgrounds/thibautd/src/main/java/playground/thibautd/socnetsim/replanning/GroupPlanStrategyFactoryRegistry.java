/* *********************************************************************** *
 * project: org.matsim.*
 * GroupPlanStrategyFactoryRegistry.java
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
package playground.thibautd.socnetsim.replanning;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.MatsimRandom;

import playground.thibautd.socnetsim.controller.ControllerRegistry;
import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;
import playground.thibautd.socnetsim.replanning.selectors.coalitionselector.CoalitionSelector;
import playground.thibautd.socnetsim.replanning.selectors.GroupLevelPlanSelector;
import playground.thibautd.socnetsim.replanning.selectors.GroupLevelSelectorFactory;
import playground.thibautd.socnetsim.replanning.selectors.highestweightselection.HighestWeightSelector;
import playground.thibautd.socnetsim.replanning.selectors.InverseScoreWeight;
import playground.thibautd.socnetsim.replanning.selectors.LossWeight;
import playground.thibautd.socnetsim.replanning.selectors.LowestScoreOfJointPlanWeight;
import playground.thibautd.socnetsim.replanning.selectors.LowestScoreSumSelectorForRemoval;
import playground.thibautd.socnetsim.replanning.selectors.ParetoWeight;
import playground.thibautd.socnetsim.replanning.selectors.WeightCalculator;
import playground.thibautd.socnetsim.replanning.selectors.WeightedWeight;
import playground.thibautd.socnetsim.replanning.selectors.whoisthebossselector.WhoIsTheBossSelector;
import playground.thibautd.socnetsim.replanning.strategies.CliqueJointTripMutatorFactory;
import playground.thibautd.socnetsim.replanning.strategies.GroupMinLossSelectExpBetaFactory;
import playground.thibautd.socnetsim.replanning.strategies.GroupMinSelectExpBetaFactory;
import playground.thibautd.socnetsim.replanning.strategies.GroupOptimizingTourVehicleAllocationFactory;
import playground.thibautd.socnetsim.replanning.strategies.GroupPlanVehicleAllocationFactory;
import playground.thibautd.socnetsim.replanning.strategies.GroupRandomJointPlanRecomposerFactory;
import playground.thibautd.socnetsim.replanning.strategies.GroupReRouteFactory;
import playground.thibautd.socnetsim.replanning.strategies.GroupSelectExpBetaFactory;
import playground.thibautd.socnetsim.replanning.strategies.GroupSubtourModeChoiceFactory;
import playground.thibautd.socnetsim.replanning.strategies.GroupTimeAllocationMutatorFactory;
import playground.thibautd.socnetsim.replanning.strategies.GroupTourVehicleAllocationFactory;
import playground.thibautd.socnetsim.replanning.strategies.GroupWhoIsTheBossSelectExpBetaFactory;
import playground.thibautd.socnetsim.replanning.strategies.ParetoExpBetaFactory;
import playground.thibautd.socnetsim.run.GroupReplanningConfigGroup;

/**
 * @author thibautd
 */
public class GroupPlanStrategyFactoryRegistry {
	private final Map<String, GroupPlanStrategyFactory> factories = new HashMap<String, GroupPlanStrategyFactory>();
	private final Map<String, GroupLevelSelectorFactory> selectors = new HashMap<String, GroupLevelSelectorFactory>();

	public GroupPlanStrategyFactoryRegistry() {
		// default factories
		// ---------------------------------------------------------------------
		addFactory( "ReRoute" , new GroupReRouteFactory() );
		addFactory( "TimeAllocationMutator" , new GroupTimeAllocationMutatorFactory( 1 ) );
		addFactory( "OptimizingCliqueJointTripMutator" , new CliqueJointTripMutatorFactory( true ) );
		addFactory( "CliqueJointTripMutator" , new CliqueJointTripMutatorFactory( false ) );
		addFactory( "SubtourModeChoice" , new GroupSubtourModeChoiceFactory() );
		addFactory( "TourVehicleAllocation" , new GroupTourVehicleAllocationFactory() );
		addFactory( "PlanVehicleAllocation" , new GroupPlanVehicleAllocationFactory() );
		addFactory( "OptimizingTourVehicleAllocation" , new GroupOptimizingTourVehicleAllocationFactory() );
		addFactory( "RandomJointPlanRecomposer" , new GroupRandomJointPlanRecomposerFactory() );
		// XXX c'tor needs parameters
		// addFactory( "ActivityInGroupLocationChoice" , new ActivityInGroupLocationChoiceFactory( type ) );

		addFactory( "SelectExpBeta" , new GroupSelectExpBetaFactory() );
		// XXX c'tor needs parameters
		// addFactory( "WeightedSelectExpBeta" , new GroupWeightedSelectExpBetaFactory() );
		addFactory( "WhoIsTheBossSelectExpBeta" , new GroupWhoIsTheBossSelectExpBetaFactory() );
		addFactory( "MinSelectExpBeta" , new GroupMinSelectExpBetaFactory() );
		addFactory( "MinLossSelectExpBeta" , new GroupMinLossSelectExpBetaFactory() );
		addFactory( "ParetoSelectExpBeta" , new ParetoExpBetaFactory() );

		// default selectors
		// ---------------------------------------------------------------------
		addSelectorFactory(
				"MinimumWeightedSum",
				new GroupLevelSelectorFactory() {
					@Override
					public GroupLevelPlanSelector createSelector(
							final ControllerRegistry controllerRegistry) {
						final GroupReplanningConfigGroup weights = (GroupReplanningConfigGroup)
								controllerRegistry.getScenario().getConfig().getModule(
									GroupReplanningConfigGroup.GROUP_NAME );
						return new HighestWeightSelector(
								true ,
								controllerRegistry.getIncompatiblePlansIdentifierFactory(),
								new WeightedWeight(
									new InverseScoreWeight(),
									weights.getWeightAttributeName(),
									controllerRegistry.getScenario().getPopulation().getPersonAttributes()  ));
					}
				});
		addSelectorFactory(
				"MinimumSum",
				new GroupLevelSelectorFactory() {
					@Override
					public GroupLevelPlanSelector createSelector(
							final ControllerRegistry controllerRegistry) {
						return new LowestScoreSumSelectorForRemoval(
								controllerRegistry.getIncompatiblePlansIdentifierFactory());
					}
				});
		addSelectorFactory(
				"MinimumOfSumOfMinimumsOfJointPlan",
				new GroupLevelSelectorFactory() {
					@Override
					public GroupLevelPlanSelector createSelector(
							final ControllerRegistry controllerRegistry) {
						final WeightCalculator baseWeight =
							new LowestScoreOfJointPlanWeight(
									controllerRegistry.getJointPlans());
						return new HighestWeightSelector(
								true ,
								controllerRegistry.getIncompatiblePlansIdentifierFactory(),
								new WeightCalculator() {
									@Override
									public double getWeight(
											final Plan indivPlan,
											final ReplanningGroup replanningGroup) {
										return -baseWeight.getWeight( indivPlan , replanningGroup );
									}
								});
					}
				});
		addSelectorFactory(
				"MinimumOfSumOfMinimumIndividualLossOfJointPlan",
				new GroupLevelSelectorFactory() {
					@Override
					public GroupLevelPlanSelector createSelector(
							final ControllerRegistry controllerRegistry) {
						final WeightCalculator baseWeight =
							new LowestScoreOfJointPlanWeight(
									new LossWeight(),
									controllerRegistry.getJointPlans());
						return new HighestWeightSelector(
								true ,
								controllerRegistry.getIncompatiblePlansIdentifierFactory(),
								new WeightCalculator() {
									@Override
									public double getWeight(
											final Plan indivPlan,
											final ReplanningGroup replanningGroup) {
										return -baseWeight.getWeight( indivPlan , replanningGroup );
									}
								});
					}
				});
		addSelectorFactory(
				"WhoIsTheBoss",
				new GroupLevelSelectorFactory() {
					@Override
					public GroupLevelPlanSelector createSelector(
							final ControllerRegistry controllerRegistry) {
						return new WhoIsTheBossSelector(
								true ,
								MatsimRandom.getLocalInstance(),
								controllerRegistry.getIncompatiblePlansIdentifierFactory(),
								new InverseScoreWeight() );
					}
				});
		addSelectorFactory(
				"Pareto",
				new GroupLevelSelectorFactory() {
					@Override
					public GroupLevelPlanSelector createSelector(
							final ControllerRegistry controllerRegistry) {
						return new HighestWeightSelector(
								true ,
								controllerRegistry.getIncompatiblePlansIdentifierFactory(),
								new ParetoWeight(
									new InverseScoreWeight() ) );
					}
				});
		addSelectorFactory(
				"Coalition",
				new GroupLevelSelectorFactory() {
					@Override
					public GroupLevelPlanSelector createSelector(
							final ControllerRegistry registry) {
						return new CoalitionSelector( new InverseScoreWeight() );
					}
				});

	}

	public GroupPlanStrategyFactory getFactory( final String name ) {
		final GroupPlanStrategyFactory f = factories.get( name );

		if ( f == null ) {
			throw new IllegalArgumentException( "strategy "+name+
					" is not known. Known names are "+factories.keySet() );
		}

		return f;
	}

	public void addFactory(
			final String name,
			final GroupPlanStrategyFactory f) {
		final GroupPlanStrategyFactory old = factories.put( name , f );

		if ( old != null ) {
			throw new IllegalArgumentException( "strategy "+name+" already known. Replacing factory is unsafe. Consider using another name for "+f );
		}
	}

	public GroupLevelSelectorFactory getSelectorFactory( final String name ) {
		final GroupLevelSelectorFactory f = selectors.get( name );

		if ( f == null ) {
			throw new IllegalArgumentException( "selector "+name+
					" is not known. Known names are "+selectors.keySet() );
		}

		return f;
	}

	public void addSelectorFactory(
			final String name,
			final GroupLevelSelectorFactory f) {
		final GroupLevelSelectorFactory old = selectors.put( name , f );

		if ( old != null ) {
			throw new IllegalArgumentException( "selector "+name+" already known. Replacing selector is unsafe. Consider using another name for "+f );
		}
	}
}

