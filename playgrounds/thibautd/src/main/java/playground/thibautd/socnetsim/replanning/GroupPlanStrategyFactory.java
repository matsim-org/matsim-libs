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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.router.CompositeStageActivityTypes;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.trafficmonitoring.DepartureDelayAverageCalculator;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.population.algorithms.TripsToLegsAlgorithm;

import playground.thibautd.router.PlanRoutingAlgorithmFactory;
import playground.thibautd.router.replanning.BlackListedTimeAllocationMutator;
import playground.thibautd.socnetsim.cliques.replanning.modules.jointtimemodechooser.JointTimeModeChooserAlgorithm;
import playground.thibautd.socnetsim.cliques.replanning.modules.jointtripinsertor.JointTripInsertorAndRemoverAlgorithm;
import playground.thibautd.socnetsim.controller.ControllerRegistry;
import playground.thibautd.socnetsim.population.DriverRoute;
import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.population.JointPlanFactory;
import playground.thibautd.socnetsim.population.PassengerRoute;
import playground.thibautd.socnetsim.replanning.grouping.GroupPlans;
import playground.thibautd.socnetsim.replanning.modules.AbstractMultithreadedGenericStrategyModule;
import playground.thibautd.socnetsim.replanning.modules.JointPlanMergingModule;
import playground.thibautd.socnetsim.replanning.modules.RecomposeJointPlanAlgorithm.PlanLinkIdentifier;
import playground.thibautd.socnetsim.replanning.modules.RecomposeJointPlanModule;
import playground.thibautd.socnetsim.replanning.modules.SynchronizeCoTravelerPlansModule;
import playground.thibautd.socnetsim.replanning.selectors.LogitSumSelector;
import playground.thibautd.socnetsim.replanning.selectors.RandomGroupLevelSelector;
import playground.thibautd.socnetsim.sharedvehicles.replanning.AllocateVehicleToPlansInGroupPlanModule;
import playground.thibautd.socnetsim.sharedvehicles.replanning.AllocateVehicleToSubtourModule;
import playground.thibautd.socnetsim.sharedvehicles.VehicleRessources;

/**
 * Provides factory methods to create standard strategies.
 * @author thibautd
 */
public class GroupPlanStrategyFactory {
	private static final Logger log =
		Logger.getLogger(GroupPlanStrategyFactory.class);

	private GroupPlanStrategyFactory() {}

	// /////////////////////////////////////////////////////////////////////////
	// strategies
	// /////////////////////////////////////////////////////////////////////////
	public static GroupPlanStrategy createReRoute(
			final Config config,
			final JointPlanFactory jpFactory,
			final PlanRoutingAlgorithmFactory planRouterFactory,
			final TripRouterFactory tripRouterFactory) {
		final GroupPlanStrategy strategy = createRandomSelectingStrategy();
	
		strategy.addStrategyModule( createReRouteModule( config , planRouterFactory , tripRouterFactory ) );

		strategy.addStrategyModule(
				createRecomposeJointPlansModule(
					config,
					jpFactory));

		strategy.addStrategyModule(
				createSynchronizerModule(
					config,
					tripRouterFactory) );

		return strategy;
	}

