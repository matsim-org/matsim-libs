/* *********************************************************************** *
 * project: org.matsim.*
 * CharyparNagelScoringFunctionTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.core.scoring.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.PersonScoreEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.ScoringConfigGroup.TypicalDurationScoreComputation;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.functions.ActivityUtilityParameters.ZeroUtilityComputation;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.vehicles.Vehicle;

/**
 * Test the correct working of the CharyparNagelScoringFunction according to the formulas in:
 * <blockquote>
 *  <p>Charypar, D. und K. Nagel (2005) <br>
 *  Generating complete all-day activity plans with genetic algorithms,<br>
 *  Transportation, 32 (4) 369-397.</p>
 * </blockquote>
 * TODO dg march 09: when walk mode is tested add a walk mode leg and modify at least testMarginalUtilityOfDistance
 *
 * TODO [MR] split this into multiple test classes for the specific parts, according to the newer, more modular scoring function
 * @author mrieser
 */

public class CharyparNagelScoringFunctionTest {

	private static final double EPSILON =1e-9;

	private ScoringFunction getScoringFunctionInstance(final Fixture f, final Person person) {
		CharyparNagelScoringFunctionFactory charyparNagelScoringFunctionFactory =
				new CharyparNagelScoringFunctionFactory( f.scenario );
		return charyparNagelScoringFunctionFactory.createNewScoringFunction(person);
	}

	private double calcScore(final Fixture f) {
		CharyparNagelScoringFunctionFactory charyparNagelScoringFunctionFactory =
				new CharyparNagelScoringFunctionFactory(
						f.scenario );
		ScoringFunction testee = charyparNagelScoringFunctionFactory.createNewScoringFunction(PopulationUtils.getFactory().createPerson(Id.create("1", Person.class)));
		for (PlanElement planElement : f.plan.getPlanElements()) {
			if (planElement instanceof Activity) {
				testee.handleActivity((Activity) planElement);
			} else if (planElement instanceof Leg) {
				testee.handleLeg((Leg) planElement);
			}
		}
		testee.finish();
		return testee.getScore();
	}

	/**
	 * The reference implementation to calculate the zero utility duration, the duration of
	 * an activity at which its utility is zero.
	 *
	 * @param typicalDuration_hrs The typical duration of the activity in hours
	 * @param priority
	 * @return the duration (in hours) at which the activity has a utility of 0.
	 */
	private double getZeroUtilDuration_hrs(final double typicalDuration_hrs, final double priority, TypicalDurationScoreComputation typicalDurationComputation) {
		// yy could/should use static function from CharyparNagelScoringUtils. kai, nov'13

		if(typicalDurationComputation.equals(TypicalDurationScoreComputation.uniform)){
			return typicalDuration_hrs * Math.exp(-10.0 / typicalDuration_hrs / priority);
		} else {
			return typicalDuration_hrs * Math.exp( -1.0 / priority );
		}
	}

	/**
	 * Test the calculation of the zero-utility-duration.
	 */
	@ParameterizedTest
	@EnumSource(TypicalDurationScoreComputation.class)
	void testZeroUtilityDuration(TypicalDurationScoreComputation typicalDurationComputation) {
		double zeroUtilDurW = getZeroUtilDuration_hrs(8.0, 1.0, typicalDurationComputation);
		double zeroUtilDurH = getZeroUtilDuration_hrs(16.0, 1.0, typicalDurationComputation);
		double zeroUtilDurW2 = getZeroUtilDuration_hrs(8.0, 2.0, typicalDurationComputation);

		ZeroUtilityComputation computation;
		if(typicalDurationComputation.equals(TypicalDurationScoreComputation.uniform)){
			computation = new ActivityUtilityParameters.SameAbsoluteScore();
		} else {
			computation = new ActivityUtilityParameters.SameRelativeScore();
		}


		{
			ActivityUtilityParameters.Builder factory = new ActivityUtilityParameters.Builder();
			factory.setType("w");
			factory.setPriority(1.0);
			factory.setTypicalDuration_s(8.0 * 3600);
			factory.setZeroUtilityComputation(computation);
			ActivityUtilityParameters params = factory.build();
			assertEquals(zeroUtilDurW, params.getZeroUtilityDuration_h(), EPSILON);

		}

		{
			ActivityUtilityParameters.Builder factory = new ActivityUtilityParameters.Builder();
			factory.setType("h");
			factory.setPriority(1.0);
			factory.setTypicalDuration_s(16.0 * 3600);
			factory.setZeroUtilityComputation(computation);
			ActivityUtilityParameters params = factory.build();
			assertEquals(zeroUtilDurH, params.getZeroUtilityDuration_h(), EPSILON);
		}

		{
			ActivityUtilityParameters.Builder factory = new ActivityUtilityParameters.Builder();
			factory.setType("w2");
			// test that the priority is respected as well
			factory.setPriority(2.0);
			factory.setTypicalDuration_s(8.0 * 3600);
			factory.setZeroUtilityComputation(computation);
			ActivityUtilityParameters params = factory.build();
			assertEquals(zeroUtilDurW2, params.getZeroUtilityDuration_h(), EPSILON);
		}
	}

