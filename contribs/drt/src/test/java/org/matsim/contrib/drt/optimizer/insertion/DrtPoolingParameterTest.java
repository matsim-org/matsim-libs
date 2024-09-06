package org.matsim.contrib.drt.optimizer.insertion;

import java.net.URL;
import java.util.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.common.zones.systems.grid.square.SquareGridZoneSystemParams;
import org.matsim.contrib.drt.optimizer.constraints.DefaultDrtOptimizationConstraintsSet;
import org.matsim.contrib.drt.optimizer.constraints.DrtOptimizationConstraintsSet;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEventHandler;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.zone.skims.DvrpTravelTimeMatrixParams;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.facilities.*;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

/**
 * This set of tests examines how the pooling (car-sharing) behavior varies based on parameters in the drt config group:
 * maxWaitTime, maxTravelTimeAlpha and maxTravelTimeBeta.
 * <p>
 * In this case, rejectRequestIfMaxWaitOrTravelTimeViolated=true (default value), which means that maxTravelTime and
 * maxWaitTime are seen as hard constraints; if those conditions aren't met, the request is rejected.
 * <p>
 * The mielec scenario is used for this purpose. There are only four agents present in this case, which matches the vehicle
 * capacity of the drt vehicles. All four agents have their home location in a small area (links are adjacent to one another).
 * The same is true of their work locations. The purpose of this is to encourage pooling.
 * <p>
 * While there are 10 DRT vehicles overall, there are only two that are closeby: drt_veh_1_1 and drt_veh_1_2. These two vehicles
 * should be used for all of the test cases.
 * <p>
 * I attempted to calculate specific thresholds for the various parameters using my own calculations. This was not successful, in part,
 * because during my work the calculation process to find the detourTime changed: within the class DetourTimesProvider.java, the method
 * createNodeToNodeBeelineTimeEstimator was replaced by createFreeSpeedZonalTimeEstimator. Therefore, the following task:
 * TODO: Calculate specific thresholds for parameters and demonstrate those calculations
 * ^The beginning of this process is commented at the end of this class
 * <p>
 * What happens if rejectRequestIfMaxWaitOrTravelTimeViolated = false? --> TODO: verify how soft constraints work.
 * The stopDuration is set to 1s --> potential TODO: reset to default of 60s
 * TODO: write tests for maxTravelTimeAlpha
 */
public class DrtPoolingParameterTest {

	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();


	/*
	The following tests vary the maxWaitTime, while keeping maxTravelTimeAlpha and maxTravelBeta fixed at high values
	 */

	/**
	 * With a low maxWaitTime of 100s, no DRT vehicle should have time to any agents.
	 */
	@Test
	void testMaxWaitTimeNoVehicles() {
		PersonEnterDrtVehicleEventHandler handler = setupAndRunScenario(50, 10.0, 10000.);
		Assertions.assertEquals(0, handler.getVehRequestCount().size(), "There should be no vehicle used");

	}

	/**
	 * With a maxWaitTime of 121s, the two closest drt vehicles should be able to reach 2 agents, but be
	 * unable to then pickup the other 2 agents without surpassing the maxWaitTime. All other DRT Vehicles
	 * too far away to reach those agents.
	 */
	@Test
	void testMaxWaitTimeTwoVehiclesForTwoAgents() {
		PersonEnterDrtVehicleEventHandler handler = setupAndRunScenario(121, 10.0, 10000.);

		Assertions.assertEquals(2, handler.getVehRequestCount().size(), "There should two vehicle used");
		Id<DvrpVehicle> drt_veh_1_1 = Id.create("drt_veh_1_1", DvrpVehicle.class);
		Id<DvrpVehicle> drt_veh_1_2 = Id.create("drt_veh_1_2", DvrpVehicle.class);
		Assertions.assertTrue(handler.getVehRequestCount().containsKey(drt_veh_1_1),
				"drt_veh_1_1 should be requested in general");
		Assertions.assertEquals(1,
				handler.getVehRequestCount().get(drt_veh_1_1), 0, "drt_veh_1_1 should be requested exactly once");
		Assertions.assertTrue(handler.getVehRequestCount().containsKey(drt_veh_1_2),
				"drt_veh_1_2 should be requested in general");
		Assertions.assertEquals(1,
				handler.getVehRequestCount().get(drt_veh_1_2), 0, "drt_veh_1_2 should be requested exactly once");

	}