	public static GroupPlanStrategy createTimeAllocationMutator(
			final Config config,
			final PlanRoutingAlgorithmFactory planRouterFactory,
			final TripRouterFactory tripRouterFactory) {
		final GroupPlanStrategy strategy = createRandomSelectingStrategy();

		strategy.addStrategyModule(
				new IndividualBasedGroupStrategyModule(
					new AbstractMultithreadedModule( config.global().getNumberOfThreads() ) {
						@Override
						public PlanAlgorithm getPlanAlgoInstance() {
							final CompositeStageActivityTypes blackList = new CompositeStageActivityTypes();
							blackList.addActivityTypes( tripRouterFactory.createTripRouter().getStageActivityTypes() );
							blackList.addActivityTypes( JointActingTypes.JOINT_STAGE_ACTS );
							final BlackListedTimeAllocationMutator algo =
									new BlackListedTimeAllocationMutator(
										blackList,
										config.timeAllocationMutator().getMutationRange(),
										MatsimRandom.getLocalInstance() );
							final int iteration = getReplanningContext().getIteration();
							final int firstIteration = config.controler().getFirstIteration();
							final double nIters = config.controler().getLastIteration() - firstIteration;
							// TODO: make temperature more configurable
							final double maxTemp = 24;
							final double minTemp = 1;
							final double startMin = (2 / 3.) * nIters;
							final double progress = (iteration - firstIteration) / startMin;
							final double temp = minTemp + Math.max(1 - progress , 0) * (maxTemp - minTemp);
							log.debug( "temperature in iteration "+iteration+": "+temp );
							algo.setTemperature( temp );
							return algo;
						}
					}));

		strategy.addStrategyModule( createReRouteModule( config , planRouterFactory , tripRouterFactory ) );

		strategy.addStrategyModule(
				createSynchronizerModule(
					config,
					tripRouterFactory) );

		return strategy;
	}

	public static GroupPlanStrategy createCliqueJointTripMutator(
			final ControllerRegistry registry) {
		return createCliqueJointTripMutator( registry , true );
	}

	/**
	 * for tests only!!!
	 */
	public static GroupPlanStrategy createNonOptimizingCliqueJointTripMutator(
			final ControllerRegistry registry) {
		return createCliqueJointTripMutator( registry , false );
	}

	private static GroupPlanStrategy createCliqueJointTripMutator(
			final ControllerRegistry registry,
			final boolean optimize) {
		final GroupPlanStrategy strategy = createRandomSelectingStrategy();
		final Config config = registry.getScenario().getConfig();

		strategy.addStrategyModule(
				new JointPlanMergingModule(
					registry.getJointPlans().getFactory(),
					config.global().getNumberOfThreads(),
					// merge everything
					1.0 ) );

		strategy.addStrategyModule(
			new JointPlanBasedGroupStrategyModule(
					new AbstractMultithreadedGenericStrategyModule<JointPlan>( config.global() ) {
						@Override
						public GenericPlanAlgorithm<JointPlan> createAlgorithm() {
							return new JointTripInsertorAndRemoverAlgorithm(
								config,
								registry.getTripRouterFactory().createTripRouter(),
								MatsimRandom.getLocalInstance(),
								true); // "iterative"
						}
					}));

		strategy.addStrategyModule(
				createReRouteModule(
					config,
					registry.getPlanRoutingAlgorithmFactory(),
					registry.getTripRouterFactory() ) );

		// split immediately after insertion/removal,
		// to make optimisation easier.
		strategy.addStrategyModule(
				createRecomposeJointPlansModule(
					config,
					registry.getJointPlans().getFactory()));

		if (optimize) {
			final DepartureDelayAverageCalculator delay =
				new DepartureDelayAverageCalculator(
					registry.getScenario().getNetwork(),
					registry.getScenario().getConfig().travelTimeCalculator().getTraveltimeBinSize());

			strategy.addStrategyModule(
					new JointPlanBasedGroupStrategyModule(
						new AbstractMultithreadedGenericStrategyModule<JointPlan>(
								registry.getScenario().getConfig().global() ) {
							@Override
							public GenericPlanAlgorithm<JointPlan> createAlgorithm() {
								return new JointTimeModeChooserAlgorithm(
									MatsimRandom.getLocalInstance(),
									null,
									delay,
									registry.getScenario(),
									registry.getScoringFunctionFactory(),
									registry.getTravelTime().getLinkTravelTimes(),
									registry.getLeastCostPathCalculatorFactory(),
									registry.getTripRouterFactory() );
							}
						}));
		}
		else {
			strategy.addStrategyModule(
					createSynchronizerModule(
						config,
						registry.getTripRouterFactory()) );
		}

		return strategy;
	}

