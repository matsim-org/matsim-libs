package org.matsim.core.config.groups;

import java.util.Map;
import java.util.Random;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.testcases.MatsimTestUtils;

public class PlanCalcScoreConfigGroupTest {
	private static final Logger log =
		Logger.getLogger(PlanCalcScoreConfigGroupTest.class);

	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testAddActivityParams() {
		PlanCalcScoreConfigGroup c = new PlanCalcScoreConfigGroup();
		Assert.assertNull(c.getActivityParams("type1"));

//		Assert.assertEquals(0, c.getActivityParams().size());
		Assert.assertEquals(4, c.getActivityParams().size());
		// yyyyyy see PlanCalcScoreConfigGroup, where I am currently interaction activities hard-wired.  kai, feb'16
		
		ActivityParams ap = new ActivityParams("type1");
		c.addActivityParams(ap);
		Assert.assertEquals(ap, c.getActivityParams("type1"));

		//		Assert.assertEquals(1, c.getActivityParams().size());
		Assert.assertEquals(5, c.getActivityParams().size());
		// yyyyyy see PlanCalcScoreConfigGroup, where I am currently adding car interaction activity hard-wired.  kai, feb'16
	}

	@Test
	public void testIODifferentVersions() {
		final PlanCalcScoreConfigGroup initialGroup = createTestConfigGroup();

		final String v1path = utils.getOutputDirectory() + "/configv1_out.xml";
		final Config configV1 = new Config();
		configV1.addModule(toUnderscoredModule(initialGroup));

		new ConfigWriter( configV1 ).writeFileV1( v1path );

		final Config configV1In = ConfigUtils.createConfig();
		new ConfigReader( configV1In ).readFile( v1path );

		assertIdentical("re-read v1", initialGroup, configV1In.planCalcScore());

		final String v2path = utils.getOutputDirectory() + "/configv2_out.xml";

		new ConfigWriter( configV1In ).writeFileV2( v2path );

		final Config configV2 = ConfigUtils.createConfig();
		new ConfigReader( configV2 ).readFile( v2path );

		assertIdentical("re-read v2", initialGroup, configV2.planCalcScore());
	}

