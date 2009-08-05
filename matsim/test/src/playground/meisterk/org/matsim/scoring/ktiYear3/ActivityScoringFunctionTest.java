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

package playground.meisterk.org.matsim.scoring.ktiYear3;

import java.util.HashMap;
import java.util.Random;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.basic.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.basic.v01.facilities.BasicOpeningTime.DayType;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.facilities.ActivityFacilities;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.facilities.OpeningTimeImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.locationchoice.facilityload.FacilityPenalty;
import org.matsim.planomat.Planomat;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.planomat.costestimators.FixedRouteLegTravelTimeEstimator;
import org.matsim.planomat.costestimators.LegTravelTimeEstimatorFactory;
import org.matsim.population.Desires;
import org.matsim.testcases.MatsimTestCase;

import playground.meisterk.kti.scoring.ActivityScoringFunction;
import playground.meisterk.kti.scoring.KTIYear3ScoringFunctionFactory;
import playground.meisterk.org.matsim.config.groups.KtiConfigGroup;

public class ActivityScoringFunctionTest extends MatsimTestCase {

	private final static Id TEST_PERSON_ID = new IdImpl("123");

	private PopulationImpl population;
	private PlanImpl plan;
	private Config config;
	private KtiConfigGroup ktiConfigGroup;
	private ActivityFacilities facilities;
	private NetworkLayer network;
	
	/*package*/ static final Logger logger = Logger.getLogger(ActivityScoringFunctionTest.class);

