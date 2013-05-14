/* *********************************************************************** *
 * project: org.matsim.*
 * RunUtils.java
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
package playground.thibautd.socnetsim.run;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.router.CompositeStageActivityTypes;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.scenario.ScenarioImpl;

import playground.thibautd.analysis.listeners.LegHistogramListenerWithoutControler;
import playground.thibautd.analysis.listeners.TripModeShares;
import playground.thibautd.router.PlanRoutingAlgorithmFactory;
import playground.thibautd.socnetsim.analysis.CliquesSizeGroupIdentifier;
import playground.thibautd.socnetsim.analysis.FilteredScoreStats;
import playground.thibautd.socnetsim.analysis.JointPlanSizeStats;
import playground.thibautd.socnetsim.analysis.JointTripsStats;
import playground.thibautd.socnetsim.controller.ControllerRegistry;
import playground.thibautd.socnetsim.controller.ImmutableJointController;
import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.replanning.grouping.FixedGroupsIdentifier;
import playground.thibautd.socnetsim.replanning.GroupPlanStrategyFactory;
import playground.thibautd.socnetsim.replanning.GroupStrategyManager;
import playground.thibautd.socnetsim.replanning.GroupStrategyRegistry;
import playground.thibautd.socnetsim.replanning.modules.RecomposeJointPlanAlgorithm.PlanLinkIdentifier;
import playground.thibautd.socnetsim.replanning.RankOfRemovedPlanListener;
import playground.thibautd.socnetsim.router.JointPlanRouterFactory;
import playground.thibautd.socnetsim.sharedvehicles.PlanRouterWithVehicleRessourcesFactory;
import playground.thibautd.socnetsim.sharedvehicles.SharedVehicleUtils;

/**
 * @author thibautd
 */
public class RunUtils {
	private static final Logger log =
		Logger.getLogger(RunUtils.class);

	private RunUtils() {}

	public static PlanRoutingAlgorithmFactory createPlanRouterFactory(
			final Scenario scenario ) {
		final PlanRoutingAlgorithmFactory jointRouterFactory =
					new JointPlanRouterFactory(
							((ScenarioImpl) scenario).getActivityFacilities() );
		return new PlanRouterWithVehicleRessourcesFactory(
					jointRouterFactory );
	}

	public static void loadStrategyRegistry(
			final GroupStrategyRegistry strategyRegistry,
			final ControllerRegistry controllerRegistry) {
		final Config config = controllerRegistry.getScenario().getConfig();
		final WeightsConfigGroup weights = (WeightsConfigGroup) config.getModule( WeightsConfigGroup.GROUP_NAME );

		strategyRegistry.addStrategy(
				GroupPlanStrategyFactory.createReRoute(
					controllerRegistry ),
				weights.getReRouteWeight());
		strategyRegistry.addStrategy(
				GroupPlanStrategyFactory.createTimeAllocationMutator(
					controllerRegistry ),
				weights.getTimeMutationWeight());
		if (weights.getJtmOptimizes()) {
			strategyRegistry.addStrategy(
					GroupPlanStrategyFactory.createCliqueJointTripMutator( controllerRegistry ),
					weights.getJointTripMutationWeight());
		}
		else {
			strategyRegistry.addStrategy(
					GroupPlanStrategyFactory.createNonOptimizingCliqueJointTripMutator( controllerRegistry ),
					weights.getJointTripMutationWeight());
		}
		strategyRegistry.addStrategy(
				GroupPlanStrategyFactory.createSubtourModeChoice(
					controllerRegistry ),
				weights.getModeMutationWeight());
		strategyRegistry.addStrategy(
				GroupPlanStrategyFactory.createSelectExpBeta(
						controllerRegistry.getIncompatiblePlansIdentifierFactory(),
						config ),
				weights.getLogitSelectionWeight() );
		strategyRegistry.addStrategy(
				GroupPlanStrategyFactory.createTourVehicleAllocation(
					controllerRegistry ),
				weights.getTourLevelReallocateVehicleWeight() );
		strategyRegistry.addStrategy(
				GroupPlanStrategyFactory.createGroupPlanVehicleAllocation(
					controllerRegistry ),
				weights.getPlanLevelReallocateVehicleWeight() );
		strategyRegistry.addStrategy(
				GroupPlanStrategyFactory.createOptimizingTourVehicleAllocation(
					controllerRegistry ),
				weights.getTourLevelOptimizeVehicleWeight() );
		strategyRegistry.addStrategy(
				GroupPlanStrategyFactory.createRandomJointPlansRecomposer(
					controllerRegistry ),
				weights.getRecomposeJointPlansRandomlyWeight() );
	}

