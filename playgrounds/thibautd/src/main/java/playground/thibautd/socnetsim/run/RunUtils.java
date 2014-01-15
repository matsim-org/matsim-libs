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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.CompositeStageActivityTypes;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactoryInternal;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.pt.PtConstants;

import playground.ivt.kticompatibility.KtiLikeScoringConfigGroup;
import playground.ivt.kticompatibility.KtiPtRoutingModule;
import playground.ivt.kticompatibility.KtiPtRoutingModule.KtiPtRoutingModuleInfo;

import playground.thibautd.analysis.listeners.LegHistogramListenerWithoutControler;
import playground.thibautd.analysis.listeners.TripModeShares;
import playground.thibautd.mobsim.PseudoSimConfigGroup;
import playground.thibautd.router.PlanRoutingAlgorithmFactory;
import playground.thibautd.scoring.KtiScoringFunctionFactoryWithJointModes;
import playground.thibautd.socnetsim.controller.ControllerRegistryBuilder;
import playground.thibautd.socnetsim.GroupReplanningConfigGroup;
import playground.thibautd.socnetsim.GroupReplanningConfigGroup.Synchro;
import playground.thibautd.socnetsim.GroupReplanningConfigGroup.StrategyParameterSet;
import playground.thibautd.socnetsim.analysis.CliquesSizeGroupIdentifier;
import playground.thibautd.socnetsim.analysis.FilteredScoreStats;
import playground.thibautd.socnetsim.analysis.JointPlanSizeStats;
import playground.thibautd.socnetsim.analysis.JointTripsStats;
import playground.thibautd.socnetsim.controller.ControllerRegistry;
import playground.thibautd.socnetsim.controller.ImmutableJointController;
import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.population.JointPlans;
import playground.thibautd.socnetsim.qsim.JointPseudoSimFactory;
import playground.thibautd.socnetsim.replanning.DefaultPlanLinkIdentifier;
import playground.thibautd.socnetsim.replanning.GenericPlanAlgorithm;
import playground.thibautd.socnetsim.replanning.GenericStrategyModule;
import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;
import playground.thibautd.socnetsim.replanning.GroupPlanStrategyFactoryRegistry;
import playground.thibautd.socnetsim.replanning.GroupReplanningListenner;
import playground.thibautd.socnetsim.replanning.GroupReplanningListennerWithPSimLoop;
import playground.thibautd.socnetsim.replanning.GroupStrategyManager;
import playground.thibautd.socnetsim.replanning.GroupStrategyRegistry;
import playground.thibautd.socnetsim.replanning.grouping.FixedGroupsIdentifier;
import playground.thibautd.socnetsim.replanning.modules.AbstractMultithreadedGenericStrategyModule;
import playground.thibautd.socnetsim.replanning.modules.RecomposeJointPlanAlgorithm.PlanLinkIdentifier;
import playground.thibautd.socnetsim.replanning.selectors.AnnealingCoalitionExpBetaFactory;
import playground.thibautd.socnetsim.replanning.selectors.EmptyIncompatiblePlansIdentifierFactory;
import playground.thibautd.socnetsim.router.JointPlanRouterFactory;
import playground.thibautd.socnetsim.router.JointTripRouterFactory;
import playground.thibautd.socnetsim.sharedvehicles.PlanRouterWithVehicleRessourcesFactory;
import playground.thibautd.socnetsim.sharedvehicles.PrepareVehicleAllocationForSimAlgorithm;
import playground.thibautd.socnetsim.sharedvehicles.SharedVehicleUtils;
import playground.thibautd.socnetsim.sharedvehicles.VehicleBasedIncompatiblePlansIdentifierFactory;
import playground.thibautd.socnetsim.sharedvehicles.VehicleRessources;
import playground.thibautd.socnetsim.utils.JointMainModeIdentifier;
import playground.thibautd.utils.DistanceFillerAlgorithm;

