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
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.router.CompositeStageActivityTypes;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactoryInternal;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.facilities.algorithms.WorldConnectLocations;
import org.matsim.population.Desires;
import org.matsim.pt.PtConstants;

import playground.ivt.kticompatibility.KtiLikeScoringConfigGroup;
import playground.ivt.kticompatibility.KtiPtRoutingModule;
import playground.ivt.kticompatibility.KtiPtRoutingModule.KtiPtRoutingModuleInfo;
import playground.thibautd.analysis.listeners.LegHistogramListenerWithoutControler;
import playground.thibautd.analysis.listeners.TripModeShares;
import playground.thibautd.config.NonFlatConfigReader;
import playground.thibautd.mobsim.PseudoSimConfigGroup;
import playground.thibautd.router.PlanRoutingAlgorithmFactory;
import playground.thibautd.scoring.BeingTogetherScoring.LinearOverlapScorer;
import playground.thibautd.scoring.BeingTogetherScoring.LogOverlapScorer;
import playground.thibautd.scoring.BeingTogetherScoring.PersonOverlapScorer;
import playground.thibautd.scoring.FireMoneyEventsForUtilityOfBeingTogether;
import playground.thibautd.scoring.KtiScoringFunctionFactoryWithJointModes;
import playground.thibautd.socnetsim.analysis.AbstractPlanAnalyzerPerGroup;
import playground.thibautd.socnetsim.GroupReplanningConfigGroup;
import playground.thibautd.socnetsim.GroupReplanningConfigGroup.StrategyParameterSet;
import playground.thibautd.socnetsim.GroupReplanningConfigGroup.Synchro;
import playground.thibautd.socnetsim.analysis.CliquesSizeGroupIdentifier;
import playground.thibautd.socnetsim.analysis.FilteredScoreStats;
import playground.thibautd.socnetsim.analysis.JointPlanSizeStats;
import playground.thibautd.socnetsim.analysis.JointTripsStats;
import playground.thibautd.socnetsim.controller.ControllerRegistry;
import playground.thibautd.socnetsim.controller.ControllerRegistryBuilder;
import playground.thibautd.socnetsim.controller.ImmutableJointController;
import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.population.JointPlans;
import playground.thibautd.socnetsim.population.SocialNetwork;
import playground.thibautd.socnetsim.qsim.JointPseudoSimFactory;
import playground.thibautd.socnetsim.replanning.DefaultPlanLinkIdentifier;
import playground.thibautd.socnetsim.replanning.GenericPlanAlgorithm;
import playground.thibautd.socnetsim.replanning.GenericStrategyModule;
import playground.thibautd.socnetsim.replanning.GroupPlanStrategyFactoryRegistry;
import playground.thibautd.socnetsim.replanning.GroupReplanningListenner;
import playground.thibautd.socnetsim.replanning.GroupReplanningListennerWithPSimLoop;
import playground.thibautd.socnetsim.replanning.GroupStrategyManager;
import playground.thibautd.socnetsim.replanning.GroupStrategyRegistry;
import playground.thibautd.socnetsim.replanning.grouping.FixedGroupsIdentifier;
import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;
import playground.thibautd.socnetsim.replanning.modules.AbstractMultithreadedGenericStrategyModule;
import playground.thibautd.socnetsim.replanning.modules.PlanLinkIdentifier;
import playground.thibautd.socnetsim.replanning.modules.randomlocationchoice.RandomJointLocationChoiceConfigGroup;
import playground.thibautd.socnetsim.replanning.selectors.AnnealingCoalitionExpBetaFactory;
import playground.thibautd.socnetsim.replanning.selectors.EmptyIncompatiblePlansIdentifierFactory;
import playground.thibautd.socnetsim.router.JointPlanRouterFactory;
import playground.thibautd.socnetsim.router.JointTripRouterFactory;
import playground.thibautd.socnetsim.sharedvehicles.HouseholdBasedVehicleRessources;
import playground.thibautd.socnetsim.sharedvehicles.PlanRouterWithVehicleRessourcesFactory;
import playground.thibautd.socnetsim.sharedvehicles.PrepareVehicleAllocationForSimAlgorithm;
import playground.thibautd.socnetsim.sharedvehicles.SharedVehicleUtils;
import playground.thibautd.socnetsim.sharedvehicles.VehicleBasedIncompatiblePlansIdentifierFactory;
import playground.thibautd.socnetsim.sharedvehicles.VehicleRessources;
import playground.thibautd.socnetsim.SocialNetworkConfigGroup;
import playground.thibautd.socnetsim.utils.JointMainModeIdentifier;
import playground.thibautd.socnetsim.utils.JointScenarioUtils;
import playground.thibautd.utils.DistanceFillerAlgorithm;
import playground.thibautd.utils.GenericFactory;

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

		final AbstractPlanAnalyzerPerGroup.GroupIdentifier groupIdentifier =
			cliques != null ?
				new CliquesSizeGroupIdentifier(
						cliques.getGroupInfo() ) :
				new AbstractPlanAnalyzerPerGroup.GroupIdentifier() {
					private final Iterable<Id> groups = Collections.<Id>singleton( new IdImpl( "all" ) );

					@Override
					public Iterable<Id> getGroups(final Person person) {
						return groups;
					}
				};

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

	public static Scenario createScenario(final String configFile) {
		final Config config = JointScenarioUtils.createConfig();
		// needed for reading a non-flat format (other solution would be to put this in reader)
		final GroupReplanningConfigGroup weights = new GroupReplanningConfigGroup();
		config.addModule( weights );
		config.addModule( new ScoringFunctionConfigGroup() );
		config.addModule( new KtiLikeScoringConfigGroup() );
		config.addModule( new KtiInputFilesConfigGroup() );
		config.addModule( new PseudoSimConfigGroup() );
		config.addModule( new SocialNetworkConfigGroup() );
		config.addModule( new RandomJointLocationChoiceConfigGroup() );
		new NonFlatConfigReader( config ).parse( configFile );
		final Scenario scenario = JointScenarioUtils.loadScenario( config );
	
		if ( config.scenario().isUseHouseholds() && weights.getUseLimitedVehicles() ) {
			scenario.addScenarioElement(
							VehicleRessources.ELEMENT_NAME,
							new HouseholdBasedVehicleRessources(
								((ScenarioImpl) scenario).getHouseholds() ) );
		}

		if ( scenario.getActivityFacilities() != null ) {
			new WorldConnectLocations( config ).connectFacilitiesWithLinks(
					scenario.getActivityFacilities(),
					(NetworkImpl) scenario.getNetwork() );
		}
	
		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (Activity act : TripStructureUtils.getActivities( plan , EmptyStageActivityTypes.INSTANCE )) {
					if (act.getCoord() != null) continue;
					if (act.getLinkId() == null) throw new NullPointerException();
					((ActivityImpl) act).setCoord(
						scenario.getNetwork().getLinks().get( act.getLinkId() ).getCoord() );
				}
			}
		}
	
		return scenario;
	}

	public static void loadBeingTogetherListenner(final ImmutableJointController controller) {
		final ControllerRegistry controllerRegistry = controller.getRegistry();
		final Scenario scenario = controllerRegistry.getScenario();
		final Config config = scenario.getConfig();
		final ScoringFunctionConfigGroup scoringFunctionConf = (ScoringFunctionConfigGroup)
					config.getModule( ScoringFunctionConfigGroup.GROUP_NAME );

		if ( scoringFunctionConf.getMarginalUtilityOfBeingTogether_s() > 0 ) {
			log.info( "add scorer for being together" );
			final FireMoneyEventsForUtilityOfBeingTogether socialScorer =
					new FireMoneyEventsForUtilityOfBeingTogether(
						controllerRegistry.getEvents(),
						scoringFunctionConf.getActTypeFilterForJointScoring(),
						scoringFunctionConf.getModeFilterForJointScoring(),
						getPersonOverlapScorerFactory(
							scoringFunctionConf,
							scenario.getPopulation() ),
						scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney(),
						(SocialNetwork) scenario.getScenarioElement(
							SocialNetwork.ELEMENT_NAME ));
			controllerRegistry.getEvents().addHandler( socialScorer );
			controller.addControlerListener( socialScorer );
		}
		else {
			log.info( "do NOT add scorer for being together" );
		}
	}

	public static GenericFactory<PersonOverlapScorer, Id> getPersonOverlapScorerFactory(
			final ScoringFunctionConfigGroup scoringFunctionConf,
			final Population population) {
		switch ( scoringFunctionConf.getTogetherScoringForm() ) {
			case linear:
				return new GenericFactory<PersonOverlapScorer, Id>() {
						@Override
						public PersonOverlapScorer create( final Id id ) {
							return new LinearOverlapScorer(
									scoringFunctionConf.getMarginalUtilityOfBeingTogether_s() );
						}
					};
			case logarithmic:
				return new GenericFactory<PersonOverlapScorer, Id>() {
						@Override
						public PersonOverlapScorer create( final Id id ) {
							final PersonImpl person = (PersonImpl) population.getPersons().get( id );
							if ( person == null ) {
								// eg transit agent
								return new LinearOverlapScorer( 0 );
							}
							final Desires desires = person.getDesires();
							final double typicalDuration = desires.getActivityDuration( "leisure" );
							final double zeroDuration = typicalDuration * Math.exp( -10.0 / typicalDuration );
							return new LogOverlapScorer(
									scoringFunctionConf.getMarginalUtilityOfBeingTogether_s(),
									typicalDuration,
									zeroDuration);
						}
					};
			default:
				throw new RuntimeException( ""+scoringFunctionConf.getTogetherScoringForm() );
		}
	}
}