	protected void setUp() throws Exception {
		super.setUp();
		
		// generate config
		this.config = super.loadConfig(null);
		CharyparNagelScoringConfigGroup scoring = this.config.charyparNagelScoring();
		scoring.setBrainExpBeta(2.0);
		scoring.setLateArrival(0.0);
		scoring.setEarlyDeparture(-6.0);
		scoring.setPerforming(+6.0);
		scoring.setTraveling(0.0);
		scoring.setTravelingPt(0.0);
		scoring.setMarginalUtlOfDistanceCar(0.0);
		scoring.setWaiting(0.0);
		
		this.config.planomat().setDoLogging(false);

		this.ktiConfigGroup = new KtiConfigGroup();
		this.config.addModule(KtiConfigGroup.GROUP_NAME, this.ktiConfigGroup);
		
		// generate person
		this.population = new PopulationImpl();
//		Id personId = new IdImpl("123");
		PersonImpl person = new PersonImpl(TEST_PERSON_ID);
		this.population.getPersons().put(person.getId(), person);

		// generate facilities
		this.facilities = new ActivityFacilitiesImpl();
		ActivityFacility facilityHome = this.facilities.createFacility(new IdImpl(1), new CoordImpl(0.0, 0.0));
		facilityHome.createActivityOption("home");
		ActivityFacility facilityWork = this.facilities.createFacility(new IdImpl(3), new CoordImpl(1000.0, 1000.0));
		facilityWork.createActivityOption("work_sector3");
		ActivityFacility facilityLeisure = this.facilities.createFacility(new IdImpl(5), new CoordImpl(1000.0, 1010.0));
		facilityLeisure.createActivityOption("leisure");
		ActivityFacility facilityShop = this.facilities.createFacility(new IdImpl(7), new CoordImpl(500.0, 0.0));
		facilityShop.createActivityOption("shop");
		
		// generate network
		this.network = new NetworkLayer();
		
		NodeImpl node1 = network.createNode(new IdImpl(1), new CoordImpl(    0.0, 0.0));
		NodeImpl node2 = network.createNode(new IdImpl(2), new CoordImpl(  500.0, 0.0));
		NodeImpl node3 = network.createNode(new IdImpl(3), new CoordImpl( 5500.0, 0.0));
		NodeImpl node4 = network.createNode(new IdImpl(4), new CoordImpl( 6000.0, 0.0));
		NodeImpl node5 = network.createNode(new IdImpl(5), new CoordImpl(11000.0, 0.0));
		network.createLink(new IdImpl(1020), node1, node2, 500, 25, 3600, 1);
		network.createLink(new IdImpl(2010), node2, node1, 500, 25, 3600, 1);
		network.createLink(new IdImpl(2030), node2, node3, 500, 25, 3600, 1);
		network.createLink(new IdImpl(3020), node3, node2, 500, 25, 3600, 1);
		network.createLink(new IdImpl(3040), node3, node4, 500, 25, 3600, 1);
		network.createLink(new IdImpl(4030), node4, node3, 500, 25, 3600, 1);
		network.createLink(new IdImpl(4050), node4, node5, 500, 25, 3600, 1);
		network.createLink(new IdImpl(5040), node5, node4, 500, 25, 3600, 1);
		
		// generate desires
		Desires desires = person.createDesires("test desires");
		desires.putActivityDuration("home", Time.parseTime("15:40:00"));
		desires.putActivityDuration("work_sector3", Time.parseTime("07:00:00"));
		desires.putActivityDuration("leisure", Time.parseTime("01:00:00"));
		desires.putActivityDuration("shop", Time.parseTime("00:20:00"));
		
		// generate plan
		plan = person.createPlan(true);

		ActivityImpl act = plan.createActivity("home", facilityHome);
		LinkImpl link = this.network.getLink("2030");
		act.setLink(link);
		act.setCoord(link.getCoord());
		LegImpl leg = this.plan.createLeg(TransportMode.car);
		NetworkRoute route = (NetworkRoute) network.getFactory().createRoute(TransportMode.car, this.network.getLink("2030"), this.network.getLink("3040"));
		leg.setRoute(route);
		route.setTravelTime(Time.parseTime("00:30:00"));

		act = plan.createActivity("work_sector3", facilityWork);
		link = this.network.getLink("3040");
		act.setLink(link);
		act.setCoord(link.getCoord());
		leg = this.plan.createLeg(TransportMode.pt);
		route = (NetworkRoute) network.getFactory().createRoute(TransportMode.car, this.network.getLink("3040"), this.network.getLink("4050"));
		leg.setRoute(route);
		route.setTravelTime(Time.parseTime("00:05:00"));

		act = plan.createActivity("leisure", facilityLeisure);
		link = this.network.getLink("4050");
		act.setLink(link);
		act.setCoord(link.getCoord());
		leg = this.plan.createLeg(TransportMode.pt);
		route = (NetworkRoute) network.getFactory().createRoute(TransportMode.car, this.network.getLink("4050"), this.network.getLink("3040"));
		leg.setRoute(route);
		route.setTravelTime(Time.parseTime("00:05:00"));

		act = plan.createActivity("work_sector3", facilityWork);
		link = this.network.getLink("3040");
		act.setLink(link);
		act.setCoord(link.getCoord());
		leg = this.plan.createLeg(TransportMode.car);
		route = (NetworkRoute) network.getFactory().createRoute(TransportMode.car, this.network.getLink("3040"), this.network.getLink("2030"));
		leg.setRoute(route);
		route.setTravelTime(Time.parseTime("00:30:00"));
		
		act = plan.createActivity("home", facilityHome);
		link = this.network.getLink("2030");
		act.setLink(link);
		act.setCoord(link.getCoord());
		leg = this.plan.createLeg(TransportMode.bike);
		route = (NetworkRoute) network.getFactory().createRoute(TransportMode.car, this.network.getLink("2030"), this.network.getLink("1020"));
		leg.setRoute(route);
		route.setTravelTime(Time.parseTime("04:00:00"));
		leg.setTravelTime(Time.parseTime("04:00:00"));

		act = plan.createActivity("shop", facilityShop);
		link = this.network.getLink("1020");
		act.setLink(link);
		act.setCoord(link.getCoord());
		leg = this.plan.createLeg(TransportMode.bike);
		route = (NetworkRoute) network.getFactory().createRoute(TransportMode.car, this.network.getLink("1020"), this.network.getLink("2030"));
		leg.setRoute(route);
		route.setTravelTime(Time.parseTime("04:00:00"));
		leg.setTravelTime(Time.parseTime("04:00:00"));

		act = plan.createActivity("home", facilityHome);
		link = this.network.getLink("2030");
		act.setLink(link);
		act.setCoord(link.getCoord());
	}

