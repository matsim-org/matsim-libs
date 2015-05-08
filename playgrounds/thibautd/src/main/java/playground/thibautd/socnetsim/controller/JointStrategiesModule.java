/* *********************************************************************** *
 * project: org.matsim.*
 * JointStrategiesModule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.thibautd.socnetsim.controller;

import org.matsim.core.controler.AbstractModule;

import playground.thibautd.socnetsim.replanning.ExtraPlanRemover;
import playground.thibautd.socnetsim.replanning.ExtraPlanRemoverFactory;
import playground.thibautd.socnetsim.replanning.GroupLevelPlanSelectorFactory;
import playground.thibautd.socnetsim.replanning.GroupPlanStrategy;
import playground.thibautd.socnetsim.replanning.NonInnovativeStrategyFactory;
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

import com.google.inject.Provider;
import com.google.inject.multibindings.MapBinder;

/**
 * @author thibautd
 */
public class JointStrategiesModule extends AbstractModule {
    private MapBinder<String, GroupPlanStrategy> planStrategyBinder;
    private MapBinder<String, GroupLevelPlanSelector> selectorBinder;
    private MapBinder<String, ExtraPlanRemover> removerBinder;

	@Override
	public void install() {
		planStrategyBinder = MapBinder.newMapBinder(binder(), String.class, GroupPlanStrategy.class);

		// default factories
		// ---------------------------------------------------------------------
		addFactory(
				"ReRoute",
				GroupReRouteFactory.class );
		addFactory(
				"TimeAllocationMutator",
				GroupTimeAllocationMutatorFactory.class );
		addFactory(
				"JointTripMutator",
				JointTripMutatorFactory.class );
		addFactory(
				"SubtourModeChoice",
				GroupSubtourModeChoiceFactory.class );
		addFactory(
				"TourVehicleAllocation",
				GroupTourVehicleAllocationFactory.class );
		addFactory(
				"PlanVehicleAllocation",
				GroupPlanVehicleAllocationFactory.class );
		addFactory(
				"OptimizingTourVehicleAllocation",
				GroupOptimizingTourVehicleAllocationFactory.class );
		addFactory(
				"RandomJointPlanRecomposer",
				GroupRandomJointPlanRecomposerFactory.class );
		addFactory(
				"ActivityInGroupLocationChoice",
				ActivityInGroupLocationChoiceFactory.class );
		addFactory(
				"ActivitySequenceMutator",
				GroupActivitySequenceMutator.class );
		addFactory(
				"JointLocationMutator",
				RandomJointLocationChoiceStrategyFactory.class );
		addFactory(
				"JointPrismLocationChoice",
				JointPrismLocationChoiceStrategyFactory.class );
		addFactory(
				"JointPrismLocationChoiceWithJointTrips",
				JointPrismLocationChoiceWithJointTripInsertionStrategyFactory.class );

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

	public void addFactory(
			final String name,
			final Class<? extends Provider<? extends GroupPlanStrategy>> f) {
		planStrategyBinder.addBinding( name ).toProvider( f );
	}

	public void addSelectorFactory(
			final String name,
			final Provider<GroupLevelPlanSelector> f) {
		selectorBinder.addBinding( name ).toProvider( f );
	}

	public void addSelectorAndStrategyFactory(
			final String name,
			final NonInnovativeStrategyFactory f) {
		addFactory( name ,
				new Provider<GroupPlanStrategy>() {
					@Override
					public GroupPlanStrategy get() {
						// TODO pass something in some way
						return f.createStrategy( null );
					}
				} );
		addSelectorFactory( name ,
				new Provider<GroupLevelPlanSelector>() {
					@Override
					public GroupLevelPlanSelector get() {
						// TODO pass something in some way
						return f.createSelector( null );
					}
				} );
	}

	public void addRemoverFactory(
			final String name,
			final ExtraPlanRemoverFactory f) {
		removerBinder
			.addBinding( name )
			.toProvider( 
				new Provider<ExtraPlanRemover>() {
					@Override
					public ExtraPlanRemover get() {
						return f.createRemover( null );
					}
				} );
	}

}