	/**
	 * With a maxWaitTime of 250s, both drt vehicles should have time to each pick up two passengers.
	 */
	@Test
	void testMaxWaitTimeTwoVehiclesForFourAgents() {
		PersonEnterDrtVehicleEventHandler handler = setupAndRunScenario(250, 10.0, 10000.);

		Assertions.assertEquals(2, handler.getVehRequestCount().size(), "There should two vehicle used");
		Id<DvrpVehicle> drt_veh_1_1 = Id.create("drt_veh_1_1", DvrpVehicle.class);
		Id<DvrpVehicle> drt_veh_1_2 = Id.create("drt_veh_1_2", DvrpVehicle.class);
		Assertions.assertTrue(handler.getVehRequestCount().containsKey(drt_veh_1_1),
				"drt_veh_1_1 should be requested in general");
		Assertions.assertEquals(2,
				handler.getVehRequestCount().get(drt_veh_1_1), 0, "drt_veh_1_1 should be requested exactly twice");
		Assertions.assertTrue(handler.getVehRequestCount().containsKey(drt_veh_1_2),
				"drt_veh_1_2 should be requested in general");
		Assertions.assertEquals(2,
				handler.getVehRequestCount().get(drt_veh_1_2), 0, "drt_veh_1_2 should be requested exactly twice");

	}

	/**
	 * With a high maxWaitTime of 500s, a single DRT vehicle should be able to pick up all four agents.
	 */
	@Test
	void testMaxWaitTimeOneVehicleForFourAgents() {

		PersonEnterDrtVehicleEventHandler handler = setupAndRunScenario(500, 10.0, 10000.);
		System.out.println(handler.getVehRequestCount());
		Assertions.assertEquals(1, handler.getVehRequestCount().size(), "There should only be one vehicle used");
		Id<DvrpVehicle> drt_veh_1_1 = Id.create("drt_veh_1_1", DvrpVehicle.class);
		Assertions.assertTrue(handler.getVehRequestCount().containsKey(drt_veh_1_1),
				"drt_veh_1_1 should be requested in general");
		Assertions.assertEquals(4,
				handler.getVehRequestCount().get(drt_veh_1_1), 0, "drt_veh_1_1 should be requested exactly four times");

	}

	/*
		e following tests vary the maxTravelTimeBeta parameter. maxTravelTimeAlpha is fixed to 1.0, while maxWaitTime is
		ixed to a high value: 5000s.
	 */

	/**
	 * With a low Beta of 0s, no DRT vehicles should be assigned to the agents.
	 */
	@Test
	void testBetaNoVehicles() {
		PersonEnterDrtVehicleEventHandler handler = setupAndRunScenario(5000, 1.0, 0.);

		Assertions.assertEquals(0, handler.getVehRequestCount().size(), "There should only be zero vehicles used");

	}

	/**
	 * With a low Beta of 150s, both vehicles should have enough time to pick up one passenger each;
	 * the remaining two agents will be left stranded
	 */
	@Test
	void testBetaTwoVehiclesForTwoAgents() {
		PersonEnterDrtVehicleEventHandler handler = setupAndRunScenario(5000, 1.0, 150);

		Assertions.assertEquals(2, handler.getVehRequestCount().size(), "There should two vehicle used");
		Id<DvrpVehicle> drt_veh_1_1 = Id.create("drt_veh_1_1", DvrpVehicle.class);
		Id<DvrpVehicle> drt_veh_1_2 = Id.create("drt_veh_1_2", DvrpVehicle.class);
		Assertions.assertTrue(handler.getVehRequestCount().containsKey(drt_veh_1_1),
				"drt_veh_1_1 should be requested in general");
		Assertions.assertEquals(1,
				handler.getVehRequestCount().get(drt_veh_1_1), 0, "drt_veh_1_1 should be requested exactly once");
		Assertions.assertTrue(handler.getVehRequestCount().containsKey(drt_veh_1_2),
				"drt_veh_1_2 should be requested in general");
		Assertions.assertEquals(1,
				handler.getVehRequestCount().get(drt_veh_1_2), 0, "drt_veh_1_2 should be requested exactly once");

	}

