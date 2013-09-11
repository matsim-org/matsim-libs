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

/**
 * @author thibautd
 */
public class GroupPlanStrategyFactoryRegistry {
	private final Map<String, GroupPlanStrategyFactory> factories = new HashMap<String, GroupPlanStrategyFactory>();

	public GroupPlanStrategyFactoryRegistry() {
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
}