	/**
	 * Test the scoring function when all parameters are set to 0.
	 */
	@Test
	void testZero() {
		Fixture f = new Fixture();
		assertEquals(0.0, calcScore(f), EPSILON);
	}

	@Test
	void testTravelingAndConstantCar() {
		Fixture f = new Fixture();
		final double traveling = -6.0;
		f.config.scoring().getModes().get(TransportMode.car).setMarginalUtilityOfTraveling(traveling);
		assertEquals(-3.0, calcScore(f), EPSILON);
		double constantCar = -6.0;
		f.config.scoring().getModes().get(TransportMode.car).setConstant(constantCar);
		assertEquals(-9.0, calcScore(f), EPSILON);
	}

	@Test
	void testTravelingPtAndConstantPt() {
		Fixture f = new Fixture();
		final double travelingPt = -9.0;
		f.config.scoring().getModes().get(TransportMode.pt).setMarginalUtilityOfTraveling(travelingPt);
		assertEquals(-2.25, calcScore(f), EPSILON);
		double constantPt = -3.0;
		f.config.scoring().getModes().get(TransportMode.pt).setConstant(constantPt);
		assertEquals(-5.25, calcScore(f), EPSILON);
	}

	@Test
	void testTravelingWalkAndConstantWalk() {
		Fixture f = new Fixture();
		final double travelingWalk = -18.0;
		f.config.scoring().getModes().get(TransportMode.walk).setMarginalUtilityOfTraveling(travelingWalk);
		assertEquals(-9.0, calcScore(f), EPSILON ) ;
		double constantWalk = -1.0;
		f.config.scoring().getModes().get(TransportMode.walk).setConstant(constantWalk);
		assertEquals(-10.0, calcScore(f), EPSILON);
	}

	@Test
	void testTravelingBikeAndConstantBike(){
		Fixture f = new Fixture();
		final double travelingBike = -6.0;
		f.config.scoring().getModes().get(TransportMode.bike).setMarginalUtilityOfTraveling(travelingBike);
		assertEquals(-1.5, calcScore(f), EPSILON ) ;
		double constantBike = -2.0;
		f.config.scoring().getModes().get(TransportMode.bike).setConstant(constantBike);
		assertEquals(-3.5, calcScore(f), EPSILON);
	}

	/**
	 * Test the performing part of the scoring function.
	 */
	@ParameterizedTest
	@EnumSource(TypicalDurationScoreComputation.class)
	void testPerforming(TypicalDurationScoreComputation typicalDurationComputation) {
		Fixture f = new Fixture();

		double perf = +6.0;
		double zeroUtilDurW = getZeroUtilDuration_hrs(3.0, 1.0, typicalDurationComputation);
		double zeroUtilDurH = getZeroUtilDuration_hrs(15.0, 1.0, typicalDurationComputation);

		f.config.scoring().setPerforming_utils_hr(perf);

		if(typicalDurationComputation.equals(TypicalDurationScoreComputation.uniform)){
			for(ActivityParams p : f.config.scoring().getActivityParams()){
				p.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.uniform);
			}
		}

		assertEquals(perf * 3.0 * Math.log(2.5 / zeroUtilDurW)
				+ perf * 3.0 * Math.log(2.75/zeroUtilDurW)
				+ perf * 3.0 * Math.log(2.5/zeroUtilDurW)
				+ perf * 15.0 * Math.log(14.75 / zeroUtilDurH), calcScore(f), EPSILON);

