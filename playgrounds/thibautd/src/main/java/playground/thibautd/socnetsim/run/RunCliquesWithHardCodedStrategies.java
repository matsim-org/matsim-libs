/* *********************************************************************** *
 * project: org.matsim.*
 * RunCliquesWithHardCodedStrategies.java
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
package playground.thibautd.socnetsim.run;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.experimental.ReflectiveModule;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactoryInternal;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.population.Desires;
import org.matsim.pt.PtConstants;

import playground.ivt.kticompatibility.KtiLikeScoringConfigGroup;
import playground.ivt.kticompatibility.KtiPtRoutingModule;
import playground.ivt.kticompatibility.KtiPtRoutingModule.KtiPtRoutingModuleInfo;
import playground.thibautd.scoring.BeingTogetherScoring;
import playground.thibautd.scoring.BeingTogetherScoring.LogOverlapScorer;
import playground.thibautd.scoring.BeingTogetherScoring.LinearOverlapScorer;
import playground.thibautd.scoring.BeingTogetherScoring.PersonOverlapScorer;
import playground.thibautd.scoring.FireMoneyEventsForUtilityOfBeingTogether;
import playground.thibautd.scoring.KtiScoringFunctionFactoryWithJointModes;
import playground.thibautd.socnetsim.cliques.config.CliquesConfigGroup;
import playground.thibautd.socnetsim.controller.ControllerRegistry;
import playground.thibautd.socnetsim.controller.ControllerRegistryBuilder;
import playground.thibautd.socnetsim.controller.ImmutableJointController;
import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.population.JointPlans;
import playground.thibautd.socnetsim.population.SocialNetwork;
import playground.thibautd.socnetsim.replanning.DefaultPlanLinkIdentifier;
import playground.thibautd.socnetsim.replanning.GenericPlanAlgorithm;
import playground.thibautd.socnetsim.replanning.GenericStrategyModule;
import playground.thibautd.socnetsim.replanning.GroupReplanningListenner;
import playground.thibautd.socnetsim.replanning.GroupStrategyManager;
import playground.thibautd.socnetsim.replanning.GroupStrategyRegistry;
import playground.thibautd.socnetsim.replanning.grouping.FixedGroupsIdentifier;
import playground.thibautd.socnetsim.replanning.grouping.FixedGroupsIdentifierFileParser;
import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;
import playground.thibautd.socnetsim.replanning.modules.AbstractMultithreadedGenericStrategyModule;
import playground.thibautd.socnetsim.replanning.modules.RecomposeJointPlanAlgorithm.PlanLinkIdentifier;
import playground.thibautd.socnetsim.replanning.selectors.EmptyIncompatiblePlansIdentifierFactory;
import playground.thibautd.socnetsim.replanning.selectors.GroupLevelPlanSelector;
import playground.thibautd.socnetsim.replanning.selectors.highestweightselection.HighestWeightSelector;
import playground.thibautd.socnetsim.replanning.selectors.InverseScoreWeight;
import playground.thibautd.socnetsim.replanning.selectors.LossWeight;
import playground.thibautd.socnetsim.replanning.selectors.LowestScoreOfJointPlanWeight;
import playground.thibautd.socnetsim.replanning.selectors.LowestScoreSumSelectorForRemoval;
import playground.thibautd.socnetsim.replanning.selectors.WeightCalculator;
import playground.thibautd.socnetsim.replanning.selectors.WeightedWeight;
import playground.thibautd.socnetsim.replanning.selectors.whoisthebossselector.WhoIsTheBossSelector;
import playground.thibautd.socnetsim.router.JointTripRouterFactory;
import playground.thibautd.socnetsim.run.WeightsConfigGroup.Synchro;
import playground.thibautd.socnetsim.sharedvehicles.HouseholdBasedVehicleRessources;
import playground.thibautd.socnetsim.sharedvehicles.PrepareVehicleAllocationForSimAlgorithm;
import playground.thibautd.socnetsim.sharedvehicles.SharedVehicleUtils;
import playground.thibautd.socnetsim.sharedvehicles.VehicleBasedIncompatiblePlansIdentifierFactory;
import playground.thibautd.socnetsim.sharedvehicles.VehicleRessources;
import playground.thibautd.socnetsim.utils.JointScenarioUtils;
import playground.thibautd.utils.GenericFactory;

/**
 * @author thibautd
 */
