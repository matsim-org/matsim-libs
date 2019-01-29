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
package org.matsim.contrib.socnetsim.usage.replanning;

import org.matsim.contrib.socnetsim.framework.replanning.selectors.WeakSelector;
import org.matsim.contrib.socnetsim.framework.replanning.selectors.highestweightselection.RandomGroupLevelSelector;

import org.matsim.contrib.socnetsim.framework.replanning.modules.PlanLinkIdentifier;
import org.matsim.contrib.socnetsim.framework.replanning.modules.PlanLinkIdentifier.Weak;
import org.matsim.contrib.socnetsim.usage.replanning.removers.CoalitionMinSelectorFactory;
import org.matsim.contrib.socnetsim.usage.replanning.removers.LexicographicRemoverFactory;
import org.matsim.contrib.socnetsim.usage.replanning.removers.MinimumSumOfMinimumLossSelectorFactory;
import org.matsim.contrib.socnetsim.usage.replanning.removers.MinimumSumOfMinimumsSelectorFactory;
import org.matsim.contrib.socnetsim.usage.replanning.removers.MinimumSumSelectorFactory;
import org.matsim.contrib.socnetsim.usage.replanning.removers.MinimumWeightedSumSelectorFactory;
import org.matsim.contrib.socnetsim.usage.replanning.removers.ParetoMinSelectorFactory;
import org.matsim.contrib.socnetsim.usage.replanning.removers.WhoIsTheBossMinSelectorFactory;
import org.matsim.contrib.socnetsim.framework.replanning.selectors.GroupLevelPlanSelector;
import org.matsim.contrib.socnetsim.framework.replanning.selectors.IncompatiblePlansIdentifierFactory;
import org.matsim.contrib.socnetsim.framework.replanning.selectors.coalitionselector.LeastAverageWeightJointPlanPruningConflictSolver;
import org.matsim.contrib.socnetsim.framework.replanning.selectors.coalitionselector.LeastPointedPlanPruningConflictSolver;
import org.matsim.contrib.socnetsim.usage.replanning.strategies.ActivityInGroupLocationChoiceFactory;
import org.matsim.contrib.socnetsim.framework.replanning.strategies.CoalitionExpBetaFactory;
import org.matsim.contrib.socnetsim.framework.replanning.strategies.CoalitionRandomFactory;
import org.matsim.contrib.socnetsim.usage.replanning.strategies.GroupActivitySequenceMutator;
import org.matsim.contrib.socnetsim.usage.replanning.strategies.GroupMinLossSelectExpBetaFactory;
import org.matsim.contrib.socnetsim.usage.replanning.strategies.GroupMinSelectExpBetaFactory;
import org.matsim.contrib.socnetsim.usage.replanning.strategies.GroupOptimizingTourVehicleAllocationFactory;
import org.matsim.contrib.socnetsim.usage.replanning.strategies.GroupPlanVehicleAllocationFactory;
import org.matsim.contrib.socnetsim.usage.replanning.strategies.GroupRandomJointPlanRecomposerFactory;
import org.matsim.contrib.socnetsim.usage.replanning.strategies.GroupReRouteFactory;
import org.matsim.contrib.socnetsim.framework.replanning.strategies.GroupSelectExpBetaFactory;
import org.matsim.contrib.socnetsim.usage.replanning.strategies.GroupSubtourModeChoiceFactory;
import org.matsim.contrib.socnetsim.usage.replanning.strategies.GroupTimeAllocationMutatorFactory;
import org.matsim.contrib.socnetsim.usage.replanning.strategies.GroupTourVehicleAllocationFactory;
import org.matsim.contrib.socnetsim.usage.replanning.strategies.GroupWeightedSelectExpBetaFactory;
import org.matsim.contrib.socnetsim.framework.replanning.strategies.GroupWhoIsTheBossSelectExpBetaFactory;
import org.matsim.contrib.socnetsim.usage.replanning.strategies.JointPrismLocationChoiceStrategyFactory;
import org.matsim.contrib.socnetsim.usage.replanning.strategies.JointPrismLocationChoiceWithJointTripInsertionStrategyFactory;
import org.matsim.contrib.socnetsim.usage.replanning.strategies.JointTripMutatorFactory;
import org.matsim.contrib.socnetsim.usage.replanning.strategies.ParetoExpBetaFactory;
import org.matsim.contrib.socnetsim.usage.replanning.strategies.RandomGroupPlanSelectorStrategyFactory;
import org.matsim.contrib.socnetsim.usage.replanning.strategies.RandomJointLocationChoiceStrategyFactory;
import org.matsim.contrib.socnetsim.usage.replanning.strategies.RandomSumGroupPlanSelectorStrategyFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.matsim.core.gbl.MatsimRandom;

/**
 * @author thibautd
 */
public class DefaultJointStrategiesModule extends AbstractJointStrategiesModule {

	@Override
	public void install() {

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
						return new WeakSelector(
								weakIdentifier,
								new RandomGroupLevelSelector(
										MatsimRandom.getLocalInstance(),
										incompatiblePlansIdentifierFactory ) );
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

}

