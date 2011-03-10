/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityScoringFunctionTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.meisterk.kti.scoring;

import java.util.HashMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.events.ActivityEndEventImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.facilities.OpeningTimeImpl;
import org.matsim.core.facilities.OpeningTime.DayType;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.core.population.routes.RouteFactory;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.locationchoice.facilityload.FacilityPenalty;
import org.matsim.population.Desires;
import org.matsim.testcases.MatsimTestCase;

import playground.meisterk.kti.config.KtiConfigGroup;
import playground.meisterk.kti.router.KtiPtRouteFactory;

public class ActivityScoringFunctionTest extends MatsimTestCase {

	private final static String ACTIVITY_TYPE_HOME = "home";
	
	private final static Id TEST_PERSON_ID = new IdImpl("123");
	private final static Id FACILITY_HOME_ID = new IdImpl(1);
	private final static Id FACILITY_WORK_ID = new IdImpl(3);
	private final static Id FACILITY_LEISURE_ID = new IdImpl(5);
	private final static Id FACILITY_SHOP_ID = new IdImpl(7);

	private ScenarioImpl scenario;
	
	private KtiConfigGroup ktiConfigGroup;

	/*package*/ static final Logger logger = Logger.getLogger(ActivityScoringFunctionTest.class);

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		// generate config
		Config config = super.loadConfig(null);
		PlanCalcScoreConfigGroup scoring = config.planCalcScore();
		scoring.setBrainExpBeta(2.0);
		scoring.setLateArrival_utils_hr(0.0);
		scoring.setEarlyDeparture_utils_hr(-6.0);
		scoring.setPerforming_utils_hr(+6.0);
		scoring.setTraveling_utils_hr(0.0);
		scoring.setTravelingPt_utils_hr(0.0);

//		scoring.setMarginalUtlOfDistanceCar(0.0);
		scoring.setMonetaryDistanceCostRateCar(0.0) ;
		scoring.setMarginalUtilityOfMoney(1.) ;

		scoring.setWaiting_utils_hr(0.0);

		config.planomat().setDoLogging(false);

		this.ktiConfigGroup = new KtiConfigGroup();
		config.addModule(KtiConfigGroup.GROUP_NAME, this.ktiConfigGroup);

