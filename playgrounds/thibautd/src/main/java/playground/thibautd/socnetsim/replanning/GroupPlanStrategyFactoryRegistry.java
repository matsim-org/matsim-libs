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

import playground.thibautd.socnetsim.controller.ControllerRegistry;
import playground.thibautd.socnetsim.replanning.removers.CoalitionMinSelectorFactory;
import playground.thibautd.socnetsim.replanning.removers.LexicographicRemoverFactory;
import playground.thibautd.socnetsim.replanning.removers.MinimumSumOfMinimumLossSelectorFactory;
import playground.thibautd.socnetsim.replanning.removers.MinimumSumOfMinimumsSelectorFactory;
import playground.thibautd.socnetsim.replanning.removers.MinimumSumSelectorFactory;
import playground.thibautd.socnetsim.replanning.removers.MinimumWeightedSumSelectorFactory;
import playground.thibautd.socnetsim.replanning.removers.ParetoMinSelectorFactory;
import playground.thibautd.socnetsim.replanning.removers.WhoIsTheBossMinSelectorFactory;
import playground.thibautd.socnetsim.replanning.selectors.GroupLevelPlanSelector;
import playground.thibautd.socnetsim.replanning.selectors.coalitionselector.LeastAverageWeightJointPlanPruningConflictSolver;
import playground.thibautd.socnetsim.replanning.selectors.coalitionselector.LeastPointedPlanPruningConflictSolver;
import playground.thibautd.socnetsim.replanning.strategies.ActivityInGroupLocationChoiceFactory;
import playground.thibautd.socnetsim.replanning.strategies.CoalitionExpBetaFactory;
import playground.thibautd.socnetsim.replanning.strategies.CoalitionRandomFactory;
import playground.thibautd.socnetsim.replanning.strategies.GroupActivitySequenceMutator;
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
import playground.thibautd.socnetsim.replanning.strategies.GroupWeightedSelectExpBetaFactory;
import playground.thibautd.socnetsim.replanning.strategies.GroupWhoIsTheBossSelectExpBetaFactory;
import playground.thibautd.socnetsim.replanning.strategies.JointPrismLocationChoiceStrategyFactory;
import playground.thibautd.socnetsim.replanning.strategies.JointPrismLocationChoiceWithJointTripInsertionStrategyFactory;
import playground.thibautd.socnetsim.replanning.strategies.JointTripMutatorFactory;
import playground.thibautd.socnetsim.replanning.strategies.ParetoExpBetaFactory;
import playground.thibautd.socnetsim.replanning.strategies.RandomGroupPlanSelectorStrategyFactory;
import playground.thibautd.socnetsim.replanning.strategies.RandomJointLocationChoiceStrategyFactory;
import playground.thibautd.socnetsim.replanning.strategies.RandomSumGroupPlanSelectorStrategyFactory;
import playground.thibautd.socnetsim.replanning.strategies.WeakSelectorFactory;

/**
 * @author thibautd
 */
public class GroupPlanStrategyFactoryRegistry {
	private final Map<String, GroupLevelPlanSelectorFactory> selectorFactories = new HashMap<String, GroupLevelPlanSelectorFactory>();
	private final Map<String, GroupPlanStrategyFactory> factories = new HashMap<String, GroupPlanStrategyFactory>();
	private final Map<String, ExtraPlanRemoverFactory> selectors = new HashMap<String, ExtraPlanRemoverFactory>();

