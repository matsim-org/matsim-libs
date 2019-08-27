/* *********************************************************************** *
 * project: org.matsim.*
 * GroupPlanStrategyFactoryUtils.java
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
package org.matsim.contrib.socnetsim.usage.replanning;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.CompositeStageActivityTypes;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripRouter;
import org.matsim.contrib.socnetsim.framework.replanning.modules.TourModeUnifierModule;
import org.matsim.contrib.socnetsim.framework.PlanRoutingAlgorithmFactory;
import org.matsim.contrib.socnetsim.framework.replanning.GenericStrategyModule;
import org.matsim.contrib.socnetsim.framework.replanning.IndividualBasedGroupStrategyModule;
import org.matsim.contrib.socnetsim.framework.replanning.JointPlanBasedGroupStrategyModule;
import org.matsim.contrib.socnetsim.jointtrips.population.JointActingTypes;
import org.matsim.contrib.socnetsim.framework.population.JointPlanFactory;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.GroupPlans;
import org.matsim.contrib.socnetsim.framework.replanning.modules.PlanLinkIdentifier;
import org.matsim.contrib.socnetsim.framework.replanning.modules.RecomposeJointPlanModule;
import org.matsim.contrib.socnetsim.jointtrips.replanning.modules.SynchronizeCoTravelerPlansModule;
import org.matsim.contrib.socnetsim.sharedvehicles.SharedVehicleUtils;
import org.matsim.contrib.socnetsim.sharedvehicles.VehicleRessources;
import org.matsim.contrib.socnetsim.sharedvehicles.replanning.AllocateVehicleToPlansInGroupPlanModule;

import javax.inject.Provider;
import java.util.List;

/**
 * @author thibautd
 */
public class GroupPlanStrategyFactoryUtils {

	//public static GroupPlanStrategy createRandomSelectingStrategy(
	//		final IncompatiblePlansIdentifierFactory fact) {
	//	return new GroupPlanStrategy(
	//			new RandomGroupLevelSelector(
	//				MatsimRandom.getLocalInstance(),
	//				fact) );
	//}

	// /////////////////////////////////////////////////////////////////////////
	// standard modules
	// /////////////////////////////////////////////////////////////////////////

	public static GenericStrategyModule<GroupPlans> createSynchronizerModule(
			final Config config,
			final Provider<TripRouter> tripRouterFactory) {
		return new JointPlanBasedGroupStrategyModule(
				new SynchronizeCoTravelerPlansModule(
					config.global().getNumberOfThreads() ) );
	}

	public static GenericStrategyModule<GroupPlans> createReRouteModule(
			final Config config,
			final PlanRoutingAlgorithmFactory planRouterFactory,
			final Provider<TripRouter> tripRouterFactory) {
		return new IndividualBasedGroupStrategyModule(
				new AbstractMultithreadedModule( config.global() ) {
					@Override
					public PlanAlgorithm getPlanAlgoInstance() {
						return planRouterFactory.createPlanRoutingAlgorithm( tripRouterFactory.get() );
					}
				});
	}

	public static GenericStrategyModule<GroupPlans> createJointTripAwareTourModeUnifierModule(
			final Config config,
			final Provider<TripRouter> tripRouterFactory) {
		final TripRouter router = tripRouterFactory.get();

		return new IndividualBasedGroupStrategyModule(
				new TourModeUnifierModule(
					config.global().getNumberOfThreads(),
					JointActingTypes.JOINT_STAGE_ACTS,
					new MainModeIdentifier() {
						@Override
						public String identifyMainMode(
								final List<? extends PlanElement> tripElements) {
							for ( PlanElement pe : tripElements ) {
								if ( pe instanceof Leg &&
										((Leg) pe).getMode().equals( JointActingTypes.PASSENGER ) ) {
									return TransportMode.pt;
								}
								if ( pe instanceof Leg &&
										((Leg) pe).getMode().equals( JointActingTypes.DRIVER ) ) {
									return TransportMode.car;
								}

							}

							return router.getMainModeIdentifier().identifyMainMode( tripElements );
						}
					}) );
	}

	public static GenericStrategyModule<GroupPlans> createRecomposeJointPlansModule(
			final Config config,
			final JointPlanFactory jpFactory,
			final PlanLinkIdentifier linkIdentifier) {
		return new RecomposeJointPlanModule(
				config.global().getNumberOfThreads(),
				jpFactory,
				linkIdentifier );
	}

	public static GenericStrategyModule<GroupPlans> createVehicleAllocationModule(
			final Config config,
			final VehicleRessources vehicles) {
		return new AllocateVehicleToPlansInGroupPlanModule(
				config.global().getNumberOfThreads(),
				vehicles,
				SharedVehicleUtils.DEFAULT_VEHICULAR_MODES,
				true,
				true);
	}
}