	public void xtestPlanomatPerformance() {
		/* disabled this test as it does not contain a single assert-statement.
		 * So, besides that the code doesn't crash, it doesn'r really test anything.
		 * JUnit is (sadly) not suitable for performance tests. This method could
		 * be run more or less the same way also in its own main-class. marcel/29may2009
		 */

		final int TEST_PLAN_NR = 0;

		TravelTimeCalculator tTravelEstimator = new TravelTimeCalculator(this.network, this.config.travelTimeCalculator());
		TravelCost travelCostEstimator = new TravelTimeDistanceCostCalculator(tTravelEstimator, this.config.charyparNagelScoring());
		DepartureDelayAverageCalculator depDelayCalc = new DepartureDelayAverageCalculator(this.network, this.config.travelTimeCalculator().getTraveltimeBinSize());

		PlansCalcRoute plansCalcRoute = new PlansCalcRoute(this.config.plansCalcRoute(), this.network, travelCostEstimator, tTravelEstimator);
		
		LegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory = new LegTravelTimeEstimatorFactory(tTravelEstimator, depDelayCalc);

		FixedRouteLegTravelTimeEstimator ltte = (FixedRouteLegTravelTimeEstimator) legTravelTimeEstimatorFactory.getLegTravelTimeEstimator(
				PlanomatConfigGroup.SimLegInterpretation.CetinCompatible, 
				PlanomatConfigGroup.RoutingCapability.fixedRoute,
				plansCalcRoute);
		TreeMap<Id, FacilityPenalty> facilityPenalties = new TreeMap<Id, FacilityPenalty>();

		PersonImpl testPerson = this.population.getPersons().get(TEST_PERSON_ID);
		// only plan of that person
		PlanImpl testPlan = testPerson.getPlans().get(TEST_PLAN_NR);

		ScoringFunctionFactory scoringFunctionFactory = new KTIYear3ScoringFunctionFactory(
				this.config.charyparNagelScoring(), 
				facilityPenalties,
				this.ktiConfigGroup);
		// init Planomat, which loads config!
		Planomat testee = new Planomat(ltte, scoringFunctionFactory, this.config.planomat());

		Random random = MatsimRandom.getLocalInstance();
		
		int check = 1;
		for (int i = 0; i < 1000; i++) {
			testee.getSeedGenerator().setSeed(random.nextLong());
			tTravelEstimator.reset(0);
			depDelayCalc.resetDepartureDelays();
			
			// actual test
			testee.run(testPlan);
			
			if (i % check == 0) {
				logger.info("Processed test person " + check + " times.");
				check *= 2;
				
			}
		}
		// write out the test person and the modified plan into a file
		PopulationImpl outputPopulation = new PopulationImpl();
		outputPopulation.getPersons().put(testPerson.getId(), testPerson);
		
		logger.info("Writing plans file...");
		PopulationWriter plans_writer = new PopulationWriter(outputPopulation, this.getOutputDirectory() + "output_plans.xml.gz", "v4");
		plans_writer.write();
		logger.info("Writing plans file...DONE.");

		
	}
	
