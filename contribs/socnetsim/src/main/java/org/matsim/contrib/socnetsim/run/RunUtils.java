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
package org.matsim.contrib.socnetsim.run;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.socnetsim.framework.scoring.GroupSizePreferencesConfigGroup;
import org.matsim.contrib.socnetsim.sharedvehicles.HouseholdBasedVehicleRessources;
import org.matsim.contrib.socnetsim.sharedvehicles.VehicleRessources;
import org.matsim.contrib.socnetsim.usage.JointScenarioUtils;
import org.matsim.contrib.socnetsim.usage.replanning.GroupReplanningConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.facilities.algorithms.WorldConnectLocations;

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

	// TODO: pass commented out part under DI
//	public static void addDistanceFillerListener(final ImmutableJointController controller) {
//		final DistanceFillerAlgorithm algo = new DistanceFillerAlgorithm();
//
//		algo.putEstimator(
//				TransportMode.transit_walk,
//				new DistanceFillerAlgorithm.CrowFlyEstimator(
//					controller.getRegistry().getScenario().getConfig().plansCalcRoute().getModeRoutingParams().get( TransportMode.transit_walk ).getBeelineDistanceFactor(), 
//					controller.getRegistry().getScenario().getNetwork() ) );
//
//		// this is done by the routing module, but not at import
//		algo.putEstimator(
//				TransportMode.walk,
//				new DistanceFillerAlgorithm.CrowFlyEstimator(
//					controller.getRegistry().getScenario().getConfig().plansCalcRoute().getModeRoutingParams().get( TransportMode.walk ).getBeelineDistanceFactor(), 
//					controller.getRegistry().getScenario().getNetwork() ) );
//
//		algo.putEstimator(
//				TransportMode.pt,
//				new DistanceFillerAlgorithm.CrowFlyEstimator(
//					// this was the hard-coded factor for in-vehicle distance in KTI...
//					// XXX not sure it makes sense to use the same approach with detailed pt
//					1.5,
//					controller.getRegistry().getScenario().getNetwork() ) );
//
//		controller.addControlerListener(
//				new BeforeMobsimListener() {
//					@Override
//					public void notifyBeforeMobsim(final BeforeMobsimEvent event) {
//						for ( Person person : controller.getRegistry().getScenario().getPopulation().getPersons().values() ) {
//							algo.run( person.getSelectedPlan() );
//						}
//					}
//				});
//	}
//
//	public static ControllerRegistryBuilder loadDefaultRegistryBuilder(
//			final ControllerRegistryBuilder builder,
//			final Scenario scenario) {
//		final GroupReplanningConfigGroup weights = (GroupReplanningConfigGroup)
//					scenario.getConfig().getModule( GroupReplanningConfigGroup.GROUP_NAME );
//
//
//
//		final ScoringFunctionConfigGroup scoringFunctionConf = (ScoringFunctionConfigGroup)
//					scenario.getConfig().getModule( ScoringFunctionConfigGroup.GROUP_NAME );
//		if ( scoringFunctionConf.isUseKtiScoring() ) {
//			builder.withScoringFunctionFactory(
//				new KtiScoringFunctionFactoryWithJointModes(
//					new StageActivityTypesImpl(
//								Arrays.asList(
//										PtConstants.TRANSIT_ACTIVITY_TYPE,
//										JointActingTypes.INTERACTION) ),
//					(KtiLikeScoringConfigGroup) scenario.getConfig().getModule( KtiLikeScoringConfigGroup.GROUP_NAME ),
//					scenario.getConfig().planCalcScore(),
//					scoringFunctionConf,
//					scenario) );
//		}
//
//		if ( scoringFunctionConf.getInternalizationNetworkFile() != null ) {
//			final String elementName = "another social network, to use for internalization";
//			new SocialNetworkReader( elementName , scenario ).parse( scoringFunctionConf.getInternalizationNetworkFile() );
//			builder.withScoringListener(
//					new InternalizingPlansScoring(
//						elementName,
//						scenario,
//						builder.getEvents(),
//						builder.getScoringFunctionFactory() ) );
//		}
//
//		builder.withMobsimFactory(
//				new SwitchingJointQSimFactory(
//						builder.getTravelTime() ) );
//
//		return builder;
//	}
//
//
//	public static ImmutableJointController initializeNonPSimController(
//			final ControllerRegistry controllerRegistry) {
//		if ( true ) throw new RuntimeException( "to remove, just there as a todo list for refactoring" );
//		final StrategyAnalysisConfigGroup analysis = (StrategyAnalysisConfigGroup)
//			config.getModule( StrategyAnalysisConfigGroup.GROUP_NAME );
//
//		if ( analysis.isDumpGroupSizes() ) {
//			final ReplanningStatsDumper replanningStats = new ReplanningStatsDumper( controller.getControlerIO().getOutputFilename( "replanningGroups.dat" ) );
//			repl.getStrategyManager().addListener( replanningStats );
//			controller.addControlerListener( replanningStats );
//		}
//
//		if ( analysis.isDumpAllocation() ) {
//			final ReplanningAllocationDumper replanningStats = new ReplanningAllocationDumper( controller.getControlerIO().getOutputFilename( "replanningAllocations.dat" ) );
//			repl.getStrategyManager().addListener( replanningStats );
//			controller.addControlerListener( replanningStats );
//		}
//
//		return controller;
//	}

	public static Config loadConfig(final String configFile) {
		final Config config = JointScenarioUtils.createConfig();
		addConfigGroups( config );
		new ConfigReader( config ).readFile( configFile );
		return config;
	}

	public static void addConfigGroups(final Config config) {
		config.addModule( new ScoringFunctionConfigGroup() );
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

		if ( config.households().getInputFile()!=null && weights.getUseLimitedVehicles() ) {
			scenario.addScenarioElement(
							VehicleRessources.ELEMENT_NAME,
							new HouseholdBasedVehicleRessources(
								scenario.getHouseholds() ) );
		}

		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (Activity act : TripStructureUtils.getActivities( plan , EmptyStageActivityTypes.INSTANCE )) {
					if (act.getCoord() != null) continue;
					if (act.getLinkId() == null) throw new NullPointerException();
					act.setCoord(
						scenario.getNetwork().getLinks().get( act.getLinkId() ).getCoord() );
				}
			}
		}
	}
	
	public static void connectLocations(final Scenario scenario) {
		if ( scenario.getActivityFacilities() != null ) {
			new WorldConnectLocations( scenario.getConfig() ).connectFacilitiesWithLinks(
					scenario.getActivityFacilities(),
					scenario.getNetwork() );
		}
	}
}