	public static void loadDefaultAnalysis(
			final int graphWriteInterval,
			final FixedGroupsIdentifier cliques,
			final GroupStrategyManager strategyManager,
			final ImmutableJointController controller) {
		controller.addControlerListener(
				new LegHistogramListenerWithoutControler(
					graphWriteInterval,
					controller.getRegistry().getEvents(),
					controller.getControlerIO() ));

		final CliquesSizeGroupIdentifier groupIdentifier =
			new CliquesSizeGroupIdentifier(
					cliques.getGroupInfo() );

		controller.addControlerListener(
				new FilteredScoreStats(
					graphWriteInterval,
					controller.getControlerIO(),
					controller.getRegistry().getScenario(),
					groupIdentifier));

		controller.addControlerListener(
				new JointPlanSizeStats(
					graphWriteInterval,
					controller.getControlerIO(),
					controller.getRegistry().getScenario(),
					groupIdentifier));

		final WeightsConfigGroup weights = (WeightsConfigGroup)
			controller.getRegistry().getScenario().getConfig().getModule(
					WeightsConfigGroup.GROUP_NAME );

		// no need to track evolution of joint trips if they do not evolve
		if ( weights.getJointTripMutationWeight() > 0 ) {
			controller.addControlerListener(
					new JointTripsStats(
						graphWriteInterval,
						controller.getControlerIO(),
						controller.getRegistry().getScenario(),
						groupIdentifier));
		}

		final CompositeStageActivityTypes actTypesForAnalysis = new CompositeStageActivityTypes();
		actTypesForAnalysis.addActivityTypes(
				controller.getRegistry().getTripRouterFactory().instantiateAndConfigureTripRouter().getStageActivityTypes() );
		actTypesForAnalysis.addActivityTypes( JointActingTypes.JOINT_STAGE_ACTS );
		controller.addControlerListener(
				new TripModeShares(
					graphWriteInterval,
					controller.getControlerIO(),
					controller.getRegistry().getScenario(),
					new MainModeIdentifier() {
						private final MainModeIdentifier d = new MainModeIdentifierImpl();

						@Override
						public String identifyMainMode(
								final List<PlanElement> tripElements) {
							for (PlanElement pe : tripElements) {
								if ( !(pe instanceof Leg) ) continue;
								final String mode = ((Leg) pe).getMode();

								if (mode.equals( JointActingTypes.DRIVER ) ||
										mode.equals( JointActingTypes.PASSENGER ) ) {
									return mode;
								}
							}
							return d.identifyMainMode( tripElements );
						}
					},
					actTypesForAnalysis));

		final RankOfRemovedPlanListener removalListener =
			new RankOfRemovedPlanListener(
					controller.getControlerIO().getOutputFilename(
						"removedPlansRank.dat" ) );
		controller.addControlerListener( removalListener );
		strategyManager.setRemovedPlanListener( removalListener );
	}