	private void assertIdentical(
			final String msg,
			final PlanCalcScoreConfigGroup initialGroup,
			final PlanCalcScoreConfigGroup inputConfigGroup) {
		Assert.assertEquals(
				"wrong brainExpBeta "+msg,
				initialGroup.getBrainExpBeta(),
				inputConfigGroup.getBrainExpBeta() );
		Assert.assertEquals(
				"wrong constantBike "+msg,
				initialGroup.getModes().get(TransportMode.bike).getConstant(),
				inputConfigGroup.getModes().get(TransportMode.bike).getConstant());
		Assert.assertEquals(
				"wrong constantCar "+msg,
				initialGroup.getModes().get(TransportMode.car).getConstant(),
				inputConfigGroup.getModes().get(TransportMode.car).getConstant());
		Assert.assertEquals(
				"wrong constantOther "+msg,
				initialGroup.getModes().get(TransportMode.other).getConstant(),
				inputConfigGroup.getModes().get(TransportMode.other).getConstant());
		Assert.assertEquals(
				"wrong constantPt "+msg,
				initialGroup.getModes().get(TransportMode.pt).getConstant(),
				inputConfigGroup.getModes().get(TransportMode.pt).getConstant());
		Assert.assertEquals(
				"wrong constantWalk "+msg,
				initialGroup.getModes().get(TransportMode.walk).getConstant(),
				inputConfigGroup.getModes().get(TransportMode.walk).getConstant());
		Assert.assertEquals(
				"wrong lateArrival_utils_hr "+msg,
				initialGroup.getLateArrival_utils_hr(),
				inputConfigGroup.getLateArrival_utils_hr() );
		Assert.assertEquals(
				"wrong earlyDeparture_utils_hr "+msg,
				initialGroup.getEarlyDeparture_utils_hr(),
				inputConfigGroup.getEarlyDeparture_utils_hr() );
		Assert.assertEquals(
				"wrong learningRate "+msg,
				initialGroup.getLearningRate(),
				inputConfigGroup.getLearningRate() );
		Assert.assertEquals(
				"wrong marginalUtilityOfMoney "+msg,
				initialGroup.getMarginalUtilityOfMoney(),
				inputConfigGroup.getMarginalUtilityOfMoney() );
		Assert.assertEquals(
				"wrong marginalUtlOfDistanceOther "+msg,
				initialGroup.getModes().get(TransportMode.other).getMarginalUtilityOfDistance(),
				inputConfigGroup.getModes().get(TransportMode.other).getMarginalUtilityOfDistance());
		Assert.assertEquals(
				"wrong marginalUtlOfDistanceWalk "+msg,
				initialGroup.getModes().get(TransportMode.walk).getMarginalUtilityOfDistance(),
				inputConfigGroup.getModes().get(TransportMode.walk).getMarginalUtilityOfDistance());
		Assert.assertEquals(
				"wrong marginalUtlOfWaiting_utils_hr "+msg,
				initialGroup.getMarginalUtlOfWaiting_utils_hr(),
				inputConfigGroup.getMarginalUtlOfWaiting_utils_hr() );
		Assert.assertEquals(
				"wrong marginalUtlOfWaitingPt_utils_hr "+msg,
				initialGroup.getMarginalUtlOfWaitingPt_utils_hr(),
				inputConfigGroup.getMarginalUtlOfWaitingPt_utils_hr() );
		Assert.assertEquals(
				"wrong monetaryDistanceCostRateCar "+msg,
				initialGroup.getModes().get(TransportMode.car).getMonetaryDistanceRate(),
				inputConfigGroup.getModes().get(TransportMode.car).getMonetaryDistanceRate());
		Assert.assertEquals(
				"wrong monetaryDistanceCostRatePt "+msg,
				initialGroup.getModes().get(TransportMode.pt).getMonetaryDistanceRate(),
				inputConfigGroup.getModes().get(TransportMode.pt).getMonetaryDistanceRate());
		Assert.assertEquals(
				"wrong pathSizeLogitBeta "+msg,
				initialGroup.getPathSizeLogitBeta(),
				inputConfigGroup.getPathSizeLogitBeta() );
		Assert.assertEquals(
				"wrong performing_utils_hr "+msg,
				initialGroup.getPerforming_utils_hr(),
				inputConfigGroup.getPerforming_utils_hr() );
		Assert.assertEquals(
				"wrong traveling_utils_hr "+msg,
				initialGroup.getModes().get(TransportMode.car).getMarginalUtilityOfTraveling(),
				inputConfigGroup.getModes().get(TransportMode.car).getMarginalUtilityOfTraveling());
		Assert.assertEquals(
				"wrong travelingBike_utils_hr "+msg,
				initialGroup.getModes().get(TransportMode.bike).getMarginalUtilityOfTraveling(),
				inputConfigGroup.getModes().get(TransportMode.bike).getMarginalUtilityOfTraveling());
		Assert.assertEquals(
				"wrong travelingOther_utils_hr "+msg,
				initialGroup.getModes().get(TransportMode.other).getMarginalUtilityOfTraveling(),
				inputConfigGroup.getModes().get(TransportMode.other).getMarginalUtilityOfTraveling());
		Assert.assertEquals(
				"wrong travelingPt_utils_hr "+msg,
				initialGroup.getModes().get(TransportMode.pt).getMarginalUtilityOfTraveling(),
				inputConfigGroup.getModes().get(TransportMode.pt).getMarginalUtilityOfTraveling());
		Assert.assertEquals(
				"wrong travelingWalk_utils_hr "+msg,
				initialGroup.getModes().get(TransportMode.walk).getMarginalUtilityOfTraveling(),
				inputConfigGroup.getModes().get(TransportMode.walk).getMarginalUtilityOfTraveling());
		Assert.assertEquals(
				"wrong utilityOfLineSwitch "+msg,
				initialGroup.getUtilityOfLineSwitch(),
				inputConfigGroup.getUtilityOfLineSwitch() );

		for ( ActivityParams initialSettings : initialGroup.getActivityParams() ) {
			final ActivityParams inputSettings =
				inputConfigGroup.getActivityParams(
						initialSettings.getActivityType() );
			Assert.assertEquals(
					"wrong type "+msg,
					initialSettings.getActivityType(),
					inputSettings.getActivityType() );
			Assert.assertEquals(
					"wrong closingTime "+msg,
					initialSettings.getClosingTime(),
					inputSettings.getClosingTime() );
			Assert.assertEquals(
					"wrong earliestEndTime "+msg,
					initialSettings.getEarliestEndTime(),
					inputSettings.getEarliestEndTime() );
			Assert.assertEquals(
					"wrong latestStartTime "+msg,
					initialSettings.getLatestStartTime(),
					inputSettings.getLatestStartTime() );
			Assert.assertEquals(
					"wrong minimalDuration "+msg,
					initialSettings.getMinimalDuration(),
					inputSettings.getMinimalDuration() );
			Assert.assertEquals(
					"wrong openingTime "+msg,
					initialSettings.getOpeningTime(),
					inputSettings.getOpeningTime() );
			Assert.assertEquals(
					"wrong priority "+msg,
					initialSettings.getPriority(),
					inputSettings.getPriority() );
			Assert.assertEquals(
					"wrong typicalDuration "+msg,
					initialSettings.getTypicalDuration(),
					inputSettings.getTypicalDuration() );
		}

		for ( ModeParams initialSettings : initialGroup.getModes().values() ) {
			final String mode = initialSettings.getMode();
			final ModeParams inputSettings = inputConfigGroup.getModes().get( mode );
			Assert.assertEquals(
					"wrong constant "+msg,
					initialSettings.getConstant(),
					inputSettings.getConstant() );
			Assert.assertEquals(
					"wrong marginalUtilityOfDistance "+msg,
					initialSettings.getMarginalUtilityOfDistance(),
					inputSettings.getMarginalUtilityOfDistance() );
			Assert.assertEquals(
					"wrong marginalUtilityOfTraveling "+msg,
					initialSettings.getMarginalUtilityOfTraveling(),
					inputSettings.getMarginalUtilityOfTraveling() );
			Assert.assertEquals(
					"wrong monetaryDistanceRate "+msg,
					initialSettings.getMonetaryDistanceRate(),
					inputSettings.getMonetaryDistanceRate() );
		}


	}