	public void testAlwaysOpen() {
		
		// []{end home, work_sector3, leisure, work_Sector3, home, shop, start home, finish, reset}
		String[] expectedTooShortDurationsSequence = new String[]{"00:00:00", "00:00:00", "00:10:00",  "00:10:00", "00:10:00", "00:10:00", "00:10:00", "02:40:00", "00:00:00"};
		String[] expectedWaitingTimeSequence = new String[]{"00:00:00", "00:00:00", "00:00:00", "00:00:00", "00:00:00", "00:00:00", "00:00:00", "00:00:00", "00:00:00"};
		HashMap<String, String[]> expectedAccumulatedActivityDurations = new HashMap<String, String[]>();
		expectedAccumulatedActivityDurations.put("home", new String[]{null, null, null, null, "02:30:00", "02:30:00", "02:30:00", "02:30:00", null});
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
		
		ActivityOption actOpt = null;
		
		actOpt = this.facilities.getFacilities().get(new IdImpl("3")).getActivityOption("work_sector3");
		actOpt.addOpeningTime(new OpeningTimeImpl(DayType.wed, Time.parseTime("07:00:00"), Time.parseTime("18:00:00")));

		actOpt = this.facilities.getFacilities().get(new IdImpl("5")).getActivityOption("leisure");
		actOpt.addOpeningTime(new OpeningTimeImpl(DayType.wed, Time.parseTime("11:00:00"), Time.parseTime("16:00:00")));

		// []{end home, work_sector3, leisure, work_Sector3, home, shop, start home, finish, reset}
		String[] expectedTooShortDurationsSequence = new String[]{"00:00:00", "00:00:00", "00:10:00",  "00:10:00", "00:10:00", "00:10:00", "00:10:00", "02:40:00", "00:00:00"};
		String[] expectedWaitingTimeSequence = new String[]{"00:00:00", "00:00:00", "00:00:00", "00:00:00", "00:00:00", "00:00:00", "00:00:00", "00:00:00", "00:00:00"};
		HashMap<String, String[]> expectedAccumulatedActivityDurations = new HashMap<String, String[]>();
		expectedAccumulatedActivityDurations.put("home", new String[]{null, null, null, null, "02:30:00", "02:30:00", "02:30:00", "02:30:00", null});
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

		ActivityOption actOpt = null;

		actOpt = this.facilities.getFacilities().get(new IdImpl("3")).getActivityOption("work_sector3");
		actOpt.addOpeningTime(new OpeningTimeImpl(DayType.wed, Time.parseTime("07:00:00"), Time.parseTime("14:00:00")));
		actOpt.addOpeningTime(new OpeningTimeImpl(DayType.wed, Time.parseTime("15:15:00"), Time.parseTime("20:00:00")));
		
		actOpt = this.facilities.getFacilities().get(new IdImpl("5")).getActivityOption("leisure");
		actOpt.addOpeningTime(new OpeningTimeImpl(DayType.wed, Time.parseTime("11:00:00"), Time.parseTime("14:00:00")));

		// []{end home, work_sector3, leisure, work_Sector3, home, shop, start home, finish, reset}
		String[] expectedTooShortDurationsSequence = new String[]{"00:00:00", "00:00:00", "00:30:00",  "00:30:00", "00:30:00", "00:30:00", "00:30:00", "03:00:00", "00:00:00"};
		String[] expectedWaitingTimeSequence = new String[]{"00:00:00", "00:30:00", "00:50:00", "01:05:00", "01:05:00", "01:05:00", "01:05:00", "01:05:00","00:00:00"};
		HashMap<String, String[]> expectedAccumulatedActivityDurations = new HashMap<String, String[]>();
		expectedAccumulatedActivityDurations.put("home", new String[]{null, null, null, null, "02:30:00", "02:30:00", "02:30:00", "02:30:00", null});
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
		
		ActivityOption actOpt = null;

		actOpt = this.facilities.getFacilities().get(new IdImpl("3")).getActivityOption("work_sector3");
		actOpt.addOpeningTime(new OpeningTimeImpl(DayType.wed, Time.parseTime("07:00:00"), Time.parseTime("10:00:00")));
		actOpt.addOpeningTime(new OpeningTimeImpl(DayType.wed, Time.parseTime("10:30:00"), Time.parseTime("14:00:00")));
		actOpt.addOpeningTime(new OpeningTimeImpl(DayType.wed, Time.parseTime("15:15:00"), Time.parseTime("20:00:00")));
		
		actOpt = this.facilities.getFacilities().get(new IdImpl("5")).getActivityOption("leisure");
		actOpt.addOpeningTime(new OpeningTimeImpl(DayType.wed, Time.parseTime("11:00:00"), Time.parseTime("14:00:00")));

		actOpt = this.facilities.getFacilities().get(new IdImpl("7")).getActivityOption("shop");
		actOpt.addOpeningTime(new OpeningTimeImpl(DayType.wed, Time.parseTime("12:00:00"), Time.parseTime("27:00:00")));
		
		// []{end home, work_sector3, leisure, work_Sector3, home, shop, start home, finish, reset}
		String[] expectedTooShortDurationsSequence = new String[]{"00:00:00", "00:00:00", "00:30:00",  "00:30:00", "00:30:00", "00:30:00", "00:30:00", "03:00:00", "00:00:00"};
		String[] expectedWaitingTimeSequence = new String[]{"00:00:00", "01:00:00", "01:20:00", "01:35:00", "01:35:00", "04:35:00", "04:35:00", "04:35:00", "00:00:00"};
		HashMap<String, String[]> expectedAccumulatedActivityDurations = new HashMap<String, String[]>();
		expectedAccumulatedActivityDurations.put("home", new String[]{null, null, null, null, "02:30:00", "02:30:00", "02:30:00", "02:30:00", null});
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
				this.config.charyparNagelScoring(), 
				emptyFacilityPenalties,
				this.ktiConfigGroup);
		ScoringFunction testee = factory.getNewScoringFunction(this.plan);

