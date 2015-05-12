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
import java.util.Collections;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReaderMatsimV2;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.ControlerListener;
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
import playground.thibautd.socnetsim.analysis.AbstractPlanAnalyzerPerGroup;
import playground.thibautd.socnetsim.analysis.CliquesSizeGroupIdentifier;
import playground.thibautd.socnetsim.analysis.FilteredScoreStats;
import playground.thibautd.socnetsim.analysis.JointPlanSizeStats;
import playground.thibautd.socnetsim.analysis.JointTripsStats;
import playground.thibautd.socnetsim.cliques.Clique;
import playground.thibautd.socnetsim.controller.listeners.GroupReplanningListenner;
import playground.thibautd.socnetsim.events.CourtesyEventsGenerator;
import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.population.JointPlans;
import playground.thibautd.socnetsim.population.SocialNetwork;
import playground.thibautd.socnetsim.population.SocialNetworkReader;
import playground.thibautd.socnetsim.qsim.SwitchingJointQSimFactory;
import playground.thibautd.socnetsim.replanning.GenericPlanAlgorithm;
import playground.thibautd.socnetsim.replanning.GenericStrategyModule;
import playground.thibautd.socnetsim.replanning.GroupStrategyManager;
import playground.thibautd.socnetsim.replanning.GroupStrategyRegistry;
import playground.thibautd.socnetsim.replanning.InnovationSwitchingGroupReplanningListenner;
import playground.thibautd.socnetsim.replanning.grouping.FixedGroupsIdentifier;
import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;
import playground.thibautd.socnetsim.replanning.modules.AbstractMultithreadedGenericStrategyModule;
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

/**
 * Groups methods too specific to go in the "frameworky" part of the code,
 * but which still needs to be called from various application-specific scripts.
 *
 * Ideally, scripts should consist mainly of calls to those methods, with a few
 * lines specific to the application.
 * @author thibautd
 */
public class RunUtils {
	static final Logger log =
		Logger.getLogger(RunUtils.class);

	private RunUtils() {}

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

	public static ControllerRegistryBuilder loadDefaultRegistryBuilder(
			final ControllerRegistryBuilder builder,
			final Scenario scenario) {
		final GroupReplanningConfigGroup weights = (GroupReplanningConfigGroup)
					scenario.getConfig().getModule( GroupReplanningConfigGroup.GROUP_NAME );



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

		return builder;
	}


	public static ImmutableJointController initializeNonPSimController(
			final ControllerRegistry controllerRegistry) {
		if ( true ) throw new RuntimeException( "to remove, just there as a todo list for refactoring" );
		final StrategyAnalysisConfigGroup analysis = (StrategyAnalysisConfigGroup)
			config.getModule( StrategyAnalysisConfigGroup.GROUP_NAME );

		if ( analysis.isDumpGroupSizes() ) {
			final ReplanningStatsDumper replanningStats = new ReplanningStatsDumper( controller.getControlerIO().getOutputFilename( "replanningGroups.dat" ) );
			repl.getStrategyManager().addListener( replanningStats );
			controller.addControlerListener( replanningStats );
		}

		if ( analysis.isDumpAllocation() ) {
			final ReplanningAllocationDumper replanningStats = new ReplanningAllocationDumper( controller.getControlerIO().getOutputFilename( "replanningAllocations.dat" ) );
			repl.getStrategyManager().addListener( replanningStats );
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
}