		// generate scenario
		this.scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);

		// generate person
		PersonImpl person = new PersonImpl(TEST_PERSON_ID);
		this.scenario.getPopulation().addPerson(person);

		// generate facilities
		ActivityFacilitiesImpl facilities = scenario.getActivityFacilities();
		ActivityFacilityImpl facilityHome = facilities.createFacility(new IdImpl(1), new CoordImpl(0.0, 0.0));
		facilityHome.createActivityOption(ACTIVITY_TYPE_HOME);
		ActivityFacilityImpl facilityWork = facilities.createFacility(new IdImpl(3), new CoordImpl(1000.0, 1000.0));
		facilityWork.createActivityOption("work_sector3");
		ActivityFacilityImpl facilityLeisure = facilities.createFacility(new IdImpl(5), new CoordImpl(1000.0, 1010.0));
		facilityLeisure.createActivityOption("leisure");
		ActivityFacilityImpl facilityShop = facilities.createFacility(new IdImpl(7), new CoordImpl(500.0, 0.0));
		facilityShop.createActivityOption("shop");

		// generate network
		NetworkImpl network = scenario.getNetwork();

		Node node1 = network.createAndAddNode(new IdImpl(1), new CoordImpl(    0.0, 0.0));
		Node node2 = network.createAndAddNode(new IdImpl(2), new CoordImpl(  500.0, 0.0));
		Node node3 = network.createAndAddNode(new IdImpl(3), new CoordImpl( 5500.0, 0.0));
		Node node4 = network.createAndAddNode(new IdImpl(4), new CoordImpl( 6000.0, 0.0));
		Node node5 = network.createAndAddNode(new IdImpl(5), new CoordImpl(11000.0, 0.0));
		network.createAndAddLink(new IdImpl(1020), node1, node2, 500, 25, 3600, 1);
		network.createAndAddLink(new IdImpl(2010), node2, node1, 500, 25, 3600, 1);
		network.createAndAddLink(new IdImpl(2030), node2, node3, 500, 25, 3600, 1);
		network.createAndAddLink(new IdImpl(3020), node3, node2, 500, 25, 3600, 1);
		network.createAndAddLink(new IdImpl(3040), node3, node4, 500, 25, 3600, 1);
		network.createAndAddLink(new IdImpl(4030), node4, node3, 500, 25, 3600, 1);
		network.createAndAddLink(new IdImpl(4050), node4, node5, 500, 25, 3600, 1);
		network.createAndAddLink(new IdImpl(5040), node5, node4, 500, 25, 3600, 1);

		RouteFactory ptRouteFactory = new KtiPtRouteFactory(null);
		network.getFactory().setRouteFactory(TransportMode.pt, ptRouteFactory);

	}

	public void testNoTrips() {
		
		PersonImpl person = (PersonImpl) this.scenario.getPopulation().getPersons().get(TEST_PERSON_ID);
		NetworkImpl network = this.scenario.getNetwork();
		ActivityFacility facilityHome = this.scenario.getActivityFacilities().getFacilities().get(FACILITY_HOME_ID);
		
		// generate desires
		Desires desires = person.createDesires("test desires");
		desires.putActivityDuration(ACTIVITY_TYPE_HOME, Time.parseTime("24:00:00"));

		// generate plan
		PlanImpl plan = person.createAndAddPlan(true);

		ActivityImpl act = plan.createAndAddActivity(ACTIVITY_TYPE_HOME);
		act.setFacilityId(facilityHome.getId());
		Link link = network.getLinks().get(new IdImpl("2030"));
		act.setLinkId(link.getId());
		act.setCoord(link.getCoord());

		TreeMap<Id, FacilityPenalty> emptyFacilityPenalties = new TreeMap<Id, FacilityPenalty>();
		KTIYear3ScoringFunctionFactory factory = new KTIYear3ScoringFunctionFactory(
				this.scenario.getConfig(),
				this.ktiConfigGroup,
				emptyFacilityPenalties,
				this.scenario.getActivityFacilities());
		
		EventsToScore eventsToScore = new EventsToScore(this.scenario.getPopulation(), factory, this.scenario.getConfig().planCalcScore().getLearningRate());

		double commonEndAndStartTime = Time.parseTime("23:40:00");
		eventsToScore.handleEvent(new ActivityEndEventImpl(commonEndAndStartTime, TEST_PERSON_ID, link.getId(), facilityHome.getId(), ACTIVITY_TYPE_HOME));
//		eventsToScore.handleEvent(new ActivityStartEventImpl(commonEndAndStartTime, TEST_PERSON_ID, link.getId(), facilityHome.getId(), ACTIVITY_TYPE_HOME));
		eventsToScore.finish();

		Double expectedScore = new Double(60.0);
		assertEquals(expectedScore, eventsToScore.getAgentScore(TEST_PERSON_ID), MatsimTestCase.EPSILON);
		
	}
	
	private void createAndAddMultiActivityPlan() {
		
		PersonImpl person = (PersonImpl) this.scenario.getPopulation().getPersons().get(TEST_PERSON_ID);
		NetworkImpl network = this.scenario.getNetwork();
		ActivityFacilityImpl facilityHome = (ActivityFacilityImpl) this.scenario.getActivityFacilities().getFacilities().get(FACILITY_HOME_ID);
		ActivityFacilityImpl facilityWork = (ActivityFacilityImpl) this.scenario.getActivityFacilities().getFacilities().get(FACILITY_WORK_ID);
		ActivityFacilityImpl facilityLeisure = (ActivityFacilityImpl) this.scenario.getActivityFacilities().getFacilities().get(FACILITY_LEISURE_ID);
		ActivityFacilityImpl facilityShop = (ActivityFacilityImpl) this.scenario.getActivityFacilities().getFacilities().get(FACILITY_SHOP_ID);
			
		// generate desires
		Desires desires = person.createDesires("test desires");
		desires.putActivityDuration(ACTIVITY_TYPE_HOME, Time.parseTime("15:40:00"));
		desires.putActivityDuration("work_sector3", Time.parseTime("07:00:00"));
		desires.putActivityDuration("leisure", Time.parseTime("01:00:00"));
		desires.putActivityDuration("shop", Time.parseTime("00:20:00"));

		// generate plan
		PlanImpl plan = person.createAndAddPlan(true);

		ActivityImpl act = plan.createAndAddActivity(ACTIVITY_TYPE_HOME);
		act.setFacilityId(facilityHome.getId());
		Link link = network.getLinks().get(new IdImpl("2030"));
		act.setLinkId(link.getId());
		act.setCoord(link.getCoord());
		LegImpl leg = plan.createAndAddLeg(TransportMode.car);
		Route route = network.getFactory().createRoute(TransportMode.car, new IdImpl("2030"), new IdImpl("3040"));
		leg.setRoute(route);

		act = plan.createAndAddActivity("work_sector3");
		act.setFacilityId(facilityWork.getId());
		link = network.getLinks().get(new IdImpl("3040"));
		act.setLinkId(link.getId());
		act.setCoord(link.getCoord());
		leg = plan.createAndAddLeg(TransportMode.pt);
		route = network.getFactory().createRoute(TransportMode.pt, new IdImpl("3040"), new IdImpl("4050"));
		route.setDistance(100.0);
		((GenericRoute) route).setRouteDescription(new IdImpl("3040"), "bla", new IdImpl("4050"));
		leg.setRoute(route);

		act = plan.createAndAddActivity("leisure");
		act.setFacilityId(facilityLeisure.getId());
		link = network.getLinks().get(new IdImpl("4050"));
		act.setLinkId(link.getId());
		act.setCoord(link.getCoord());
		leg = plan.createAndAddLeg(TransportMode.pt);
		route = network.getFactory().createRoute(TransportMode.pt, new IdImpl("4050"), new IdImpl("3040"));
		route.setDistance(100.0);
		((GenericRoute) route).setRouteDescription(new IdImpl("3040"), "bla", new IdImpl("4050"));
		leg.setRoute(route);

		act = plan.createAndAddActivity("work_sector3");
		act.setFacilityId(facilityWork.getId());
		link = network.getLinks().get(new IdImpl("3040"));
		act.setLinkId(link.getId());
		act.setCoord(link.getCoord());
		leg = plan.createAndAddLeg(TransportMode.car);
		route = network.getFactory().createRoute(TransportMode.car, new IdImpl("3040"), new IdImpl("2030"));
		leg.setRoute(route);

		act = plan.createAndAddActivity(ACTIVITY_TYPE_HOME);
		act.setFacilityId(facilityHome.getId());
		link = network.getLinks().get(new IdImpl("2030"));
		act.setLinkId(link.getId());
		act.setCoord(link.getCoord());
		leg = plan.createAndAddLeg(TransportMode.bike);
		route = network.getFactory().createRoute(TransportMode.bike, new IdImpl("2030"), new IdImpl("1020"));
		leg.setRoute(route);

		act = plan.createAndAddActivity("shop");
		act.setFacilityId(facilityShop.getId());
		link = network.getLinks().get(new IdImpl("1020"));
		act.setLinkId(link.getId());
		act.setCoord(link.getCoord());
		leg = plan.createAndAddLeg(TransportMode.bike);
		route = network.getFactory().createRoute(TransportMode.bike, new IdImpl("1020"), new IdImpl("2030"));
		leg.setRoute(route);

		act = plan.createAndAddActivity(ACTIVITY_TYPE_HOME);
		act.setFacilityId(facilityHome.getId());
		link = network.getLinks().get(new IdImpl("2030"));
		act.setLinkId(link.getId());
		act.setCoord(link.getCoord());
		
		
	}
	
	public void testAlwaysOpen() {

		this.createAndAddMultiActivityPlan();
		
		// []{end home, work_sector3, leisure, work_Sector3, home, shop, start home, finish, reset}
		String[] expectedTooShortDurationsSequence = new String[]{"00:00:00", "00:00:00", "00:10:00",  "00:10:00", "00:10:00", "00:10:00", "00:10:00", "02:40:00", "00:00:00"};
		String[] expectedWaitingTimeSequence = new String[]{"00:00:00", "00:00:00", "00:00:00", "00:00:00", "00:00:00", "00:00:00", "00:00:00", "00:00:00", "00:00:00"};
		HashMap<String, String[]> expectedAccumulatedActivityDurations = new HashMap<String, String[]>();
		expectedAccumulatedActivityDurations.put(ACTIVITY_TYPE_HOME, new String[]{null, null, null, null, "02:30:00", "02:30:00", "02:30:00", "02:30:00", null});
		expectedAccumulatedActivityDurations.put("work_sector3", new String[]{null, "06:00:00", "06:00:00", "08:00:00", "08:00:00", "08:00:00", "08:00:00", "08:00:00", null});
		expectedAccumulatedActivityDurations.put("leisure", new String[]{null, null, "00:20:00", "00:20:00", "00:20:00", "00:20:00", "00:20:00", "00:20:00", null});
		expectedAccumulatedActivityDurations.put("shop", new String[]{null, null, null, null, null, "06:00:00", "06:00:00", "06:00:00", null});
		String[] expectedNegativeDurationsSequence = new String[]{"00:00:00", "00:00:00", "00:00:00", "00:00:00", "00:00:00", "00:00:00", "00:00:00", "-02:00:00", "00:00:00"};
		this.runTest(
				expectedTooShortDurationsSequence,
				expectedWaitingTimeSequence,
				expectedAccumulatedActivityDurations,
				expectedNegativeDurationsSequence);
	}

	public void testOpenLongEnough() {

		this.createAndAddMultiActivityPlan();

		ActivityOptionImpl actOpt = null;

		actOpt = (ActivityOptionImpl) this.scenario.getActivityFacilities().getFacilities().get(new IdImpl("3")).getActivityOptions().get("work_sector3");
		actOpt.addOpeningTime(new OpeningTimeImpl(DayType.wed, Time.parseTime("07:00:00"), Time.parseTime("18:00:00")));

		actOpt = (ActivityOptionImpl) this.scenario.getActivityFacilities().getFacilities().get(new IdImpl("5")).getActivityOptions().get("leisure");
		actOpt.addOpeningTime(new OpeningTimeImpl(DayType.wed, Time.parseTime("11:00:00"), Time.parseTime("16:00:00")));

		// []{end home, work_sector3, leisure, work_Sector3, home, shop, start home, finish, reset}
		String[] expectedTooShortDurationsSequence = new String[]{"00:00:00", "00:00:00", "00:10:00",  "00:10:00", "00:10:00", "00:10:00", "00:10:00", "02:40:00", "00:00:00"};
		String[] expectedWaitingTimeSequence = new String[]{"00:00:00", "00:00:00", "00:00:00", "00:00:00", "00:00:00", "00:00:00", "00:00:00", "00:00:00", "00:00:00"};
		HashMap<String, String[]> expectedAccumulatedActivityDurations = new HashMap<String, String[]>();
		expectedAccumulatedActivityDurations.put(ACTIVITY_TYPE_HOME, new String[]{null, null, null, null, "02:30:00", "02:30:00", "02:30:00", "02:30:00", null});
		expectedAccumulatedActivityDurations.put("work_sector3", new String[]{null, "06:00:00", "06:00:00", "08:00:00", "08:00:00", "08:00:00", "08:00:00", "08:00:00", null});
		expectedAccumulatedActivityDurations.put("leisure", new String[]{null, null, "00:20:00", "00:20:00", "00:20:00", "00:20:00", "00:20:00", "00:20:00", null});
		expectedAccumulatedActivityDurations.put("shop", new String[]{null, null, null, null, null, "06:00:00", "06:00:00", "06:00:00", null});
		String[] expectedNegativeDurationsSequence = new String[]{"00:00:00", "00:00:00", "00:00:00", "00:00:00", "00:00:00", "00:00:00", "00:00:00", "-02:00:00", "00:00:00"};
		this.runTest(
				expectedTooShortDurationsSequence,
				expectedWaitingTimeSequence,
				expectedAccumulatedActivityDurations,
				expectedNegativeDurationsSequence);

	}

	public void testWaiting() {

		this.createAndAddMultiActivityPlan();

		ActivityOptionImpl actOpt = null;

		actOpt = (ActivityOptionImpl) this.scenario.getActivityFacilities().getFacilities().get(new IdImpl("3")).getActivityOptions().get("work_sector3");
		actOpt.addOpeningTime(new OpeningTimeImpl(DayType.wed, Time.parseTime("07:00:00"), Time.parseTime("14:00:00")));
		actOpt.addOpeningTime(new OpeningTimeImpl(DayType.wed, Time.parseTime("15:15:00"), Time.parseTime("20:00:00")));

		actOpt = (ActivityOptionImpl) this.scenario.getActivityFacilities().getFacilities().get(new IdImpl("5")).getActivityOptions().get("leisure");
		actOpt.addOpeningTime(new OpeningTimeImpl(DayType.wed, Time.parseTime("11:00:00"), Time.parseTime("14:00:00")));

		// []{end home, work_sector3, leisure, work_Sector3, home, shop, start home, finish, reset}
		String[] expectedTooShortDurationsSequence = new String[]{"00:00:00", "00:00:00", "00:30:00",  "00:30:00", "00:30:00", "00:30:00", "00:30:00", "03:00:00", "00:00:00"};
		String[] expectedWaitingTimeSequence = new String[]{"00:00:00", "00:30:00", "00:50:00", "01:05:00", "01:05:00", "01:05:00", "01:05:00", "01:05:00","00:00:00"};
		HashMap<String, String[]> expectedAccumulatedActivityDurations = new HashMap<String, String[]>();
		expectedAccumulatedActivityDurations.put(ACTIVITY_TYPE_HOME, new String[]{null, null, null, null, "02:30:00", "02:30:00", "02:30:00", "02:30:00", null});
		expectedAccumulatedActivityDurations.put("work_sector3", new String[]{null, "05:30:00", "05:30:00", "07:15:00", "07:15:00", "07:15:00", "07:15:00", "07:15:00", null});
		expectedAccumulatedActivityDurations.put("leisure", new String[]{null, null, "00:00:00", "00:00:00", "00:00:00", "00:00:00", "00:00:00", "00:00:00", null});
		expectedAccumulatedActivityDurations.put("shop", new String[]{null, null, null, null, null, "06:00:00", "06:00:00", "06:00:00", null});
		String[] expectedNegativeDurationsSequence = new String[]{"00:00:00", "00:00:00", "00:00:00", "00:00:00", "00:00:00", "00:00:00", "00:00:00", "-02:00:00", "00:00:00"};
		this.runTest(
				expectedTooShortDurationsSequence,
				expectedWaitingTimeSequence,
				expectedAccumulatedActivityDurations,
				expectedNegativeDurationsSequence);

	}

	public void testOverlapping() {

		this.createAndAddMultiActivityPlan();

		ActivityOptionImpl actOpt = null;

		actOpt = (ActivityOptionImpl) this.scenario.getActivityFacilities().getFacilities().get(new IdImpl("3")).getActivityOptions().get("work_sector3");
		actOpt.addOpeningTime(new OpeningTimeImpl(DayType.wed, Time.parseTime("07:00:00"), Time.parseTime("10:00:00")));
		actOpt.addOpeningTime(new OpeningTimeImpl(DayType.wed, Time.parseTime("10:30:00"), Time.parseTime("14:00:00")));
		actOpt.addOpeningTime(new OpeningTimeImpl(DayType.wed, Time.parseTime("15:15:00"), Time.parseTime("20:00:00")));

		actOpt = (ActivityOptionImpl) this.scenario.getActivityFacilities().getFacilities().get(new IdImpl("5")).getActivityOptions().get("leisure");
		actOpt.addOpeningTime(new OpeningTimeImpl(DayType.wed, Time.parseTime("11:00:00"), Time.parseTime("14:00:00")));

		actOpt = (ActivityOptionImpl) this.scenario.getActivityFacilities().getFacilities().get(new IdImpl("7")).getActivityOptions().get("shop");
		actOpt.addOpeningTime(new OpeningTimeImpl(DayType.wed, Time.parseTime("12:00:00"), Time.parseTime("27:00:00")));

		// []{end home, work_sector3, leisure, work_Sector3, home, shop, start home, finish, reset}
		String[] expectedTooShortDurationsSequence = new String[]{"00:00:00", "00:00:00", "00:30:00",  "00:30:00", "00:30:00", "00:30:00", "00:30:00", "03:00:00", "00:00:00"};
		String[] expectedWaitingTimeSequence = new String[]{"00:00:00", "01:00:00", "01:20:00", "01:35:00", "01:35:00", "04:35:00", "04:35:00", "04:35:00", "00:00:00"};
		HashMap<String, String[]> expectedAccumulatedActivityDurations = new HashMap<String, String[]>();
		expectedAccumulatedActivityDurations.put(ACTIVITY_TYPE_HOME, new String[]{null, null, null, null, "02:30:00", "02:30:00", "02:30:00", "02:30:00", null});
		expectedAccumulatedActivityDurations.put("work_sector3", new String[]{null, "05:00:00", "05:00:00", "06:45:00", "06:45:00", "06:45:00", "06:45:00", "06:45:00", null});
		expectedAccumulatedActivityDurations.put("leisure", new String[]{null, null, "00:00:00", "00:00:00", "00:00:00", "00:00:00", "00:00:00", "00:00:00", null});
		expectedAccumulatedActivityDurations.put("shop", new String[]{null, null, null, null, null, "03:00:00", "03:00:00", "03:00:00", null});
		String[] expectedNegativeDurationsSequence = new String[]{"00:00:00", "00:00:00", "00:00:00", "00:00:00", "00:00:00", "00:00:00", "00:00:00", "-02:00:00", "00:00:00"};
		this.runTest(
				expectedTooShortDurationsSequence,
				expectedWaitingTimeSequence,
				expectedAccumulatedActivityDurations,
				expectedNegativeDurationsSequence);

	}

	protected void runTest(
			String[] expectedTooShortDurationsSequence,
			String[] expectedWaitingTimeSequence,
			HashMap<String, String[]> expectedAccumulatedActivityDurations,
			String[] expectedNegativeDurationsSequence) {

		TreeMap<Id, FacilityPenalty> emptyFacilityPenalties = new TreeMap<Id, FacilityPenalty>();
		KTIYear3ScoringFunctionFactory factory = new KTIYear3ScoringFunctionFactory(
				this.scenario.getConfig(),
				this.ktiConfigGroup,
				emptyFacilityPenalties,
				this.scenario.getActivityFacilities());
		
		Plan plan = this.scenario.getPopulation().getPersons().get(TEST_PERSON_ID).getSelectedPlan(); 
		ScoringFunction testee = factory.createNewScoringFunction(plan);

		assertTrue(testee instanceof ScoringFunctionAccumulator);
		assertEquals(1, ((ScoringFunctionAccumulator) testee).getActivityScoringFunctions().size());
		assertEquals(ActivityScoringFunction.class, ((ScoringFunctionAccumulator) testee).getActivityScoringFunctions().get(0).getClass());
		ActivityScoringFunction asf = (ActivityScoringFunction) ((ScoringFunctionAccumulator) testee).getActivityScoringFunctions().get(0);

		testee.endActivity(Time.parseTime("08:00:00"));

		assertEquals(0, asf.getAccumulatedDurations().size());
		assertEquals(expectedTooShortDurationsSequence[0], Time.writeTime(asf.getAccumulatedTooShortDuration()));
		assertEquals(expectedWaitingTimeSequence[0], Time.writeTime(asf.getTimeSpentWaiting()));
		assertEquals(expectedNegativeDurationsSequence[0], Time.writeTime(asf.getAccumulatedNegativeDuration()));

		testee.startLeg(Time.parseTime("08:00:00"), (LegImpl) plan.getPlanElements().get(1));
		testee.endLeg(Time.parseTime("08:30:00"));
		testee.startActivity(Time.parseTime("08:30:00"), (ActivityImpl) plan.getPlanElements().get(2));
		testee.endActivity(Time.parseTime("14:30:00"));

		for (String actType : expectedAccumulatedActivityDurations.keySet()) {
			if (asf.getAccumulatedDurations().containsKey(actType)) {
				assertEquals(expectedAccumulatedActivityDurations.get(actType)[1], Time.writeTime(asf.getAccumulatedDurations().get(actType)));
			}
		}
		assertEquals(expectedTooShortDurationsSequence[1], Time.writeTime(asf.getAccumulatedTooShortDuration()));
		assertEquals(expectedWaitingTimeSequence[1], Time.writeTime(asf.getTimeSpentWaiting()));
		assertEquals(expectedNegativeDurationsSequence[1], Time.writeTime(asf.getAccumulatedNegativeDuration()));

		testee.startLeg(Time.parseTime("14:30:00"), (LegImpl) plan.getPlanElements().get(3));
		testee.endLeg(Time.parseTime("14:35:00"));
		testee.startActivity(Time.parseTime("14:35:00"), (ActivityImpl) plan.getPlanElements().get(4));
		testee.endActivity(Time.parseTime("14:55:00"));

		for (String actType : expectedAccumulatedActivityDurations.keySet()) {
			if (asf.getAccumulatedDurations().containsKey(actType)) {
				assertEquals(expectedAccumulatedActivityDurations.get(actType)[2], Time.writeTime(asf.getAccumulatedDurations().get(actType)));
			}
		}
		assertEquals(expectedTooShortDurationsSequence[2], Time.writeTime(asf.getAccumulatedTooShortDuration()));
		assertEquals(expectedWaitingTimeSequence[2], Time.writeTime(asf.getTimeSpentWaiting()));
		assertEquals(expectedNegativeDurationsSequence[2], Time.writeTime(asf.getAccumulatedNegativeDuration()));

		testee.startLeg(Time.parseTime("14:55:00"), (LegImpl) plan.getPlanElements().get(5));
		testee.endLeg(Time.parseTime("15:00:00"));
		testee.startActivity(Time.parseTime("15:00:00"), (ActivityImpl) plan.getPlanElements().get(6));
		testee.endActivity(Time.parseTime("17:00:00"));

		for (String actType : expectedAccumulatedActivityDurations.keySet()) {
			if (asf.getAccumulatedDurations().containsKey(actType)) {
				assertEquals(expectedAccumulatedActivityDurations.get(actType)[3], Time.writeTime(asf.getAccumulatedDurations().get(actType)));
			}
		}
		assertEquals(expectedTooShortDurationsSequence[3], Time.writeTime(asf.getAccumulatedTooShortDuration()));
		assertEquals(expectedWaitingTimeSequence[3], Time.writeTime(asf.getTimeSpentWaiting()));
		assertEquals(expectedNegativeDurationsSequence[3], Time.writeTime(asf.getAccumulatedNegativeDuration()));

		testee.startLeg(Time.parseTime("17:00:00"), (LegImpl) plan.getPlanElements().get(7));
		testee.endLeg(Time.parseTime("17:30:00"));
		testee.startActivity(Time.parseTime("17:30:00"), (ActivityImpl) plan.getPlanElements().get(8));
		testee.endActivity(Time.parseTime("20:00:00"));

		for (String actType : expectedAccumulatedActivityDurations.keySet()) {
			if (asf.getAccumulatedDurations().containsKey(actType)) {
				assertEquals(expectedAccumulatedActivityDurations.get(actType)[4], Time.writeTime(asf.getAccumulatedDurations().get(actType)));
			}
		}
		assertEquals(expectedTooShortDurationsSequence[4], Time.writeTime(asf.getAccumulatedTooShortDuration()));
		assertEquals(expectedWaitingTimeSequence[4], Time.writeTime(asf.getTimeSpentWaiting()));
		assertEquals(expectedNegativeDurationsSequence[4], Time.writeTime(asf.getAccumulatedNegativeDuration()));

		testee.startLeg(Time.parseTime("20:00:00"), (LegImpl) plan.getPlanElements().get(9));
		testee.endLeg(Time.parseTime("24:00:00"));
		testee.startActivity(Time.parseTime("24:00:00"), (ActivityImpl) plan.getPlanElements().get(10));
		testee.endActivity(Time.parseTime("30:00:00"));

		for (String actType : expectedAccumulatedActivityDurations.keySet()) {
			if (asf.getAccumulatedDurations().containsKey(actType)) {
				assertEquals(expectedAccumulatedActivityDurations.get(actType)[5], Time.writeTime(asf.getAccumulatedDurations().get(actType)));
			}
		}
		assertEquals(expectedTooShortDurationsSequence[5], Time.writeTime(asf.getAccumulatedTooShortDuration()));
		assertEquals(expectedWaitingTimeSequence[5], Time.writeTime(asf.getTimeSpentWaiting()));
		assertEquals(expectedNegativeDurationsSequence[5], Time.writeTime(asf.getAccumulatedNegativeDuration()));

		testee.startLeg(Time.parseTime("30:00:00"), (LegImpl) plan.getPlanElements().get(11));
		testee.endLeg(Time.parseTime("34:00:00"));
		testee.startActivity(Time.parseTime("34:00:00"), (ActivityImpl) plan.getPlanElements().get(12));

		for (String actType : expectedAccumulatedActivityDurations.keySet()) {
			if (asf.getAccumulatedDurations().containsKey(actType)) {
				assertEquals(expectedAccumulatedActivityDurations.get(actType)[6], Time.writeTime(asf.getAccumulatedDurations().get(actType)));
			}
		}
		assertEquals(expectedTooShortDurationsSequence[6], Time.writeTime(asf.getAccumulatedTooShortDuration()));
		assertEquals(expectedWaitingTimeSequence[6], Time.writeTime(asf.getTimeSpentWaiting()));
		assertEquals(expectedNegativeDurationsSequence[6], Time.writeTime(asf.getAccumulatedNegativeDuration()));

		testee.finish();

		for (String actType : expectedAccumulatedActivityDurations.keySet()) {
			if (asf.getAccumulatedDurations().containsKey(actType)) {
				assertEquals(expectedAccumulatedActivityDurations.get(actType)[7], Time.writeTime(asf.getAccumulatedDurations().get(actType)));
			}
		}
		assertEquals(expectedTooShortDurationsSequence[7], Time.writeTime(asf.getAccumulatedTooShortDuration()));
		assertEquals(expectedWaitingTimeSequence[7], Time.writeTime(asf.getTimeSpentWaiting()));
		assertEquals(expectedNegativeDurationsSequence[7], Time.writeTime(asf.getAccumulatedNegativeDuration()));

		double expectedScore = 0.0;
		expectedScore += factory.getParams().marginalUtilityOfEarlyDeparture_s * Time.parseTime(expectedTooShortDurationsSequence[7]);
		expectedScore += factory.getParams().marginalUtilityOfWaiting_s * Time.parseTime(expectedWaitingTimeSequence[7]);
		expectedScore += factory.getParams().marginalUtilityOfLateArrival_s * 2 * Time.parseTime(expectedNegativeDurationsSequence[7]);
		double duration, zeroUtilityDuration, typicalDuration;
		for (String actType : expectedAccumulatedActivityDurations.keySet()) {
			if (asf.getAccumulatedDurations().containsKey(actType)) {
				typicalDuration = ((PersonImpl) this.scenario.getPopulation().getPersons().get(TEST_PERSON_ID)).getDesires().getActivityDuration(actType);
				duration = Time.parseTime(expectedAccumulatedActivityDurations.get(actType)[7]);
				zeroUtilityDuration = (typicalDuration / 3600.0) * Math.exp( -10.0 / (typicalDuration / 3600.0) / ActivityScoringFunction.DEFAULT_PRIORITY);
				double utilPerf = factory.getParams().marginalUtilityOfPerforming_s * typicalDuration * Math.log((duration / 3600.0) / zeroUtilityDuration);
				double utilWait = factory.getParams().marginalUtilityOfWaiting_s * duration;
				expectedScore += Math.max(0, Math.max(utilPerf, utilWait));

				// check zero utility durations
				assertEquals(zeroUtilityDuration, asf.getZeroUtilityDurations().get(actType), MatsimTestCase.EPSILON);

			}
		}

		// score legs
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Leg) {
				Leg leg = (Leg) pe;
				if (leg.getMode().equals(TransportMode.bike)) {
					expectedScore += this.ktiConfigGroup.getConstBike();
				}
			}
		}
		assertEquals(expectedScore, testee.getScore(), EPSILON);

	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		this.scenario = null;
	}

}