	public static void addConsistencyCheckingListeners(final ImmutableJointController controller) {
		controller.addControlerListener(
				new IterationEndsListener() {
					@Override
					public void notifyIterationEnds(final IterationEndsEvent event) {
						log.info( "Checking consistency of vehicle allocation" );
						final Set<Id> knownVehicles = new HashSet<Id>();
						final Set<JointPlan> knownJointPlans = new HashSet<JointPlan>();

						boolean hadNull = false;
						boolean hadNonNull = false;
						for ( Person person : controller.getRegistry().getScenario().getPopulation().getPersons().values() ) {
							final Plan plan = person.getSelectedPlan();
							final JointPlan jp = controller.getRegistry().getJointPlans().getJointPlan( plan );

							final Set<Id> vehsOfPlan = 
								 jp != null && knownJointPlans.add( jp ) ?
									SharedVehicleUtils.getVehiclesInJointPlan(
											jp,
											SharedVehicleUtils.DEFAULT_VEHICULAR_MODES) :
									(jp == null ?
										SharedVehicleUtils.getVehiclesInPlan(
												plan,
												SharedVehicleUtils.DEFAULT_VEHICULAR_MODES) :
										Collections.<Id>emptySet());

							for ( Id v : vehsOfPlan ) {
								if ( v == null ) {
									if ( hadNonNull ) throw new RuntimeException( "got null and non-null vehicles" );
									hadNull = true;
								}
								else {
									if ( hadNull ) throw new RuntimeException( "got null and non-null vehicles" );
									if ( !knownVehicles.add( v ) ) throw new RuntimeException( "inconsistent allocation of vehicle "+v+" (found in several distinct joint plans)" );
									hadNonNull = true;
								}
							}
						}
					}
				});

		controller.addControlerListener(
				new IterationEndsListener() {
					@Override
					public void notifyIterationEnds(final IterationEndsEvent event) {
						log.info( "Checking consistency of joint plan selection" );
						final Map<JointPlan, Set<Plan>> plansOfJointPlans = new HashMap<JointPlan, Set<Plan>>();

						for ( Person person : controller.getRegistry().getScenario().getPopulation().getPersons().values() ) {
							final Plan plan = person.getSelectedPlan();
							final JointPlan jp = controller.getRegistry().getJointPlans().getJointPlan( plan );

							if ( jp != null ) {
								Set<Plan> plans = plansOfJointPlans.get( jp );

								if ( plans == null ) {
									plans = new HashSet<Plan>();
									plansOfJointPlans.put( jp , plans );
								}

								plans.add( plan );
							}
						}

						for ( Map.Entry<JointPlan , Set<Plan>> entry : plansOfJointPlans.entrySet() ) {
							if ( entry.getKey().getIndividualPlans().size() != entry.getValue().size() ) {
								throw new RuntimeException( "joint plan "+entry.getKey()+
										" of size "+entry.getKey().getIndividualPlans().size()+
										" has only the "+entry.getValue().size()+" following plans selected: "+
										entry.getValue() );
							}
						}
					}
				});

		controller.addControlerListener(
				new IterationEndsListener() {
					@Override
					public void notifyIterationEnds(final IterationEndsEvent event) {
						log.info( "Checking minimality of joint plan composition" );
						final PlanLinkIdentifier links = controller.getRegistry().getPlanLinkIdentifier();
						final Set<JointPlan> jointPlans = new HashSet<JointPlan>();

						for ( Person person : controller.getRegistry().getScenario().getPopulation().getPersons().values() ) {
							final Plan plan = person.getSelectedPlan();
							final JointPlan jp = controller.getRegistry().getJointPlans().getJointPlan( plan );

							if ( jp != null ) {
								jointPlans.add( jp );
							}
						}

						for ( JointPlan jp : jointPlans ) {
							for ( Plan p : jp.getIndividualPlans().values() ) {
								if ( !hasLinkedPlan( links , p , jp.getIndividualPlans().values() ) ) {
									throw new RuntimeException( "plan "+p+" is in "+jp+" but is not linked with any plan" );
								}
							}
						}
					}

					private boolean hasLinkedPlan(
							final PlanLinkIdentifier links,
							final Plan p,
							final Collection<Plan> plans) {
						for ( Plan p2 : plans ) {
							if ( p == p2 ) continue;
							if ( links.areLinked( p , p2 ) ) return true;
						}
						return false;
					}
				});
	}
}