/**
 * Groups methods too specific to go in the "frameworky" part of the code,
 * but which still needs to be called from various application-specific scripts.
 *
 * Ideally, scripts should consist mainly of calls to those methods, with a few
 * lines specific to the application.
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

	public static PlanLinkIdentifier createLinkIdentifier(final Synchro synchro) {
		switch ( synchro ) {
			case all:
				return new PlanLinkIdentifier() {
					@Override
					public boolean areLinked(
							final Plan p1,
							final Plan p2) {
						return true;
					}
				};
			case dynamic:
				return new DefaultPlanLinkIdentifier();
			case none:
				return new PlanLinkIdentifier() {
					@Override
					public boolean areLinked(
							final Plan p1,
							final Plan p2) {
						return false; 
					}
				};
			default:
				throw new IllegalArgumentException( synchro.toString() );
		}
	}

	public static ControllerRegistryBuilder loadDefaultRegistryBuilder(final Scenario scenario) {
		final ControllerRegistryBuilder builder = new ControllerRegistryBuilder( scenario );

		final GroupReplanningConfigGroup weights = (GroupReplanningConfigGroup)
					scenario.getConfig().getModule( GroupReplanningConfigGroup.GROUP_NAME );

		if ( scenario.getScenarioElement( VehicleRessources.ELEMENT_NAME ) != null ) {
			final PlanLinkIdentifier planLinkIdentifier =
				RunUtils.createLinkIdentifier( weights.getSynchronize() );

			builder.withPlanLinkIdentifier(
					planLinkIdentifier );

			final GenericStrategyModule<ReplanningGroup> additionalPrepareModule =
				new AbstractMultithreadedGenericStrategyModule<ReplanningGroup>(
						scenario.getConfig().global() ) {
					@Override
					public GenericPlanAlgorithm<ReplanningGroup> createAlgorithm() {
						return 
							new PrepareVehicleAllocationForSimAlgorithm(
									MatsimRandom.getLocalInstance(),
									(JointPlans) scenario.getScenarioElement( JointPlans.ELEMENT_NAME ),
									(VehicleRessources) scenario.getScenarioElement( VehicleRessources.ELEMENT_NAME ),
									planLinkIdentifier );
					}

					@Override
					protected String getName() {
						return "PrepareVehiclesForSim";
					}

				};

			builder.withAdditionalPrepareForSimModule(
					additionalPrepareModule );
		}

		builder.withPlanRoutingAlgorithmFactory(
				RunUtils.createPlanRouterFactory( scenario ) );

		builder.withIncompatiblePlansIdentifierFactory(
				weights.getConsiderVehicleIncompatibilities() &&
				!weights.getSynchronize().equals( Synchro.none ) &&
				scenario.getScenarioElement( VehicleRessources.ELEMENT_NAME ) != null ?
					new VehicleBasedIncompatiblePlansIdentifierFactory(
							SharedVehicleUtils.DEFAULT_VEHICULAR_MODES ) :
					new EmptyIncompatiblePlansIdentifierFactory() );


		final ScoringFunctionConfigGroup scoringFunctionConf = (ScoringFunctionConfigGroup)
					scenario.getConfig().getModule( ScoringFunctionConfigGroup.GROUP_NAME );
		if ( scoringFunctionConf.isUseKtiScoring() ) {
			builder.withScoringFunctionFactory(
				new KtiScoringFunctionFactoryWithJointModes(
					scoringFunctionConf.getAdditionalUtilityOfBeingDriver_s(),
					new StageActivityTypesImpl(
								Arrays.asList(
										PtConstants.TRANSIT_ACTIVITY_TYPE,
										JointActingTypes.INTERACTION) ),
						(KtiLikeScoringConfigGroup) scenario.getConfig().getModule( KtiLikeScoringConfigGroup.GROUP_NAME ),
						scenario.getConfig().planCalcScore(),
						scenario) );
		}

		if ( scoringFunctionConf.isUseKtiScoring() && !scenario.getConfig().scenario().isUseTransit() ) {
			final KtiInputFilesConfigGroup ktiInputFilesConf = (KtiInputFilesConfigGroup)
						scenario.getConfig().getModule( KtiInputFilesConfigGroup.GROUP_NAME );

			builder.withTripRouterFactory(
					new TripRouterFactoryInternal() {
						private final TripRouterFactoryInternal delegate =
							new JointTripRouterFactory(
								scenario,
								builder.getTravelDisutilityFactory(),
								builder.getTravelTime().getLinkTravelTimes(),
								builder.getLeastCostPathCalculatorFactory(),
								null);
						private final KtiPtRoutingModuleInfo info =
							new KtiPtRoutingModuleInfo(
									ktiInputFilesConf.getIntrazonalPtSpeed(),
									ktiInputFilesConf.getWorldFile(),
									ktiInputFilesConf.getTravelTimesFile(),
									ktiInputFilesConf.getPtStopsFile(),
									scenario.getNetwork());

						@Override
						public TripRouter instantiateAndConfigureTripRouter() {
							final TripRouter tripRouter = delegate.instantiateAndConfigureTripRouter();

							tripRouter.setRoutingModule(
								TransportMode.pt,
								new KtiPtRoutingModule(
									scenario.getConfig().plansCalcRoute(),
									info,
									scenario.getNetwork()) );

							final MainModeIdentifier identifier = tripRouter.getMainModeIdentifier();
							tripRouter.setMainModeIdentifier(
								new MainModeIdentifier() {
									@Override
									public String identifyMainMode(
											final List<PlanElement> tripElements) {
										for ( PlanElement pe : tripElements ) {
											if ( pe instanceof Activity && ((Activity) pe).getType().equals( PtConstants.TRANSIT_ACTIVITY_TYPE ) ) {
												return TransportMode.pt;
											}
										}
										return identifier.identifyMainMode( tripElements );
									}
								});

							return tripRouter;
						}
					});
		}

		return builder;
	}

	public static ImmutableJointController initializeController(
			final ControllerRegistry controllerRegistry) {
		final Config config = controllerRegistry.getScenario().getConfig();

		final PseudoSimConfigGroup pSimConf = (PseudoSimConfigGroup)
					config.getModule( PseudoSimConfigGroup.GROUP_NAME );
		final GroupReplanningConfigGroup groupReplanningConf = (GroupReplanningConfigGroup)
					config.getModule( GroupReplanningConfigGroup.GROUP_NAME );

		if ( !pSimConf.isIsUsePSimAtAll() ) {
			final GroupStrategyRegistry strategyRegistry = new GroupStrategyRegistry();
			final AnnealingCoalitionExpBetaFactory annealingSelectorFactory =
				new AnnealingCoalitionExpBetaFactory(
					Double.MIN_VALUE, // TODO pass by config
					//0.01,
					config.planCalcScore().getBrainExpBeta(),
					config.controler().getFirstIteration(),
					groupReplanningConf.getDisableInnovationAfterIter() );

			{
				final GroupPlanStrategyFactoryRegistry factories = new GroupPlanStrategyFactoryRegistry();
				factories.addSelectorFactory( "AnnealingCoalitionExpBeta" , annealingSelectorFactory );
				RunUtils.loadStrategyRegistryFromGroupStrategyConfigGroup(
						factories,
						strategyRegistry,
						controllerRegistry );
			}

			// create strategy manager
			final GroupStrategyManager strategyManager =
				new GroupStrategyManager( 
						strategyRegistry );

			// create controler
			final ImmutableJointController controller =
				new ImmutableJointController(
						controllerRegistry,
						new GroupReplanningListenner(
							controllerRegistry,
							strategyManager));
			controller.addControlerListener( annealingSelectorFactory );

			strategyManager.setStopWatch( controller.stopwatch );

			return controller;
		}

		final GroupPlanStrategyFactoryRegistry factories = new GroupPlanStrategyFactoryRegistry();

		final GroupStrategyRegistry mainStrategyRegistry = new GroupStrategyRegistry();
		RunUtils.loadStrategyRegistryWithNonInnovativeStrategiesOnly(
				factories,
				mainStrategyRegistry,
				controllerRegistry );

		final GroupStrategyManager mainStrategyManager =
			new GroupStrategyManager( 
					mainStrategyRegistry );

		final GroupStrategyRegistry innovativeStrategyRegistry = new GroupStrategyRegistry();
		RunUtils.loadStrategyRegistryWithInnovativeStrategiesOnly(
				factories,
				innovativeStrategyRegistry,
				controllerRegistry );

		final GroupStrategyManager innovativeStrategyManager =
			new GroupStrategyManager( 
					innovativeStrategyRegistry );

		// create controler
		final GroupReplanningListennerWithPSimLoop listenner =
					new GroupReplanningListennerWithPSimLoop(
						controllerRegistry,
						mainStrategyManager,
						innovativeStrategyManager,
						new JointPseudoSimFactory( 
							controllerRegistry.getTravelTime()) );
		final ImmutableJointController controller =
			new ImmutableJointController(
					controllerRegistry,
					listenner );

		listenner.setStopWatch( controller.stopwatch );
		listenner.setOutputDirectoryHierarchy( controller.getControlerIO() );

		return controller;
	}
}

