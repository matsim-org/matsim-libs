/* *********************************************************************** *
 * 
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
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReaderMatsimV2;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.router.CompositeStageActivityTypes;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.facilities.algorithms.WorldConnectLocations;
import org.matsim.population.Desires;
import org.matsim.pt.PtConstants;

import playground.ivt.kticompatibility.KtiLikeScoringConfigGroup;
import playground.ivt.utils.TripModeShares;
import playground.thibautd.analysis.listeners.LegHistogramListenerWithoutControler;
import playground.thibautd.pseudoqsim.PseudoSimConfigGroup;
import playground.thibautd.pseudoqsim.PseudoSimConfigGroup.PSimType;
import playground.thibautd.router.PlanRoutingAlgorithmFactory;
import playground.thibautd.socnetsim.GroupReplanningConfigGroup;
import playground.thibautd.socnetsim.GroupReplanningConfigGroup.StrategyParameterSet;
import playground.thibautd.socnetsim.analysis.AbstractPlanAnalyzerPerGroup;
import playground.thibautd.socnetsim.analysis.CliquesSizeGroupIdentifier;
import playground.thibautd.socnetsim.analysis.FilteredScoreStats;
import playground.thibautd.socnetsim.analysis.JointPlanSizeStats;
import playground.thibautd.socnetsim.analysis.JointTripsStats;
import playground.thibautd.socnetsim.cliques.Clique;
import playground.thibautd.socnetsim.controller.ControllerRegistry;
import playground.thibautd.socnetsim.controller.ControllerRegistryBuilder;
import playground.thibautd.socnetsim.controller.ImmutableJointController;
import playground.thibautd.socnetsim.controller.listeners.GroupReplanningListenner;
import playground.thibautd.socnetsim.events.CourtesyEventsGenerator;
import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.population.JointPlans;
import playground.thibautd.socnetsim.population.SocialNetwork;
import playground.thibautd.socnetsim.population.SocialNetworkReader;
import playground.thibautd.socnetsim.qsim.SwitchingJointQSimFactory;
import playground.thibautd.socnetsim.replanning.GenericPlanAlgorithm;
import playground.thibautd.socnetsim.replanning.GenericStrategyModule;
import playground.thibautd.socnetsim.replanning.GroupPlanStrategyFactoryRegistry;
import playground.thibautd.socnetsim.replanning.GroupStrategyManager;
import playground.thibautd.socnetsim.replanning.GroupStrategyRegistry;
import playground.thibautd.socnetsim.replanning.InnovationSwitchingGroupReplanningListenner;
import playground.thibautd.socnetsim.replanning.grouping.FixedGroupsIdentifier;
import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;
import playground.thibautd.socnetsim.replanning.modules.AbstractMultithreadedGenericStrategyModule;
import playground.thibautd.socnetsim.replanning.modules.PlanLinkIdentifier;
import playground.thibautd.socnetsim.replanning.selectors.AnnealingCoalitionExpBetaFactory;
import playground.thibautd.socnetsim.replanning.selectors.EmptyIncompatiblePlansIdentifierFactory;
import playground.thibautd.socnetsim.router.JointPlanRouterFactory;
import playground.thibautd.socnetsim.scoring.BeingTogetherScoring.LinearOverlapScorer;
import playground.thibautd.socnetsim.scoring.BeingTogetherScoring.LogOverlapScorer;
import playground.thibautd.socnetsim.scoring.BeingTogetherScoring.PersonOverlapScorer;
import playground.thibautd.socnetsim.scoring.FireMoneyEventsForUtilityOfBeingTogether;
import playground.thibautd.socnetsim.scoring.GroupSizePreferencesConfigGroup;
import playground.thibautd.socnetsim.scoring.KtiScoringFunctionFactoryWithJointModes;
import playground.thibautd.socnetsim.scoring.UniformlyInternalizingPlansScoring;
import playground.thibautd.socnetsim.sharedvehicles.HouseholdBasedVehicleRessources;
import playground.thibautd.socnetsim.sharedvehicles.PlanRouterWithVehicleRessourcesFactory;
import playground.thibautd.socnetsim.sharedvehicles.PrepareVehicleAllocationForSimAlgorithm;
import playground.thibautd.socnetsim.sharedvehicles.SharedVehicleUtils;
import playground.thibautd.socnetsim.sharedvehicles.VehicleBasedIncompatiblePlansIdentifierFactory;
import playground.thibautd.socnetsim.sharedvehicles.VehicleRessources;
import playground.thibautd.socnetsim.utils.JointMainModeIdentifier;
import playground.thibautd.socnetsim.utils.JointScenarioUtils;
import playground.thibautd.utils.DistanceFillerAlgorithm;
import playground.thibautd.utils.GenericFactory;
import playground.thibautd.utils.TravelTimeRetrofittingEventHandler;

import com.google.inject.Inject;

/**
 * Groups methods too specific to go in the "frameworky" part of the code,
 * but which still needs to be called from various application-specific scripts.
 *
 * Ideally, scripts should consist mainly of calls to those methods, with a few
 * lines specific to the application.
 * @author thibautd
 */