	/**
	 * With a Beta value of 250s, two DRT vehicles should have enough time to pick up two passengers each.
	 */
	@Test
	void testBetaTwoVehiclesForFourAgents() {
		PersonEnterDrtVehicleEventHandler handler = setupAndRunScenario(5000, 1.0, 250);

		Assertions.assertEquals(2, handler.getVehRequestCount().size(), "There should two vehicle used");
		Id<DvrpVehicle> drt_veh_1_1 = Id.create("drt_veh_1_1", DvrpVehicle.class);
		Id<DvrpVehicle> drt_veh_1_2 = Id.create("drt_veh_1_2", DvrpVehicle.class);
		Assertions.assertTrue(handler.getVehRequestCount().containsKey(drt_veh_1_1),
				"drt_veh_1_1 should be requested in general");
		Assertions.assertEquals(2,
				handler.getVehRequestCount().get(drt_veh_1_1), 0, "drt_veh_1_1 should be requested exactly twice");
		Assertions.assertTrue(handler.getVehRequestCount().containsKey(drt_veh_1_2),
				"drt_veh_1_2 should be requested in general");
		Assertions.assertEquals(2,
				handler.getVehRequestCount().get(drt_veh_1_2), 0, "drt_veh_1_2 should be requested exactly twice");
	}

	/**
	 * With a high Beta of 400s, one DRT vehicle should be used to pick up all four agents
	 */
	@Test
	void testBetaOneVehicleForFourAgents() {
		PersonEnterDrtVehicleEventHandler handler = setupAndRunScenario(5000, 1.0, 400);

		Assertions.assertEquals(1, handler.getVehRequestCount().size(), "There should only be one vehicle used");
		Id<DvrpVehicle> drt_veh_1_1 = Id.create("drt_veh_1_1", DvrpVehicle.class);
		Assertions.assertTrue(handler.getVehRequestCount().containsKey(drt_veh_1_1),
				"drt_veh_1_1 should be requested in general");
		Assertions.assertEquals(4,
				handler.getVehRequestCount().get(drt_veh_1_1), 0, "drt_veh_1_1 should be requested exactly four times");

	}