	public static GroupPlanStrategy createSelectExpBeta(final Config config) {
		return new GroupPlanStrategy(
				new LogitSumSelector(
					MatsimRandom.getLocalInstance(),
					config.planCalcScore().getBrainExpBeta()) );
	}

	public static GroupPlanStrategy createSubtourModeChoice(
			final ControllerRegistry registry) {
		final GroupPlanStrategy strategy = createRandomSelectingStrategy();

		// Why the hell did I put this here???
		//strategy.addStrategyModule(
		//		createReRouteModule(
		//			registry.getScenario().getConfig(),
		//			registry.getPlanRoutingAlgorithmFactory(),
		//			registry.getTripRouterFactory() ) );

		strategy.addStrategyModule(
				new IndividualBasedGroupStrategyModule(
					new SubtourModeChoice(
						registry.getScenario().getConfig() ) ) );

		// TODO: add an option to enable or disable this part?
		strategy.addStrategyModule(
				new AllocateVehicleToPlansInGroupPlanModule(
					registry.getScenario().getConfig().global().getNumberOfThreads(),
					registry.getScenario().getScenarioElement(
						VehicleRessources.class ),
					TransportMode.car,
					true));

		strategy.addStrategyModule(
				createReRouteModule(
					registry.getScenario().getConfig(),
					registry.getPlanRoutingAlgorithmFactory(),
					registry.getTripRouterFactory() ) );

		strategy.addStrategyModule(
				createRecomposeJointPlansModule(
					registry.getScenario().getConfig(),
					registry.getJointPlans().getFactory()));

		strategy.addStrategyModule(
				createSynchronizerModule(
					registry.getScenario().getConfig(),
					registry.getTripRouterFactory()) );

		return strategy;
	}

	public static GroupPlanStrategy createTourVehicleAllocation(
			final ControllerRegistry registry) {
		final GroupPlanStrategy strategy = createRandomSelectingStrategy();

		strategy.addStrategyModule(
				new IndividualBasedGroupStrategyModule(
					new AllocateVehicleToSubtourModule(
						registry.getScenario().getConfig().global().getNumberOfThreads(),
						TransportMode.car,
						registry.getScenario().getScenarioElement(
							VehicleRessources.class ) ) ) );

		strategy.addStrategyModule(
				createRecomposeJointPlansModule(
					registry.getScenario().getConfig(),
					registry.getJointPlans().getFactory()));

		strategy.addStrategyModule(
				createReRouteModule(
					registry.getScenario().getConfig(),
					registry.getPlanRoutingAlgorithmFactory(),
					registry.getTripRouterFactory() ) );

		return strategy;
	}

	public static GroupPlanStrategy createGroupPlanVehicleAllocation(
			final ControllerRegistry registry) {
		final GroupPlanStrategy strategy = createRandomSelectingStrategy();

		strategy.addStrategyModule(
				new AllocateVehicleToPlansInGroupPlanModule(
					registry.getScenario().getConfig().global().getNumberOfThreads(),
					registry.getScenario().getScenarioElement(
						VehicleRessources.class ),
					TransportMode.car,
					false));

		strategy.addStrategyModule(
				createRecomposeJointPlansModule(
					registry.getScenario().getConfig(),
					registry.getJointPlans().getFactory()));

		strategy.addStrategyModule(
				createReRouteModule(
					registry.getScenario().getConfig(),
					registry.getPlanRoutingAlgorithmFactory(),
					registry.getTripRouterFactory() ) );

		return strategy;
	}

	// /////////////////////////////////////////////////////////////////////////
	// standard modules
	// /////////////////////////////////////////////////////////////////////////
	public static GenericStrategyModule<GroupPlans> createTripsToLegsModule(
			final Config config,
			final TripRouterFactory tripRouterFactory) {
		return new IndividualBasedGroupStrategyModule(
				new AbstractMultithreadedModule( config.global() ) {
					@Override
					public PlanAlgorithm getPlanAlgoInstance() {
						return new TripsToLegsAlgorithm( tripRouterFactory.createTripRouter() );
					}
				});
	}