		testee.endActivity(Time.parseTime("08:00:00"));
		
		assertEquals(0, factory.getActivities().getAccumulatedDurations().size());
		assertEquals(expectedTooShortDurationsSequence[0], Time.writeTime(factory.getActivities().getAccumulatedTooShortDuration()));
		assertEquals(expectedWaitingTimeSequence[0], Time.writeTime(factory.getActivities().getTimeSpentWaiting()));
		assertEquals(expectedNegativeDurationsSequence[0], Time.writeTime(factory.getActivities().getAccumulatedNegativeDuration()));
		
		testee.startLeg(Time.parseTime("08:00:00"), (LegImpl) this.plan.getPlanElements().get(1));
		testee.endLeg(Time.parseTime("08:30:00"));
		testee.startActivity(Time.parseTime("08:30:00"), (ActivityImpl) this.plan.getPlanElements().get(2));
		testee.endActivity(Time.parseTime("14:30:00"));

		for (String actType : expectedAccumulatedActivityDurations.keySet()) {
			if (factory.getActivities().getAccumulatedDurations().containsKey(actType)) {
				assertEquals(expectedAccumulatedActivityDurations.get(actType)[1], Time.writeTime(factory.getActivities().getAccumulatedDurations().get(actType)));
			}
		}
		assertEquals(expectedTooShortDurationsSequence[1], Time.writeTime(factory.getActivities().getAccumulatedTooShortDuration()));
		assertEquals(expectedWaitingTimeSequence[1], Time.writeTime(factory.getActivities().getTimeSpentWaiting()));
		assertEquals(expectedNegativeDurationsSequence[1], Time.writeTime(factory.getActivities().getAccumulatedNegativeDuration()));

		testee.startLeg(Time.parseTime("14:30:00"), (LegImpl) this.plan.getPlanElements().get(3));
		testee.endLeg(Time.parseTime("14:35:00"));
		testee.startActivity(Time.parseTime("14:35:00"), (ActivityImpl) this.plan.getPlanElements().get(4));
		testee.endActivity(Time.parseTime("14:55:00"));

