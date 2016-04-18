package playground.artemc.heterogeneity.scoring;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.functions.ActivityUtilityParameters;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.vehicles.Vehicle;


/**
 * Based on CharyparNagelScoringFunctionFactoryTest from 25/04/2015
 * Test the correct working of the HeterogeneuosCharyparNagelScoringFunction
 * TODO dg march 09: when walk mode is tested add a walk mode leg and modify at least testMarginalUtilityOfDistance
 * <p/>
 * TODO [MR] split this into multiple test classes for the specific parts, according to the newer, more modular scoring function
 *
 * @author mrieser, artemc
 */
public class HeterogeneousCharyparNagelScoringFunctionForAnalysisFactoryTest {

	private static final double EPSILON = 1e-9;
	private static final double testee_incomeAlphaFactor = 2.0;
	private static final double testee_betaFactor= 0.5;

	private ScoringFunction getScoringFunctionInstance(final Fixture f, final Person person) {
		HeterogeneousCharyparNagelScoringFunctionForAnalysisFactory heterogeneousCharyparNagelScoringFunctionForAnalysisFactory = new HeterogeneousCharyparNagelScoringFunctionForAnalysisFactory(f.config.planCalcScore(), f.scenario.getNetwork());
		heterogeneousCharyparNagelScoringFunctionForAnalysisFactory.setSimulationType("homo");
		return heterogeneousCharyparNagelScoringFunctionForAnalysisFactory.createNewScoringFunction(person);
	}

	private double calcScore(final Fixture f) {
		/* The following was deleted because route distance calculation was changed in the mobsim. 
		 * Therefore, a leg no longer contains all information to calculate route distance correctly. 
		 * (see MATSIM-227) tt feb'2016 
		 */
//		HeterogeneousCharyparNagelScoringFunctionForAnalysisFactory heterogeneousCharyparNagelScoringFunctionForAnalysisFactory 
//			= new HeterogeneousCharyparNagelScoringFunctionForAnalysisFactory(f.config.planCalcScore(), f.scenario.getNetwork());
//		heterogeneousCharyparNagelScoringFunctionForAnalysisFactory.setSimulationType("homo");
//		ScoringFunction testee = heterogeneousCharyparNagelScoringFunctionForAnalysisFactory.createNewScoringFunction(PopulationUtils.createPerson(Id.create("1", Person.class)));
//		for (PlanElement planElement : f.plan.getPlanElements()) {
//			if (planElement instanceof Activity) {
//				testee.handleActivity((Activity) planElement);
//			} else if (planElement instanceof Leg) {
//				testee.handleLeg((Leg) planElement);
//			}
//		}
//		testee.finish();
//		double score = testee.getScore();
//		EventsManager eventsManager = EventsUtils.createEventsManager();
//		EventsToScore eventsToScore = EventsToScore.createWithScoreUpdating(f.scenario, heterogeneousCharyparNagelScoringFunctionForAnalysisFactory, eventsManager);
//		double scoreFromEvents = calcScoreFromEvents(eventsManager, eventsToScore, f);
//		assertEquals("Score computed from the plan elements should be the same as score computed from stream of events constructed from plan elements.", score, scoreFromEvents, EPSILON);
//		return score;

		HeterogeneousCharyparNagelScoringFunctionForAnalysisFactory heterogeneousCharyparNagelScoringFunctionForAnalysisFactory = new HeterogeneousCharyparNagelScoringFunctionForAnalysisFactory(
				f.config.planCalcScore(), f.scenario.getNetwork());
		heterogeneousCharyparNagelScoringFunctionForAnalysisFactory.setSimulationType("homo");
		EventsManager eventsManager = EventsUtils.createEventsManager();
		EventsToScore eventsToScore = EventsToScore.createWithScoreUpdating(f.scenario, heterogeneousCharyparNagelScoringFunctionForAnalysisFactory, eventsManager);
		return calcScoreFromEvents(eventsManager, eventsToScore, f);
	}

	private double calcScore(final Fixture f, String sdHeterogeneityType) {
		HeterogeneousCharyparNagelScoringFunctionForAnalysisFactory heterogeneousCharyparNagelScoringFunctionForAnalysisFactory = new HeterogeneousCharyparNagelScoringFunctionForAnalysisFactory(f.config.planCalcScore(), f.scenario.getNetwork());
		heterogeneousCharyparNagelScoringFunctionForAnalysisFactory.setSimulationType(sdHeterogeneityType);
		ScoringFunction testee = heterogeneousCharyparNagelScoringFunctionForAnalysisFactory.createNewScoringFunction(f.scenario.getPopulation().getPersons().get(Id.create("1", Person.class)));
		for (PlanElement planElement : f.plan.getPlanElements()) {
			if (planElement instanceof Activity) {
				testee.handleActivity((Activity) planElement);
			} else if (planElement instanceof Leg) {
				testee.handleLeg((Leg) planElement);
			}
		}
		testee.finish();
		double score = testee.getScore();
		EventsManager eventsManager = EventsUtils.createEventsManager();
		EventsToScore eventsToScore = EventsToScore.createWithScoreUpdating(f.scenario, heterogeneousCharyparNagelScoringFunctionForAnalysisFactory, eventsManager);
		double scoreFromEvents = calcScoreFromEvents(eventsManager, eventsToScore, f);
		assertEquals("Score computed from the plan elements should be the same as score computed from stream of events constructed from plan elements.", score, scoreFromEvents, EPSILON);
		return score;
	}

