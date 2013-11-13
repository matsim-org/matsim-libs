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

import playground.thibautd.socnetsim.replanning.removers.CoalitionMinSelectorFactory;
import playground.thibautd.socnetsim.replanning.removers.LexicographicRemoverFactory;
import playground.thibautd.socnetsim.replanning.removers.MinimumSumOfMinimumLossSelectorFactory;
import playground.thibautd.socnetsim.replanning.removers.MinimumSumOfMinimumsSelectorFactory;
import playground.thibautd.socnetsim.replanning.removers.MinimumSumSelectorFactory;
import playground.thibautd.socnetsim.replanning.removers.MinimumWeightedSumSelectorFactory;
import playground.thibautd.socnetsim.replanning.removers.ParetoMinSelectorFactory;
import playground.thibautd.socnetsim.replanning.removers.WhoIsTheBossMinSelectorFactory;
import playground.thibautd.socnetsim.replanning.strategies.ActivityInGroupLocationChoiceFactory;
import playground.thibautd.socnetsim.replanning.strategies.CliqueJointTripMutatorFactory;
import playground.thibautd.socnetsim.replanning.strategies.CoalitionExpBetaFactory;
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
import playground.thibautd.socnetsim.replanning.strategies.GroupWhoIsTheBossSelectExpBetaFactory;
import playground.thibautd.socnetsim.replanning.strategies.ParetoExpBetaFactory;

/**
 * @author thibautd
 */
public class GroupPlanStrategyFactoryRegistry {
	private final Map<String, GroupPlanStrategyFactory> factories = new HashMap<String, GroupPlanStrategyFactory>();
	private final Map<String, ExtraPlanRemoverFactory> selectors = new HashMap<String, ExtraPlanRemoverFactory>();

	public GroupPlanStrategyFactoryRegistry() {
		// default factories
		// ---------------------------------------------------------------------
		addFactory( "ReRoute" , new GroupReRouteFactory() );
		addFactory( "TimeAllocationMutator" , new GroupTimeAllocationMutatorFactory( 1 ) );
		addFactory( "CliqueJointTripMutator" , new CliqueJointTripMutatorFactory( ) );
		addFactory( "SubtourModeChoice" , new GroupSubtourModeChoiceFactory() );
		addFactory( "TourVehicleAllocation" , new GroupTourVehicleAllocationFactory() );
		addFactory( "PlanVehicleAllocation" , new GroupPlanVehicleAllocationFactory() );
		addFactory( "OptimizingTourVehicleAllocation" , new GroupOptimizingTourVehicleAllocationFactory() );
		addFactory( "RandomJointPlanRecomposer" , new GroupRandomJointPlanRecomposerFactory() );
		addFactory( "ActivityInGroupLocationChoice" , new ActivityInGroupLocationChoiceFactory() );

		addFactory( "SelectExpBeta" , new GroupSelectExpBetaFactory() );
		// XXX c'tor needs parameters
		// addFactory( "WeightedSelectExpBeta" , new GroupWeightedSelectExpBetaFactory() );
		addFactory( "WhoIsTheBossSelectExpBeta" , new GroupWhoIsTheBossSelectExpBetaFactory() );
		addFactory( "MinSelectExpBeta" , new GroupMinSelectExpBetaFactory() );
		addFactory( "MinLossSelectExpBeta" , new GroupMinLossSelectExpBetaFactory() );
		addFactory( "ParetoSelectExpBeta" , new ParetoExpBetaFactory() );
		addFactory( "CoalitionSelectExpBeta" , new CoalitionExpBetaFactory() );

		addFactory( "ActivitySequenceMutator" , new GroupActivitySequenceMutator() );

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

	public ExtraPlanRemoverFactory getRemoverFactory( final String name ) {
		final ExtraPlanRemoverFactory f = selectors.get( name );

		if ( f == null ) {
			throw new IllegalArgumentException( "remover factory "+name+
					" is not known. Known names are "+selectors.keySet() );
		}

		return f;
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