		for (String actType : expectedAccumulatedActivityDurations.keySet()) {
			if (factory.getActivities().getAccumulatedDurations().containsKey(actType)) {
				assertEquals(expectedAccumulatedActivityDurations.get(actType)[2], Time.writeTime(factory.getActivities().getAccumulatedDurations().get(actType)));
			}
		}
		assertEquals(expectedTooShortDurationsSequence[2], Time.writeTime(factory.getActivities().getAccumulatedTooShortDuration()));
		assertEquals(expectedWaitingTimeSequence[2], Time.writeTime(factory.getActivities().getTimeSpentWaiting()));
		assertEquals(expectedNegativeDurationsSequence[2], Time.writeTime(factory.getActivities().getAccumulatedNegativeDuration()));
		
		testee.startLeg(Time.parseTime("14:55:00"), (LegImpl) this.plan.getPlanElements().get(5));
		testee.endLeg(Time.parseTime("15:00:00"));
		testee.startActivity(Time.parseTime("15:00:00"), (ActivityImpl) this.plan.getPlanElements().get(6));
		testee.endActivity(Time.parseTime("17:00:00"));

		for (String actType : expectedAccumulatedActivityDurations.keySet()) {
			if (factory.getActivities().getAccumulatedDurations().containsKey(actType)) {
				assertEquals(expectedAccumulatedActivityDurations.get(actType)[3], Time.writeTime(factory.getActivities().getAccumulatedDurations().get(actType)));
			}
		}
		assertEquals(expectedTooShortDurationsSequence[3], Time.writeTime(factory.getActivities().getAccumulatedTooShortDuration()));
		assertEquals(expectedWaitingTimeSequence[3], Time.writeTime(factory.getActivities().getTimeSpentWaiting()));
		assertEquals(expectedNegativeDurationsSequence[3], Time.writeTime(factory.getActivities().getAccumulatedNegativeDuration()));

		testee.startLeg(Time.parseTime("17:00:00"), (LegImpl) this.plan.getPlanElements().get(7));
		testee.endLeg(Time.parseTime("17:30:00"));
		testee.startActivity(Time.parseTime("17:30:00"), (ActivityImpl) this.plan.getPlanElements().get(8));
		testee.endActivity(Time.parseTime("20:00:00"));

		for (String actType : expectedAccumulatedActivityDurations.keySet()) {
			if (factory.getActivities().getAccumulatedDurations().containsKey(actType)) {
				assertEquals(expectedAccumulatedActivityDurations.get(actType)[4], Time.writeTime(factory.getActivities().getAccumulatedDurations().get(actType)));
			}
		}
		assertEquals(expectedTooShortDurationsSequence[4], Time.writeTime(factory.getActivities().getAccumulatedTooShortDuration()));
		assertEquals(expectedWaitingTimeSequence[4], Time.writeTime(factory.getActivities().getTimeSpentWaiting()));
		assertEquals(expectedNegativeDurationsSequence[4], Time.writeTime(factory.getActivities().getAccumulatedNegativeDuration()));

		testee.startLeg(Time.parseTime("20:00:00"), (LegImpl) this.plan.getPlanElements().get(9));
		testee.endLeg(Time.parseTime("24:00:00"));
		testee.startActivity(Time.parseTime("24:00:00"), (ActivityImpl) this.plan.getPlanElements().get(10));
		testee.endActivity(Time.parseTime("30:00:00"));

		for (String actType : expectedAccumulatedActivityDurations.keySet()) {
			if (factory.getActivities().getAccumulatedDurations().containsKey(actType)) {
				assertEquals(expectedAccumulatedActivityDurations.get(actType)[5], Time.writeTime(factory.getActivities().getAccumulatedDurations().get(actType)));
			}
		}
		assertEquals(expectedTooShortDurationsSequence[5], Time.writeTime(factory.getActivities().getAccumulatedTooShortDuration()));
		assertEquals(expectedWaitingTimeSequence[5], Time.writeTime(factory.getActivities().getTimeSpentWaiting()));
		assertEquals(expectedNegativeDurationsSequence[5], Time.writeTime(factory.getActivities().getAccumulatedNegativeDuration()));

