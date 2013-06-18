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
import java.util.List;


import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.experimental.ReflectiveModule;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.pt.PtConstants;

import playground.ivt.kticompatibility.KtiPtRoutingModule;
import playground.ivt.kticompatibility.KtiPtRoutingModule.KtiPtRoutingModuleInfo;
import playground.ivt.scoring.KtiLikeActivitiesScoringFunctionFactory;
import playground.ivt.scoring.KtiLikeScoringConfigGroup;
import playground.thibautd.socnetsim.cliques.config.CliquesConfigGroup;
import playground.thibautd.socnetsim.controller.ControllerRegistry;
import playground.thibautd.socnetsim.controller.ControllerRegistryBuilder;
import playground.thibautd.socnetsim.controller.ImmutableJointController;
import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.population.JointPlans;
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
import playground.thibautd.socnetsim.replanning.selectors.LowestScoreSumSelectorForRemoval;
import playground.thibautd.socnetsim.replanning.selectors.EmptyIncompatiblePlansIdentifierFactory;
import playground.thibautd.socnetsim.replanning.selectors.highestweightselection.HighestWeightSelector;
import playground.thibautd.socnetsim.router.JointTripRouterFactory;
import playground.thibautd.socnetsim.run.WeightsConfigGroup.Synchro;
import playground.thibautd.socnetsim.sharedvehicles.HouseholdBasedVehicleRessources;
import playground.thibautd.socnetsim.sharedvehicles.PrepareVehicleAllocationForSimAlgorithm;
import playground.thibautd.socnetsim.sharedvehicles.SharedVehicleUtils;
import playground.thibautd.socnetsim.sharedvehicles.VehicleBasedIncompatiblePlansIdentifierFactory;
import playground.thibautd.socnetsim.sharedvehicles.VehicleRessources;
import playground.thibautd.socnetsim.utils.JointScenarioUtils;

/**
 * @author thibautd
 */
public class RunCliquesWithHardCodedStrategies {
	private static final boolean DO_STRATEGY_TRACE = false;
	private static final boolean DO_SELECT_TRACE = false;
	private static final boolean DO_SCORING_TRACE = false;

	public static Scenario createScenario(final String configFile) {
		final Config config = JointScenarioUtils.loadConfig( configFile );
		config.addModule( WeightsConfigGroup.GROUP_NAME , new WeightsConfigGroup() );
		config.addModule( ScoringFunctionConfigGroup.GROUP_NAME , new ScoringFunctionConfigGroup() );
		config.addModule( KtiLikeScoringConfigGroup.GROUP_NAME , new KtiLikeScoringConfigGroup() );
		config.addModule( KtiInputFilesConfigGroup.GROUP_NAME , new KtiInputFilesConfigGroup() );
		final Scenario scenario = JointScenarioUtils.loadScenario( config );

		if ( config.scenario().isUseHouseholds() ) {
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
						!weights.getSynchronize().equals( Synchro.none ) &&
						scenario.getScenarioElement( VehicleRessources.class ) != null ?
							new VehicleBasedIncompatiblePlansIdentifierFactory(
									SharedVehicleUtils.DEFAULT_VEHICULAR_MODES ) :
							new EmptyIncompatiblePlansIdentifierFactory() )
					.withScoringFunctionFactory(
							scoringFunctionConf.isUseKtiScoring() ?
							new KtiLikeActivitiesScoringFunctionFactory(
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

		if ( scoringFunctionConf.isUseKtiScoring() ) {
			builder.withTripRouterFactory(
					new TripRouterFactory() {
						private final TripRouterFactory delegate =
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

		final ControllerRegistry controllerRegistry = builder.build();

		// init strategies
		final GroupStrategyRegistry strategyRegistry = new GroupStrategyRegistry();
		RunUtils.loadStrategyRegistry( strategyRegistry , controllerRegistry );

		// create strategy manager
		final GroupStrategyManager strategyManager =
			new GroupStrategyManager( 
					new LowestScoreSumSelectorForRemoval(
							controllerRegistry.getIncompatiblePlansIdentifierFactory()),
					strategyRegistry,
					config.strategy().getMaxAgentPlanMemorySize());

		// create controler
		final ImmutableJointController controller =
			new ImmutableJointController(
					controllerRegistry,
					new GroupReplanningListenner(
						controllerRegistry,
						strategyManager));

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