	private double calcScoreFromEvents(EventsManager eventsManager, EventsToScore eventsToScore, final Fixture f) {
		eventsToScore.beginIteration(0);
		handleFirstActivity(eventsManager, f, (Activity) f.plan.getPlanElements().get(0));
		handleLeg(eventsManager, f, (Leg) f.plan.getPlanElements().get(1));
		handleActivity(eventsManager, f, (Activity) f.plan.getPlanElements().get(2));
		handleLeg(eventsManager, f, (Leg) f.plan.getPlanElements().get(3));
		handleActivity(eventsManager, f, (Activity) f.plan.getPlanElements().get(4));
		handleLeg(eventsManager, f, (Leg) f.plan.getPlanElements().get(5));
		handleActivity(eventsManager, f, (Activity) f.plan.getPlanElements().get(6));
		handleLeg(eventsManager, f, (Leg) f.plan.getPlanElements().get(7));
		handleLastActivity(eventsManager, f, (Activity) f.plan.getPlanElements().get(8));
		eventsToScore.finish();
		return f.plan.getScore();
	}

	private void handleFirstActivity(EventsManager eventsToScore, Fixture f, Activity activity) {
		eventsToScore.processEvent(new ActivityEndEvent(activity.getEndTime(), f.person.getId(), activity.getLinkId(), activity.getFacilityId(), activity.getType()));
	}

	private void handleLastActivity(EventsManager eventsToScore, Fixture f, Activity activity) {
		eventsToScore.processEvent(new ActivityStartEvent(activity.getStartTime(), f.person.getId(), activity.getLinkId(), activity.getFacilityId(), activity.getType()));
	}

	private void handleLeg(EventsManager eventsToScore, Fixture f, Leg leg) {
		final double time = leg.getDepartureTime();
		final Id<Person> driverId = f.person.getId();
		final String networkMode = leg.getMode();
		eventsToScore.processEvent(new PersonDepartureEvent(time, driverId, leg.getRoute().getStartLinkId(), networkMode));
		if (leg.getRoute() instanceof NetworkRoute) {
			NetworkRoute networkRoute = (NetworkRoute) leg.getRoute();
			final Id<Vehicle> vehicleId = networkRoute.getVehicleId();
			double relativePositionOnLink = 1.0 ;
			eventsToScore.processEvent(new VehicleEntersTrafficEvent(time, driverId, leg.getRoute().getStartLinkId(), vehicleId, networkMode, relativePositionOnLink));
			eventsToScore.processEvent(new LinkLeaveEvent(time, vehicleId, leg.getRoute().getStartLinkId()));
			for (Id<Link> linkId : networkRoute.getLinkIds()) {
				eventsToScore.processEvent(new LinkEnterEvent(time, vehicleId, linkId));
				eventsToScore.processEvent(new LinkLeaveEvent(time, vehicleId, linkId));
			}
			eventsToScore.processEvent(new LinkEnterEvent(time + leg.getTravelTime(), vehicleId, leg.getRoute().getEndLinkId()));
			eventsToScore.processEvent(new VehicleLeavesTrafficEvent(time + leg.getTravelTime() + 1, driverId, leg.getRoute().getEndLinkId(), vehicleId, networkMode, relativePositionOnLink));
		} else {
			eventsToScore.processEvent(new TeleportationArrivalEvent(time + leg.getTravelTime(), driverId, leg.getRoute().getDistance()));
		}
		eventsToScore.processEvent(new PersonArrivalEvent(time + leg.getTravelTime(), driverId, leg.getRoute().getEndLinkId(), networkMode));
	}

	private void handleActivity(EventsManager eventsToScore, Fixture f, Activity activity) {
		eventsToScore.processEvent(new ActivityStartEvent(activity.getStartTime(), f.person.getId(), activity.getLinkId(), activity.getFacilityId(), activity.getType()));
		eventsToScore.processEvent(new ActivityEndEvent(activity.getEndTime(), f.person.getId(), activity.getLinkId(), activity.getFacilityId(), activity.getType()));
	}