	private static ConfigGroup toUnderscoredModule(final PlanCalcScoreConfigGroup initialGroup) {
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
			module.addParam( "activityClosingTime_"+suffix , ""+settings.getClosingTime() );
			module.addParam( "activityEarliestEndTime_"+suffix , ""+settings.getEarliestEndTime() );
			module.addParam( "activityLatestStartTime_"+suffix , ""+settings.getLatestStartTime() );
			module.addParam( "activityMinimalDuration_"+suffix , ""+settings.getMinimalDuration() );
			module.addParam( "activityOpeningTime_"+suffix , ""+settings.getOpeningTime() );
			module.addParam( "activityPriority_"+suffix , ""+settings.getPriority() );
			module.addParam( "activityTypicalDuration_"+suffix , ""+settings.getTypicalDuration() );
		}

		for ( ModeParams settings : initialGroup.getModes().values() ) {
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

	private PlanCalcScoreConfigGroup createTestConfigGroup() {
		final PlanCalcScoreConfigGroup group = new PlanCalcScoreConfigGroup();

		group.setBrainExpBeta( 124);
		group.getModes().get(TransportMode.bike).setConstant((double) 98);
		group.getModes().get(TransportMode.car).setConstant((double) 345);
		group.getModes().get(TransportMode.other).setConstant((double) 345);
		group.getModes().get(TransportMode.pt).setConstant((double) 983);
		group.getModes().get(TransportMode.walk).setConstant((double) 89);
		group.setLateArrival_utils_hr( 345 );
		group.setEarlyDeparture_utils_hr( 5 );
		group.setLearningRate( 98 );
		group.setMarginalUtilityOfMoney( 9 );
		group.getModes().get(TransportMode.other).setMarginalUtilityOfDistance((double) 23);
		group.getModes().get(TransportMode.walk).setMarginalUtilityOfDistance((double) 8675);
		group.setMarginalUtlOfWaiting_utils_hr( 65798 );
		group.setMarginalUtlOfWaitingPt_utils_hr( 9867 );
		group.getModes().get(TransportMode.car).setMonetaryDistanceRate((double) 240358);
		group.getModes().get(TransportMode.pt).setMonetaryDistanceRate((double) 9835);
		group.setPathSizeLogitBeta( 8 );
		group.setPerforming_utils_hr( 678 );
		group.getModes().get(TransportMode.car).setMarginalUtilityOfTraveling((double) 246);
		group.getModes().get(TransportMode.bike).setMarginalUtilityOfTraveling((double) 968);
		group.getModes().get(TransportMode.other).setMarginalUtilityOfTraveling((double) 206);
		group.getModes().get(TransportMode.pt).setMarginalUtilityOfTraveling((double) 957);
		group.getModes().get(TransportMode.walk).setMarginalUtilityOfTraveling((double) 983455);
		group.setUtilityOfLineSwitch( 396 );

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