	public GroupPlanStrategyFactoryRegistry() {
		// default factories
		// ---------------------------------------------------------------------
		addFactory(
				"ReRoute",
				new GroupReRouteFactory( this ) );
		addFactory(
				"TimeAllocationMutator",
				new GroupTimeAllocationMutatorFactory( this , 1 ) );
		addFactory(
				"JointTripMutator",
				new JointTripMutatorFactory( this ) );
		addFactory(
				"SubtourModeChoice",
				new GroupSubtourModeChoiceFactory( this ) );
		addFactory(
				"TourVehicleAllocation",
				new GroupTourVehicleAllocationFactory( this ) );
		addFactory(
				"PlanVehicleAllocation",
				new GroupPlanVehicleAllocationFactory( this ) );
		addFactory(
				"OptimizingTourVehicleAllocation",
				new GroupOptimizingTourVehicleAllocationFactory( this ) );
		addFactory(
				"RandomJointPlanRecomposer",
				new GroupRandomJointPlanRecomposerFactory() );
		addFactory(
				"ActivityInGroupLocationChoice",
				new ActivityInGroupLocationChoiceFactory( this ) );
		addFactory(
				"ActivitySequenceMutator",
				new GroupActivitySequenceMutator( this ) );
		addFactory(
				"JointLocationMutator",
				new RandomJointLocationChoiceStrategyFactory( this ) );
		addFactory(
				"JointPrismLocationChoice",
				new JointPrismLocationChoiceStrategyFactory( this ) );
		addFactory(
				"JointPrismLocationChoiceWithJointTrips",
				new JointPrismLocationChoiceWithJointTripInsertionStrategyFactory( this ) );

		// selectors
		// ---------------------------------------------------------------------
		addSelectorAndStrategyFactory(
				"SelectExpBeta",
				new GroupSelectExpBetaFactory() );
		addSelectorAndStrategyFactory(
				"WeightedSelectExpBeta",
				new GroupWeightedSelectExpBetaFactory() );
		addSelectorAndStrategyFactory(
				"WhoIsTheBossSelectExpBeta",
				new GroupWhoIsTheBossSelectExpBetaFactory() );
		addSelectorAndStrategyFactory(
				"MinSelectExpBeta",
				new GroupMinSelectExpBetaFactory() );
		addSelectorAndStrategyFactory(
				"MinLossSelectExpBeta",
				new GroupMinLossSelectExpBetaFactory() );
		addSelectorAndStrategyFactory(
				"ParetoSelectExpBeta",
				new ParetoExpBetaFactory() );
		addSelectorAndStrategyFactory(
				"CoalitionSelectExpBeta_LeastPointedConflictResolution",
				new CoalitionExpBetaFactory(
					new LeastPointedPlanPruningConflictSolver() ) );
		addSelectorAndStrategyFactory(
				"CoalitionSelectExpBeta_LeastAverageConflictResolution",
				new CoalitionExpBetaFactory(
					new LeastAverageWeightJointPlanPruningConflictSolver() ) );
		addSelectorAndStrategyFactory(
				"CoalitionRandom_LeastPointedConflictResolution",
				new CoalitionRandomFactory(
					new LeastPointedPlanPruningConflictSolver() ) );
		addSelectorAndStrategyFactory(
				"CoalitionRandom_LeastAverageConflictResolution",
				new CoalitionRandomFactory(
					new LeastAverageWeightJointPlanPruningConflictSolver() ) );
		addSelectorAndStrategyFactory(
				"RandomSelection",
				new RandomGroupPlanSelectorStrategyFactory() );
		addSelectorAndStrategyFactory(
				"RandomSumSelection",
				new RandomSumGroupPlanSelectorStrategyFactory() );

		// "Weak" versions of selectors (for configurable selection strategies)
		// ---------------------------------------------------------------------
		addSelectorFactory(
				"WeakRandomSelection",
				new WeakSelectorFactory(
					new RandomGroupPlanSelectorStrategyFactory() ) );

		// default removers
		// ---------------------------------------------------------------------
		addRemoverFactory(
				"MinimumWeightedSum",
				new MinimumWeightedSumSelectorFactory());
		addRemoverFactory(
				"MinimumSum",
				new MinimumSumSelectorFactory());
		addRemoverFactory(
				"MinimumOfSumOfMinimumsOfJointPlan",
				new MinimumSumOfMinimumsSelectorFactory());
		addRemoverFactory(
				"MinimumOfSumOfMinimumIndividualLossOfJointPlan",
				new MinimumSumOfMinimumLossSelectorFactory());
		addRemoverFactory(
				"WhoIsTheBoss",
				new WhoIsTheBossMinSelectorFactory());
		addRemoverFactory(
				"Pareto",
				new ParetoMinSelectorFactory());
		addRemoverFactory(
				"Coalition",
				new CoalitionMinSelectorFactory());
		addRemoverFactory(
				"LexicographicPerComposition",
				new LexicographicRemoverFactory());

	}

	public GroupPlanStrategy createStrategy(
			final String name,
			final ControllerRegistry registry) {
		final GroupPlanStrategyFactory f = factories.get( name );

		if ( f == null ) {
			throw new IllegalArgumentException( "strategy "+name+
					" is not known. Known names are "+factories.keySet() );
		}

		return f.createStrategy( registry );
	}

	public void addFactory(
			final String name,
			final GroupPlanStrategyFactory f) {
		final GroupPlanStrategyFactory old = factories.put( name , f );

		if ( old != null ) {
			throw new IllegalArgumentException( "strategy "+name+" already known. Replacing factory is unsafe. Consider using another name for "+f );
		}
	}

	public void addSelectorFactory(
			final String name,
			final GroupLevelPlanSelectorFactory f) {
		final GroupLevelPlanSelectorFactory old = selectorFactories.put( name , f );

		if ( old != null ) {
			throw new IllegalArgumentException( "selector "+name+" already known. Replacing factory is unsafe. Consider using another name for "+f );
		}
	}

	public <T extends GroupLevelPlanSelectorFactory & GroupPlanStrategyFactory> void
			addSelectorAndStrategyFactory(
				final String name,
				final T f) {
		addFactory( name , f );
		addSelectorFactory( name , f );
	}

	public GroupLevelPlanSelector createSelector(
			final String name,
			final ControllerRegistry registry) {
		final GroupLevelPlanSelectorFactory f = selectorFactories.get( name );

		if ( f == null ) {
			throw new IllegalArgumentException( "remover factory "+name+
					" is not known. Known names are "+selectors.keySet() );
		}

		return f.createSelector( registry );
	}

	public ExtraPlanRemover createRemover(
			final String name,
			final ControllerRegistry registry) {
		final ExtraPlanRemoverFactory f = selectors.get( name );

		if ( f == null ) {
			throw new IllegalArgumentException( "remover factory "+name+
					" is not known. Known names are "+selectors.keySet() );
		}

		return f.createRemover( registry );
	}

	public void addRemoverFactory(
			final String name,
			final ExtraPlanRemoverFactory f) {
		final ExtraPlanRemoverFactory old = selectors.put( name , f );

		if ( old != null ) {
			throw new IllegalArgumentException( "removerFactory "+name+" already known. Replacing selector is unsafe. Consider using another name for "+f );
		}
	}
}