	/**
	 * The reference implementation to calculate the zero utility duration, the duration of
	 * an activity at which its utility is zero.
	 *
	 * @param typicalDuration_hrs The typical duration of the activity in hours
	 * @param priority
	 * @return the duration (in hours) at which the activity has a utility of 0.
	 */
	private double getZeroUtilDuration_hrs(final double typicalDuration_hrs, final double priority) {
		// yy could/should use static function from CharyparNagelScoringUtils. kai, nov'13
		return typicalDuration_hrs * Math.exp(-10.0 / typicalDuration_hrs / priority);
	}

	/**
	 * Test the calculation of the zero-utility-duration.
	 */
	@Test
	public void testZeroUtilityDuration() {
		double zeroUtilDurW = getZeroUtilDuration_hrs(8.0, 1.0);
		double zeroUtilDurH = getZeroUtilDuration_hrs(16.0, 1.0);
		double zeroUtilDurW2 = getZeroUtilDuration_hrs(8.0, 2.0);

		{
			ActivityUtilityParameters.Builder factory = new ActivityUtilityParameters.Builder();
			factory.setType("w");
			factory.setPriority(1.0);
			factory.setTypicalDuration_s(8.0 * 3600);
			factory.setZeroUtilityComputation(new ActivityUtilityParameters.SameAbsoluteScore());
			ActivityUtilityParameters params = factory.build();
			assertEquals(zeroUtilDurW, params.getZeroUtilityDuration_h(), EPSILON);

		}

		{
			ActivityUtilityParameters.Builder factory = new ActivityUtilityParameters.Builder();
			factory.setType("h");
			factory.setPriority(1.0);
			factory.setTypicalDuration_s(16.0 * 3600);
			factory.setZeroUtilityComputation(new ActivityUtilityParameters.SameAbsoluteScore());
			ActivityUtilityParameters params = factory.build();
			assertEquals(zeroUtilDurH, params.getZeroUtilityDuration_h(), EPSILON);
		}

		{
			ActivityUtilityParameters.Builder factory = new ActivityUtilityParameters.Builder();
			factory.setType("w2");
			// test that the priority is respected as well
			factory.setPriority(2.0);
			factory.setTypicalDuration_s(8.0 * 3600);
			factory.setZeroUtilityComputation(new ActivityUtilityParameters.SameAbsoluteScore());
			ActivityUtilityParameters params = factory.build();
			assertEquals(zeroUtilDurW2, params.getZeroUtilityDuration_h(), EPSILON);
		}
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
		final double traveling = -6.0;
		f.config.planCalcScore().getModes().get(TransportMode.car).setMarginalUtilityOfTraveling(traveling);
		assertEquals(-3.0, calcScore(f), EPSILON);
		assertEquals(-6.0, calcScore(f,"hetero"), EPSILON);
		assertEquals(-6.0, calcScore(f, "heteroAlpha"), EPSILON);
		assertEquals(-6.0, calcScore(f,"heteroGamma"), EPSILON);

		double constantCar = -6.0;
		f.config.planCalcScore().getModes().get(TransportMode.car).setConstant(constantCar);
		assertEquals(-9.0, calcScore(f), EPSILON);
		assertEquals(-12.0, calcScore(f,"hetero"), EPSILON);
		assertEquals(-12.0, calcScore(f, "heteroAlpha"), EPSILON);
		assertEquals(-12.0, calcScore(f,"heteroGamma"), EPSILON);
	}

	@Test
	public void testTravelingPtAndConstantPt() {
		Fixture f = new Fixture();
		final double travelingPt = -9.0;
		f.config.planCalcScore().getModes().get(TransportMode.pt).setMarginalUtilityOfTraveling(travelingPt);
		assertEquals(-2.25, calcScore(f), EPSILON);
		assertEquals(-4.5, calcScore(f,"hetero"), EPSILON);
		assertEquals(-4.5, calcScore(f, "heteroAlpha"), EPSILON);
		assertEquals(-4.5, calcScore(f,"heteroGamma"), EPSILON);

		double constantPt = -3.0;
		f.config.planCalcScore().getModes().get(TransportMode.pt).setConstant(constantPt);
		assertEquals(-5.25, calcScore(f), EPSILON);
		assertEquals(-7.5, calcScore(f,"hetero"), EPSILON);
		assertEquals(-7.5, calcScore(f, "heteroAlpha"), EPSILON);
		assertEquals(-7.5, calcScore(f,"heteroGamma"), EPSILON);
	}