	public static GenericStrategyModule<GroupPlans> createSynchronizerModule(
			final Config config,
			final TripRouterFactory tripRouterFactory) {
		return new JointPlanBasedGroupStrategyModule(
				new SynchronizeCoTravelerPlansModule(
					config.global().getNumberOfThreads(),
					tripRouterFactory.createTripRouter().getStageActivityTypes() ) );
	}

	public static GenericStrategyModule<GroupPlans> createReRouteModule(
			final Config config,
			final PlanRoutingAlgorithmFactory planRouterFactory,
			final TripRouterFactory tripRouterFactory) {
		return new IndividualBasedGroupStrategyModule(
				new AbstractMultithreadedModule( config.global() ) {
					@Override
					public PlanAlgorithm getPlanAlgoInstance() {
						return planRouterFactory.createPlanRoutingAlgorithm( tripRouterFactory.createTripRouter() );
					}
				});
	}

	public static GenericStrategyModule<GroupPlans> createRecomposeJointPlansModule(
			final Config config,
			final JointPlanFactory jpFactory) {
		return new RecomposeJointPlanModule(
				config.global().getNumberOfThreads(),
				jpFactory,
				new DefaultPlanLinkIdentifier() );
	}

	private static GroupPlanStrategy createRandomSelectingStrategy() {
		return new GroupPlanStrategy(
				new RandomGroupLevelSelector(
					MatsimRandom.getLocalInstance() ) );
	}

	// /////////////////////////////////////////////////////////////////////////
	// helpers
	// /////////////////////////////////////////////////////////////////////////
	private static class DefaultPlanLinkIdentifier implements PlanLinkIdentifier {
		@Override
		public boolean areLinked(
				final Plan p1,
				final Plan p2) {
			return areLinkedByJointTrips( p1 , p2 ) || areLinkedByVehicles( p1 , p2 );
		}

		private static boolean areLinkedByVehicles(
				final Plan p1,
				final Plan p2) {
			final List<Id> vehIdsIn1 = new ArrayList<Id>();

			for ( Trip t : TripStructureUtils.getTrips( p1 , EmptyStageActivityTypes.INSTANCE ) ) {
				for ( Leg l : t.getLegsOnly() ) {
					if ( l.getRoute() instanceof NetworkRoute ) {
						final Id vehId = ((NetworkRoute) l.getRoute()).getVehicleId();
						if ( vehId == null ) continue;
						vehIdsIn1.add( vehId );
					}
				}
			}

			if ( vehIdsIn1.isEmpty() ) return false;

			for ( Trip t : TripStructureUtils.getTrips( p2 , EmptyStageActivityTypes.INSTANCE ) ) {
				for ( Leg l : t.getLegsOnly() ) {
					if ( l.getRoute() instanceof NetworkRoute &&
							vehIdsIn1.contains( ((NetworkRoute) l.getRoute()).getVehicleId() ) ) {
						return true;
					}
				}
			}

			return false;
		}

		private static boolean areLinkedByJointTrips(
				final Plan p1,
				final Plan p2) {
			final boolean areLinked = containsCoTraveler( p1 , p2.getPerson().getId() );
			assert areLinked == containsCoTraveler( p2 , p1.getPerson().getId() ) : "inconsistent plans";
			return areLinked;
		}

		private static boolean containsCoTraveler(
				final Plan plan,
				final Id cotraveler) {
			for ( Trip t : TripStructureUtils.getTrips( plan , EmptyStageActivityTypes.INSTANCE ) ) {
				for ( Leg l : t.getLegsOnly() ) {
					if ( l.getRoute() instanceof DriverRoute ) {
						if ( ((DriverRoute) l.getRoute()).getPassengersIds().contains( cotraveler ) ) {
							return true;
						}
					}
					else if ( l.getRoute() instanceof PassengerRoute ) {
						if ( ((PassengerRoute) l.getRoute()).getDriverId().equals( cotraveler ) ) {
							return true;
						}
					}
				}
			}
			return false;
		}
	}
}