		testee.startLeg(Time.parseTime("30:00:00"), (LegImpl) this.plan.getPlanElements().get(11));
		testee.endLeg(Time.parseTime("34:00:00"));
		testee.startActivity(Time.parseTime("34:00:00"), (ActivityImpl) this.plan.getPlanElements().get(12));

		for (String actType : expectedAccumulatedActivityDurations.keySet()) {
			if (factory.getActivities().getAccumulatedDurations().containsKey(actType)) {
				assertEquals(expectedAccumulatedActivityDurations.get(actType)[6], Time.writeTime(factory.getActivities().getAccumulatedDurations().get(actType)));
			}
		}
		assertEquals(expectedTooShortDurationsSequence[6], Time.writeTime(factory.getActivities().getAccumulatedTooShortDuration()));
		assertEquals(expectedWaitingTimeSequence[6], Time.writeTime(factory.getActivities().getTimeSpentWaiting()));
		assertEquals(expectedNegativeDurationsSequence[6], Time.writeTime(factory.getActivities().getAccumulatedNegativeDuration()));
		
		testee.finish();

		for (String actType : expectedAccumulatedActivityDurations.keySet()) {
			if (factory.getActivities().getAccumulatedDurations().containsKey(actType)) {
				assertEquals(expectedAccumulatedActivityDurations.get(actType)[7], Time.writeTime(factory.getActivities().getAccumulatedDurations().get(actType)));
			}
		}
		assertEquals(expectedTooShortDurationsSequence[7], Time.writeTime(factory.getActivities().getAccumulatedTooShortDuration()));
		assertEquals(expectedWaitingTimeSequence[7], Time.writeTime(factory.getActivities().getTimeSpentWaiting()));
		assertEquals(expectedNegativeDurationsSequence[7], Time.writeTime(factory.getActivities().getAccumulatedNegativeDuration()));

		double expectedScore = 0.0;
		expectedScore += factory.getParams().marginalUtilityOfEarlyDeparture * Time.parseTime(expectedTooShortDurationsSequence[7]);
		expectedScore += factory.getParams().marginalUtilityOfWaiting * Time.parseTime(expectedWaitingTimeSequence[7]);
		expectedScore += factory.getParams().marginalUtilityOfLateArrival * 2 * Time.parseTime(expectedNegativeDurationsSequence[7]);
		double duration, zeroUtilityDuration, typicalDuration;
		for (String actType : expectedAccumulatedActivityDurations.keySet()) {
			if (factory.getActivities().getAccumulatedDurations().containsKey(actType)) {
				typicalDuration = this.population.getPersons().get(TEST_PERSON_ID).getDesires().getActivityDuration(actType);
				duration = Time.parseTime(expectedAccumulatedActivityDurations.get(actType)[7]);
				zeroUtilityDuration = (typicalDuration / 3600.0) * Math.exp( -10.0 / (typicalDuration / 3600.0) / ActivityScoringFunction.DEFAULT_PRIORITY);
				double utilPerf = factory.getParams().marginalUtilityOfPerforming * typicalDuration * Math.log((duration / 3600.0) / zeroUtilityDuration);
				double utilWait = factory.getParams().marginalUtilityOfWaiting * duration;
				expectedScore += Math.max(0, Math.max(utilPerf, utilWait));

				// check zero utility durations
				assertEquals(zeroUtilityDuration, factory.getActivities().getZeroUtilityDurations().get(actType), MatsimTestCase.EPSILON);
				
			}
		}
		
		// score legs
		for (PlanElement pe : this.plan.getPlanElements()) {
			if (pe instanceof LegImpl) {
				LegImpl leg = (LegImpl) pe;
				if (leg.getMode().equals(TransportMode.bike)) {
					expectedScore += this.ktiConfigGroup.getConstBike();
				}
			}
		}
		assertEquals(expectedScore, testee.getScore());
		
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		this.population = null;
		this.plan = null;
		this.config = null;
		this.facilities = null;
		this.network = null;
	}
	
}
