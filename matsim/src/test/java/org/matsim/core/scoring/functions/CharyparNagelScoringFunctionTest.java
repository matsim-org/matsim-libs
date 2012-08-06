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

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.events.ActivityEndEventImpl;
import org.matsim.core.events.ActivityStartEventImpl;
import org.matsim.core.events.AgentArrivalEventImpl;
import org.matsim.core.events.AgentDepartureEventImpl;
import org.matsim.core.events.AgentMoneyEventImpl;
import org.matsim.core.events.LinkEnterEventImpl;
import org.matsim.core.events.LinkLeaveEventImpl;
import org.matsim.core.events.TravelledEventImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.core.utils.geometry.CoordImpl;

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

	private ScoringFunction getScoringFunctionInstance(final Fixture f, final PlanImpl somePlan) {
		CharyparNagelScoringFunctionFactory charyparNagelScoringFunctionFactory = new CharyparNagelScoringFunctionFactory(f.config.planCalcScore(), f.scenario.getNetwork());
		return charyparNagelScoringFunctionFactory.createNewScoringFunction(somePlan);
	}

	private double calcScore(final Fixture f) {
		CharyparNagelScoringFunctionFactory charyparNagelScoringFunctionFactory = new CharyparNagelScoringFunctionFactory(f.config.planCalcScore(), f.scenario.getNetwork());
		ScoringFunctionAccumulator testee = (ScoringFunctionAccumulator) charyparNagelScoringFunctionFactory.createNewScoringFunction(new PlanImpl());
		testee.handleActivity((Activity) f.plan.getPlanElements().get(0));
		testee.handleLeg((Leg) f.plan.getPlanElements().get(1));
		testee.handleActivity((Activity) f.plan.getPlanElements().get(2));
		testee.handleLeg((Leg) f.plan.getPlanElements().get(3));
		testee.handleActivity((Activity) f.plan.getPlanElements().get(4));
		testee.handleLeg((Leg) f.plan.getPlanElements().get(5));
		testee.handleActivity((Activity) f.plan.getPlanElements().get(6));
		testee.handleLeg((Leg) f.plan.getPlanElements().get(7));
		testee.handleActivity((Activity) f.plan.getPlanElements().get(8));
		testee.finish();
		double score = testee.getScore();
		EventsToScore eventsToScore = new EventsToScore(f.scenario, charyparNagelScoringFunctionFactory);
		double scoreFromEvents = calcScoreFromEvents(eventsToScore, f);
		assertEquals("Score computed from the plan elements should be the same as score computed from stream of events constructed from plan elements.", score, scoreFromEvents, EPSILON);
		return score;
	}

	private double calcScoreFromEvents(EventsToScore eventsToScore, final Fixture f) {
		handleFirstActivity(eventsToScore, f, (Activity) f.plan.getPlanElements().get(0));
		handleLeg(eventsToScore, f, (Leg) f.plan.getPlanElements().get(1));
		handleActivity(eventsToScore, f, (Activity) f.plan.getPlanElements().get(2));
		handleLeg(eventsToScore, f, (Leg) f.plan.getPlanElements().get(3));
		handleActivity(eventsToScore, f, (Activity) f.plan.getPlanElements().get(4));
		handleLeg(eventsToScore, f, (Leg) f.plan.getPlanElements().get(5));
		handleActivity(eventsToScore, f, (Activity) f.plan.getPlanElements().get(6));
		handleLeg(eventsToScore, f, (Leg) f.plan.getPlanElements().get(7));
		handleLastActivity(eventsToScore, f, (Activity) f.plan.getPlanElements().get(8));
		eventsToScore.finish();
		return f.plan.getScore();
	}

	private void handleFirstActivity(EventsToScore eventsToScore, Fixture f, Activity activity) {
		eventsToScore.handleEvent(new ActivityEndEventImpl(activity.getEndTime(), f.person.getId(), activity.getLinkId(), activity.getFacilityId(), activity.getType()));
	}

	private void handleLastActivity(EventsToScore eventsToScore, Fixture f, Activity activity) {
		eventsToScore.handleEvent(new ActivityStartEventImpl(activity.getStartTime(), f.person.getId(), activity.getLinkId(), activity.getFacilityId(), activity.getType()));
	}

	private void handleLeg(EventsToScore eventsToScore, Fixture f, Leg leg) {
		eventsToScore.handleEvent(new AgentDepartureEventImpl(leg.getDepartureTime(), f.person.getId(), leg.getRoute().getStartLinkId(), leg.getMode()));
		if (leg.getRoute() instanceof NetworkRoute) {
			NetworkRoute networkRoute = (NetworkRoute) leg.getRoute();
			eventsToScore.handleEvent(new LinkLeaveEventImpl(leg.getDepartureTime(), f.person.getId(), leg.getRoute().getStartLinkId(), networkRoute.getVehicleId()));
			for (Id linkId : networkRoute.getLinkIds()) {
				eventsToScore.handleEvent(new LinkEnterEventImpl(leg.getDepartureTime(), f.person.getId(), linkId, networkRoute.getVehicleId()));
				eventsToScore.handleEvent(new LinkLeaveEventImpl(leg.getDepartureTime(), f.person.getId(), linkId, networkRoute.getVehicleId()));
			}
			eventsToScore.handleEvent(new LinkEnterEventImpl(leg.getDepartureTime() + leg.getTravelTime(), f.person.getId(), leg.getRoute().getEndLinkId(), null));
		} else {
			eventsToScore.handleEvent(new TravelledEventImpl(leg.getDepartureTime() + leg.getTravelTime(), f.person.getId(), leg.getRoute().getDistance()));
		}
		eventsToScore.handleEvent(new AgentArrivalEventImpl(leg.getDepartureTime() + leg.getTravelTime(), f.person.getId(), leg.getRoute().getEndLinkId(), leg.getMode()));
	}

	private void handleActivity(EventsToScore eventsToScore, Fixture f, Activity activity) {
		eventsToScore.handleEvent(new ActivityStartEventImpl(activity.getStartTime(), f.person.getId(), activity.getLinkId(), activity.getFacilityId(), activity.getType()));
		eventsToScore.handleEvent(new ActivityEndEventImpl(activity.getEndTime(), f.person.getId(), activity.getLinkId(), activity.getFacilityId(), activity.getType()));
	}

	/**
	 * The reference implementation to calculate the zero utility duration, the duration of
	 * an activity at which its utility is zero.
	 *
	 * @param typicalDuration_h The typical duration of the activity in hours
	 * @param priority
	 * @return the duration (in hours) at which the activity has a utility of 0.
	 */
	private double getZeroUtilDuration_h(final double typicalDuration_h, final double priority) {
		return typicalDuration_h * Math.exp(-10.0 / typicalDuration_h / priority);
	}

	/**
	 * Test the calculation of the zero-utility-duration.
	 */
	@Test
	public void testZeroUtilityDuration() {
		double zeroUtilDurW = getZeroUtilDuration_h(8.0, 1.0);
		double zeroUtilDurH = getZeroUtilDuration_h(16.0, 1.0);
		double zeroUtilDurW2 = getZeroUtilDuration_h(8.0, 2.0);

		ActivityUtilityParameters params = new ActivityUtilityParameters("w", 1.0, 8.0 * 3600);
		assertEquals(zeroUtilDurW, params.getZeroUtilityDuration(), EPSILON);

		params = new ActivityUtilityParameters("h", 1.0, 16.0 * 3600);
		assertEquals(zeroUtilDurH, params.getZeroUtilityDuration(), EPSILON);

		params = new ActivityUtilityParameters("w2", 2.0, 8.0 * 3600); // test that the priority is respected as well
		assertEquals(zeroUtilDurW2, params.getZeroUtilityDuration(), EPSILON);
	}

	/**
	 * Test the scoring function when all parameters are set to 0.
	 */
	@Test
	public void testZero() {
		Fixture f = new Fixture();
		assertEquals(0.0, calcScore(f), EPSILON);
	}

	@Test
	public void testTravelingAndConstantCar() {
		Fixture f = new Fixture();
		f.config.planCalcScore().setTraveling_utils_hr(-6.0);
		assertEquals(-3.0, calcScore(f), EPSILON);
		f.config.planCalcScore().setConstantCar(-6.0) ;
		assertEquals(-9.0, calcScore(f), EPSILON);
	}

	@Test
	public void testTravelingPtAndConstantPt() {
		Fixture f = new Fixture();
		f.config.planCalcScore().setTravelingPt_utils_hr(-9.0);
		assertEquals(-2.25, calcScore(f), EPSILON);
		f.config.planCalcScore().setConstantPt(-3.0) ;
		assertEquals(-5.25, calcScore(f), EPSILON);
	}

	@Test
	public void testTravelingWalkAndConstantWalk() {
		Fixture f = new Fixture();
		f.config.planCalcScore().setTravelingWalk_utils_hr(-18.0);
		assertEquals(-9.0, calcScore(f), EPSILON ) ;
		f.config.planCalcScore().setConstantWalk(-1.0);
		assertEquals(-10.0, calcScore(f), EPSILON);
	}

	@Test
	public void testTravelingBikeAndConstantBike(){
		Fixture f = new Fixture();
		f.config.planCalcScore().setTravelingBike_utils_hr(-6.0);
		assertEquals(-1.5, calcScore(f), EPSILON ) ;
		f.config.planCalcScore().setConstantBike(-2.0);
		assertEquals(-3.5, calcScore(f), EPSILON);
	}

	/**
	 * Test the performing part of the scoring function.
	 */
	@Test
	public void testPerforming() {
		Fixture f = new Fixture();
		
		double perf = +6.0;
		double zeroUtilDurW = getZeroUtilDuration_h(3.0, 1.0);
		double zeroUtilDurH = getZeroUtilDuration_h(15.0, 1.0);

		f.config.planCalcScore().setPerforming_utils_hr(perf);
		assertEquals(perf * 3.0 * Math.log(2.5 / zeroUtilDurW)
				+ perf * 3.0 * Math.log(2.75/zeroUtilDurW)
				+ perf * 3.0 * Math.log(2.5/zeroUtilDurW)
				+ perf * 15.0 * Math.log(14.75 / zeroUtilDurH), calcScore(f), EPSILON);

		perf = +3.0;
		f.config.planCalcScore().setPerforming_utils_hr(perf);
		assertEquals(perf * 3.0 * Math.log(2.5 / zeroUtilDurW)
				+ perf * 3.0 * Math.log(2.75/zeroUtilDurW)
				+ perf * 3.0 * Math.log(2.5/zeroUtilDurW)
				+ perf * 15.0 * Math.log(14.75 / zeroUtilDurH), calcScore(f), EPSILON);
	}

	/**
	 * Test the performing part of the scoring function when an activity has an OpeningTime set.
	 */
	@Test
	public void testOpeningTime() {
		Fixture f = new Fixture();
		double perf = +6.0;
		f.config.planCalcScore().setPerforming_utils_hr(perf);
		double initialScore = calcScore(f);

		ActivityParams wParams = f.config.planCalcScore().getActivityParams("w");
		wParams.setOpeningTime(8*3600.0); // now the agent arrives 30min early to the FIRST work activity and has to wait
		double score = calcScore(f);

		// check the difference between 2.5 and 2.0 hours of working
		assertEquals(perf * 3.0 * Math.log(2.5 / 2.0), initialScore - score, EPSILON);
	}

	/**
	 * Test the performing part of the scoring function when an activity has a ClosingTime set.
	 */
	@Test
	public void testClosingTime() {
		Fixture f = new Fixture();
		double perf = +6.0;
		f.config.planCalcScore().setPerforming_utils_hr(perf);
		double initialScore = calcScore(f);

		ActivityParams wParams = f.config.planCalcScore().getActivityParams("w");
		wParams.setClosingTime(15*3600.0); // now the agent stays 1h too long at the LAST work activity
		double score = calcScore(f);

		// check the difference between 2.5 and 1.5 hours working
		assertEquals(perf * 3.0 * Math.log(2.5 / 1.5), initialScore - score, EPSILON);
	}

	/**
	 * Test the performing part of the scoring function when an activity has OpeningTime and ClosingTime set.
	 */
	@Test
	public void testOpeningClosingTime() {
		Fixture f = new Fixture();
		double perf = +6.0;
		double zeroUtilDurH = getZeroUtilDuration_h(15.0, 1.0);
		f.config.planCalcScore().setPerforming_utils_hr(perf);
		double initialScore = calcScore(f);

		// test1: agents has to wait before and after

		ActivityParams wParams = f.config.planCalcScore().getActivityParams("w");
		wParams.setOpeningTime( 8*3600.0); // the agent arrives 30min early
		wParams.setClosingTime(15*3600.0); // the agent stays 1h too long
		double score = calcScore(f);

		// check the differences for all work activities
		assertEquals(perf * 3.0 * Math.log(2.5 / 2.0)
				+ perf * 3.0 * Math.log(2.75 / 2.75)
				+ perf * 3.0 * Math.log(2.5 / 1.5)
				, initialScore - score, EPSILON);

		// test 2: agents has to wait all the time, because work place opens later

		wParams.setOpeningTime(20*3600.0);
		wParams.setClosingTime(21*3600.0);

		// only the home-activity should add to the score
		assertEquals(perf * 15.0 * Math.log(14.75 / zeroUtilDurH), calcScore(f), EPSILON);

		// test 3: agents has to wait all the time, because work place opened earlier

		wParams.setOpeningTime(1*3600.0);
		wParams.setClosingTime(2*3600.0);

		// only the home-activity should add to the score
		assertEquals(perf * 15.0 * Math.log(14.75 / zeroUtilDurH), calcScore(f), EPSILON);
	}

	/**
	 * Test the waiting part of the scoring function.
	 */
	@Test
	public void testWaitingTime() {
		Fixture f = new Fixture();
		double waiting = -10.0;
		f.config.planCalcScore().setWaiting_utils_hr(waiting);

		ActivityParams wParams = f.config.planCalcScore().getActivityParams("w");
		wParams.setOpeningTime( 8*3600.0); // the agent arrives 30min early
		wParams.setClosingTime(15*3600.0); // the agent stays 1h too long

		// the agent spends 1.5h waiting at the work place
		assertEquals(waiting * 1.5, calcScore(f), EPSILON);
	}

	/**
	 * Test the scoring function in regards to early departures.
	 */
	@Test
	public void testEarlyDeparture() {
		Fixture f = new Fixture();
		double disutility = -10.0;
		f.config.planCalcScore().setEarlyDeparture_utils_hr(disutility);

		ActivityParams wParams = f.config.planCalcScore().getActivityParams("w");
		wParams.setEarliestEndTime(10.75 * 3600.0); // require the agent to work until 16:45

		// the agent left 45mins too early
		assertEquals(disutility * 0.75, calcScore(f), EPSILON);
	}

	/**
	 * Test the scoring function in regards to early departures.
	 */
	@Test
	public void testMinimumDuration() {
		Fixture f = new Fixture();
		double disutility = -10.0;
		f.config.planCalcScore().setEarlyDeparture_utils_hr(disutility);

		ActivityParams wParams = f.config.planCalcScore().getActivityParams("w");
		wParams.setMinimalDuration(3 * 3600.0); // require the agent to be 3 hours at every working activity

		// the agent overall works 1.25h too short
		assertEquals(disutility * 1.25, calcScore(f), EPSILON);
	}

	/**
	 * Test the scoring function in regards to late arrival.
	 */
	@Test
	public void testLateArrival() {
		Fixture f = new Fixture();
		double disutility = -10.0;
		f.config.planCalcScore().setLateArrival_utils_hr(disutility);

		ActivityParams wParams = f.config.planCalcScore().getActivityParams("w");
		wParams.setLatestStartTime(13 * 3600.0); // agent should start working latest at 13 o'clock

		// the agent arrived 30mins late
		assertEquals(disutility * 0.5, calcScore(f), EPSILON);
	}

	/**
	 * Test that the stuck penalty is correctly computed. It should be the worst (dis)utility the agent
	 * could gain.
	 */
	@Test
	public void testStuckPenalty() {
		Fixture f = new Fixture();
		// test 1 where late arrival has the biggest impact
		f.config.planCalcScore().setLateArrival_utils_hr(-18.0);
		f.config.planCalcScore().setTraveling_utils_hr(-6.0);

		ScoringFunction testee = getScoringFunctionInstance(f, f.plan);
		testee.handleActivity((Activity) f.plan.getPlanElements().get(0));
		testee.handleLeg((Leg) f.plan.getPlanElements().get(1));
		testee.handleActivity((Activity) f.plan.getPlanElements().get(2));
		testee.handleLeg((Leg) f.plan.getPlanElements().get(3));

		testee.agentStuck(16*3600 + 7.5*60);
		testee.finish();
		testee.getScore();

		assertEquals(24 * -18.0 - 6.0 * 0.50, testee.getScore(), EPSILON); // stuck penalty + 30min traveling

		// test 2 where traveling has the biggest impact
		f.config.planCalcScore().setLateArrival_utils_hr(-3.0);
		f.config.planCalcScore().setTraveling_utils_hr(-6.0);

		testee = getScoringFunctionInstance(f, f.plan);
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
	public void testDistanceCostScoringCar() {
		Fixture f = new Fixture();
		// test 1 where marginalUtitityOfMoney is fixed to 1.0
		f.config.planCalcScore().setMarginalUtilityOfMoney(1.0);
		//		this.config.charyparNagelScoring().setMarginalUtlOfDistanceCar(-0.00001);
		f.config.planCalcScore().setMonetaryDistanceCostRateCar(-0.00001) ;

		assertEquals(-0.25, calcScore(f), EPSILON);

		// test 2 where MonetaryDistanceCostRate is fixed to -1.0
		f.config.planCalcScore().setMonetaryDistanceCostRateCar(-1.0) ;
		f.config.planCalcScore().setMarginalUtilityOfMoney(0.5);

		assertEquals(-12500.0, calcScore(f), EPSILON);
	}

	@Test
	public void testDistanceCostScoringPt() {
		Fixture f = new Fixture();
		// test 1 where marginalUtitityOfMoney is fixed to 1.0
		f.config.planCalcScore().setMarginalUtilityOfMoney(1.0);
		//		this.config.charyparNagelScoring().setMarginalUtlOfDistancePt(-0.00001);
		f.config.planCalcScore().setMonetaryDistanceCostRatePt(-0.00001) ;

		assertEquals(-0.20, calcScore(f), EPSILON);

		// test 2 where MonetaryDistanceCostRate is fixed to -1.0
		f.config.planCalcScore().setMonetaryDistanceCostRatePt(-1.0) ;
		f.config.planCalcScore().setMarginalUtilityOfMoney(0.5);

		assertEquals(-10000.0, calcScore(f), EPSILON);
	}

	/**
	 * Test how the scoring function reacts when the first and the last activity do not have the same act-type.
	 */
	@Test
	public void testDifferentFirstLastAct() {
		Fixture f = new Fixture();
		// change the last act to something different than the first act
		((Activity) f.plan.getPlanElements().get(8)).setType("h2");

		PlanCalcScoreConfigGroup.ActivityParams params = new PlanCalcScoreConfigGroup.ActivityParams("h2");
		params.setTypicalDuration(8*3600);
		f.config.planCalcScore().addActivityParams(params);
		f.config.planCalcScore().getActivityParams("h").setTypicalDuration(6.0 * 3600);
		
		double perf = +6.0;
		f.config.planCalcScore().setPerforming_utils_hr(perf);
		double zeroUtilDurW = getZeroUtilDuration_h(3.0, 1.0);
		double zeroUtilDurH = getZeroUtilDuration_h(6.0, 1.0);
		double zeroUtilDurH2 = getZeroUtilDuration_h(8.0, 1.0);

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
	@Test
	public void testNoNightActivity() {
		
		double zeroUtilDurW = getZeroUtilDuration_h(3.0, 1.0);
		double zeroUtilDurH = getZeroUtilDuration_h(7.0, 1.0);
		double perf = +3.0;
		
		Fixture f = new Fixture();
		// Need to change the typical duration of the home activity
		// for this test, since with the setting of 15 hours for "home",
		// this would amount to a smaller-than-zero expected contribution of
		// the home activity at 7 hours, and smaller-than-zero contributions
		// are truncated, so we wouldn't test anything. :-/
		f.config.planCalcScore().getActivityParams("h").setTypicalDuration(7.0 * 3600);
		f.config.planCalcScore().setPerforming_utils_hr(perf);
		
		ScoringFunction testee = getScoringFunctionInstance(f, f.plan);
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
	 * Sets up the configuration to be useful for scoring plans. This implementation
	 * sets the parameters for scoring functions returned by
	 * {@link CharyparNagelScoringFunctionFactory}, overwrite it to test your own
	 * custom scoring function.
	 *
	 * @param config
	 */
	protected void setupScoringConfig(final Config config) {
		PlanCalcScoreConfigGroup scoring = config.planCalcScore();
		scoring.setBrainExpBeta(2.0);
		scoring.setLateArrival_utils_hr(-18.0);
		scoring.setEarlyDeparture_utils_hr(0.0);
		scoring.setPerforming_utils_hr(6.0);
		scoring.setTraveling_utils_hr(-6.0);
		scoring.setTravelingPt_utils_hr(0.0);
		scoring.setMonetaryDistanceCostRateCar(0.0) ;
		scoring.setMarginalUtilityOfMoney(1.);

		scoring.setWaiting_utils_hr(0.0);

		// setup activity types h and w for scoring
		PlanCalcScoreConfigGroup.ActivityParams params = new PlanCalcScoreConfigGroup.ActivityParams("home");
		params.setTypicalDuration(16*3600);
		scoring.addActivityParams(params);

		params = new PlanCalcScoreConfigGroup.ActivityParams("work");
		params.setTypicalDuration(8*3600);
		scoring.addActivityParams(params);
	}

	/**
	 * Tests if the scoring function correctly handles {@link AgentMoneyEventImpl}.
	 * It generates one person with one plan having two activities (home, work)
	 * and a car-leg in between. It then tests the scoring function by calling
	 * several methods on an instance of the scoring function with the
	 * aforementioned plan.
	 */
	@Test
	public void testAddMoney() {
		Fixture f = new Fixture();
		setupScoringConfig(f.config);

		// score the same plan twice
		PersonImpl person1 = new PersonImpl(new IdImpl(1));
		PlanImpl plan1 = person1.createAndAddPlan(true);
		Activity act1a = plan1.createAndAddActivity("home", (Id)null);//, 0, 7.0*3600, 7*3600, false);
		Leg leg1 = plan1.createAndAddLeg(TransportMode.car);//, 7*3600, 100, 7*3600+100);
		leg1.setDepartureTime(f.secondLegStartTime);
		leg1.setTravelTime(f.secondLegTravelTime);
		Route route2 = new GenericRouteImpl(null, null);
		leg1.setRoute(route2);
		route2.setDistance(20000.0);
		Activity act1b = plan1.createAndAddActivity("work", (Id)null);//, 7.0*3600+100, Time.UNDEFINED_TIME, Time.UNDEFINED_TIME, false);
		ScoringFunction sf1 = getScoringFunctionInstance(f, new PlanImpl());
		sf1.handleActivity(act1a);
		sf1.handleLeg(leg1);
		sf1.handleActivity(act1b);

		sf1.finish();
		double score1 = sf1.getScore();

		ScoringFunction sf2 = getScoringFunctionInstance(f, new PlanImpl());
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

	private static class Fixture {
		protected Config config = null;
		private PersonImpl person = null;
		private PlanImpl plan = null;
		private Scenario scenario;
		private NetworkImpl network;
		private int firstLegStartTime;
		private int firstLegTravelTime;
		private int thirdLegTravelTime;
		private int fourthLegTravelTime;
		private int secondLegTravelTime;
		private int secondLegStartTime;
		private int thirdLegStartTime;
		private int fourthLegStartTime;

		public Fixture() {
			firstLegStartTime = 07 * 3600;
			firstLegTravelTime = 30 * 60;
			thirdLegTravelTime = 30 * 60;
			secondLegStartTime = 10 * 3600;
			secondLegTravelTime = 15 * 60;
			thirdLegStartTime = 13 * 3600;
			fourthLegStartTime = 16 * 3600;
			fourthLegTravelTime = 15 * 60;
			this.config = ConfigUtils.createConfig();
			PlanCalcScoreConfigGroup scoring = this.config.planCalcScore();
			scoring.setBrainExpBeta(2.0);

			scoring.setConstantCar(0.0);
			scoring.setConstantPt(0.0);
			scoring.setConstantWalk(0.0);
			scoring.setConstantBike(0.0);

			scoring.setEarlyDeparture_utils_hr(0.0);
			scoring.setLateArrival_utils_hr(0.0);
			scoring.setWaiting_utils_hr(0.0);
			scoring.setPerforming_utils_hr(0.0);
			scoring.setTraveling_utils_hr(0.0);
			scoring.setTravelingPt_utils_hr(0.0);
			scoring.setTravelingWalk_utils_hr(0.0);
			scoring.setTravelingBike_utils_hr(0.0);

			scoring.setMarginalUtilityOfMoney(1.) ;
			scoring.setMonetaryDistanceCostRateCar(0.0) ;
			scoring.setMonetaryDistanceCostRatePt(0.0);


			// setup activity types h and w for scoring
			PlanCalcScoreConfigGroup.ActivityParams params = new PlanCalcScoreConfigGroup.ActivityParams("h");
			params.setTypicalDuration(15*3600);
			scoring.addActivityParams(params);
			

			params = new PlanCalcScoreConfigGroup.ActivityParams("w");
			params.setTypicalDuration(3*3600);
			scoring.addActivityParams(params);

			this.scenario = ScenarioUtils.createScenario(config);
			this.network = (NetworkImpl) this.scenario.getNetwork();
			Node node1 = this.network.createAndAddNode(new IdImpl("1"), new CoordImpl(    0.0, 0.0));
			Node node2 = this.network.createAndAddNode(new IdImpl("2"), new CoordImpl(  500.0, 0.0));
			Node node3 = this.network.createAndAddNode(new IdImpl("3"), new CoordImpl( 5500.0, 0.0));
			Node node4 = this.network.createAndAddNode(new IdImpl("4"), new CoordImpl( 6000.0, 0.0));
			Node node5 = this.network.createAndAddNode(new IdImpl("5"), new CoordImpl(11000.0, 0.0));
			Node node6 = this.network.createAndAddNode(new IdImpl("6"), new CoordImpl(11500.0, 0.0));
			Node node7 = this.network.createAndAddNode(new IdImpl("7"), new CoordImpl(16500.0, 0.0));
			Node node8 = this.network.createAndAddNode(new IdImpl("8"), new CoordImpl(17000.0, 0.0));
			Node node9 = this.network.createAndAddNode(new IdImpl("9"), new CoordImpl(22000.0, 0.0));
			Node node10 = this.network.createAndAddNode(new IdImpl("10"), new CoordImpl(22500.0, 0.0));

			Link link1 = this.network.createAndAddLink(new IdImpl("1"), node1, node2, 500, 25, 3600, 1);
			Link link2 = this.network.createAndAddLink(new IdImpl("2"), node2, node3, 25000, 50, 3600, 1);
			Link link3 = this.network.createAndAddLink(new IdImpl("3"), node3, node4, 500, 25, 3600, 1);
			this.network.createAndAddLink(new IdImpl("4"), node4, node5, 5000, 50, 3600, 1);
			Link link5 = this.network.createAndAddLink(new IdImpl("5"), node5, node6, 500, 25, 3600, 1);
			this.network.createAndAddLink(new IdImpl("6"), node6, node7, 5000, 50, 3600, 1);
			Link link7 = this.network.createAndAddLink(new IdImpl("7"), node7, node8, 500, 25, 3600, 1);
			this.network.createAndAddLink(new IdImpl("8"), node8, node9, 5000, 50, 3600, 1);
			Link link9 = this.network.createAndAddLink(new IdImpl("9"), node9, node10, 500, 25, 3600, 1);

			this.person = new PersonImpl(new IdImpl("1"));
			this.plan = this.person.createAndAddPlan(true);

			ActivityImpl firstActivity = this.plan.createAndAddActivity("h", link1.getId());
			firstActivity.setEndTime(firstLegStartTime);

			Leg leg = this.plan.createAndAddLeg(TransportMode.car);
			leg.setDepartureTime(firstLegStartTime);
			leg.setTravelTime(firstLegTravelTime);
			NetworkRoute route1 = new LinkNetworkRouteImpl(link1.getId(), link3.getId());
			route1.setLinkIds(link1.getId(), Arrays.asList(link2.getId()), link3.getId());
			leg.setRoute(route1);

			ActivityImpl secondActivity = this.plan.createAndAddActivity("w", link3.getId());
			secondActivity.setStartTime(firstLegStartTime + firstLegTravelTime);
			secondActivity.setEndTime(secondLegStartTime);
			leg = this.plan.createAndAddLeg(TransportMode.pt);
			leg.setDepartureTime(secondLegStartTime);
			leg.setTravelTime(secondLegTravelTime);
			Route route2 = new GenericRouteImpl(link3.getId(), link5.getId());
			leg.setRoute(route2);
			route2.setDistance(20000.0);

			ActivityImpl thirdActivity = this.plan.createAndAddActivity("w", link5.getId());
			thirdActivity.setStartTime(secondLegStartTime + secondLegTravelTime);
			thirdActivity.setEndTime(thirdLegStartTime);
			leg = this.plan.createAndAddLeg(TransportMode.walk);
			leg.setDepartureTime(thirdLegStartTime);
			leg.setTravelTime(thirdLegTravelTime);
			Route route3 = new GenericRouteImpl(link5.getId(), link7.getId());
			leg.setRoute(route3);

			ActivityImpl fourthActivity = this.plan.createAndAddActivity("w", link7.getId());
			fourthActivity.setStartTime(thirdLegStartTime + thirdLegTravelTime);
			fourthActivity.setEndTime(fourthLegStartTime);
			leg = this.plan.createAndAddLeg(TransportMode.bike);
			leg.setDepartureTime(fourthLegStartTime);
			leg.setTravelTime(fourthLegTravelTime);
			Route route4 = new GenericRouteImpl(link7.getId(), link9.getId());
			leg.setRoute(route4);

			ActivityImpl fifthActivity = this.plan.createAndAddActivity("h", link9.getId());
			fifthActivity.setStartTime(fourthLegStartTime + fourthLegTravelTime);
			this.scenario.getPopulation().addPerson(this.person);
		}      
	}
}