public class RunUtils {
	public static final class JointPlanCompositionMinimalityChecker implements IterationEndsListener, IterationStartsListener {
		// Fail only on start of next iteration, to let all consistency checkers print their error, if any.
		private boolean gotError = false;
		private final PlanLinkIdentifier linkIdentifier;
		private final Population population;
		private final JointPlans jointPlansStruct;

		@Inject
		public JointPlanCompositionMinimalityChecker(
				final PlanLinkIdentifier linkIdentifier,
				final Scenario sc ) {
			this.linkIdentifier = linkIdentifier;
			this.population = sc.getPopulation();
			this.jointPlansStruct = (JointPlans) sc.getScenarioElement( JointPlans.ELEMENT_NAME );
		}

		private JointPlanCompositionMinimalityChecker( ImmutableJointController controller ) {
			this( controller.getRegistry().getPlanLinkIdentifier(),
					controller.getRegistry().getScenario() );
		}

		@Override
		public void notifyIterationEnds(final IterationEndsEvent event) {
			log.info( "Checking minimality of joint plan composition" );
			final PlanLinkIdentifier links = linkIdentifier;
			final Set<JointPlan> jointPlans = new HashSet<JointPlan>();

			for ( Person person : population.getPersons().values() ) {
				final Plan plan = person.getSelectedPlan();
				final JointPlan jp = jointPlansStruct.getJointPlan( plan );

				if ( jp != null ) {
					jointPlans.add( jp );
				}
			}

			for ( JointPlan jp : jointPlans ) {
				for ( Plan p : jp.getIndividualPlans().values() ) {
					if ( !hasLinkedPlan( links , p , jp.getIndividualPlans().values() ) ) {
						log.error( "plan "+p+" is in "+jp+" but is not linked with any plan" );
						gotError = true ;
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
				final boolean l = links.areLinked( p , p2 );
				if ( l != links.areLinked( p2 , p ) ) {
					log.error( "inconsistent plan link identifier revealed by "+p+" and "+p2 );
					log.error( p.getPerson().getId()+" is "+(l ? "" : "NOT")+" linked with "+p2.getPerson().getId() );
					log.error( p2.getPerson().getId()+" is "+(l ? "NOT" : "")+" linked with "+p.getPerson().getId() );
					gotError = true;
				}
				if ( l ) return true;
			}
			return false;
		}

		@Override
		public void notifyIterationStarts( IterationStartsEvent event ) {
			if ( gotError ) throw new RuntimeException( "inconsistency detected. Look at error messages for details" );
		}
	}

	public static final class JointPlanSelectionConsistencyChecker implements IterationEndsListener, IterationStartsListener {
		private boolean gotError;
		private final Population population;
		private final JointPlans jointPlans;

		@Inject
		public JointPlanSelectionConsistencyChecker( final Scenario sc ) {
			this.population = sc.getPopulation();
			this.jointPlans = (JointPlans) sc.getScenarioElement( JointPlans.ELEMENT_NAME );
		}

		private JointPlanSelectionConsistencyChecker( ImmutableJointController controller ) {
			this( controller.getRegistry().getScenario() );
		}

		@Override
		public void notifyIterationEnds(final IterationEndsEvent event) {
			log.info( "Checking consistency of joint plan selection" );
			final Map<JointPlan, Set<Plan>> plansOfJointPlans = new HashMap<JointPlan, Set<Plan>>();

			for ( Person person : population.getPersons().values() ) {
				final Plan plan = person.getSelectedPlan();
				final JointPlan jp = jointPlans.getJointPlan( plan );

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
					log.error( "joint plan "+entry.getKey()+
							" of size "+entry.getKey().getIndividualPlans().size()+
							" has only the "+entry.getValue().size()+" following plans selected: "+
							entry.getValue() );
					gotError = true;
				}
			}
		}

		@Override
		public void notifyIterationStarts( IterationStartsEvent event ) {
			if ( gotError ) throw new RuntimeException( "inconsistency detected. Look at error messages for details" );
		}
	}

