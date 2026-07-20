
/* *********************************************************************** *
 * project: org.matsim.*
 * PlanCalcScoreConfigGroupTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.core.config.groups;

import static org.matsim.core.config.groups.ScoringConfigGroup.createStageActivityType;

import java.util.Map;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.ScoringConfigGroup.ModeParams;
import org.matsim.testcases.MatsimTestUtils;

	public class ScoringConfigGroupTest {
	private static final Logger log =
		LogManager.getLogger(ScoringConfigGroupTest.class);

	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();

	private void testResultsBeforeCheckConsistency( Config config, boolean fullyHierarchical ) {
		ScoringConfigGroup scoringConfig = config.scoring() ;

		if ( ! fullyHierarchical ){
			// mode params are there for default modes:
			Assertions.assertNotNull( scoringConfig.getModeParams().get( TransportMode.car ) );
			Assertions.assertNotNull( scoringConfig.getModeParams().get( TransportMode.walk ) );
			Assertions.assertNotNull( scoringConfig.getModeParams().get( TransportMode.bike ) );
			Assertions.assertNotNull( scoringConfig.getModeParams().get( TransportMode.ride ) );
			Assertions.assertNotNull( scoringConfig.getModeParams().get( TransportMode.pt ) );
			Assertions.assertNotNull( scoringConfig.getModeParams().get( TransportMode.other ) );

			// default stage/interaction params are there for pt and drt (as a service):
			Assertions.assertNotNull( scoringConfig.getActivityParams( createStageActivityType( TransportMode.pt ) ) );
			Assertions.assertNotNull( scoringConfig.getActivityParams( createStageActivityType( TransportMode.drt ) ) );
		}
		// default stage/interaction params for modes routed on the network are not yet there:
//		for( String networkMode : config.plansCalcRoute().getNetworkModes() ){
//			Assert.assertNull( scoringConfig.getActivityParams( createStageActivityType( networkMode ) ) );
//		}
	}
	private void testResultsAfterCheckConsistency( Config config ) {
		ScoringConfigGroup scoringConfig = config.scoring() ;

		// default stage/interaction params for modes routed on the network are now there:
		for( String networkMode : config.routing().getNetworkModes() ){
			Assertions.assertNotNull( scoringConfig.getActivityParams( createStageActivityType( networkMode ) ) );
		}
	}

	 @Test
	 void testFullyHierarchicalVersion() {
		Config config = ConfigUtils.loadConfig( utils.getClassInputDirectory() + "config_v2_w_scoringparams.xml" ) ;
		ScoringConfigGroup scoringConfig = config.scoring() ;
		testResultsBeforeCheckConsistency( config, true ) ;
		log.warn( "" );
		for( ModeParams modeParams : scoringConfig.getModeParams().values() ){
			log.warn(  modeParams );
		}
		log.warn( "" );
		for( ActivityParams activityParams : scoringConfig.getActivityParams() ){
			log.warn(  activityParams );
		}
		log.warn( "" );
		log.warn( "checking consistency ..." );
		config.routing().setAccessEgressType(RoutingConfigGroup.AccessEgressType.accessEgressModeToLink);
		scoringConfig.checkConsistency( config );
		testResultsAfterCheckConsistency( config );
		log.warn( "" );
		for( ModeParams modeParams : scoringConfig.getModeParams().values() ){
			log.warn(  modeParams );
		}
		log.warn( "" );
		for( ActivityParams activityParams : scoringConfig.getActivityParams() ){
			log.warn(  activityParams );
		}
		log.warn( "" );
	}

	 @Test
	 void testVersionWoScoringparams() {
		Config config = ConfigUtils.loadConfig( utils.getClassInputDirectory() + "config_v2_wo_scoringparams.xml" ) ;
		ScoringConfigGroup scoringConfig = config.scoring() ;
		testResultsBeforeCheckConsistency( config, false ) ;
		log.warn( "" );
		for( ModeParams modeParams : scoringConfig.getModeParams().values() ){
			log.warn(  modeParams );
		}
		log.warn( "" );
		for( ActivityParams activityParams : scoringConfig.getActivityParams() ){
			log.warn(  activityParams );
		}
		log.warn( "" );
		log.warn( "checking consistency ..." );
		config.routing().setAccessEgressType( RoutingConfigGroup.AccessEgressType.accessEgressModeToLink);
		scoringConfig.checkConsistency( config );
		testResultsAfterCheckConsistency( config );
		log.warn( "" );
		for( ModeParams modeParams : scoringConfig.getModeParams().values() ){
			log.warn(  modeParams );
		}
		log.warn( "" );
		for( ActivityParams activityParams : scoringConfig.getActivityParams() ){
			log.warn(  activityParams );
		}
		log.warn( "" );
	}

	 @Test
	 void testAddActivityParams() {
		ScoringConfigGroup c = new ScoringConfigGroup();
        int originalSize = c.getActivityParams().size();
		Assertions.assertNull(c.getActivityParams("type1"));
        Assertions.assertEquals(originalSize, c.getActivityParams().size());

		ActivityParams ap = new ActivityParams("type1");
		c.addActivityParams(ap);
		Assertions.assertEquals(ap, c.getActivityParams("type1"));
        Assertions.assertEquals(originalSize + 1, c.getActivityParams().size());
	}

	@Test
	void testExplicitSubpopulationGettersDoNotFallback() {
		// Explicit subpopulation accessors should only return parameters defined for exactly that subpopulation.
		ScoringConfigGroup scoringConfigGroup = new ScoringConfigGroup();

		ScoringConfigGroup.ScoringParameterSet freightParams = scoringConfigGroup.getOrCreateScoringParameters("freight");
		ModeParams truckModeParams = new ModeParams("truck");
		ActivityParams freightActivityParams = new ActivityParams("freightInteraction");
		freightActivityParams.setTypicalDuration(600.);
		freightParams.addModeParams(truckModeParams);
		freightParams.addActivityParams(freightActivityParams);

		Assertions.assertThrows(RuntimeException.class,
			() -> scoringConfigGroup.getModeParamsForSubpopulation("missing"));
		Assertions.assertThrows(RuntimeException.class,
			() -> scoringConfigGroup.getActivityParamsForSubpopulation("missing"));
		Assertions.assertThrows(RuntimeException.class,
			() -> scoringConfigGroup.getActivityParamsForSubpopulation("freightInteraction", "missing"));
		Assertions.assertThrows(RuntimeException.class,
			() -> scoringConfigGroup.getMarginalUtilityOfMoney("missing"));
		Assertions.assertThrows(RuntimeException.class,
			() -> scoringConfigGroup.getMarginalUtlOfWaiting_utils_hr("missing"));

		Assertions.assertSame(truckModeParams,
			scoringConfigGroup.getModeParamsForSubpopulation("freight").get("truck"));
		Assertions.assertSame(freightActivityParams,
			scoringConfigGroup.getActivityParamsForSubpopulation("freightInteraction", "freight"));
		Assertions.assertEquals(freightParams.getMarginalUtilityOfMoney(),
			scoringConfigGroup.getMarginalUtilityOfMoney("freight"), 1e-7);
		Assertions.assertEquals(freightParams.getMarginalUtlOfWaiting_utils_hr(),
			scoringConfigGroup.getMarginalUtlOfWaiting_utils_hr("freight"), 1e-7);
		Assertions.assertTrue(scoringConfigGroup.getAllScoringParameterSetsPerSubpopulation().containsKey("freight"));
		Assertions.assertTrue(scoringConfigGroup.getExplicitScoringParameterSetsPerSubpopulation().containsKey("freight"));
		Assertions.assertFalse(scoringConfigGroup.getExplicitScoringParameterSetsPerSubpopulation().containsKey(null));
	}

	@Test
	void testAddParamsForSubpopulationRequiresExplicitSubpopulation() {
		// Adding mode and activity parameters for a subpopulation should require that the subpopulation already exists.
		ScoringConfigGroup scoringConfigGroup = new ScoringConfigGroup();

		scoringConfigGroup.getOrCreateScoringParameters("freight");

		ModeParams truckModeParams = new ModeParams("truck");
		ActivityParams freightActivityParams = new ActivityParams("freightInteraction");
		freightActivityParams.setTypicalDuration(600.);

		scoringConfigGroup.addModeParamsForSubpopulation(truckModeParams, "freight");
		scoringConfigGroup.addActivityParamsForSubpopulation(freightActivityParams, "freight");

		Assertions.assertSame(truckModeParams,
			scoringConfigGroup.getModeParamsForSubpopulation("freight").get("truck"));
		Assertions.assertSame(freightActivityParams,
			scoringConfigGroup.getActivityParamsForSubpopulation("freightInteraction", "freight"));

		Assertions.assertThrows(RuntimeException.class,
			() -> scoringConfigGroup.addModeParamsForSubpopulation(new ModeParams("van"), "missing"));
		Assertions.assertThrows(RuntimeException.class,
			() -> scoringConfigGroup.addActivityParamsForSubpopulation(new ActivityParams("missingInteraction"), "missing"));
	}

	@Test
	void testSetScoringParametersAsDefaultSubpopulationUsesExistingParameters() {
		// Setting an existing subpopulation as default should create an independent default parameter set.
		ScoringConfigGroup scoringConfigGroup = new ScoringConfigGroup();

		ScoringConfigGroup.ScoringParameterSet freightParams = scoringConfigGroup.getOrCreateScoringParameters("freight");
		ModeParams truckModeParams = new ModeParams("truck");
		truckModeParams.setConstant(23.);
		ActivityParams freightActivityParams = new ActivityParams("freightInteraction");
		freightActivityParams.setTypicalDuration(600.);
		freightParams.addModeParams(truckModeParams);
		freightParams.addActivityParams(freightActivityParams);
		freightParams.setMarginalUtilityOfMoney(4.);

		ScoringConfigGroup.ScoringParameterSet defaultParams =
			scoringConfigGroup.setScoringParametersAsDefaultSubpopulation("freight");

		Assertions.assertNotSame(freightParams, defaultParams);
		Assertions.assertSame(defaultParams,
			scoringConfigGroup.getAllScoringParameterSetsPerSubpopulation().get(ScoringConfigGroup.DEFAULT_SUBPOPULATION));
		Assertions.assertSame(freightParams,
			scoringConfigGroup.getAllScoringParameterSetsPerSubpopulation().get("freight"));
		Assertions.assertFalse(scoringConfigGroup.getExplicitScoringParameterSetsPerSubpopulation().containsKey(ScoringConfigGroup.DEFAULT_SUBPOPULATION));
		Assertions.assertEquals(4., scoringConfigGroup.getDefaultMarginalUtilityOfMoney(), 1e-7);
		Assertions.assertEquals(23.,
			scoringConfigGroup.getModeParams().get("truck").getConstant(), 1e-7);
		Assertions.assertEquals(freightActivityParams.getTypicalDuration(),
			scoringConfigGroup.getActivityParams("freightInteraction").getTypicalDuration());
		Assertions.assertThrows(RuntimeException.class,
			() -> scoringConfigGroup.getModeParamsForSubpopulation(ScoringConfigGroup.DEFAULT_SUBPOPULATION));
		Assertions.assertThrows(RuntimeException.class,
			() -> scoringConfigGroup.getActivityParamsForSubpopulation(ScoringConfigGroup.DEFAULT_SUBPOPULATION));
		Assertions.assertThrows(RuntimeException.class,
			() -> scoringConfigGroup.getActivityParamsForSubpopulation("freightInteraction", ScoringConfigGroup.DEFAULT_SUBPOPULATION));
		Assertions.assertThrows(RuntimeException.class,
			() -> scoringConfigGroup.getMarginalUtilityOfMoney(ScoringConfigGroup.DEFAULT_SUBPOPULATION));
		Assertions.assertThrows(RuntimeException.class,
			() -> scoringConfigGroup.getMarginalUtlOfWaiting_utils_hr(ScoringConfigGroup.DEFAULT_SUBPOPULATION));

		scoringConfigGroup.setDefaultMarginalUtilityOfMoney(5.);
		scoringConfigGroup.setDefaultMarginalUtlOfWaiting_utils_hr(-2.);
		scoringConfigGroup.setDefaultMarginalUtlOfWaitingPt_utils_hr(-3.);
		scoringConfigGroup.setDefaultLateArrival_utils_hr(-4.);
		scoringConfigGroup.setDefaultEarlyDeparture_utils_hr(-5.);
		scoringConfigGroup.setDefaultPerforming_utils_hr(6.);
		scoringConfigGroup.setDefaultUtilityOfLineSwitch(-7.);

		Assertions.assertEquals(5., scoringConfigGroup.getDefaultMarginalUtilityOfMoney(), 1e-7);
		Assertions.assertEquals(4., scoringConfigGroup.getMarginalUtilityOfMoney("freight"), 1e-7);
		Assertions.assertEquals(-2., scoringConfigGroup.getDefaultMarginalUtlOfWaiting_utils_hr(), 1e-7);
		Assertions.assertEquals(0., scoringConfigGroup.getMarginalUtlOfWaiting_utils_hr("freight"), 1e-7);
		Assertions.assertEquals(-3., scoringConfigGroup.getDefaultMarginalUtlOfWaitingPt_utils_hr(), 1e-7);
		Assertions.assertEquals(-4., scoringConfigGroup.getDefaultLateArrival_utils_hr(), 1e-7);
		Assertions.assertEquals(-18., freightParams.getLateArrival_utils_hr(), 1e-7);
		Assertions.assertEquals(-5., scoringConfigGroup.getDefaultEarlyDeparture_utils_hr(), 1e-7);
		Assertions.assertEquals(-0., freightParams.getEarlyDeparture_utils_hr(), 1e-7);
		Assertions.assertEquals(6., scoringConfigGroup.getDefaultPerforming_utils_hr(), 1e-7);
		Assertions.assertEquals(6., freightParams.getPerforming_utils_hr(), 1e-7);
		Assertions.assertEquals(-7., scoringConfigGroup.getDefaultUtilityOfLineSwitch(), 1e-7);
		Assertions.assertEquals(-1., freightParams.getUtilityOfLineSwitch(), 1e-7);

		truckModeParams.setConstant(42.);

		Assertions.assertEquals(23.,
			scoringConfigGroup.getModeParams().get("truck").getConstant(), 1e-7);
		Assertions.assertEquals(42.,
			scoringConfigGroup.getModeParamsForSubpopulation("freight").get("truck").getConstant(), 1e-7);
	}

	@Test
	void testSetScoringParametersAsDefaultSubpopulationRequiresExistingSubpopulation() {
		// Missing subpopulations should fail instead of creating an empty default by accident.
		ScoringConfigGroup scoringConfigGroup = new ScoringConfigGroup();

		Assertions.assertThrows(RuntimeException.class,
			() -> scoringConfigGroup.setScoringParametersAsDefaultSubpopulation("missing"));
	}

	@Test
	void testSetScoringParametersAsDefaultSubpopulationFailsIfDefaultAlreadyExists() {
		// An already explicit default should not be overwritten by another subpopulation.
		ScoringConfigGroup scoringConfigGroup = new ScoringConfigGroup();

		scoringConfigGroup.getOrCreateScoringParameters("freight");
		scoringConfigGroup.setScoringParametersAsDefaultSubpopulation("freight");
		scoringConfigGroup.getOrCreateScoringParameters("person");

		Assertions.assertThrows(RuntimeException.class,
			() -> scoringConfigGroup.setScoringParametersAsDefaultSubpopulation("person"));
	}

	 @Test
	 void testIODifferentVersions() {
		final ScoringConfigGroup initialGroup = createTestConfigGroup();

		final String v1path = utils.getOutputDirectory() + "/configv1_out.xml";
		final Config configV1 = new Config();
		configV1.addModule(toUnderscoredModule(initialGroup));

		new ConfigWriter( configV1 ).writeFileV1( v1path );

		final Config configV1In = ConfigUtils.createConfig();
		new ConfigReader( configV1In ).readFile( v1path );

		assertIdentical("re-read v1", initialGroup, configV1In.scoring());

		final String v2path = utils.getOutputDirectory() + "/configv2_out.xml";

		new ConfigWriter( configV1In ).writeFileV2( v2path );

		final Config configV2 = ConfigUtils.createConfig();
		new ConfigReader( configV2 ).readFile( v2path );

		assertIdentical("re-read v2", initialGroup, configV2.scoring());
	}

	private void assertIdentical(
			final String msg,
			final ScoringConfigGroup initialGroup,
			final ScoringConfigGroup inputConfigGroup) {
		Assertions.assertEquals(
				initialGroup.getBrainExpBeta(),
				inputConfigGroup.getBrainExpBeta(),
				1e-7,
				"wrong brainExpBeta "+msg);
		Assertions.assertEquals(
				initialGroup.getModeParams().get(TransportMode.bike).getConstant(),
				inputConfigGroup.getModeParams().get(TransportMode.bike).getConstant(),
				1e-7,
				"wrong constantBike "+msg);
		Assertions.assertEquals(
				initialGroup.getModeParams().get(TransportMode.car).getConstant(),
				inputConfigGroup.getModeParams().get(TransportMode.car).getConstant(),
				1e-7,
				"wrong constantCar "+msg);
		Assertions.assertEquals(
				initialGroup.getModeParams().get(TransportMode.other).getConstant(),
				inputConfigGroup.getModeParams().get(TransportMode.other).getConstant(),
				1e-7,
				"wrong constantOther "+msg);
		Assertions.assertEquals(
				initialGroup.getModeParams().get(TransportMode.pt).getConstant(),
				inputConfigGroup.getModeParams().get(TransportMode.pt).getConstant(),
				1e-7,
				"wrong constantPt "+msg);
		Assertions.assertEquals(
				initialGroup.getModeParams().get(TransportMode.walk).getConstant(),
				inputConfigGroup.getModeParams().get(TransportMode.walk).getConstant(),
				1e-7,
				"wrong constantWalk "+msg);
		Assertions.assertEquals(
				initialGroup.getDefaultLateArrival_utils_hr(),
				inputConfigGroup.getDefaultLateArrival_utils_hr(),
				1e-7,
				"wrong lateArrival_utils_hr "+msg );
		Assertions.assertEquals(
				initialGroup.getDefaultEarlyDeparture_utils_hr(),
				inputConfigGroup.getDefaultEarlyDeparture_utils_hr(),
				1e-7,
				"wrong earlyDeparture_utils_hr "+msg );
		Assertions.assertEquals(
				initialGroup.getLearningRate(),
				inputConfigGroup.getLearningRate(),
				1e-7,
				"wrong learningRate "+msg );
		Assertions.assertEquals(
				initialGroup.getDefaultMarginalUtilityOfMoney(),
				inputConfigGroup.getDefaultMarginalUtilityOfMoney() ,
				1e-7,
				"wrong marginalUtilityOfMoney "+msg);
		Assertions.assertEquals(
				initialGroup.getModeParams().get(TransportMode.other).getMarginalUtilityOfDistance(),
				inputConfigGroup.getModeParams().get(TransportMode.other).getMarginalUtilityOfDistance(),
				1e-7,
				"wrong marginalUtlOfDistanceOther "+msg);
		Assertions.assertEquals(
				initialGroup.getModeParams().get(TransportMode.walk).getMarginalUtilityOfDistance(),
				inputConfigGroup.getModeParams().get(TransportMode.walk).getMarginalUtilityOfDistance(),
				1e-7,
				"wrong marginalUtlOfDistanceWalk "+msg);
		Assertions.assertEquals(
				initialGroup.getDefaultMarginalUtlOfWaiting_utils_hr(),
				inputConfigGroup.getDefaultMarginalUtlOfWaiting_utils_hr(),
				1e-7,
				"wrong marginalUtlOfWaiting_utils_hr "+msg );
		Assertions.assertEquals(
				initialGroup.getDefaultMarginalUtlOfWaitingPt_utils_hr(),
				inputConfigGroup.getDefaultMarginalUtlOfWaitingPt_utils_hr(),
				1e-7,
				"wrong marginalUtlOfWaitingPt_utils_hr "+msg );
		Assertions.assertEquals(
				initialGroup.getModeParams().get(TransportMode.car).getMonetaryDistanceRate(),
				inputConfigGroup.getModeParams().get(TransportMode.car).getMonetaryDistanceRate(),
				1e-7,
				"wrong monetaryDistanceCostRateCar "+msg);
		Assertions.assertEquals(
				initialGroup.getModeParams().get(TransportMode.pt).getMonetaryDistanceRate(),
				inputConfigGroup.getModeParams().get(TransportMode.pt).getMonetaryDistanceRate(),
				1e-7,
				"wrong monetaryDistanceCostRatePt "+msg);
		Assertions.assertEquals(
				initialGroup.getPathSizeLogitBeta(),
				inputConfigGroup.getPathSizeLogitBeta(),
				1e-7,
				"wrong pathSizeLogitBeta "+msg );
		Assertions.assertEquals(
				initialGroup.getDefaultPerforming_utils_hr(),
				inputConfigGroup.getDefaultPerforming_utils_hr(),
				1e-7,
				"wrong performing_utils_hr "+msg );
		Assertions.assertEquals(
				initialGroup.getModeParams().get(TransportMode.car).getMarginalUtilityOfTraveling(),
				inputConfigGroup.getModeParams().get(TransportMode.car).getMarginalUtilityOfTraveling(),
				1e-7,
				"wrong traveling_utils_hr "+msg);
		Assertions.assertEquals(
				initialGroup.getModeParams().get(TransportMode.bike).getMarginalUtilityOfTraveling(),
				inputConfigGroup.getModeParams().get(TransportMode.bike).getMarginalUtilityOfTraveling(),
				1e-7,
				"wrong travelingBike_utils_hr "+msg);
		Assertions.assertEquals(
				initialGroup.getModeParams().get(TransportMode.other).getMarginalUtilityOfTraveling(),
				inputConfigGroup.getModeParams().get(TransportMode.other).getMarginalUtilityOfTraveling(),
				1e-7,
				"wrong travelingOther_utils_hr "+msg);
		Assertions.assertEquals(
				initialGroup.getModeParams().get(TransportMode.pt).getMarginalUtilityOfTraveling(),
				inputConfigGroup.getModeParams().get(TransportMode.pt).getMarginalUtilityOfTraveling(),
				1e-7,
				"wrong travelingPt_utils_hr "+msg);
		Assertions.assertEquals(
				initialGroup.getModeParams().get(TransportMode.walk).getMarginalUtilityOfTraveling(),
				inputConfigGroup.getModeParams().get(TransportMode.walk).getMarginalUtilityOfTraveling(),
				1e-7,
				"wrong travelingWalk_utils_hr "+msg);
		Assertions.assertEquals(
				initialGroup.getDefaultUtilityOfLineSwitch(),
				inputConfigGroup.getDefaultUtilityOfLineSwitch(),
				1e-7,
				"wrong utilityOfLineSwitch "+msg );

		for ( ActivityParams initialSettings : initialGroup.getActivityParams() ) {
			final ActivityParams inputSettings =
				inputConfigGroup.getActivityParams(
						initialSettings.getActivityType() );
			Assertions.assertEquals(
					initialSettings.getActivityType(),
					inputSettings.getActivityType(),
					"wrong type "+msg );
			Assertions.assertEquals(
					initialSettings.getClosingTime(),
					inputSettings.getClosingTime(),
					"wrong closingTime "+msg);
			Assertions.assertEquals(
					initialSettings.getEarliestEndTime(),
					inputSettings.getEarliestEndTime(),
					"wrong earliestEndTime "+msg);
			Assertions.assertEquals(
					initialSettings.getLatestStartTime(),
					inputSettings.getLatestStartTime(),
					"wrong latestStartTime "+msg);
			Assertions.assertEquals(
					initialSettings.getMinimalDuration(),
					inputSettings.getMinimalDuration(),
					"wrong minimalDuration "+msg);
			Assertions.assertEquals(
					initialSettings.getOpeningTime(),
					inputSettings.getOpeningTime(),
					"wrong openingTime "+msg);
			Assertions.assertEquals(
					initialSettings.getPriority(),
					inputSettings.getPriority(),
					1e-7,
					"wrong priority "+msg );
			Assertions.assertEquals(
					initialSettings.getTypicalDuration(),
					inputSettings.getTypicalDuration(),
					"wrong typicalDuration "+msg);
		}

		for ( ModeParams initialSettings : initialGroup.getModeParams().values() ) {
			final String mode = initialSettings.getMode();
			final ModeParams inputSettings = inputConfigGroup.getModeParams().get( mode );
			Assertions.assertEquals(
					initialSettings.getConstant(),
					inputSettings.getConstant(),
					1e-7,
					"wrong constant "+msg );
			Assertions.assertEquals(
					initialSettings.getMarginalUtilityOfDistance(),
					inputSettings.getMarginalUtilityOfDistance(),
					1e-7,
					"wrong marginalUtilityOfDistance "+msg );
			Assertions.assertEquals(
					initialSettings.getMarginalUtilityOfTraveling(),
					inputSettings.getMarginalUtilityOfTraveling(),
					1e-7,
					"wrong marginalUtilityOfTraveling "+msg );
			Assertions.assertEquals(
					initialSettings.getMonetaryDistanceRate(),
					inputSettings.getMonetaryDistanceRate(),
					1e-7,
					"wrong monetaryDistanceRate "+msg );
		}


	}

	private static ConfigGroup toUnderscoredModule(final ScoringConfigGroup initialGroup) {
		final ConfigGroup module = new ConfigGroup( initialGroup.getName() );

		for ( Map.Entry<String, String> e : initialGroup.getParams().entrySet() ) {
			log.info( "add param "+e.getKey() );
			module.addParam( e.getKey() , e.getValue() );
		}

		final Random r = new Random( 456 );
		int ca = 0;
		for ( ActivityParams settings : initialGroup.getActivityParams() ) {
			final String suffix = r.nextBoolean() ? ""+ca++ : settings.getActivityType();

			if ( !suffix.equals( settings.getActivityType() ) ) {
				module.addParam( "activityType_"+suffix , ""+settings.getActivityType() );
			}

			settings.getClosingTime().ifDefined(t -> module.addParam("activityClosingTime_" + suffix, "" + t));
			settings.getEarliestEndTime().ifDefined(t -> module.addParam("activityEarliestEndTime_" + suffix, "" + t));
			settings.getLatestStartTime().ifDefined(t -> module.addParam("activityLatestStartTime_" + suffix, "" + t));
			settings.getMinimalDuration().ifDefined(t -> module.addParam("activityMinimalDuration_" + suffix, "" + t));
			settings.getOpeningTime().ifDefined(t -> module.addParam("activityOpeningTime_" + suffix, "" + t));
			module.addParam("activityPriority_" + suffix, "" + settings.getPriority());
			settings.getTypicalDuration().ifDefined(t -> module.addParam("activityTypicalDuration_" + suffix, "" + t));
		}

		for ( ModeParams settings : initialGroup.getModeParams().values() ) {
			final String mode = settings.getMode();
			module.addParam( "constant_"+mode , ""+settings.getConstant() );
			module.addParam( "marginalUtlOfDistance_"+mode , ""+settings.getMarginalUtilityOfDistance() );
			module.addParam( "traveling_"+mode , ""+settings.getMarginalUtilityOfTraveling() );
			module.addParam( "monetaryDistanceRate_"+mode , ""+settings.getMonetaryDistanceRate() );
		}

		for ( Map.Entry<String, String> params : initialGroup.getScoringParameters( null ).getParams().entrySet() ) {
			if ( params.getKey().equals( "subpopulation" ) ) continue;
			module.addParam( params.getKey() , params.getValue() );
		}

		return module;
	}

	private ScoringConfigGroup createTestConfigGroup() {
		final ScoringConfigGroup group = new ScoringConfigGroup();

		group.setBrainExpBeta( 124);
		group.getModeParams().get(TransportMode.bike).setConstant((double) 98);
		group.getModeParams().get(TransportMode.car).setConstant((double) 345);
		group.getModeParams().get(TransportMode.other).setConstant((double) 345);
		group.getModeParams().get(TransportMode.pt).setConstant((double) 983);
		group.getModeParams().get(TransportMode.walk).setConstant((double) 89);
		group.setDefaultLateArrival_utils_hr( 345 );
		group.setDefaultEarlyDeparture_utils_hr( 5 );
		group.setLearningRate( 98 );
		group.setDefaultMarginalUtilityOfMoney( 9 );
		group.getModeParams().get(TransportMode.other).setMarginalUtilityOfDistance((double) 23);
		group.getModeParams().get(TransportMode.walk).setMarginalUtilityOfDistance((double) 8675);
		group.setDefaultMarginalUtlOfWaiting_utils_hr( 65798 );
		group.setDefaultMarginalUtlOfWaitingPt_utils_hr( 9867 );
		group.getModeParams().get(TransportMode.car).setMonetaryDistanceRate((double) 240358);
		group.getModeParams().get(TransportMode.pt).setMonetaryDistanceRate((double) 9835);
		group.setPathSizeLogitBeta( 8 );
		group.setDefaultPerforming_utils_hr( 678 );
		group.getModeParams().get(TransportMode.car).setMarginalUtilityOfTraveling((double) 246);
		group.getModeParams().get(TransportMode.bike).setMarginalUtilityOfTraveling((double) 968);
		group.getModeParams().get(TransportMode.other).setMarginalUtilityOfTraveling((double) 206);
		group.getModeParams().get(TransportMode.pt).setMarginalUtilityOfTraveling((double) 957);
		group.getModeParams().get(TransportMode.walk).setMarginalUtilityOfTraveling((double) 983455);
		group.setDefaultUtilityOfLineSwitch( 396 );

		final Random random = new Random( 925 );
		for ( int i=0; i < 10; i++ ) {
			final ActivityParams settings = new ActivityParams();
			settings.setActivityType( "activity-type-"+i );
			settings.setClosingTime( random.nextInt( 24*3600 ) );
			settings.setEarliestEndTime( random.nextInt( 24*3600 ) );
			settings.setLatestStartTime( random.nextInt( 24*3600 ) );
			settings.setMinimalDuration( random.nextInt( 24*3600 ) );
			settings.setOpeningTime( random.nextInt( 24*3600 ) );
			settings.setPriority( random.nextInt( 10 ) );
			settings.setTypicalDuration( random.nextInt( 24*3600 ) );

			group.addActivityParams( settings );
		}

		for ( int i=0; i < 10; i++ ) {
			final ModeParams settings = new ModeParams();
			settings.setMode( "mode-"+i );
			settings.setConstant( random.nextDouble() );
			settings.setMarginalUtilityOfDistance( random.nextDouble() );
			settings.setMarginalUtilityOfTraveling( random.nextDouble() );
			settings.setMonetaryDistanceRate( random.nextDouble() );

			group.addParameterSet( settings );
		}


		return group;
	}
}