public class RunCliquesWithHardCodedStrategies {
	private static final boolean DO_STRATEGY_TRACE = false;
	private static final boolean DO_SELECT_TRACE = false;
	private static final boolean DO_SCORING_TRACE = false;

	public static Scenario createScenario(final String configFile) {
		final Config config = JointScenarioUtils.loadConfig( configFile );
		final WeightsConfigGroup weights = new WeightsConfigGroup();
		config.addModule( weights );
		config.addModule( new ScoringFunctionConfigGroup() );
		config.addModule( new KtiLikeScoringConfigGroup() );
		config.addModule( new KtiInputFilesConfigGroup() );
		final Scenario scenario = JointScenarioUtils.loadScenario( config );

		if ( config.scenario().isUseHouseholds() && weights.getUseLimitedVehicles() ) {
			scenario.addScenarioElement(
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

		return scenario;
	}

	public static void runScenario( final Scenario scenario, final boolean produceAnalysis ) {
		final Config config = scenario.getConfig();
		final CliquesConfigGroup cliquesConf = (CliquesConfigGroup)
					config.getModule( CliquesConfigGroup.GROUP_NAME );
		final WeightsConfigGroup weights = (WeightsConfigGroup)
					config.getModule( WeightsConfigGroup.GROUP_NAME );
		final ScoringFunctionConfigGroup scoringFunctionConf = (ScoringFunctionConfigGroup)
					config.getModule( ScoringFunctionConfigGroup.GROUP_NAME );
		final KtiInputFilesConfigGroup ktiInputFilesConf = (KtiInputFilesConfigGroup)
					config.getModule( KtiInputFilesConfigGroup.GROUP_NAME );

		final FixedGroupsIdentifier cliques = 
			config.scenario().isUseHouseholds() ?
			new FixedGroupsIdentifier(
					((ScenarioImpl) scenario).getHouseholds() ) :
			FixedGroupsIdentifierFileParser.readCliquesFile(
					cliquesConf.getInputFile() );

		final PlanLinkIdentifier planLinkIdentifier =
			linkIdentifier( weights.getSynchronize() );

		final GenericStrategyModule<ReplanningGroup> additionalPrepareModule =
			new AbstractMultithreadedGenericStrategyModule<ReplanningGroup>(
					config.global() ) {
				@Override
				public GenericPlanAlgorithm<ReplanningGroup> createAlgorithm() {
					return 
						scenario.getScenarioElement( VehicleRessources.class ) != null ?
						new PrepareVehicleAllocationForSimAlgorithm(
								MatsimRandom.getLocalInstance(),
								scenario.getScenarioElement( JointPlans.class ),
								scenario.getScenarioElement( VehicleRessources.class ),
								planLinkIdentifier) :
						new GenericPlanAlgorithm<ReplanningGroup>() {
							@Override
							public void run(final ReplanningGroup plan) {
								// do nothing more than default
							}
						};
				}

				@Override
				protected String getName() {
					return "PrepareVehiclesForSim";
				}

			};

		final ControllerRegistryBuilder builder =
			new ControllerRegistryBuilder( scenario )
					.withPlanRoutingAlgorithmFactory(
							RunUtils.createPlanRouterFactory( scenario ) )
					.withGroupIdentifier(
							cliques )
					.withPlanLinkIdentifier(
							planLinkIdentifier )
					.withAdditionalPrepareForSimModule(
							additionalPrepareModule )
					.withIncompatiblePlansIdentifierFactory(
							weights.getConsiderVehicleIncompatibilities() &&
							!weights.getSynchronize().equals( Synchro.none ) &&
							scenario.getScenarioElement( VehicleRessources.class ) != null ?
								new VehicleBasedIncompatiblePlansIdentifierFactory(
										SharedVehicleUtils.DEFAULT_VEHICULAR_MODES ) :
								new EmptyIncompatiblePlansIdentifierFactory() )
					.withScoringFunctionFactory(
							scoringFunctionConf.isUseKtiScoring() ?
							new KtiScoringFunctionFactoryWithJointModes(
								scoringFunctionConf.getAdditionalUtilityOfBeingDriver_s(),
								new StageActivityTypesImpl(
										Arrays.asList(
												PtConstants.TRANSIT_ACTIVITY_TYPE,
												JointActingTypes.PICK_UP,
												JointActingTypes.DROP_OFF) ),
								(KtiLikeScoringConfigGroup) config.getModule( KtiLikeScoringConfigGroup.GROUP_NAME ),
								config.planCalcScore(),
								scenario) :
							// if null, default will be used
							// XXX not nice...
							null );

		if ( scoringFunctionConf.isUseKtiScoring() && !config.scenario().isUseTransit() ) {
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
		// else the JointTripRouterFactory is used with pt from the config file

		final ControllerRegistry controllerRegistry = builder.build();

		// init strategies
		final GroupStrategyRegistry strategyRegistry = new GroupStrategyRegistry();
		RunUtils.loadStrategyRegistry( strategyRegistry , controllerRegistry );

		// create strategy manager
		final GroupStrategyManager strategyManager =
			new GroupStrategyManager( 
					getSelectorForRemoval(
						weights,
						controllerRegistry),
					strategyRegistry,
					config.strategy().getMaxAgentPlanMemorySize());

		// create controler
		final ImmutableJointController controller =
			new ImmutableJointController(
					controllerRegistry,
					new GroupReplanningListenner(
						controllerRegistry,
						strategyManager));


		final FireMoneyEventsForUtilityOfBeingTogether socialScorer =
				new FireMoneyEventsForUtilityOfBeingTogether(
					controllerRegistry.getEvents(),
					scoringFunctionConf.getActTypeFilterForJointScoring(),
					scoringFunctionConf.getModeFilterForJointScoring(),
					getPersonOverlapScorerFactory(
						scoringFunctionConf,
						scenario.getPopulation() ),
					scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney(),
					toSocialNetwork( cliques ) );
		controllerRegistry.getEvents().addHandler( socialScorer );
		controller.addControlerListener( socialScorer );

		if (produceAnalysis) {
			RunUtils.loadDefaultAnalysis(
					weights.getGraphWriteInterval(),
					cliques,
					strategyManager,
					controller );
		}

		if ( weights.getCheckConsistency() ) {
			// those listenners check the coordination behavior:
			// do not ad if not used
			RunUtils.addConsistencyCheckingListeners( controller );
		}
		RunUtils.addDistanceFillerListener( controller );

		// run it
		controller.run();
	}

	private static GroupLevelPlanSelector getSelectorForRemoval(
			final WeightsConfigGroup weights,
			final ControllerRegistry controllerRegistry) {
		switch ( weights.getGroupScoringType() ) {
			case weightedSum:
				return new HighestWeightSelector(
						true ,
						controllerRegistry.getIncompatiblePlansIdentifierFactory(),
						new WeightedWeight(
							new InverseScoreWeight(),
							weights.getWeightAttributeName(),
							controllerRegistry.getScenario().getPopulation().getPersonAttributes()  ));
			case sum:
				return new LowestScoreSumSelectorForRemoval(
						controllerRegistry.getIncompatiblePlansIdentifierFactory());
			case min:
				{ // scope to avoid errors with baseWeight in next case.
				final WeightCalculator baseWeight =
					new LowestScoreOfJointPlanWeight(
							controllerRegistry.getJointPlans());
				return new HighestWeightSelector(
						true ,
						controllerRegistry.getIncompatiblePlansIdentifierFactory(),
						new WeightCalculator() {
							@Override
							public double getWeight(
									final Plan indivPlan,
									final ReplanningGroup replanningGroup) {
								return -baseWeight.getWeight( indivPlan , replanningGroup );
							}
						});
				}
			case minLoss:
				final WeightCalculator baseWeight =
					new LowestScoreOfJointPlanWeight(
							new LossWeight(),
							controllerRegistry.getJointPlans());
				return new HighestWeightSelector(
						true ,
						controllerRegistry.getIncompatiblePlansIdentifierFactory(),
						new WeightCalculator() {
							@Override
							public double getWeight(
									final Plan indivPlan,
									final ReplanningGroup replanningGroup) {
								return -baseWeight.getWeight( indivPlan , replanningGroup );
							}
						});
			case whoIsTheBoss:
				return new WhoIsTheBossSelector(
						true ,
						MatsimRandom.getLocalInstance(),
						controllerRegistry.getIncompatiblePlansIdentifierFactory(),
						new InverseScoreWeight() );
			default:
				throw new RuntimeException( "unkown: "+weights.getGroupScoringType() );
		}
	}

	private static GenericFactory<PersonOverlapScorer, Id> getPersonOverlapScorerFactory(
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

	private static SocialNetwork toSocialNetwork(
			final FixedGroupsIdentifier cliques) {
		final SocialNetwork socNet = new SocialNetwork();
		for ( Collection<? extends Id> clique : cliques.getGroupInfo() ) {
			for ( Id id1 : clique ) {
				for ( Id id2 : clique ) {
					socNet.addMonodirectionalTie( id1 , id2 );
				}
			}
		}
		return socNet;
	}

	private static PlanLinkIdentifier linkIdentifier(final Synchro synchro) {
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

	public static void main(final String[] args) {
		OutputDirectoryLogging.catchLogEntries();
		if (DO_STRATEGY_TRACE) Logger.getLogger( GroupStrategyManager.class.getName() ).setLevel( Level.TRACE );
		if (DO_SELECT_TRACE) Logger.getLogger( HighestWeightSelector.class.getName() ).setLevel( Level.TRACE );
		if (DO_SCORING_TRACE) Logger.getLogger( "playground.thibautd.scoring" ).setLevel( Level.TRACE );
		final String configFile = args[ 0 ];

		// load "registry"
		final Scenario scenario = createScenario( configFile );
		runScenario( scenario , true );
	}
}

class ScoringFunctionConfigGroup extends ReflectiveModule {
	public static final String GROUP_NAME = "scoringFunction";
	private boolean useKtiScoring = false;
	private double marginalUtilityOfBeingTogether_h = 0;
	private double additionalUtilityOfBeingDriver_h = 0;

	static enum TogetherScoringForm {
		linear,
		logarithmic;
	}
	private TogetherScoringForm togetherScoringForm = TogetherScoringForm.linear;
	
	static enum TogetherScoringType {
		allModesAndActs,
		leisureOnly;
	}
	private TogetherScoringType togetherScoringType = TogetherScoringType.allModesAndActs;

	public ScoringFunctionConfigGroup() {
		super( GROUP_NAME );
	}

	@StringSetter( "useKtiScoring" )
	public void setUseKtiScoring(final boolean v) {
		this.useKtiScoring = v;
	}

	@StringGetter( "useKtiScoring" )
	public boolean isUseKtiScoring() {
		return useKtiScoring;
	}

	@StringGetter( "marginalUtilityOfBeingTogether_h" )
	public double getMarginalUtilityOfBeingTogether_h() {
		return this.marginalUtilityOfBeingTogether_h;
	}

	public double getMarginalUtilityOfBeingTogether_s() {
		return this.marginalUtilityOfBeingTogether_h / 3600;
	}

	@StringSetter( "marginalUtilityOfBeingTogether_h" )
	public void setMarginalUtilityOfBeingTogether_h(
			double marginalUtilityOfBeingTogether_h) {
		this.marginalUtilityOfBeingTogether_h = marginalUtilityOfBeingTogether_h;
	}

	@StringGetter( "togetherScoringType" )
	public TogetherScoringType getTogetherScoringType() {
		return this.togetherScoringType;
	}

	@StringSetter( "togetherScoringType" )
	public void setTogetherScoringType(final String v) {
		setTogetherScoringType( TogetherScoringType.valueOf( v ) );
	}

	public void setTogetherScoringType(final TogetherScoringType togetherScoringType) {
		this.togetherScoringType = togetherScoringType;
	}

	@StringGetter( "additionalUtilityOfBeingDriver_h" )
	public double getAdditionalUtilityOfBeingDriver_h() {
		return this.additionalUtilityOfBeingDriver_h;
	}

	public double getAdditionalUtilityOfBeingDriver_s() {
		return this.additionalUtilityOfBeingDriver_h / 3600.;
	}

	@StringSetter( "additionalUtilityOfBeingDriver_h" )
	public void setAdditionalUtilityOfBeingDriver_h(
			double additionalUtilityOfBeingDriver_h) {
		this.additionalUtilityOfBeingDriver_h = additionalUtilityOfBeingDriver_h;
	}

	@StringGetter( "togetherScoringForm" )
	public TogetherScoringForm getTogetherScoringForm() {
		return this.togetherScoringForm;
	}

	@StringSetter( "togetherScoringForm" )
	public void setTogetherScoringForm(final String v) {
		setTogetherScoringForm( TogetherScoringForm.valueOf( v ) );
	}

	public void setTogetherScoringForm(final TogetherScoringForm togetherScoringForm) {
		this.togetherScoringForm = togetherScoringForm;
	}

	// I do not like so much this kind of "intelligent" method in Modules...
	public BeingTogetherScoring.Filter getActTypeFilterForJointScoring() {
		switch ( togetherScoringType ) {
			case allModesAndActs:
				return new BeingTogetherScoring.AcceptAllFilter();
			case leisureOnly:
				return new BeingTogetherScoring.AcceptAllInListFilter( "leisure" );
			default:
				throw new IllegalStateException( "gné?! "+togetherScoringType );
		}
	}

	public BeingTogetherScoring.Filter getModeFilterForJointScoring() {
		switch ( togetherScoringType ) {
			case allModesAndActs:
				return new BeingTogetherScoring.AcceptAllFilter();
			case leisureOnly:
				return new BeingTogetherScoring.RejectAllFilter();
			default:
				throw new IllegalStateException( "gné?! "+togetherScoringType );
		}
	}
}

class KtiInputFilesConfigGroup extends ReflectiveModule {
	public static final String GROUP_NAME = "ktiInputFiles";

	private String worldFile = null;
	private String travelTimesFile = null;
	private String ptStopsFile = null;
	private double intrazonalPtSpeed = 4.361111;

	public KtiInputFilesConfigGroup() {
		super( GROUP_NAME );
	}

	@StringGetter( "worldFile" )
	public String getWorldFile() {
		return this.worldFile;
	}

	@StringSetter( "worldFile" )
	public void setWorldFile(String worldFile) {
		this.worldFile = worldFile;
	}

	@StringGetter( "travelTimesFile" )
	public String getTravelTimesFile() {
		return this.travelTimesFile;
	}

	@StringSetter( "travelTimesFile" )
	public void setTravelTimesFile(String travelTimesFile) {
		this.travelTimesFile = travelTimesFile;
	}

	@StringGetter( "ptStopsFile" )
	public String getPtStopsFile() {
		return this.ptStopsFile;
	}

	@StringSetter( "ptStopsFile" )
	public void setPtStopsFile(String ptStopsFile) {
		this.ptStopsFile = ptStopsFile;
	}

	@StringGetter( "intrazonalPtSpeed" )
	public double getIntrazonalPtSpeed() {
		return intrazonalPtSpeed;
	}

	@StringSetter( "intrazonalPtSpeed" )
	public void setIntrazonalPtSpeed(final double v) {
		this.intrazonalPtSpeed = v;
	}
}