	public static final class VehicleAllocationConsistencyChecker implements IterationEndsListener, IterationStartsListener {
		private boolean gotError = false;

		private final Population population;
		private final JointPlans jointPlans;

		@Inject
		public VehicleAllocationConsistencyChecker( final Scenario sc ) {
			this.population = sc.getPopulation();
			this.jointPlans = (JointPlans) sc.getScenarioElement( JointPlans.ELEMENT_NAME );
		}

		private VehicleAllocationConsistencyChecker( ImmutableJointController controller ) {
			this( controller.getRegistry().getScenario() );
		}

		@Override
		public void notifyIterationEnds(final IterationEndsEvent event) {
			log.info( "Checking consistency of vehicle allocation" );
			final Set<Id> knownVehicles = new HashSet<Id>();
			final Set<JointPlan> knownJointPlans = new HashSet<JointPlan>();

			boolean hadNull = false;
			boolean hadNonNull = false;
			for ( Person person : population.getPersons().values() ) {
				final Plan plan = person.getSelectedPlan();
				final JointPlan jp = jointPlans.getJointPlan( plan );

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
						if ( hadNonNull ) {
							log.error( "got null and non-null vehicles" );
							gotError = true;
						}

						hadNull = true;
					}
					else {
						if ( hadNull ) {
							log.error( "got null and non-null vehicles" );
							gotError = true;
						}
						if ( !knownVehicles.add( v ) ) {
							log.error( "inconsistent allocation of vehicle "+v+" (found in several distinct joint plans)" );
							gotError = true;
						}
						hadNonNull = true;
					}
				}
			}
		}