	@Test
	public void testTravelingWalkAndConstantWalk() {
		Fixture f = new Fixture();
		final double travelingWalk = -18.0;
		f.config.planCalcScore().getModes().get(TransportMode.walk).setMarginalUtilityOfTraveling(travelingWalk);
		assertEquals(-9.0, calcScore(f), EPSILON);
		assertEquals(-18.0, calcScore(f,"hetero"), EPSILON);
		assertEquals(-18.0, calcScore(f, "heteroAlpha"), EPSILON);
		assertEquals(-18.0, calcScore(f,"heteroGamma"), EPSILON);

		double constantWalk = -1.0;
		f.config.planCalcScore().getModes().get(TransportMode.walk).setConstant(constantWalk);
		assertEquals(-10.0, calcScore(f), EPSILON);
		assertEquals(-19.0, calcScore(f,"hetero"), EPSILON);
		assertEquals(-19.0, calcScore(f, "heteroAlpha"), EPSILON);
		assertEquals(-19.0, calcScore(f,"heteroGamma"), EPSILON);
	}

	@Test
	public void testTravelingBikeAndConstantBike() {
		Fixture f = new Fixture();
		final double travelingBike = -6.0;
		f.config.planCalcScore().getModes().get(TransportMode.bike).setMarginalUtilityOfTraveling(travelingBike);
		assertEquals(-1.5, calcScore(f), EPSILON);
		assertEquals(-3.0, calcScore(f,"hetero"), EPSILON);
		assertEquals(-3.0, calcScore(f, "heteroAlpha"), EPSILON);
		assertEquals(-3.0, calcScore(f,"heteroGamma"), EPSILON);

		double constantBike = -2.0;
		f.config.planCalcScore().getModes().get(TransportMode.bike).setConstant(constantBike);
		assertEquals(-3.5, calcScore(f), EPSILON);
		assertEquals(-5.0, calcScore(f,"hetero"), EPSILON);
		assertEquals(-5.0, calcScore(f, "heteroAlpha"), EPSILON);
		assertEquals(-5.0, calcScore(f,"heteroGamma"), EPSILON);
	}

