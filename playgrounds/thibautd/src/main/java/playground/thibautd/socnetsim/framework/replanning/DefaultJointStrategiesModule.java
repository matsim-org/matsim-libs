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
package playground.thibautd.socnetsim.framework.replanning;

import java.util.Map;

import org.matsim.core.controler.AbstractModule;

import playground.thibautd.socnetsim.replanning.modules.PlanLinkIdentifier;
import playground.thibautd.socnetsim.replanning.modules.PlanLinkIdentifier.Weak;
import playground.thibautd.socnetsim.replanning.removers.CoalitionMinSelectorFactory;
import playground.thibautd.socnetsim.replanning.removers.LexicographicRemoverFactory;
import playground.thibautd.socnetsim.replanning.removers.MinimumSumOfMinimumLossSelectorFactory;
import playground.thibautd.socnetsim.replanning.removers.MinimumSumOfMinimumsSelectorFactory;
import playground.thibautd.socnetsim.replanning.removers.MinimumSumSelectorFactory;
import playground.thibautd.socnetsim.replanning.removers.MinimumWeightedSumSelectorFactory;
import playground.thibautd.socnetsim.replanning.removers.ParetoMinSelectorFactory;
import playground.thibautd.socnetsim.replanning.removers.WhoIsTheBossMinSelectorFactory;
import playground.thibautd.socnetsim.replanning.selectors.GroupLevelPlanSelector;
import playground.thibautd.socnetsim.replanning.selectors.IncompatiblePlansIdentifierFactory;
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

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.multibindings.MapBinder;

/**
 * @author thibautd
 */
public class DefaultJointStrategiesModule extends AbstractModule {
    private MapBinder<String, GroupPlanStrategy> planStrategyBinder;
    private MapBinder<String, GroupLevelPlanSelector> selectorBinder;
    private MapBinder<String, ExtraPlanRemover> removerBinder;
    private MapBinder<String, NonInnovativeStrategyFactory> nonInnovativeBinder;

	@Override
	public void install() {
		planStrategyBinder = MapBinder.newMapBinder(binder(), String.class, GroupPlanStrategy.class);
		selectorBinder =  MapBinder.newMapBinder( binder() , String.class , GroupLevelPlanSelector.class );
		removerBinder =  MapBinder.newMapBinder( binder() , String.class , ExtraPlanRemover.class );
		nonInnovativeBinder =  MapBinder.newMapBinder( binder() , String.class , NonInnovativeStrategyFactory.class );

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
				GroupSelectExpBetaFactory.class );
		addSelectorAndStrategyFactory(
				"WeightedSelectExpBeta",
				GroupWeightedSelectExpBetaFactory.class );
		addSelectorAndStrategyFactory(
				"WhoIsTheBossSelectExpBeta",
				GroupWhoIsTheBossSelectExpBetaFactory.class );
		addSelectorAndStrategyFactory(
				"MinSelectExpBeta",
				GroupMinSelectExpBetaFactory.class );
		addSelectorAndStrategyFactory(
				"MinLossSelectExpBeta",
				GroupMinLossSelectExpBetaFactory.class );
		addSelectorAndStrategyFactory(
				"ParetoSelectExpBeta",
				ParetoExpBetaFactory.class );
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
				RandomGroupPlanSelectorStrategyFactory.class );
		addSelectorAndStrategyFactory(
				"RandomSumSelection",
				RandomSumGroupPlanSelectorStrategyFactory.class );

		// "Weak" versions of selectors (for configurable selection strategies)
		// ---------------------------------------------------------------------
		addSelectorFactory(
				"WeakRandomSelection",
				new Provider<GroupLevelPlanSelector>() {
					@Inject
					private IncompatiblePlansIdentifierFactory incompatiblePlansIdentifierFactory = null;
					@Inject @Weak
					private PlanLinkIdentifier weakIdentifier;

					@Override
					public GroupLevelPlanSelector get() {
						return new WeakSelectorFactory(
								weakIdentifier,
								new RandomGroupPlanSelectorStrategyFactory(
										incompatiblePlansIdentifierFactory ) ).createSelector();
					}
				});

		// default removers
		// ---------------------------------------------------------------------
		addRemoverFactory(
				"MinimumWeightedSum",
				MinimumWeightedSumSelectorFactory.class );
		addRemoverFactory(
				"MinimumSum",
				MinimumSumSelectorFactory.class );
		addRemoverFactory(
				"MinimumOfSumOfMinimumsOfJointPlan",
				MinimumSumOfMinimumsSelectorFactory.class );
		addRemoverFactory(
				"MinimumOfSumOfMinimumIndividualLossOfJointPlan",
				MinimumSumOfMinimumLossSelectorFactory.class );
		addRemoverFactory(
				"WhoIsTheBoss",
				WhoIsTheBossMinSelectorFactory.class );
		addRemoverFactory(
				"Pareto",
				ParetoMinSelectorFactory.class );
		addRemoverFactory(
				"Coalition",
				CoalitionMinSelectorFactory.class );
		addRemoverFactory(
				"LexicographicPerComposition",
				LexicographicRemoverFactory.class );
	}

	public void addFactory(
			final String name,
			final Class<? extends Provider<? extends GroupPlanStrategy>> f) {
		planStrategyBinder.addBinding( name ).toProvider( f );
	}

	public void addFactory(
			final String name,
			final Provider<? extends GroupPlanStrategy> f) {
		planStrategyBinder.addBinding( name ).toProvider( f );
	}

	public void addSelectorFactory(
			final String name,
			final Provider<GroupLevelPlanSelector> f) {
		selectorBinder.addBinding( name ).toProvider( f );
	}

	public void addSelectorAndStrategyFactory(
			final String name,
			final Class<? extends NonInnovativeStrategyFactory> f) {
		// Not really nice, but could not come with something better right now:
		// still need constructor to be injected,
		// and the class to provide two providers (which one cannot implement
		// at the same time)
		nonInnovativeBinder.addBinding( name ).to( f );
		addFactory( name , f );
		addSelectorFactory( name ,
				new Provider<GroupLevelPlanSelector>() {
					@Inject Map<String, NonInnovativeStrategyFactory> map;

					@Override
					public GroupLevelPlanSelector get() {
						return map.get( name ).createSelector();
					}
				} );
	}

	public void addSelectorAndStrategyFactory(
			final String name,
			final NonInnovativeStrategyFactory f) {
		// Not really nice, but could not come with something better right now:
		// still need constructor to be injected,
		// and the class to provide two providers (which one cannot implement
		// at the same time)
		nonInnovativeBinder.addBinding( name ).toInstance( f );
		addFactory( name , f );
		addSelectorFactory( name ,
				new Provider<GroupLevelPlanSelector>() {
					@Inject Map<String, NonInnovativeStrategyFactory> map;

					@Override
					public GroupLevelPlanSelector get() {
						return map.get( name ).createSelector();
					}
				} );
	}

	public void addRemoverFactory(
			final String name,
			final Class<? extends Provider<ExtraPlanRemover>> f) {
		removerBinder
			.addBinding( name )
			.toProvider( f );
	}

}