		@Override
		public void notifyIterationStarts( IterationStartsEvent event ) {
			if ( gotError ) throw new RuntimeException( "inconsistency detected. Look at error messages for details" );
		}
	}

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
					set.getSubpopulation(),
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
						set.getSubpopulation(),
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
					private final Iterable<Id<Clique>> groups = Collections.<Id<Clique>>singleton( Id.create( "all" , Clique.class ) );

					@Override
					public Iterable<Id<Clique>> getGroups(final Person person) {
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
				controller.getRegistry().getTripRouterFactory().get().getStageActivityTypes() );
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
				new VehicleAllocationConsistencyChecker( controller ));

		controller.addControlerListener(
				new JointPlanSelectionConsistencyChecker( controller ));

		controller.addControlerListener(
				new JointPlanCompositionMinimalityChecker( controller ));
	}

	public static void addDistanceFillerListener(final ImmutableJointController controller) {
		final DistanceFillerAlgorithm algo = new DistanceFillerAlgorithm();

		algo.putEstimator(
				TransportMode.transit_walk,
				new DistanceFillerAlgorithm.CrowFlyEstimator(
					controller.getRegistry().getScenario().getConfig().plansCalcRoute().getModeRoutingParams().get( TransportMode.transit_walk ).getBeelineDistanceFactor(), 
					controller.getRegistry().getScenario().getNetwork() ) );

		// this is done by the routing module, but not at import
		algo.putEstimator(
				TransportMode.walk,
				new DistanceFillerAlgorithm.CrowFlyEstimator(
					controller.getRegistry().getScenario().getConfig().plansCalcRoute().getModeRoutingParams().get( TransportMode.walk ).getBeelineDistanceFactor(), 
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

	public static ControllerRegistryBuilder loadDefaultRegistryBuilder(final Scenario scenario) {
		final ControllerRegistryBuilder builder = new ControllerRegistryBuilder( scenario );
		loadDefaultRegistryBuilder( builder, scenario );
		return builder;
	}

	public static ControllerRegistryBuilder loadDefaultRegistryBuilder(
			final ControllerRegistryBuilder builder,
			final Scenario scenario) {
		final GroupReplanningConfigGroup weights = (GroupReplanningConfigGroup)
					scenario.getConfig().getModule( GroupReplanningConfigGroup.GROUP_NAME );

		if ( scenario.getScenarioElement( VehicleRessources.ELEMENT_NAME ) != null ) {
			if ( !scenario.getConfig().qsim().getVehicleBehavior().equals( "wait" ) ) {
				throw new RuntimeException( "agents should wait for vehicles when vehicle ressources are used! Setting is "+
						scenario.getConfig().qsim().getVehicleBehavior() );
			}

			log.warn( "Adding the vehicle preparation algorithm with the *default* plan link identifier" );
			log.warn( "this should be modified, or it will cause inconsistencies" );

			final GenericStrategyModule<ReplanningGroup> additionalPrepareModule =
				new AbstractMultithreadedGenericStrategyModule<ReplanningGroup>(
						scenario.getConfig().global() ) {
					@Override
					public GenericPlanAlgorithm<ReplanningGroup> createAlgorithm(ReplanningContext replanningContext) {
						return 
							new PrepareVehicleAllocationForSimAlgorithm(
									MatsimRandom.getLocalInstance(),
									(JointPlans) scenario.getScenarioElement( JointPlans.ELEMENT_NAME ),
									(VehicleRessources) scenario.getScenarioElement( VehicleRessources.ELEMENT_NAME ),
									// do not bother with plan links: it can cause problems,
									// as it can let no individual plans...
									null );
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
				scenario.getScenarioElement( VehicleRessources.ELEMENT_NAME ) != null ?
					new VehicleBasedIncompatiblePlansIdentifierFactory(
							SharedVehicleUtils.DEFAULT_VEHICULAR_MODES ) :
					new EmptyIncompatiblePlansIdentifierFactory() );


		final ScoringFunctionConfigGroup scoringFunctionConf = (ScoringFunctionConfigGroup)
					scenario.getConfig().getModule( ScoringFunctionConfigGroup.GROUP_NAME );
		if ( scoringFunctionConf.isUseKtiScoring() ) {
			builder.withScoringFunctionFactory(
				new KtiScoringFunctionFactoryWithJointModes(
					new StageActivityTypesImpl(
								Arrays.asList(
										PtConstants.TRANSIT_ACTIVITY_TYPE,
										JointActingTypes.INTERACTION) ),
					(KtiLikeScoringConfigGroup) scenario.getConfig().getModule( KtiLikeScoringConfigGroup.GROUP_NAME ),
					scenario.getConfig().planCalcScore(),
					scoringFunctionConf,
					scenario) );
		}

		if ( scoringFunctionConf.getInternalizationNetworkFile() != null ) {
			final String elementName = "another social network, to use for internalization";
			new SocialNetworkReader( elementName , scenario ).parse( scoringFunctionConf.getInternalizationNetworkFile() );
			builder.withScoringListener(
					new UniformlyInternalizingPlansScoring(
						elementName,
						scenario,
						builder.getEvents(),
						builder.getScoringFunctionFactory() ) );
		}

		builder.withMobsimFactory(
				new SwitchingJointQSimFactory(
						builder.getTravelTime() ) );

		if ( scenario.getScenarioElement( SocialNetwork.ELEMENT_NAME ) != null ) {
			final SocialNetwork sn = (SocialNetwork)
				scenario.getScenarioElement(
						SocialNetwork.ELEMENT_NAME );
			builder.getEvents().addHandler(
					new CourtesyEventsGenerator(
						builder.getEvents(),
						sn ) );
		}
		else {
			log.warn( "NO COURTESY EVENTS WILL BE GENERATED!" );
		}

		return builder;
	}

	public static ImmutableJointController initializeController(
			final ControllerRegistry controllerRegistry) {
		final Config config = controllerRegistry.getScenario().getConfig();

		final PseudoSimConfigGroup pSimConf = (PseudoSimConfigGroup)
					config.getModule( PseudoSimConfigGroup.GROUP_NAME );

		return pSimConf.getNPSimIters() <= 0 ?
			initializeNonPSimController( controllerRegistry ) :
			initializePSimController( controllerRegistry );
	}

	public static ImmutableJointController initializePSimController(
			final ControllerRegistry controllerRegistry) {
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

		final InnovationSwitchingGroupReplanningListenner listenner =
					new InnovationSwitchingGroupReplanningListenner(
						controllerRegistry,
						mainStrategyManager,
						innovativeStrategyManager);
		final ImmutableJointController controller =
			new ImmutableJointController(
					controllerRegistry,
					listenner );

		if ( controllerRegistry.getMobsimFactory() instanceof ControlerListener ) {
			// XXX not that nice, but we do not have the Controller yet when we build
			// the SwitchingJointQSimFactory...
			controller.addControlerListener( (ControlerListener) controllerRegistry.getMobsimFactory() );
		}

		final PseudoSimConfigGroup pSimConf = (PseudoSimConfigGroup)
					controllerRegistry.getScenario().getConfig().getModule(
							PseudoSimConfigGroup.GROUP_NAME );

		if ( pSimConf.getPsimType().equals( PSimType.teleported ) ) {
			// This is pretty hackish, so do it only if necessary...
			// Something like this is necessary, otherwise out-of-date
			// travel times keep being used in pSim (out-of-date including
			// the freeflow estimations from the first iterations!).
			// This goes in the direction of the "traditional" pSim,
			// without the cost of iterating through all links of all routes
			// (which is the costly part of QSim, much more than what is done
			// for each link: it is mainly a complexity problem, that pSim does
			// solve only by allowing more parallelism).
			controllerRegistry.getEvents().addHandler(
					new TravelTimeRetrofittingEventHandler(
						controllerRegistry.getScenario().getPopulation() ) );
		}

		return controller;
	}

	public static ImmutableJointController initializeNonPSimController(
			final ControllerRegistry controllerRegistry) {
		final Config config = controllerRegistry.getScenario().getConfig();

		final GroupReplanningConfigGroup groupReplanningConf = (GroupReplanningConfigGroup)
					config.getModule( GroupReplanningConfigGroup.GROUP_NAME );

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

		if ( controllerRegistry.getMobsimFactory() instanceof ControlerListener ) {
			// XXX not that nice, but we do not have the Controller yet when we build
			// the SwitchingJointQSimFactory...
			controller.addControlerListener( (ControlerListener) controllerRegistry.getMobsimFactory() );
		}

		strategyManager.setStopWatch( controller.stopwatch );

		if ( !(config.getModule( StrategyAnalysisConfigGroup.GROUP_NAME ) instanceof StrategyAnalysisConfigGroup) ) {
			config.addModule( new StrategyAnalysisConfigGroup() );
		}

		final StrategyAnalysisConfigGroup analysis = (StrategyAnalysisConfigGroup)
			config.getModule( StrategyAnalysisConfigGroup.GROUP_NAME );

		if ( analysis.isDumpGroupSizes() ) {
			final ReplanningStatsDumper replanningStats = new ReplanningStatsDumper( controller.getControlerIO().getOutputFilename( "replanningGroups.dat" ) );
			strategyManager.addListener( replanningStats );
			controller.addControlerListener( replanningStats );
		}

		if ( analysis.isDumpAllocation() ) {
			final ReplanningAllocationDumper replanningStats = new ReplanningAllocationDumper( controller.getControlerIO().getOutputFilename( "replanningAllocations.dat" ) );
			strategyManager.addListener( replanningStats );
			controller.addControlerListener( replanningStats );
		}

		return controller;
	}

	public static Config loadConfig(final String configFile) {
		final Config config = JointScenarioUtils.createConfig();
		addConfigGroups( config );
		new ConfigReaderMatsimV2( config ).parse( configFile );
		return config;
	}

	public static void addConfigGroups(final Config config) {
		config.addModule( new ScoringFunctionConfigGroup() );
		config.addModule( new KtiInputFilesConfigGroup() );
		config.addModule( new GroupSizePreferencesConfigGroup() );
	}

	public static Scenario createScenario(final String configFile) {
		final Config config = loadConfig( configFile );
		return loadScenario( config );
	}

	public static Scenario loadScenario(final Config config) {
		final Scenario scenario = JointScenarioUtils.loadScenario( config );
		enrichScenario( scenario );
		connectLocations( scenario );
		return scenario;
	}
	
	public static void enrichScenario(final Scenario scenario) {
		final Config config = scenario.getConfig();
		final GroupReplanningConfigGroup weights =  (GroupReplanningConfigGroup)
			config.getModule( GroupReplanningConfigGroup.GROUP_NAME );

		if ( config.scenario().isUseHouseholds() && weights.getUseLimitedVehicles() ) {
			scenario.addScenarioElement(
							VehicleRessources.ELEMENT_NAME,
							new HouseholdBasedVehicleRessources(
								((ScenarioImpl) scenario).getHouseholds() ) );
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
	}
	
	public static void connectLocations(final Scenario scenario) {
		if ( scenario.getActivityFacilities() != null ) {
			new WorldConnectLocations( scenario.getConfig() ).connectFacilitiesWithLinks(
					scenario.getActivityFacilities(),
					(NetworkImpl) scenario.getNetwork() );
		}
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
							scenario ),
						scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney(),
						scenario.getActivityFacilities(),
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
			final Scenario scenario ) {
		final ScoringFunctionConfigGroup scoringFunctionConf = (ScoringFunctionConfigGroup)
			scenario.getConfig().getModule( ScoringFunctionConfigGroup.GROUP_NAME );
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
							final PersonImpl person = (PersonImpl) scenario.getPopulation().getPersons().get( id );
							if ( person == null ) {
								// eg transit agent
								return new LinearOverlapScorer( 0 );
							}
							final double typicalDuration =
								getTypicalDuration( 
										scenario,
										person,
										scoringFunctionConf.getActivityTypeForContactInDesires() );
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

	public static double getTypicalDuration(
			final Scenario scenario,
			final PersonImpl person,
			final String type ) {
		final Desires desires = person.getDesires();
		if ( desires != null ) {
			return desires.getActivityDuration( type );
		}

		final Double typicalDuration =
					(Double) scenario.getPopulation().getPersonAttributes().getAttribute(
						person.getId().toString(),
						"typicalDuration_"+type );

		if ( typicalDuration != null ) return typicalDuration.doubleValue();

		final ActivityParams params = scenario.getConfig().planCalcScore().getActivityParams( type );
		
		if ( params == null ) {
			throw new RuntimeException( "could not find typical duration for Person "+person.getId()+" for type "+type );
		}
		
		return params.getTypicalDuration();
	}
}

