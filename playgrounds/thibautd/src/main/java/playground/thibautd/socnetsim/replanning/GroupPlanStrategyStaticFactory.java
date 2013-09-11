/* *********************************************************************** *
 * project: org.matsim.*
 * GroupPlanStrategyFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

import playground.thibautd.socnetsim.controller.ControllerRegistry;
import playground.thibautd.socnetsim.replanning.strategies.ActivityInGroupLocationChoiceFactory;
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
import playground.thibautd.socnetsim.replanning.strategies.GroupWeightedSelectExpBetaFactory;
import playground.thibautd.socnetsim.replanning.strategies.GroupWhoIsTheBossSelectExpBetaFactory;

/**
 * Provides factory methods to create standard strategies.
 * @author thibautd
 */
public class GroupPlanStrategyStaticFactory {
	private GroupPlanStrategyStaticFactory() {}

	// /////////////////////////////////////////////////////////////////////////
	// strategies
	// /////////////////////////////////////////////////////////////////////////
	public static GroupPlanStrategy createReRoute(
			final ControllerRegistry registry) {
		return new GroupReRouteFactory().createStrategy( registry );
	}

	public static GroupPlanStrategy createTimeAllocationMutator(
			final ControllerRegistry registry) {
		return createTimeAllocationMutator( 24 , registry );
	}

	public static GroupPlanStrategy createTimeAllocationMutator(
			final double maxTemp,
			final ControllerRegistry registry) {
		return new GroupTimeAllocationMutatorFactory( maxTemp ).createStrategy( registry );
	}

	public static GroupPlanStrategy createCliqueJointTripMutator(
			final ControllerRegistry registry) {
		return new CliqueJointTripMutatorFactory( true ).createStrategy( registry );
	}

	/**
	 * for tests only!!!
	 */
	public static GroupPlanStrategy createNonOptimizingCliqueJointTripMutator(
			final ControllerRegistry registry) {
		return new CliqueJointTripMutatorFactory( false ).createStrategy( registry );
	}

	public static GroupPlanStrategy createSelectExpBeta(
			final ControllerRegistry registry) {
		return new GroupSelectExpBetaFactory().createStrategy( registry );
	}

	public static GroupPlanStrategy createWeightedSelectExpBeta(
			final String weightAttributeName,
			final ControllerRegistry registry) {
		return new GroupWeightedSelectExpBetaFactory( weightAttributeName ).createStrategy( registry );
	}

	public static GroupPlanStrategy createWhoIsTheBossSelectExpBeta(
			final ControllerRegistry registry) {
		return new GroupWhoIsTheBossSelectExpBetaFactory().createStrategy( registry );
	}

	public static GroupPlanStrategy createMinSelectExpBeta(
			final ControllerRegistry registry) {
		return new GroupMinSelectExpBetaFactory().createStrategy( registry );
	}

	public static GroupPlanStrategy createMinLossSelectExpBeta(
			final ControllerRegistry registry) {
		return new GroupMinLossSelectExpBetaFactory().createStrategy( registry );
	}

	public static GroupPlanStrategy createSubtourModeChoice(
			final ControllerRegistry registry) {
		return new GroupSubtourModeChoiceFactory().createStrategy( registry );
	}

	public static GroupPlanStrategy createTourVehicleAllocation(
			final ControllerRegistry registry) {
		return new GroupTourVehicleAllocationFactory().createStrategy( registry );
	}

	public static GroupPlanStrategy createGroupPlanVehicleAllocation(
			final ControllerRegistry registry) {
		return new GroupPlanVehicleAllocationFactory().createStrategy( registry );
	}

	public static GroupPlanStrategy createOptimizingTourVehicleAllocation(
			final ControllerRegistry registry) {
		return new GroupOptimizingTourVehicleAllocationFactory().createStrategy( registry );
	}

	public static GroupPlanStrategy createRandomJointPlansRecomposer(
			final ControllerRegistry registry) {
		return new GroupRandomJointPlanRecomposerFactory().createStrategy( registry );
	}

	public static GroupPlanStrategy createActivityInGroupLocationChoice(
			final String type,
			final ControllerRegistry registry) {
		return new ActivityInGroupLocationChoiceFactory( type ).createStrategy( registry );
	}
}

