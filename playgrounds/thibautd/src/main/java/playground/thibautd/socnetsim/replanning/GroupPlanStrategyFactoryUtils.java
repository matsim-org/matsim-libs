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
package playground.thibautd.socnetsim.replanning;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.CompositeStageActivityTypes;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripRouter;
import org.matsim.population.algorithms.PlanAlgorithm;
import playground.thibautd.replanning.TourModeUnifierModule;
import playground.thibautd.socnetsim.framework.PlanRoutingAlgorithmFactory;
import playground.thibautd.socnetsim.framework.replanning.GenericStrategyModule;
import playground.thibautd.socnetsim.framework.replanning.IndividualBasedGroupStrategyModule;
import playground.thibautd.socnetsim.framework.replanning.JointPlanBasedGroupStrategyModule;
import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.framework.population.JointPlanFactory;
import playground.thibautd.socnetsim.framework.replanning.grouping.GroupPlans;
import playground.thibautd.socnetsim.framework.replanning.modules.PlanLinkIdentifier;
import playground.thibautd.socnetsim.framework.replanning.modules.RecomposeJointPlanModule;
import playground.thibautd.socnetsim.replanning.modules.SynchronizeCoTravelerPlansModule;
import playground.thibautd.socnetsim.sharedvehicles.SharedVehicleUtils;
import playground.thibautd.socnetsim.sharedvehicles.VehicleRessources;
import playground.thibautd.socnetsim.sharedvehicles.replanning.AllocateVehicleToPlansInGroupPlanModule;

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
					config.global().getNumberOfThreads(),
					tripRouterFactory.get().getStageActivityTypes() ) );
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
		final CompositeStageActivityTypes stageTypes = new CompositeStageActivityTypes();
		stageTypes.addActivityTypes( router.getStageActivityTypes() );
		stageTypes.addActivityTypes( JointActingTypes.JOINT_STAGE_ACTS );

		return new IndividualBasedGroupStrategyModule(
				new TourModeUnifierModule(
					config.global().getNumberOfThreads(),
					stageTypes,
					new MainModeIdentifier() {
						@Override
						public String identifyMainMode(
								final List<PlanElement> tripElements) {
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