	/**
	 * Test the performing part of the scoring function.
	 */
	@Test
	public void testPerforming() {
		Fixture f = new Fixture();

		double perf = +6.0;
		double zeroUtilDurW = getZeroUtilDuration_hrs(3.0, 1.0);
		double zeroUtilDurH = getZeroUtilDuration_hrs(15.0, 1.0);

		f.config.planCalcScore().setPerforming_utils_hr(perf);
		double score = perf * 3.0 * Math.log(2.5 / zeroUtilDurW) + perf * 3.0 * Math.log(2.75 / zeroUtilDurW) + perf * 3.0 * Math.log(2.5 / zeroUtilDurW) + perf * 15.0 * Math.log(14.75 / zeroUtilDurH);
		assertEquals(score, calcScore(f), EPSILON);

		perf = perf * testee_incomeAlphaFactor;
		score = perf * 3.0 * Math.log(2.5 / zeroUtilDurW) + perf * 3.0 * Math.log(2.75 / zeroUtilDurW) + perf * 3.0 * Math.log(2.5 / zeroUtilDurW) + perf * 15.0 * Math.log(14.75 / zeroUtilDurH);
		assertEquals(score, calcScore(f,"hetero"), EPSILON);
		assertEquals(score, calcScore(f, "heteroAlpha"), EPSILON);
		assertEquals(score, calcScore(f,"heteroGamma"), EPSILON);

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
	public void testOpeningTime() {
		Fixture f = new Fixture();
		double perf = +6.0;
		f.config.planCalcScore().setPerforming_utils_hr(perf);
		double initialScore = calcScore(f);

		PlanCalcScoreConfigGroup.ActivityParams wParams = f.config.planCalcScore().getActivityParams("w");
		wParams.setOpeningTime(8 * 3600.0); // now the agent arrives 30min early to the FIRST work activity and has to wait
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

		PlanCalcScoreConfigGroup.ActivityParams wParams = f.config.planCalcScore().getActivityParams("w");
		wParams.setClosingTime(15 * 3600.0); // now the agent stays 1h too long at the LAST work activity
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
		double perf_hrs = +6.0;
		f.config.planCalcScore().setPerforming_utils_hr(perf_hrs);
		double initialScore = calcScore(f);

		// test1: agents has to wait before and after

		PlanCalcScoreConfigGroup.ActivityParams wParams = f.config.planCalcScore().getActivityParams("w");
		wParams.setOpeningTime(8 * 3600.0); // the agent arrives 30min early
		wParams.setClosingTime(15 * 3600.0); // the agent stays 1h too long
		double score = calcScore(f);

		// check the differences for all work activities
		assertEquals(perf_hrs * 3.0 * Math.log(2.5 / 2.0) + perf_hrs * 3.0 * Math.log(2.75 / 2.75) + perf_hrs * 3.0 * Math.log(2.5 / 1.5), initialScore - score, EPSILON);

		// test 2: agents has to wait all the time, because work place opens later

		wParams.setOpeningTime(20 * 3600.0);
		wParams.setClosingTime(21 * 3600.0);

		//		// only the home-activity should add to the score
		//		assertEquals(perf * 15.0 * Math.log(14.75 / zeroUtilDurH), calcScore(f), EPSILON);
		// not longer true, since not doing a scheduled activity now carries a penalty.  kai, nov'13
		double score_home = perf_hrs * 15.0 * Math.log(14.75 / getZeroUtilDuration_hrs(15.0, 1.0));

		final double typicalDuration_work_sec = wParams.getTypicalDuration();
		final double zeroUtilityDuration_work_sec = 3600. * getZeroUtilDuration_hrs(typicalDuration_work_sec / 3600., 1.);
		double slope_work_at_zero_utility_h = perf_hrs * typicalDuration_work_sec / zeroUtilityDuration_work_sec;
		double score_work = -zeroUtilityDuration_work_sec * slope_work_at_zero_utility_h / 3600.;
		assertEquals(score_home + 3. * score_work, calcScore(f), EPSILON);

		// test 3: agents has to wait all the time, because work place opened earlier

		wParams.setOpeningTime(1 * 3600.0);
		wParams.setClosingTime(2 * 3600.0);

		// only the home-activity should add to the score
		assertEquals(score_home + 3. * score_work, calcScore(f), EPSILON);

		// test 4: work opens and closes at same time but while agent is there
		// (this may be useful to emulate that the activity is never open ... such as pt interaction)

		wParams.setOpeningTime(8. * 3600.0 + 15. * 60.);
		wParams.setClosingTime(8. * 3600.0 + 15. * 60.);
		// (note that even _some_ opening time causes zero score, since the minDuration needs to be overcome!)

		assertEquals(score_home + 3. * score_work, calcScore(f), EPSILON);
	}

	/**
	 * Test the waiting part of the scoring function.
	 */
	@Test
	public void testWaitingTime() {
		Fixture f = new Fixture();
		double waiting = -10.0;
		f.config.planCalcScore().setMarginalUtlOfWaiting_utils_hr(waiting);

		PlanCalcScoreConfigGroup.ActivityParams wParams = f.config.planCalcScore().getActivityParams("w");
		wParams.setOpeningTime(8 * 3600.0); // the agent arrives 30min early
		wParams.setClosingTime(15 * 3600.0); // the agent stays 1h too long

		// the agent spends 1.5h waiting at the work place
		assertEquals(waiting * 1.5, calcScore(f), EPSILON);


		assertEquals(testee_incomeAlphaFactor * waiting * 1.5, calcScore(f, "hetero"), EPSILON);
		assertEquals(testee_incomeAlphaFactor * waiting * 1.5, calcScore(f, "heteroGamma"), EPSILON);

		double perf = +6.0;
		double zeroUtilDurW = getZeroUtilDuration_hrs(3.0, 1.0);
		double zeroUtilDurH = getZeroUtilDuration_hrs(15.0, 1.0);
		f.config.planCalcScore().setPerforming_utils_hr(perf);
		perf = perf * testee_incomeAlphaFactor;
		double score = perf * 3.0 * Math.log(2 / zeroUtilDurW) + perf * 3.0 * Math.log(2.75 / zeroUtilDurW) + perf * 3.0 * Math.log(1.5 / zeroUtilDurW) + perf * 15.0 * Math.log(14.75 / zeroUtilDurH);

		double std = 0.25;
		double mean = Math.log(1) - (std * std) / 2;
		double lnBetaFactor = Math.exp(mean + std * testee_betaFactor);

		assertEquals(score + (perf - lnBetaFactor * perf) * 1.5, calcScore(f, "heteroAlpha"), EPSILON);
	}

	/**
	 * Test the scoring function in regards to early departures.
	 */
	@Test
	public void testEarlyDeparture() {
		Fixture f = new Fixture();
		double disutility = -10.0;
		f.config.planCalcScore().setEarlyDeparture_utils_hr(disutility);

		PlanCalcScoreConfigGroup.ActivityParams wParams = f.config.planCalcScore().getActivityParams("w");
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

		PlanCalcScoreConfigGroup.ActivityParams wParams = f.config.planCalcScore().getActivityParams("w");
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

		PlanCalcScoreConfigGroup.ActivityParams wParams = f.config.planCalcScore().getActivityParams("w");
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
		final double traveling1 = -6.0;
		f.config.planCalcScore().getModes().get(TransportMode.car).setMarginalUtilityOfTraveling(traveling1);

		ScoringFunction testee = getScoringFunctionInstance(f, f.person);
		testee.handleActivity((Activity) f.plan.getPlanElements().get(0));
		testee.handleLeg((Leg) f.plan.getPlanElements().get(1));
		testee.handleActivity((Activity) f.plan.getPlanElements().get(2));
		testee.handleLeg((Leg) f.plan.getPlanElements().get(3));

		testee.agentStuck(16 * 3600 + 7.5 * 60);
		testee.finish();
		testee.getScore();

		assertEquals(24 * -18.0 - 6.0 * 0.50, testee.getScore(), EPSILON); // stuck penalty + 30min traveling

		// test 2 where traveling has the biggest impact
		f.config.planCalcScore().setLateArrival_utils_hr(-3.0);
		final double traveling = -6.0;
		f.config.planCalcScore().getModes().get(TransportMode.car).setMarginalUtilityOfTraveling(traveling);

		testee = getScoringFunctionInstance(f, f.person);
		testee.handleActivity((Activity) f.plan.getPlanElements().get(0));
		testee.handleLeg((Leg) f.plan.getPlanElements().get(1));
		testee.handleActivity((Activity) f.plan.getPlanElements().get(2));
		testee.handleLeg((Leg) f.plan.getPlanElements().get(3));
		testee.agentStuck(16 * 3600 + 7.5 * 60);
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
		double monetaryDistanceRateCar1 = -0.00001;
		f.config.planCalcScore().getModes().get(TransportMode.car).setMonetaryDistanceRate(monetaryDistanceRateCar1);

		assertEquals(-0.255, calcScore(f), EPSILON);

		// test 2 where MonetaryDistanceCostRate is fixed to -1.0
		double monetaryDistanceRateCar = -1.0;
		f.config.planCalcScore().getModes().get(TransportMode.car).setMonetaryDistanceRate(monetaryDistanceRateCar);
		f.config.planCalcScore().setMarginalUtilityOfMoney(0.5);

		assertEquals(-12750.0, calcScore(f), EPSILON);
	}

	@Test
	public void testDistanceCostScoringPt() {
		Fixture f = new Fixture();
		// test 1 where marginalUtitityOfMoney is fixed to 1.0
		f.config.planCalcScore().setMarginalUtilityOfMoney(1.0);
		//		this.config.charyparNagelScoring().setMarginalUtlOfDistancePt(-0.00001);
		double monetaryDistanceRatePt1 = -0.00001;
		f.config.planCalcScore().getModes().get(TransportMode.pt).setMonetaryDistanceRate(monetaryDistanceRatePt1);

		assertEquals(-0.20, calcScore(f), EPSILON);

		// test 2 where MonetaryDistanceCostRate is fixed to -1.0
		double monetaryDistanceRatePt = -1.0;
		f.config.planCalcScore().getModes().get(TransportMode.pt).setMonetaryDistanceRate(monetaryDistanceRatePt);
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
		params.setTypicalDuration(8 * 3600);
		f.config.planCalcScore().addActivityParams(params);
		f.config.planCalcScore().getActivityParams("h").setTypicalDuration(6.0 * 3600);

		double perf = +6.0;
		f.config.planCalcScore().setPerforming_utils_hr(perf);
		double zeroUtilDurW = getZeroUtilDuration_hrs(3.0, 1.0);
		double zeroUtilDurH = getZeroUtilDuration_hrs(6.0, 1.0);
		double zeroUtilDurH2 = getZeroUtilDuration_hrs(8.0, 1.0);

		assertEquals(perf * 3.0 * Math.log(2.5 / zeroUtilDurW) + perf * 3.0 * Math.log(2.75 / zeroUtilDurW) + perf * 3.0 * Math.log(2.5 / zeroUtilDurW) + perf * 6.0 * Math.log(7.0 / zeroUtilDurH) + perf * 8.0 * Math.log(7.75 / zeroUtilDurH2), calcScore(f), EPSILON);
	}

	/**
	 * Test that the scoring function works even when we don't spend the night at home, but
	 * don't end the day with an ongoing activity at all. This is half of the case
	 * when the first and last activity aren't the same.
	 */
	@Test
	public void testNoNightActivity() {

		double zeroUtilDurW = getZeroUtilDuration_hrs(3.0, 1.0);
		double zeroUtilDurH = getZeroUtilDuration_hrs(7.0, 1.0);
		double perf = +3.0;

		Fixture f = new Fixture();
		// Need to change the typical duration of the home activity
		// for this test, since with the setting of 15 hours for "home",
		// this would amount to a smaller-than-zero expected contribution of
		// the home activity at 7 hours, and smaller-than-zero contributions
		// are truncated, so we wouldn't test anything. :-/
		f.config.planCalcScore().getActivityParams("h").setTypicalDuration(7.0 * 3600);
		f.config.planCalcScore().setPerforming_utils_hr(perf);

		ScoringFunction testee = getScoringFunctionInstance(f, f.person);
		testee.handleActivity((Activity) f.plan.getPlanElements().get(0));
		testee.handleLeg((Leg) f.plan.getPlanElements().get(1));
		testee.handleActivity((Activity) f.plan.getPlanElements().get(2));
		testee.finish();

		assertEquals(perf * 3.0 * Math.log(2.5 / zeroUtilDurW) + perf * 7.0 * Math.log(7.0 / zeroUtilDurH), testee.getScore(), EPSILON);
	}

	/**
	 * Tests if the scoring function correctly handles {@link PersonMoneyEvent}.
	 * It generates one person with one plan having two activities (home, work)
	 * and a car-leg in between. It then tests the scoring function by calling
	 * several methods on an instance of the scoring function with the
	 * aforementioned plan.
	 */
	@Test
	public void testAddMoney() {
		Fixture f = new Fixture();

		// score the same plan twice
		Person person1 = PopulationUtils.createPerson(Id.create(1, Person.class));
		PlanImpl plan1 = PersonUtils.createAndAddPlan(person1, true);
		Activity act1a = plan1.createAndAddActivity("home", (Id<Link>) null);//, 0, 7.0*3600, 7*3600, false);
		act1a.setEndTime(f.secondLegStartTime);
		Leg leg1 = plan1.createAndAddLeg(TransportMode.car);//, 7*3600, 100, 7*3600+100);
		leg1.setDepartureTime(f.secondLegStartTime);
		leg1.setTravelTime(f.secondLegTravelTime);
		Route route2 = new GenericRouteImpl(null, null);
		leg1.setRoute(route2);
		route2.setDistance(20000.0);
		Activity act1b = plan1.createAndAddActivity("work", (Id<Link>) null);//, 7.0*3600+100, Time.UNDEFINED_TIME, Time.UNDEFINED_TIME, false);
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

	@Test
	public void testUnusualMode() {
		Fixture f = new Fixture();
		Leg leg = (Leg) f.plan.getPlanElements().get(1);
		leg.setMode("sackhuepfen");
		assertEquals(-3.0, calcScore(f), EPSILON); // default for unknown modes
		f.config.planCalcScore().addParam("traveling_sackhuepfen", "-30.0");
		assertEquals(-15.0, calcScore(f), EPSILON);
	}


	private static class Fixture {
		protected Config config = null;
		private Person person = null;
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
			firstLegStartTime = 7 * 3600;
			firstLegTravelTime = 30 * 60;
			secondLegStartTime = 10 * 3600;
			secondLegTravelTime = 15 * 60;
			thirdLegStartTime = 13 * 3600;
			thirdLegTravelTime = 30 * 60;
			fourthLegStartTime = 16 * 3600;
			fourthLegTravelTime = 15 * 60;
			// home act end 7am
			// work 7:30 to 10:00
			// work 10:15 to 13:00
			// work 13:30 to 16:00
			// home 15:15 to ...

			this.config = ConfigUtils.createConfig();
			PlanCalcScoreConfigGroup scoring = this.config.planCalcScore();
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

			scoring.setMarginalUtilityOfMoney(1.);
			scoring.getModes().get(TransportMode.car).setMonetaryDistanceRate(0.0);
			scoring.getModes().get(TransportMode.pt).setMonetaryDistanceRate(0.0);


			// setup activity types h and w for scoring
			PlanCalcScoreConfigGroup.ActivityParams params = new PlanCalcScoreConfigGroup.ActivityParams("h");
			params.setTypicalDuration(15 * 3600);
			scoring.addActivityParams(params);


			params = new PlanCalcScoreConfigGroup.ActivityParams("w");
			params.setTypicalDuration(3 * 3600);
			scoring.addActivityParams(params);

			this.scenario = ScenarioUtils.createScenario(config);
			this.network = (NetworkImpl) this.scenario.getNetwork();
			Node node1 = this.network.createAndAddNode(Id.create("1", Node.class), new Coord(0.0, 0.0));
			Node node2 = this.network.createAndAddNode(Id.create("2", Node.class), new Coord(500.0, 0.0));
			Node node3 = this.network.createAndAddNode(Id.create("3", Node.class), new Coord(5500.0, 0.0));
			Node node4 = this.network.createAndAddNode(Id.create("4", Node.class), new Coord(6000.0, 0.0));
			Node node5 = this.network.createAndAddNode(Id.create("5", Node.class), new Coord(11000.0, 0.0));
			Node node6 = this.network.createAndAddNode(Id.create("6", Node.class), new Coord(11500.0, 0.0));
			Node node7 = this.network.createAndAddNode(Id.create("7", Node.class), new Coord(16500.0, 0.0));
			Node node8 = this.network.createAndAddNode(Id.create("8", Node.class), new Coord(17000.0, 0.0));
			Node node9 = this.network.createAndAddNode(Id.create("9", Node.class), new Coord(22000.0, 0.0));
			Node node10 = this.network.createAndAddNode(Id.create("10", Node.class), new Coord(22500.0, 0.0));

			Link link1 = this.network.createAndAddLink(Id.create("1", Link.class), node1, node2, 500, 25, 3600, 1);
			Link link2 = this.network.createAndAddLink(Id.create("2", Link.class), node2, node3, 25000, 50, 3600, 1);
			Link link3 = this.network.createAndAddLink(Id.create("3", Link.class), node3, node4, 500, 25, 3600, 1);
			this.network.createAndAddLink(Id.create("4", Link.class), node4, node5, 5000, 50, 3600, 1);
			Link link5 = this.network.createAndAddLink(Id.create("5", Link.class), node5, node6, 500, 25, 3600, 1);
			this.network.createAndAddLink(Id.create("6", Link.class), node6, node7, 5000, 50, 3600, 1);
			Link link7 = this.network.createAndAddLink(Id.create("7", Link.class), node7, node8, 500, 25, 3600, 1);
			this.network.createAndAddLink(Id.create("8", Link.class), node8, node9, 5000, 50, 3600, 1);
			Link link9 = this.network.createAndAddLink(Id.create("9", Link.class), node9, node10, 500, 25, 3600, 1);

			this.person = PopulationUtils.createPerson(Id.create("1", Person.class));
			this.plan = PersonUtils.createAndAddPlan(this.person, true);

			this.person.getCustomAttributes().put("incomeAlphaFactor",testee_incomeAlphaFactor);
			this.person.getCustomAttributes().put("betaFactor",testee_betaFactor);

			ActivityImpl firstActivity = this.plan.createAndAddActivity("h", link1.getId());
			firstActivity.setEndTime(firstLegStartTime);

			Leg leg = this.plan.createAndAddLeg(TransportMode.car);
			leg.setDepartureTime(firstLegStartTime);
			leg.setTravelTime(firstLegTravelTime);
			NetworkRoute route1 = new LinkNetworkRouteImpl(link1.getId(), link3.getId());
			route1.setLinkIds(link1.getId(), Arrays.asList(link2.getId()), link3.getId());
			route1.setTravelTime(firstLegTravelTime);
			route1.setDistance(RouteUtils.calcDistanceExcludingStartEndLink(route1, this.network));
			route1.setVehicleId(Id.createVehicleId("dummyVehicle1"));
			leg.setRoute(route1);

			ActivityImpl secondActivity = this.plan.createAndAddActivity("w", link3.getId());
			secondActivity.setStartTime(firstLegStartTime + firstLegTravelTime);
			secondActivity.setEndTime(secondLegStartTime);
			leg = this.plan.createAndAddLeg(TransportMode.pt);
			leg.setDepartureTime(secondLegStartTime);
			leg.setTravelTime(secondLegTravelTime);
			Route route2 = new GenericRouteImpl(link3.getId(), link5.getId());
			route2.setTravelTime(secondLegTravelTime);
			route2.setDistance(20000.0);
			leg.setRoute(route2);

			ActivityImpl thirdActivity = this.plan.createAndAddActivity("w", link5.getId());
			thirdActivity.setStartTime(secondLegStartTime + secondLegTravelTime);
			thirdActivity.setEndTime(thirdLegStartTime);
			leg = this.plan.createAndAddLeg(TransportMode.walk);
			leg.setDepartureTime(thirdLegStartTime);
			leg.setTravelTime(thirdLegTravelTime);
			Route route3 = new GenericRouteImpl(link5.getId(), link7.getId());
			route3.setTravelTime(thirdLegTravelTime);
			route3.setDistance(CoordUtils.calcEuclideanDistance(link5.getCoord(), link7.getCoord()));
			leg.setRoute(route3);

			ActivityImpl fourthActivity = this.plan.createAndAddActivity("w", link7.getId());
			fourthActivity.setStartTime(thirdLegStartTime + thirdLegTravelTime);
			fourthActivity.setEndTime(fourthLegStartTime);
			leg = this.plan.createAndAddLeg(TransportMode.bike);
			leg.setDepartureTime(fourthLegStartTime);
			leg.setTravelTime(fourthLegTravelTime);
			Route route4 = new GenericRouteImpl(link7.getId(), link9.getId());
			route4.setTravelTime(fourthLegTravelTime);
			route4.setDistance(CoordUtils.calcEuclideanDistance(link7.getCoord(), link9.getCoord()));
			leg.setRoute(route4);

			ActivityImpl fifthActivity = this.plan.createAndAddActivity("h", link9.getId());
			fifthActivity.setStartTime(fourthLegStartTime + fourthLegTravelTime);
			this.scenario.getPopulation().addPerson(this.person);
		}
	}
}