		//		perf = +3.0;
		//		f.config.planCalcScore().setPerforming_utils_hr(perf);
		//		assertEquals(perf * 3.0 * Math.log(2.5 / zeroUtilDurW)
		//				+ perf * 3.0 * Math.log(2.75/zeroUtilDurW)
		//				+ perf * 3.0 * Math.log(2.5/zeroUtilDurW)
		//				+ perf * 15.0 * Math.log(14.75 / zeroUtilDurH), calcScore(f), EPSILON);
	}

	/**
	 * Test the performing part of the scoring function when an activity has an OpeningTime set.
	 */
	@Test
	void testOpeningTime() {
		Fixture f = new Fixture();
		double perf = +6.0;
		f.config.scoring().setPerforming_utils_hr(perf);
		double initialScore = calcScore(f);

		ActivityParams wParams = f.config.scoring().getActivityParams("w");
		wParams.setOpeningTime(8*3600.0); // now the agent arrives 30min early to the FIRST work activity and has to wait
		double score = calcScore(f);

		// check the difference between 2.5 and 2.0 hours of working
		assertEquals(perf * 3.0 * Math.log(2.5 / 2.0), initialScore - score, EPSILON);
	}

	/**
	 * Test the performing part of the scoring function when an activity has a ClosingTime set.
	 */
	@Test
	void testClosingTime() {
		Fixture f = new Fixture();
		double perf = +6.0;
		f.config.scoring().setPerforming_utils_hr(perf);
		double initialScore = calcScore(f);

		ActivityParams wParams = f.config.scoring().getActivityParams("w");
		wParams.setClosingTime(15*3600.0); // now the agent stays 1h too long at the LAST work activity
		double score = calcScore(f);

		// check the difference between 2.5 and 1.5 hours working
		assertEquals(perf * 3.0 * Math.log(2.5 / 1.5), initialScore - score, EPSILON);
	}

	/**
	 * Test the performing part of the scoring function when an activity has OpeningTime and ClosingTime set.
	 */
	@ParameterizedTest
	@EnumSource(TypicalDurationScoreComputation.class)
	void testOpeningClosingTime(TypicalDurationScoreComputation typicalDurationComputation) {
		Fixture f = new Fixture();
		double perf_hrs = +6.0;
		f.config.scoring().setPerforming_utils_hr(perf_hrs);

		if(typicalDurationComputation.equals(TypicalDurationScoreComputation.uniform)){
			for(ActivityParams p : f.config.scoring().getActivityParams()){
				p.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.uniform);
			}
		}

		double initialScore = calcScore(f);

		// test1: agents has to wait before and after

		ActivityParams wParams = f.config.scoring().getActivityParams("w");
		wParams.setOpeningTime( 8*3600.0); // the agent arrives 30min early
		wParams.setClosingTime(15*3600.0); // the agent stays 1h too long
		double score = calcScore(f);

		// check the differences for all work activities
		assertEquals(perf_hrs * 3.0 * Math.log(2.5 / 2.0)
				+ perf_hrs * 3.0 * Math.log(2.75 / 2.75)
				+ perf_hrs * 3.0 * Math.log(2.5 / 1.5)
				, initialScore - score, EPSILON);

		// test 2: agents has to wait all the time, because work place opens later

		wParams.setOpeningTime(20*3600.0);
		wParams.setClosingTime(21*3600.0);

		//		// only the home-activity should add to the score
		//		assertEquals(perf * 15.0 * Math.log(14.75 / zeroUtilDurH), calcScore(f), EPSILON);
		// not longer true, since not doing a scheduled activity now carries a penalty.  kai, nov'13
		double score_home = perf_hrs * 15.0 * Math.log(14.75 / getZeroUtilDuration_hrs(15.0, 1.0, typicalDurationComputation)) ;

		final double typicalDuration_work_sec = wParams.getTypicalDuration().seconds();
		final double zeroUtilityDuration_work_sec = 3600. * getZeroUtilDuration_hrs(typicalDuration_work_sec/3600., 1., typicalDurationComputation );
		double slope_work_at_zero_utility_h = perf_hrs * typicalDuration_work_sec / zeroUtilityDuration_work_sec ;
		double score_work = - zeroUtilityDuration_work_sec * slope_work_at_zero_utility_h / 3600. ;
		assertEquals( score_home+3.*score_work  , calcScore(f), EPSILON ) ;

		// test 3: agents has to wait all the time, because work place opened earlier

		wParams.setOpeningTime(1*3600.0);
		wParams.setClosingTime(2*3600.0);

		// only the home-activity should add to the score
		assertEquals(score_home+3.*score_work , calcScore(f), EPSILON);

		// test 4: work opens and closes at same time but while agent is there
		// (this may be useful to emulate that the activity is never open ... such as pt interaction)

		wParams.setOpeningTime(8.*3600.0 + 15.*60. );
		wParams.setClosingTime (8.*3600.0 + 15.*60. );
		// (note that even _some_ opening time causes zero score, since the minDuration needs to be overcome!)

		assertEquals(score_home+3.*score_work , calcScore(f), EPSILON);
	}

	/**
	 * Test the waiting part of the scoring function.
	 */
	@Test
	void testWaitingTime() {
		Fixture f = new Fixture();
		double waiting = -10.0;
		f.config.scoring().setMarginalUtlOfWaiting_utils_hr(waiting);

		ActivityParams wParams = f.config.scoring().getActivityParams("w");
		wParams.setOpeningTime( 8*3600.0); // the agent arrives 30min early
		wParams.setClosingTime(15*3600.0); // the agent stays 1h too long

		// the agent spends 1.5h waiting at the work place
		assertEquals(waiting * 1.5, calcScore(f), EPSILON);
	}

	/**
	 * Test the scoring function in regards to early departures.
	 */
	@Test
	void testEarlyDeparture() {
		Fixture f = new Fixture();
		double disutility = -10.0;
		f.config.scoring().setEarlyDeparture_utils_hr(disutility);

		ActivityParams wParams = f.config.scoring().getActivityParams("w");
		wParams.setEarliestEndTime(10.75 * 3600.0); // require the agent to work until 16:45

		// the agent left 45mins too early
		assertEquals(disutility * 0.75, calcScore(f), EPSILON);
	}

	/**
	 * Test the scoring function in regards to early departures.
	 */
	@Test
	void testMinimumDuration() {
		Fixture f = new Fixture();
		double disutility = -10.0;
		f.config.scoring().setEarlyDeparture_utils_hr(disutility);

		ActivityParams wParams = f.config.scoring().getActivityParams("w");
		wParams.setMinimalDuration(3 * 3600.0); // require the agent to be 3 hours at every working activity

		// the agent overall works 1.25h too short
		assertEquals(disutility * 1.25, calcScore(f), EPSILON);
	}

	/**
	 * Test the scoring function in regards to late arrival.
	 */
	@Test
	void testLateArrival() {
		Fixture f = new Fixture();
		double disutility = -10.0;
		f.config.scoring().setLateArrival_utils_hr(disutility);

		ActivityParams wParams = f.config.scoring().getActivityParams("w");
		wParams.setLatestStartTime(13 * 3600.0); // agent should start working latest at 13 o'clock

		// the agent arrived 30mins late
		assertEquals(disutility * 0.5, calcScore(f), EPSILON);
	}

	/**
	 * Test that the stuck penalty is correctly computed. It should be the worst (dis)utility the agent
	 * could gain.
	 */
	@Test
	void testStuckPenalty() {
		Fixture f = new Fixture();
		// test 1 where late arrival has the biggest impact
		f.config.scoring().setLateArrival_utils_hr(-18.0);
		final double traveling1 = -6.0;
		f.config.scoring().getModes().get(TransportMode.car).setMarginalUtilityOfTraveling(traveling1);

		ScoringFunction testee = getScoringFunctionInstance(f, f.person);
		testee.handleActivity((Activity) f.plan.getPlanElements().get(0));
		testee.handleLeg((Leg) f.plan.getPlanElements().get(1));
		testee.handleActivity((Activity) f.plan.getPlanElements().get(2));
		testee.handleLeg((Leg) f.plan.getPlanElements().get(3));

		testee.agentStuck(16*3600 + 7.5*60);
		testee.finish();
		testee.getScore();

		assertEquals(24 * -18.0 - 6.0 * 0.50, testee.getScore(), EPSILON); // stuck penalty + 30min traveling

		// test 2 where traveling has the biggest impact
		f.config.scoring().setLateArrival_utils_hr(-3.0);
		final double traveling = -6.0;
		f.config.scoring().getModes().get(TransportMode.car).setMarginalUtilityOfTraveling(traveling);

		testee = getScoringFunctionInstance(f, f.person);
		testee.handleActivity((Activity) f.plan.getPlanElements().get(0));
		testee.handleLeg((Leg) f.plan.getPlanElements().get(1));
		testee.handleActivity((Activity) f.plan.getPlanElements().get(2));
		testee.handleLeg((Leg) f.plan.getPlanElements().get(3));
		testee.agentStuck(16*3600 + 7.5*60);
		testee.finish();
		testee.getScore();

		assertEquals(24 * -6.0 - 6.0 * 0.50, testee.getScore(), EPSILON); // stuck penalty + 30min traveling
	}

	@Test
	void testDistanceCostScoringCar() {
		Fixture f = new Fixture();
		// test 1 where marginalUtitityOfMoney is fixed to 1.0
		f.config.scoring().setMarginalUtilityOfMoney(1.0);
		//		this.config.charyparNagelScoring().setMarginalUtlOfDistanceCar(-0.00001);
		double monetaryDistanceRateCar1 = -0.00001;
		f.config.scoring().getModes().get(TransportMode.car).setMonetaryDistanceRate(monetaryDistanceRateCar1);

		assertEquals(-0.25, calcScore(f), EPSILON);

		// test 2 where MonetaryDistanceCostRate is fixed to -1.0
		double monetaryDistanceRateCar = -1.0;
		f.config.scoring().getModes().get(TransportMode.car).setMonetaryDistanceRate(monetaryDistanceRateCar);
		f.config.scoring().setMarginalUtilityOfMoney(0.5);

		assertEquals(-12500.0, calcScore(f), EPSILON);
	}

	@Test
	void testDistanceCostScoringPt() {
		Fixture f = new Fixture();
		// test 1 where marginalUtitityOfMoney is fixed to 1.0
		f.config.scoring().setMarginalUtilityOfMoney(1.0);
		//		this.config.charyparNagelScoring().setMarginalUtlOfDistancePt(-0.00001);
		double monetaryDistanceRatePt1 = -0.00001;
		f.config.scoring().getModes().get(TransportMode.pt).setMonetaryDistanceRate(monetaryDistanceRatePt1);

		assertEquals(-0.20, calcScore(f), EPSILON);

		// test 2 where MonetaryDistanceCostRate is fixed to -1.0
		double monetaryDistanceRatePt = -1.0;
		f.config.scoring().getModes().get(TransportMode.pt).setMonetaryDistanceRate(monetaryDistanceRatePt);
		f.config.scoring().setMarginalUtilityOfMoney(0.5);

		assertEquals(-10000.0, calcScore(f), EPSILON);
	}

	/**
	 * Test how the scoring function reacts when the first and the last activity do not have the same act-type.
	 */
	@ParameterizedTest
	@EnumSource(TypicalDurationScoreComputation.class)
	void testDifferentFirstLastAct(TypicalDurationScoreComputation typicalDurationComputation) {
		Fixture f = new Fixture();
		// change the last act to something different than the first act
		((Activity) f.plan.getPlanElements().get(8)).setType("h2");

		ScoringConfigGroup.ActivityParams params = new ScoringConfigGroup.ActivityParams("h2");
		params.setTypicalDuration(8*3600);

		f.config.scoring().addActivityParams(params);
		f.config.scoring().getActivityParams("h").setTypicalDuration(6.0 * 3600);

		if(typicalDurationComputation.equals(TypicalDurationScoreComputation.uniform)){
			for(ActivityParams p : f.config.scoring().getActivityParams()){
				p.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.uniform);
			}
		}

		double perf = +6.0;
		f.config.scoring().setPerforming_utils_hr(perf);
		double zeroUtilDurW = getZeroUtilDuration_hrs(3.0, 1.0, typicalDurationComputation);
		double zeroUtilDurH = getZeroUtilDuration_hrs(6.0, 1.0, typicalDurationComputation);
		double zeroUtilDurH2 = getZeroUtilDuration_hrs(8.0, 1.0, typicalDurationComputation);

		assertEquals(perf * 3.0 * Math.log(2.5 / zeroUtilDurW)
				+ perf * 3.0 * Math.log(2.75/zeroUtilDurW)
				+ perf * 3.0 * Math.log(2.5/zeroUtilDurW)
				+ perf * 6.0 * Math.log(7.0 / zeroUtilDurH)
				+ perf * 8.0 * Math.log(7.75 / zeroUtilDurH2), calcScore(f), EPSILON);
	}

	/**
	 * Test that the scoring function works even when we don't spend the night at home, but
	 * don't end the day with an ongoing activity at all. This is half of the case
	 * when the first and last activity aren't the same.
	 */
	@ParameterizedTest
	@EnumSource(TypicalDurationScoreComputation.class)
	void testNoNightActivity(TypicalDurationScoreComputation typicalDurationComputation) {

		double zeroUtilDurW = getZeroUtilDuration_hrs(3.0, 1.0, typicalDurationComputation);
		double zeroUtilDurH = getZeroUtilDuration_hrs(7.0, 1.0, typicalDurationComputation);
		double perf = +3.0;

		Fixture f = new Fixture();
		// Need to change the typical duration of the home activity
		// for this test, since with the setting of 15 hours for "home",
		// this would amount to a smaller-than-zero expected contribution of
		// the home activity at 7 hours, and smaller-than-zero contributions
		// are truncated, so we wouldn't test anything. :-/
		f.config.scoring().getActivityParams("h").setTypicalDuration(7.0 * 3600);
		f.config.scoring().setPerforming_utils_hr(perf);

		if(typicalDurationComputation.equals(TypicalDurationScoreComputation.uniform)){
			for(ActivityParams p : f.config.scoring().getActivityParams()){
				p.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.uniform);
			}
		}

		ScoringFunction testee = getScoringFunctionInstance(f, f.person);
		testee.handleActivity((Activity) f.plan.getPlanElements().get(0));
		testee.handleLeg((Leg) f.plan.getPlanElements().get(1));
		testee.handleActivity((Activity) f.plan.getPlanElements().get(2));
		testee.finish();

		assertEquals(
				perf * 3.0 * Math.log(2.5 / zeroUtilDurW) +
				perf * 7.0 * Math.log(7.0 / zeroUtilDurH),
				testee.getScore(), EPSILON);
	}

	/**
	 * Tests if the scoring function correctly handles {@link PersonMoneyEvent}.
	 * It generates one person with one plan having two activities (home, work)
	 * and a car-leg in between. It then tests the scoring function by calling
	 * several methods on an instance of the scoring function with the
	 * aforementioned plan.
	 */
	@Test
	void testAddMoney() {
		Fixture f = new Fixture();

		// score the same plan twice
		Person person1 = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
		Plan plan1 = PersonUtils.createAndAddPlan(person1, true);
		Activity act1a = PopulationUtils.createAndAddActivityFromLinkId(plan1, "home", (Id<Link>)null);//, 0, 7.0*3600, 7*3600, false);
		act1a.setEndTime(f.secondLegStartTime);
		Leg leg1 = PopulationUtils.createAndAddLeg( plan1, TransportMode.car );//, 7*3600, 100, 7*3600+100);
		leg1.setDepartureTime(f.secondLegStartTime);
		leg1.setTravelTime(f.secondLegTravelTime);
		Route route2 = RouteUtils.createGenericRouteImpl(null, null);
		leg1.setRoute(route2);
		route2.setDistance(20000.0);
		Activity act1b = PopulationUtils.createAndAddActivityFromLinkId(plan1, "work", (Id<Link>)null);//, 7.0*3600+100, Time.getUndefinedTime(), Time.getUndefinedTime(), false);
		act1b.setStartTime(f.secondLegStartTime + f.secondLegTravelTime);
		ScoringFunction sf1 = getScoringFunctionInstance(f, person1);
		sf1.handleActivity(act1a);
		sf1.handleLeg(leg1);
		sf1.handleActivity(act1b);

		sf1.finish();
		double score1 = sf1.getScore();

		ScoringFunction sf2 = getScoringFunctionInstance(f, person1);
		sf2.handleActivity(act1a);
		sf2.addMoney(1.23);
		sf2.handleLeg(leg1);
		sf2.addMoney(-2.46);
		sf2.handleActivity(act1b);
		sf2.addMoney(4.86);
		sf2.addMoney(-0.28);
		sf2.finish();
		double score2 = sf2.getScore();

		assertEquals(1.23 - 2.46 + 4.86 - 0.28, score2 - score1, EPSILON);
	}

	/**
	 * Tests if the scoring function correctly handles {@link PersonScoreEvent}.
	 */
	@Test
	void testAddScore() {
		Fixture f = new Fixture();

		// score the same plan twice
		Person person1 = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
		Plan plan1 = PersonUtils.createAndAddPlan(person1, true);
		Activity act1a = PopulationUtils.createAndAddActivityFromLinkId(plan1, "home", (Id<Link>)null);//, 0, 7.0*3600, 7*3600, false);
		act1a.setEndTime(f.secondLegStartTime);
		Leg leg1 = PopulationUtils.createAndAddLeg( plan1, TransportMode.car );//, 7*3600, 100, 7*3600+100);
		leg1.setDepartureTime(f.secondLegStartTime);
		leg1.setTravelTime(f.secondLegTravelTime);
		Route route2 = RouteUtils.createGenericRouteImpl(null, null);
		leg1.setRoute(route2);
		route2.setDistance(20000.0);
		Activity act1b = PopulationUtils.createAndAddActivityFromLinkId(plan1, "work", (Id<Link>)null);//, 7.0*3600+100, Time.getUndefinedTime(), Time.getUndefinedTime(), false);
		act1b.setStartTime(f.secondLegStartTime + f.secondLegTravelTime);
		ScoringFunction sf1 = getScoringFunctionInstance(f, person1);
		sf1.handleActivity(act1a);
		sf1.handleLeg(leg1);
		sf1.handleActivity(act1b);

		sf1.finish();
		double score1 = sf1.getScore();

		ScoringFunction sf2 = getScoringFunctionInstance(f, person1);
		sf2.handleActivity(act1a);
		sf2.addScore(1.23);
		sf2.handleLeg(leg1);
		sf2.addScore(-2.46);
		sf2.handleActivity(act1b);
		sf2.addScore(4.86);
		sf2.addScore(-0.28);
		sf2.finish();
		double score2 = sf2.getScore();

		assertEquals(1.23 - 2.46 + 4.86 - 0.28, score2 - score1, EPSILON);
	}

	@Test
	void testUnusualMode() {
		Fixture f = new Fixture();
		Leg leg = (Leg) f.plan.getPlanElements().get(1);
		leg.setMode("sackhuepfen");
		boolean exception = false ;
		try {
			assertEquals(-3.0, calcScore(f), EPSILON); // default for unknown modes
			// no longer allowed.  kai, may'17
		} catch ( Exception ee ) {
			// this is expected
			exception = true ;
		}
		assertTrue( exception ) ;
		f.config.scoring().addParam("traveling_sackhuepfen", "-30.0");
		assertEquals(-15.0, calcScore(f), EPSILON);
	}


	private static class Fixture {
		protected Config config = null;
		private Person person = null;
		private Plan plan = null;
		private Scenario scenario;
		private Network network;
		private int firstLegStartTime;
		private int firstLegTravelTime;
		private int thirdLegTravelTime;
		private int fourthLegTravelTime;
		private int secondLegTravelTime;
		private int secondLegStartTime;
		private int thirdLegStartTime;
		private int fourthLegStartTime;

		public Fixture() {
			firstLegStartTime = 7 * 3600;
			firstLegTravelTime = 30 * 60;
			thirdLegTravelTime = 30 * 60;
			secondLegStartTime = 10 * 3600;
			secondLegTravelTime = 15 * 60;
			thirdLegStartTime = 13 * 3600;
			fourthLegStartTime = 16 * 3600;
			fourthLegTravelTime = 15 * 60;
			// home act end 7am
			// work 7:30 to 10:00
			// work 10:15 to 13:00
			// work 13:30 to 16:00
			// home 15:15 to ...

			this.config = ConfigUtils.createConfig();
			ScoringConfigGroup scoring = this.config.scoring();
			scoring.setBrainExpBeta(2.0);

			scoring.getModes().get(TransportMode.car).setConstant(0.0);
			scoring.getModes().get(TransportMode.pt).setConstant(0.0);
			scoring.getModes().get(TransportMode.walk).setConstant(0.0);
			scoring.getModes().get(TransportMode.bike).setConstant(0.0);

			scoring.setEarlyDeparture_utils_hr(0.0);
			scoring.setLateArrival_utils_hr(0.0);
			scoring.setMarginalUtlOfWaiting_utils_hr(0.0);
			scoring.setPerforming_utils_hr(0.0);
			scoring.getModes().get(TransportMode.car).setMarginalUtilityOfTraveling(0.0);
			scoring.getModes().get(TransportMode.pt).setMarginalUtilityOfTraveling(0.0);
			scoring.getModes().get(TransportMode.walk).setMarginalUtilityOfTraveling(0.0);
			scoring.getModes().get(TransportMode.bike).setMarginalUtilityOfTraveling(0.0);

			scoring.setMarginalUtilityOfMoney(1.) ;
			scoring.getModes().get(TransportMode.car).setMonetaryDistanceRate(0.0);
			scoring.getModes().get(TransportMode.pt).setMonetaryDistanceRate(0.0);


			// setup activity types h and w for scoring
			ScoringConfigGroup.ActivityParams params = new ScoringConfigGroup.ActivityParams("h");
			params.setTypicalDuration(15*3600);
			scoring.addActivityParams(params);


			params = new ScoringConfigGroup.ActivityParams("w");
			params.setTypicalDuration(3*3600);
			scoring.addActivityParams(params);

			this.scenario = ScenarioUtils.createScenario(config);
			this.network = (Network) this.scenario.getNetwork();
			Node node1 = NetworkUtils.createAndAddNode(this.network, Id.create("1", Node.class), new Coord(0.0, 0.0));
			Node node2 = NetworkUtils.createAndAddNode(this.network, Id.create("2", Node.class), new Coord(500.0, 0.0));
			Node node3 = NetworkUtils.createAndAddNode(this.network, Id.create("3", Node.class), new Coord(5500.0, 0.0));
			Node node4 = NetworkUtils.createAndAddNode(this.network, Id.create("4", Node.class), new Coord(6000.0, 0.0));
			Node node5 = NetworkUtils.createAndAddNode(this.network, Id.create("5", Node.class), new Coord(11000.0, 0.0));
			Node node6 = NetworkUtils.createAndAddNode(this.network, Id.create("6", Node.class), new Coord(11500.0, 0.0));
			Node node7 = NetworkUtils.createAndAddNode(this.network, Id.create("7", Node.class), new Coord(16500.0, 0.0));
			Node node8 = NetworkUtils.createAndAddNode(this.network, Id.create("8", Node.class), new Coord(17000.0, 0.0));
			Node node9 = NetworkUtils.createAndAddNode(this.network, Id.create("9", Node.class), new Coord(22000.0, 0.0));
			Node node10 = NetworkUtils.createAndAddNode(this.network, Id.create("10", Node.class), new Coord(22500.0, 0.0));
			final Node fromNode = node1;
			final Node toNode = node2;

			Link link1 = NetworkUtils.createAndAddLink(this.network,Id.create("1", Link.class), fromNode, toNode, (double) 500, (double) 25, (double) 3600, (double) 1 );
			final Node fromNode1 = node2;
			final Node toNode1 = node3;
			Link link2 = NetworkUtils.createAndAddLink(this.network,Id.create("2", Link.class), fromNode1, toNode1, (double) 25000, (double) 50, (double) 3600, (double) 1 );
			final Node fromNode2 = node3;
			final Node toNode2 = node4;
			Link link3 = NetworkUtils.createAndAddLink(this.network,Id.create("3", Link.class), fromNode2, toNode2, (double) 500, (double) 25, (double) 3600, (double) 1 );
			final Node fromNode3 = node4;
			final Node toNode3 = node5;
			NetworkUtils.createAndAddLink(this.network,Id.create("4", Link.class), fromNode3, toNode3, (double) 5000, (double) 50, (double) 3600, (double) 1 );
			final Node fromNode4 = node5;
			final Node toNode4 = node6;
			Link link5 = NetworkUtils.createAndAddLink(this.network,Id.create("5", Link.class), fromNode4, toNode4, (double) 500, (double) 25, (double) 3600, (double) 1 );
			final Node fromNode5 = node6;
			final Node toNode5 = node7;
			NetworkUtils.createAndAddLink(this.network,Id.create("6", Link.class), fromNode5, toNode5, (double) 5000, (double) 50, (double) 3600, (double) 1 );
			final Node fromNode6 = node7;
			final Node toNode6 = node8;
			Link link7 = NetworkUtils.createAndAddLink(this.network,Id.create("7", Link.class), fromNode6, toNode6, (double) 500, (double) 25, (double) 3600, (double) 1 );
			final Node fromNode7 = node8;
			final Node toNode7 = node9;
			NetworkUtils.createAndAddLink(this.network,Id.create("8", Link.class), fromNode7, toNode7, (double) 5000, (double) 50, (double) 3600, (double) 1 );
			final Node fromNode8 = node9;
			final Node toNode8 = node10;
			Link link9 = NetworkUtils.createAndAddLink(this.network,Id.create("9", Link.class), fromNode8, toNode8, (double) 500, (double) 25, (double) 3600, (double) 1 );

			this.person = PopulationUtils.getFactory().createPerson(Id.create("1", Person.class));
			this.plan = PersonUtils.createAndAddPlan(this.person, true);

			Activity firstActivity = PopulationUtils.createAndAddActivityFromLinkId(this.plan, "h", link1.getId());
			firstActivity.setEndTime(firstLegStartTime);

			Leg leg = PopulationUtils.createAndAddLeg( this.plan, TransportMode.car );
			leg.setDepartureTime(firstLegStartTime);
			leg.setTravelTime(firstLegTravelTime);
			NetworkRoute route1 = RouteUtils.createLinkNetworkRouteImpl(link1.getId(), link3.getId());
			route1.setLinkIds(link1.getId(), Arrays.asList(link2.getId()), link3.getId());
			route1.setTravelTime(firstLegTravelTime);
			route1.setDistance(RouteUtils.calcDistanceExcludingStartEndLink(route1, this.network));
			route1.setVehicleId( Id.create( "dummy1Vehicle", Vehicle.class) );
			leg.setRoute(route1);

			Activity secondActivity = PopulationUtils.createAndAddActivityFromLinkId(this.plan, "w", link3.getId());
			secondActivity.setStartTime(firstLegStartTime + firstLegTravelTime);
			secondActivity.setEndTime(secondLegStartTime);
			leg = PopulationUtils.createAndAddLeg( this.plan, TransportMode.pt );
			leg.setDepartureTime(secondLegStartTime);
			leg.setTravelTime(secondLegTravelTime);
			Route route2 = RouteUtils.createGenericRouteImpl(link3.getId(), link5.getId());
			route2.setTravelTime(secondLegTravelTime);
			route2.setDistance(20000.0);
			leg.setRoute(route2);

			Activity thirdActivity = PopulationUtils.createAndAddActivityFromLinkId(this.plan, "w", link5.getId());
			thirdActivity.setStartTime(secondLegStartTime + secondLegTravelTime);
			thirdActivity.setEndTime(thirdLegStartTime);
			leg = PopulationUtils.createAndAddLeg( this.plan, TransportMode.walk );
			leg.setDepartureTime(thirdLegStartTime);
			leg.setTravelTime(thirdLegTravelTime);
			Route route3 = RouteUtils.createGenericRouteImpl(link5.getId(), link7.getId());
			route3.setTravelTime(thirdLegTravelTime);
			route3.setDistance(CoordUtils.calcEuclideanDistance(link5.getCoord(), link7.getCoord()));
			leg.setRoute(route3);

			Activity fourthActivity = PopulationUtils.createAndAddActivityFromLinkId(this.plan, "w", link7.getId());
			fourthActivity.setStartTime(thirdLegStartTime + thirdLegTravelTime);
			fourthActivity.setEndTime(fourthLegStartTime);
			leg = PopulationUtils.createAndAddLeg( this.plan, TransportMode.bike );
			leg.setDepartureTime(fourthLegStartTime);
			leg.setTravelTime(fourthLegTravelTime);
			Route route4 = RouteUtils.createGenericRouteImpl(link7.getId(), link9.getId());
			route4.setTravelTime(fourthLegTravelTime);
			route4.setDistance(CoordUtils.calcEuclideanDistance(link7.getCoord(), link9.getCoord()));
			leg.setRoute(route4);

			Activity fifthActivity = PopulationUtils.createAndAddActivityFromLinkId(this.plan, "h", link9.getId());
			fifthActivity.setStartTime(fourthLegStartTime + fourthLegTravelTime);
			this.scenario.getPopulation().addPerson(this.person);
		}
	}
}