	private PersonEnterDrtVehicleEventHandler setupAndRunScenario(double maxWaitTime, double maxTravelTimeAlpha,
			double maxTravelTimeBeta) {
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");

		DvrpConfigGroup dvrpConfig = new DvrpConfigGroup();
		DvrpTravelTimeMatrixParams matrixParams = dvrpConfig.getTravelTimeMatrixParams();
		matrixParams.addParameterSet(matrixParams.createParameterSet(SquareGridZoneSystemParams.SET_NAME));

		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), dvrpConfig,
				new OTFVisConfigGroup());

		config.plans().setInputFile(null);
		config.controller()
				.setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());

		MultiModeDrtConfigGroup mm = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
		mm.getModalElements().forEach(x -> {
			DefaultDrtOptimizationConstraintsSet defaultConstraintsSet =
					(DefaultDrtOptimizationConstraintsSet) x.addOrGetDrtOptimizationConstraintsParams()
							.addOrGetDefaultDrtOptimizationConstraintsSet();
			defaultConstraintsSet.maxWaitTime = maxWaitTime;
			defaultConstraintsSet.maxTravelTimeAlpha = maxTravelTimeAlpha;
			defaultConstraintsSet.maxTravelTimeBeta = maxTravelTimeBeta;
			x.stopDuration = 1.;
		});

		Controler controler = DrtControlerCreator.createControler(config, false);
		Scenario scenario = controler.getScenario();

		Population population = scenario.getPopulation();
		PopulationFactory pf = population.getFactory();
		ActivityFacilitiesFactory ff = controler.getScenario().getActivityFacilities().getFactory();

		ActivityFacility homeFac1 = ff.createActivityFacility(Id.create("hf1", ActivityFacility.class),
				Id.createLinkId("359"));
		ActivityFacility homeFac2 = ff.createActivityFacility(Id.create("hf2", ActivityFacility.class),
				Id.createLinkId("364"));
		ActivityFacility homeFac3 = ff.createActivityFacility(Id.create("hf3", ActivityFacility.class),
				Id.createLinkId("373"));
		ActivityFacility homeFac4 = ff.createActivityFacility(Id.create("hf4", ActivityFacility.class),
				Id.createLinkId("353"));

		ActivityFacility workFac1 = ff.createActivityFacility(Id.create("wf1", ActivityFacility.class),
				Id.createLinkId("185"));
		ActivityFacility workFac2 = ff.createActivityFacility(Id.create("wf2", ActivityFacility.class),
				Id.createLinkId("245"));
		ActivityFacility workFac3 = ff.createActivityFacility(Id.create("wf3", ActivityFacility.class),
				Id.createLinkId("237"));
		ActivityFacility workFac4 = ff.createActivityFacility(Id.create("wf4", ActivityFacility.class),
				Id.createLinkId("240"));

		scenario.getActivityFacilities().addActivityFacility(homeFac1);
		scenario.getActivityFacilities().addActivityFacility(homeFac2);
		scenario.getActivityFacilities().addActivityFacility(homeFac3);
		scenario.getActivityFacilities().addActivityFacility(homeFac4);
		scenario.getActivityFacilities().addActivityFacility(workFac1);
		scenario.getActivityFacilities().addActivityFacility(workFac2);
		scenario.getActivityFacilities().addActivityFacility(workFac3);
		scenario.getActivityFacilities().addActivityFacility(workFac4);

		Person p1 = createPerson(8 * 3600 + 0., homeFac1, workFac1, "1", pf);
		Person p2 = createPerson(8 * 3600 + 10., homeFac2, workFac2, "2", pf);
		Person p3 = createPerson(8 * 3600 + 20., homeFac3, workFac3, "3", pf);
		Person p4 = createPerson(8 * 3600 + 30., homeFac4, workFac4, "4", pf);

		population.addPerson(p1);
		population.addPerson(p2);
		population.addPerson(p3);
		population.addPerson(p4);

		EventsManager events = controler.getEvents();
		PersonEnterDrtVehicleEventHandler handler = new PersonEnterDrtVehicleEventHandler();
		events.addHandler(handler);

		controler.run();

		return handler;
	}

	private Person createPerson(double depTime, ActivityFacility homeFacility, ActivityFacility workFacility,
			String pId, PopulationFactory pf) {
		Id<Person> personId = Id.createPersonId(pId);
		Person person = pf.createPerson(personId);
		Plan plan = pf.createPlan();
		Activity activity1 = pf.createActivityFromActivityFacilityId("dummy", homeFacility.getId());
		activity1.setEndTime(depTime);
		plan.addActivity(activity1);
		plan.addLeg(pf.createLeg("drt"));
		Activity activity2 = pf.createActivityFromActivityFacilityId("dummy", workFacility.getId());
		activity2.setEndTimeUndefined();
		plan.addActivity(activity2);
		person.addPlan(plan);
		return person;
	}

	private class PersonEnterDrtVehicleEventHandler implements PassengerRequestScheduledEventHandler {

		private Map<Id<DvrpVehicle>, Integer> vehRequestCount = new HashMap<>();

		@Override
		public void handleEvent(PassengerRequestScheduledEvent event) {

			Id<DvrpVehicle> vehicleId = event.getVehicleId();
			Integer vehCnt = vehRequestCount.getOrDefault(vehicleId, 0);
			vehRequestCount.put(vehicleId, ++vehCnt);

		}

		Map<Id<DvrpVehicle>, Integer> getVehRequestCount() {
			return vehRequestCount;
		}
	}


	/*
	The following is an attempt to find very specific thresholds for the various pooling parameters. Unfortunately,
	this process wasn't completed. Once the ExtensiveInsertionSearch methodology is completed, and a the method
	for finding DetourTimes is finalized, this process can be continued. See todos at top of class.
	 */
	//	@Test
	//	public void findThresholdsForParameters() {
	//		PersonEnterDrtVehicleEventHandler handler = setupAndRunScenario(5000,
	//			5.0,
	//			10000.); // 116 does not work! 120 does work!
	//
	//		Scenario scenario = controler.getScenario();
	//		TripRouter tripRouter = controler.getTripRouterProvider().get();
	//
	//		Map<Id<ActivityFacility>, ? extends ActivityFacility> facilities = scenario.getActivityFacilities().getFacilities();
	//
	//		ActivityFacility homeFac1 = facilities.get(Id.create("hf1", ActivityFacility.class));
	//		ActivityFacility homeFac2 = facilities.get(Id.create("hf2", ActivityFacility.class));
	//		ActivityFacility workFac1 = facilities.get(Id.create("wf1", ActivityFacility.class));
	//		ActivityFacility workFac2 = facilities.get(Id.create("wf2", ActivityFacility.class));
	//
	//		Person p1 = scenario.getPopulation().getPersons().get(Id.createPersonId("1"));
	//		ActivityFacility vehLoc = scenario.getActivityFacilities().getFactory().createActivityFacility(Id.create("vehLoc", ActivityFacility.class), Id.createLinkId(385));
	//
	//		double directRideTimeP1 = getDirectTT(homeFac1, workFac1, p1, tripRouter);
	//
	//		DrtConfigGroup drtCfg = ConfigUtils.addOrGetModule( scenario.getConfig(), DrtConfigGroup.class );
	//		double beelineTimeV_H1 = findBeelineTime(vehLoc, homeFac1, drtCfg);
	//		double beelineTimeH1_W1 = findBeelineTime(homeFac1, workFac1, drtCfg);
	//		double beelineTimeP1 = beelineTimeV_H1 + beelineTimeH1_W1;
	//
	//		double beta_threshold_expected = beelineTimeP1 - directRideTimeP1;
	//
	//
	//		System.out.println("direct ride time p1: " + directRideTimeP1); // 526.36
	//		System.out.println("beeline time p1 (veh to h1 to w1): " + beelineTimeP1); // 668.2
	//		System.out.println("expected beta threshold: " + beta_threshold_expected); // 141.84
	//
	//	}
	//
	//	private double findBeelineTime(ActivityFacility actFac1, ActivityFacility actFac2, DrtConfigGroup drtCfg) {
	//		ExtensiveInsertionSearchParams drtInsertionSearchParams = (ExtensiveInsertionSearchParams) drtCfg.getDrtInsertionSearchParams();
	////		double admissibleBeelineSpeed = drtInsertionSearchParams.getAdmissibleBeelineSpeedFactor()
	////			* drtCfg.getEstimatedDrtSpeed() / drtCfg.getEstimatedBeelineDistanceFactor();
	//		double admissibleBeelineSpeed = 8.012820512820513;
	//		Coord fromCoord = controler.getScenario().getNetwork().getLinks().get(actFac1.getLinkId()).getCoord();
	//		Coord toCoord = controler.getScenario().getNetwork().getLinks().get(actFac2.getLinkId()).getCoord();
	//		double beelineDistance = CoordUtils.calcEuclideanDistance(fromCoord, toCoord);
	//
	//		return beelineDistance / admissibleBeelineSpeed;
	//
	//	}
	//	private Double getDirectTT(ActivityFacility homeFac1, ActivityFacility workFac1, Person p1, TripRouter router) {
	//
	//		List<? extends PlanElement> tripP1 = router.getRoutingModule("drt").calcRoute(homeFac1, workFac1, 0., p1);
	//		DrtRoute routeP1 = (DrtRoute) ((Leg) tripP1.get(2)).getRoute();
	//		return routeP1.getDirectRideTime();
	//	}
	//
	//
	//	private Double getDirectTT(ActivityFacility[] facilities, Person p1, TripRouter router) {
	//		Double tt = 0.;
	//		for (int i = 0; i < facilities.length - 1; i++) {
	//			tt += getDirectTT(facilities[i], facilities[i + 1], p1, router);
	//		}
	//		return tt;
	//	}
	//	private Double getDirectTT_OLD(ActivityFacility homeFac1, ActivityFacility workFac1, Person p1, TripRouter router) {
	//
	//		List<? extends PlanElement> planElements = router.calcRoute("car", homeFac1, workFac1, 8 * 3600 + 0., p1);
	//
	//		Leg leg = (Leg) planElements.get(0);
	//
	//		Id<Link> startLinkId = leg.getRoute().getEndLinkId();
	//		Link startLink = controler.getScenario().getNetwork().getLinks().get(startLinkId);
	//		double travelTimeStartLink = startLink.getLength() / startLink.getFreespeed();
	//
	//
	//		Id<Link> endLinkId = leg.getRoute().getEndLinkId();
	//		Link endLink = controler.getScenario().getNetwork().getLinks().get(endLinkId);
	//		double travelTimeEndLink = endLink.getLength() / endLink.getFreespeed();
	//
	//		double travelTimeBetweenEnds = leg.getTravelTime().seconds();
	//
	//		return travelTimeStartLink + travelTimeBetweenEnds + travelTimeEndLink;
	//	}

}

