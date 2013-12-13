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
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.router.CompositeStageActivityTypes;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.scenario.ScenarioImpl;

import playground.thibautd.analysis.listeners.LegHistogramListenerWithoutControler;
import playground.thibautd.analysis.listeners.TripModeShares;
import playground.thibautd.router.PlanRoutingAlgorithmFactory;
import playground.thibautd.socnetsim.GroupReplanningConfigGroup;
import playground.thibautd.socnetsim.GroupReplanningConfigGroup.StrategyParameterSet;
import playground.thibautd.socnetsim.analysis.CliquesSizeGroupIdentifier;
import playground.thibautd.socnetsim.analysis.FilteredScoreStats;
import playground.thibautd.socnetsim.analysis.JointPlanSizeStats;
import playground.thibautd.socnetsim.analysis.JointTripsStats;
import playground.thibautd.socnetsim.controller.ControllerRegistry;
import playground.thibautd.socnetsim.controller.ImmutableJointController;
import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.replanning.GroupPlanStrategyFactoryRegistry;
import playground.thibautd.socnetsim.replanning.GroupStrategyRegistry;
import playground.thibautd.socnetsim.replanning.grouping.FixedGroupsIdentifier;
import playground.thibautd.socnetsim.replanning.modules.RecomposeJointPlanAlgorithm.PlanLinkIdentifier;
import playground.thibautd.socnetsim.router.JointPlanRouterFactory;
import playground.thibautd.socnetsim.sharedvehicles.PlanRouterWithVehicleRessourcesFactory;
import playground.thibautd.socnetsim.sharedvehicles.SharedVehicleUtils;
import playground.thibautd.socnetsim.utils.JointMainModeIdentifier;
import playground.thibautd.utils.DistanceFillerAlgorithm;

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

	public static void loadStrategyRegistryFromGroupStrategyConfigGroup(
			final GroupPlanStrategyFactoryRegistry factories, 
			final GroupStrategyRegistry strategyRegistry,
			final ControllerRegistry controllerRegistry) {
		final Config config = controllerRegistry.getScenario().getConfig();
		final GroupReplanningConfigGroup weights = (GroupReplanningConfigGroup) config.getModule( GroupReplanningConfigGroup.GROUP_NAME );

		strategyRegistry.setExtraPlanRemover(
				factories.createRemover(
					weights.getSelectorForRemoval(),
					controllerRegistry ) );

		for ( StrategyParameterSet set : weights.getStrategyParameterSets() ) {
			strategyRegistry.addStrategy(
					factories.createStrategy(
						set.getStrategyName(),
						controllerRegistry ),
					set.getWeight(),
					set.isInnovative() ?
						weights.getDisableInnovationAfterIter() :
						-1 );
		}
	}

	public static void loadStrategyRegistryWithInnovativeStrategiesOnly(
			final GroupPlanStrategyFactoryRegistry factories, 
			final GroupStrategyRegistry strategyRegistry,
			final ControllerRegistry controllerRegistry) {
		loadStrategyRegistry(
				true,
				factories,
				strategyRegistry,
				controllerRegistry );
	}

	public static void loadStrategyRegistryWithNonInnovativeStrategiesOnly(
			final GroupPlanStrategyFactoryRegistry factories, 
			final GroupStrategyRegistry strategyRegistry,
			final ControllerRegistry controllerRegistry) {
		loadStrategyRegistry(
				false,
				factories,
				strategyRegistry,
				controllerRegistry );
	}

	private static void loadStrategyRegistry(
			final boolean innovativeness,
			final GroupPlanStrategyFactoryRegistry factories, 
			final GroupStrategyRegistry strategyRegistry,
			final ControllerRegistry controllerRegistry) {
		final Config config = controllerRegistry.getScenario().getConfig();
		final GroupReplanningConfigGroup weights = (GroupReplanningConfigGroup) config.getModule( GroupReplanningConfigGroup.GROUP_NAME );

		strategyRegistry.setExtraPlanRemover(
				factories.createRemover(
					weights.getSelectorForRemoval(),
					controllerRegistry ) );

		for ( StrategyParameterSet set : weights.getStrategyParameterSets() ) {
			if ( set.isInnovative() == innovativeness ) {
				strategyRegistry.addStrategy(
						factories.createStrategy(
							set.getStrategyName(),
							controllerRegistry ),
						set.getWeight(),
						-1 );
			}
		}
	}

	public static void loadDefaultAnalysis(
			final int graphWriteInterval,
			final FixedGroupsIdentifier cliques,
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

		controller.addControlerListener(
				new JointTripsStats(
					graphWriteInterval,
					controller.getControlerIO(),
					controller.getRegistry().getScenario(),
					groupIdentifier));

		final CompositeStageActivityTypes actTypesForAnalysis = new CompositeStageActivityTypes();
		actTypesForAnalysis.addActivityTypes(
				controller.getRegistry().getTripRouterFactory().instantiateAndConfigureTripRouter().getStageActivityTypes() );
		actTypesForAnalysis.addActivityTypes( JointActingTypes.JOINT_STAGE_ACTS );
		controller.addControlerListener(
				new TripModeShares(
					graphWriteInterval,
					controller.getControlerIO(),
					controller.getRegistry().getScenario(),
					new JointMainModeIdentifier( new MainModeIdentifierImpl() ),
					actTypesForAnalysis));
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

	public static void addDistanceFillerListener(final ImmutableJointController controller) {
		final DistanceFillerAlgorithm algo = new DistanceFillerAlgorithm();

		algo.putEstimator(
				TransportMode.transit_walk,
				new DistanceFillerAlgorithm.CrowFlyEstimator(
					controller.getRegistry().getScenario().getConfig().plansCalcRoute().getBeelineDistanceFactor(), 
					controller.getRegistry().getScenario().getNetwork() ) );

		// this is done by the routing module, but not at import
		algo.putEstimator(
				TransportMode.walk,
				new DistanceFillerAlgorithm.CrowFlyEstimator(
					controller.getRegistry().getScenario().getConfig().plansCalcRoute().getBeelineDistanceFactor(), 
					controller.getRegistry().getScenario().getNetwork() ) );

		algo.putEstimator(
				TransportMode.pt,
				new DistanceFillerAlgorithm.CrowFlyEstimator(
					// this was the hard-coded factor for in-vehicle distance in KTI...
					// XXX not sure it makes sense to use the same approach with detailed pt
					1.5,
					controller.getRegistry().getScenario().getNetwork() ) );

		controller.addControlerListener(
				new BeforeMobsimListener() {
					@Override
					public void notifyBeforeMobsim(final BeforeMobsimEvent event) {
						for ( Person person : controller.getRegistry().getScenario().getPopulation().getPersons().values() ) {
							algo.run( person.getSelectedPlan() );
						}
					}
				});
	}
}

