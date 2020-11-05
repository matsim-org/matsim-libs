package org.matsim.contrib.drt.optimizer.insertion;

import java.net.URL;
import java.util.*;

import com.google.inject.Provider;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteCreator;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEventHandler;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpOfflineTravelTimeEstimator;
import org.matsim.contrib.dvrp.trafficmonitoring.QSimFreeSpeedTravelTime;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.router.FastAStarLandmarksFactory;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.facilities.*;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

public class DrtPoolingTest {

	private Controler controler;

	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testMaxWaitTimeLow() {
		PersonEnterDrtVehicleEventHandler handler = setupAndRunScenario(100,
			10.0,
			10000.);
		Assert.assertEquals("There should be no vehicle used" , 0, handler.getVehRequestCount().size());

	}

	@Test
	public void testMaxWaitTimeOneVehicleUsed() {
		PersonEnterDrtVehicleEventHandler handler = setupAndRunScenario(150,
			10.0,
			10000.);
		Assert.assertEquals("There should be one vehicle used" , 1, handler.getVehRequestCount().size());
		Assert.assertTrue("Each vehicle should be requested once", handler.getVehRequestCount().values().contains(1));;

	}
	@Test
	public void testMaxWaitTimeTwoSeperateCarsUsed() {
		PersonEnterDrtVehicleEventHandler handler = setupAndRunScenario(160,
			10.0,
			10000.);
		Assert.assertEquals("There should be two vehicles used" , 2, handler.getVehRequestCount().size());
		Assert.assertTrue("Each vehicle should be requested once", handler.getVehRequestCount().values().contains(1));;


	}


	@Test
	public void testMaxWaitTimeHigh() {

		PersonEnterDrtVehicleEventHandler handler = setupAndRunScenario(5000,
			10.0,
			10000.);
		Assert.assertEquals("There should only be one vehicle used", 1, handler.getVehRequestCount().size());
		Assert.assertTrue("One Vehicle should have been requested twice", handler.getVehRequestCount().values().contains(2));

	}

	@Test
	public void testAlphaBetaHigh() {
		PersonEnterDrtVehicleEventHandler handler = setupAndRunScenario(5000,
			5.0,
			500);

		System.out.println(handler.getVehRequestCount());
		Assert.assertEquals("There should only be one vehicle used" , 1, handler.getVehRequestCount().size());
		Id<DvrpVehicle> drt_veh_1_1 = Id.create("drt_veh_1_1", DvrpVehicle.class);
		Assert.assertTrue("drt_veh_1_1 should be requested in general", handler.getVehRequestCount().containsKey(drt_veh_1_1));
		Assert.assertEquals("drt_veh_1_1 should be requested exactly twice", 2, handler.getVehRequestCount().get(drt_veh_1_1),0);

	}

	@Test
	public void testAlphaBetaLow() {
		PersonEnterDrtVehicleEventHandler handler = setupAndRunScenario(5000,
			1.0,
			0.);

		System.out.println(handler.getVehRequestCount());
		Assert.assertEquals("There should only be zero vehicles used" , 0, handler.getVehRequestCount().size());

	}

	/**
	 *
	 * p1:  12631.819285714286-526.3638571428572 = 12105.45542857143
	 * p2:  11627.663571428571-325.532714285714 = 11302.130857142856
	 *
	 * 100 --> 0 vehicles used
	 * 150 --> 0
	 * 175 --> 1 veh used for one passenger
	 * 200 --> 2 vehicles
	 * 300 --> 2 vehicles used
	 * 500 --> pooling
	 * 700 --> pooling
	 *
	 */



	@Test
	public void testAlphaBetaMid() {
		PersonEnterDrtVehicleEventHandler handler = setupAndRunScenario(5000,
			1.0,
			175);

		System.out.println(handler.getVehRequestCount());
		Assert.assertEquals("There should only be 2 vehicles used" , 2, handler.getVehRequestCount().size());

	}


	private PersonEnterDrtVehicleEventHandler setupAndRunScenario(double maxWaitTime, double maxTravelTimeAlpha, double maxTravelTimeBeta) {
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
			new OTFVisConfigGroup());

		config.plans().setInputFile(null);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());

//		config.facilities().setInputFile("");


		MultiModeDrtConfigGroup mm = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
		mm.getModalElements()
			.forEach(x -> x
				.setMaxWaitTime(maxWaitTime)
				.setMaxTravelTimeAlpha(maxTravelTimeAlpha)
				.setMaxTravelTimeBeta(maxTravelTimeBeta)
				.setStopDuration(1.));


		controler = DrtControlerCreator.createControler(config, false);
		Scenario scenario = controler.getScenario();

		Population population = scenario.getPopulation();
		PopulationFactory pf = population.getFactory();
		ActivityFacilitiesFactory ff = controler.getScenario().getActivityFacilities().getFactory();

		ActivityFacility homeFac1 = ff.createActivityFacility(Id.create("hf1", ActivityFacility.class), Id.createLinkId("359"));
		ActivityFacility homeFac2 = ff.createActivityFacility(Id.create("hf2", ActivityFacility.class), Id.createLinkId("302"));
		ActivityFacility homeFac3 = ff.createActivityFacility(Id.create("hf3", ActivityFacility.class), Id.createLinkId("359"));
		ActivityFacility homeFac4 = ff.createActivityFacility(Id.create("hf4", ActivityFacility.class), Id.createLinkId("353"));

		ActivityFacility workFac1 = ff.createActivityFacility(Id.create("wf1", ActivityFacility.class), Id.createLinkId("185"));
		ActivityFacility workFac2 = ff.createActivityFacility(Id.create("wf2", ActivityFacility.class), Id.createLinkId("185"));
		ActivityFacility workFac3 = ff.createActivityFacility(Id.create("wf3", ActivityFacility.class), Id.createLinkId("237"));
		ActivityFacility workFac4 = ff.createActivityFacility(Id.create("wf4", ActivityFacility.class), Id.createLinkId("240"));

		scenario.getActivityFacilities().addActivityFacility(homeFac1);
		scenario.getActivityFacilities().addActivityFacility(homeFac2);
		scenario.getActivityFacilities().addActivityFacility(homeFac3);
		scenario.getActivityFacilities().addActivityFacility(homeFac4);
		scenario.getActivityFacilities().addActivityFacility(workFac1);
		scenario.getActivityFacilities().addActivityFacility(workFac2);
		scenario.getActivityFacilities().addActivityFacility(workFac3);
		scenario.getActivityFacilities().addActivityFacility(workFac4);

		Person p1 = makePerson(8 * 3600 + 0.,  homeFac1, workFac1, "1", pf);
		Person p2 = makePerson(8 * 3600 + 10., homeFac2, workFac2, "2", pf);
//		Person p3 = makePerson(8 * 3600 + 20., homeFac3, workFac3, "3", pf);
//		Person p4 = makePerson(8 * 3600 + 30., homeFac4, workFac4, "4", pf);

//		Person p1 = makePerson(8 * 3600 + 0. , "360", "285", "1", pf);
//		Person p2 = makePerson(8 * 3600 + 10., "359", "239", "2", pf);
//		Person p3 = makePerson(8 * 3600 + 20., "361", "237", "3", pf);
//		Person p4 = makePerson(8 * 3600 + 30., "353", "240", "4", pf);
		population.addPerson(p1);
		population.addPerson(p2);
//		population.addPerson(p3);
//		population.addPerson(p4);


//		RouteFactory drtRouteFactory = new DrtRouteFactory();
//		scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DrtRoute.class, drtRouteFactory);



		EventsManager events = controler.getEvents();
		PersonEnterDrtVehicleEventHandler handler = new PersonEnterDrtVehicleEventHandler();
		events.addHandler(handler);

//		new PopulationWriter(population).write("C:\\Users\\jakob\\projects\\matsim-libs\\contribs\\drt\\src\\test\\java\\org\\matsim\\contrib\\drt\\optimizer\\insertion\\revisedPop2.xml");
//		new FacilitiesWriter(controler.getScenario().getActivityFacilities()).write("C:\\Users\\jakob\\projects\\matsim-libs\\contribs\\drt\\src\\test\\java\\org\\matsim\\contrib\\drt\\optimizer\\insertion\\facilities.xml");
//		new ConfigWriter(config).write("C:\\Users\\jakob\\projects\\matsim-libs\\contribs\\drt\\src\\test\\java\\org\\matsim\\contrib\\drt\\optimizer\\insertion\\revisedConfig2.xml");

		controler.run();

		return handler;
	}

//	private Double getDirectTT(ActivityFacility homeFac1, ActivityFacility workFac1, Person p1, TripRouter router) {
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


	private Double getDirectTT(ActivityFacility homeFac1, ActivityFacility workFac1, Person p1, TripRouter router) {

		List<? extends PlanElement> tripP1 = router.getRoutingModule("drt").calcRoute(homeFac1, workFac1, 0., p1);
		DrtRoute routeP1 = (DrtRoute) ((Leg) tripP1.get(2)).getRoute();
		double directRideTimeP1 = routeP1.getDirectRideTime();

		return directRideTimeP1;
	}


	private Double getDirectTT(ActivityFacility[] facilities, Person p1, TripRouter router) {
		Double tt = 0.;
		for (int i = 0; i < facilities.length - 1; i++) {
			tt += getDirectTT(facilities[i], facilities[i + 1], p1, router);
		}
		return tt;
	}
//	private Person makePerson(double depTime, ActivityFacility homeFacility, ActivityFacility workFacility, String pId, PopulationFactory pf) {
//		Id<Person> personId = Id.createPersonId(pId);
//		Person person = pf.createPerson(personId);
//		Plan plan = pf.createPlan();
//		Activity activity1 = pf.createActivityFromActivityFacilityId("dummý", homeFacility.getId());
//		activity1.setEndTime(depTime);
//		plan.addActivity(activity1);
//		plan.addLeg(pf.createLeg("drt"));
//		Activity activity2 = pf.createActivityFromActivityFacilityId("dummy", workFacility.getId());
//		activity2.setEndTimeUndefined();
//		plan.addActivity(activity2);
//		person.addPlan(plan);
//		return person;
//	}


	private Person makePerson(double depTime, ActivityFacility homeFacility, ActivityFacility workFacility, String pId, PopulationFactory pf) {
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

	private Person makePerson(double depTime, String homeLink, String workLink, String pId, PopulationFactory pf) {
		Id<Person> personId = Id.createPersonId(pId);
		Person person = pf.createPerson(personId);
		Plan plan = pf.createPlan();
		Activity activity1 = pf.createActivityFromLinkId("dummy", Id.createLinkId(homeLink));
		activity1.setEndTime(depTime);
		plan.addActivity(activity1);
		plan.addLeg(pf.createLeg("drt"));
		Activity activity2 = pf.createActivityFromLinkId("dummy", Id.createLinkId(workLink));
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
			vehRequestCount.put(vehicleId, ++ vehCnt);

		}

		public Map<Id<DvrpVehicle>, Integer> getVehRequestCount() {
			return vehRequestCount;
		}
	}


	@Test
	public void getParams() {
		PersonEnterDrtVehicleEventHandler handler = setupAndRunScenario(5000,
			5.0,
			10000.); // 116 does not work! 120 does work!

//		System.out.println(handler.getVehRequestCount());
//		Assert.assertEquals("There should only be one vehicle used" , 1, handler.getVehRequestCount().size());
//		Assert.assertTrue("One Vehicle should have been requested twice", handler.getVehRequestCount().values().contains(2));;

		Scenario scenario = controler.getScenario();
		TripRouter tripRouter = controler.getTripRouterProvider().get();





////		ArrayList<ActivityFacility> route =(ArrayList<ActivityFacility>) Arrays.asList(homeFac1, homeFac2,workFac1,workFac2);
//
		Map<Id<ActivityFacility>, ? extends ActivityFacility> facilities = scenario.getActivityFacilities().getFacilities();
		System.out.println(facilities);

		ActivityFacility homeFac1 = facilities.get(Id.create("hf1", ActivityFacility.class));
		ActivityFacility homeFac2 = facilities.get(Id.create("hf2", ActivityFacility.class));
		ActivityFacility workFac1 = facilities.get(Id.create("wf1", ActivityFacility.class));
		ActivityFacility workFac2 = facilities.get(Id.create("wf2", ActivityFacility.class));
//
		Person p1 = scenario.getPopulation().getPersons().get(Id.createPersonId("1"));
		ActivityFacility vehLoc = scenario.getActivityFacilities().getFactory().createActivityFacility(Id.create("vehLoc", ActivityFacility.class), Id.createLinkId(385));

		double directRideTimeP1 = getDirectTT(homeFac1, workFac1, p1, tripRouter);
		double directRideTimeP2 = getDirectTT(homeFac2, workFac2, p1, tripRouter);


		List<? extends PlanElement> tripP2 = tripRouter.getRoutingModule("drt").calcRoute(homeFac2, workFac2, 0., p1);
		DrtRoute routeP2 = (DrtRoute) ((Leg) tripP2.get(2)).getRoute();
		double directRideTimeP2 = routeP2.getDirectRideTime();
		double ttP2 = routeP2.getTravelTime().seconds();
		double waitP2 = ttP2 - directRideTimeP2;

		List<? extends PlanElement> vehToP1 = tripRouter.getRoutingModule("drt").calcRoute(vehLoc, homeFac1, 0., p1);
		DrtRoute routeVehToP1 = (DrtRoute) ((Leg) vehToP1.get(2)).getRoute();
		double directRideTimeVehToP1 = routeVehToP1.getDirectRideTime();
		double ttVehToP1 = routeVehToP1.getTravelTime().seconds();
		double waitVehToP1 = ttVehToP1 - directRideTimeVehToP1;

		List<? extends PlanElement> vehToP2 = tripRouter.getRoutingModule("drt").calcRoute(vehLoc, homeFac2, 0., p1);
		DrtRoute routeVehToP2 = (DrtRoute) ((Leg) vehToP2.get(2)).getRoute();
		double directRideTimeVehToP2 = routeVehToP2.getDirectRideTime();
		double ttVehToP2 = routeVehToP2.getTravelTime().seconds();
		double waitVehToP2 = ttVehToP2 - directRideTimeVehToP2;


//		List<? extends PlanElement> usualTripP2 = tripRouter1.getRoutingModule("drt").calcRoute(homeFac2, workFac2, 0., p1);
//		double ttUsualP2 = ((Leg) usualTripP2.get(2)).getTravelTime().seconds();

		// direct ride time : 526.3638571428572
		//  travTime : 12631.819285714286


//		List<? extends PlanElement> car = tripRouter1.getRoutingModule("car").calcRoute(homeFac1, workFac1, 0., p1);
//		double ttCAR = ((Leg) car.get(0)).getTravelTime().seconds();


		////		Double r1 = getDirectTT(new ActivityFacility[]{vehLoc, homeFac1, homeFac2, workFac1, workFac2}, p1, tripRouter1);
////		Double r2 = getDirectTT(new ActivityFacility[]{vehLoc, homeFac1, homeFac2, workFac2, workFac1}, p1, tripRouter1);
////		Double r3 = getDirectTT(new ActivityFacility[]{vehLoc, homeFac2, homeFac1, workFac1, workFac2}, p1, tripRouter1);
////		Double r4 = getDirectTT(new ActivityFacility[]{vehLoc, homeFac2, homeFac1, workFac2, workFac1}, p1, tripRouter1);
		Double usual1 = getDirectTT(homeFac1, workFac1, p1, tripRouter);
		Double usual2 = getDirectTT(homeFac2, workFac2, p1, tripRouter);

		System.out.println("usual travel time - p1: " + usual1);

		System.out.println("usual travel time - p2: " + usual2);


		DrtRouteFactory drtRouteFactory = new DrtRouteFactory();
//		scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DrtRoute.class, drtRouteFactory);


//		Route route1 = drtRouteFactory.createRoute(homeFac1.getLinkId(), workFac1.getLinkId());
		Route route2 = scenario.getPopulation().getFactory().getRouteFactories().createRoute(DrtRoute.class, homeFac1.getLinkId(), workFac1.getLinkId());
		route2.getTravelTime();




		DrtConfigGroup drtCfg = ConfigUtils.addOrGetModule( scenario.getConfig(), DrtConfigGroup.class );
		LeastCostPathCalculatorFactory leastCostPathCalculatorFactory = new FastAStarLandmarksFactory(scenario.getConfig().global());
		TravelTime travelTime =  new QSimFreeSpeedTravelTime(scenario.getConfig().qsim());
		TravelDisutilityFactory travelDisutilityFactory = new OnlyTimeDependentTravelDisutilityFactory();
//		RouteFactories routeFactories = new RouteFactories();
		DrtRouteCreator drtRouteCreator = new DrtRouteCreator(drtCfg, scenario.getNetwork(),
			leastCostPathCalculatorFactory,
			travelTime,
			travelDisutilityFactory);


//		DvrpTravelTimeModule.DVRP_ESTIMATED
		Route route = drtRouteCreator.createRoute(0.,
			scenario.getNetwork().getLinks().get(homeFac1.getLinkId()),
			scenario.getNetwork().getLinks().get(workFac1.getLinkId()),
			new RouteFactories());




		route.getTravelTime().seconds();



		TravelTime initialTT = new QSimFreeSpeedTravelTime(scenario.getConfig().qsim());
		Network network = scenario.getNetwork();
		TravelTimeCalculatorConfigGroup configGroup = new TravelTimeCalculatorConfigGroup();
		DvrpOfflineTravelTimeEstimator travelTimeEstimator = new DvrpOfflineTravelTimeEstimator(initialTT, initialTT, network, configGroup, 1.);
		Vehicle vehicle = scenario.getVehicles().getVehicles().get(Id.createVehicleId("drt_veh_1_1"));
		double linkTravelTime = travelTimeEstimator.getLinkTravelTime(
			scenario.getNetwork().getLinks().get(Id.createLinkId(362)),
			0.,
			null,
			null);

		System.out.println(linkTravelTime);



//		TravelTimeUtils.initTravelTimeCalculatorFromEvents();
//
//		VrpPathWithTravelData unsharedPath = VrpPaths.calcAndCreatePath(accessActLink, egressActLink, departureTime,
//			tripRouter1, travelTime);
//		double unsharedRideTime = unsharedPath.getTravelTime();//includes first & last link
//		double maxTravelTime = getMaxTravelTime(drtCfg, unsharedRideTime);

//		Leg h1_h2 = (Leg) tripRouter1.calcRoute("car",facilities.get("hf1"), facilities.get("hf2"), 8 * 3600 + 0., p1).get(0);
//		Double tt_h1_h2 = h1_h2.getTravelTime().seconds();
//		System.out.println("h1_h2: " + tt_h1_h2);
		Double tt_vehLoc_h1 = getDirectTT(vehLoc, homeFac1, p1, tripRouter);
//		System.out.println("vehLoc_h1: " + tt_vehLoc_h1);
//
		Double tt_vehLoc_h2 = getDirectTT(vehLoc, homeFac2, p1, tripRouter);
		System.out.println("vehLoc_h2: " + tt_vehLoc_h2);


		Double tt_h1_w1 = getDirectTT(homeFac1,workFac1 , p1, tripRouter);

		System.out.println(tt_h1_w1);

//		Double deltaPool = Double.max(tt_vehLoc_h1, tt_vehLoc_h2) + tt_h1_h2 + tt_w1_w2 + 4*60.;
	}

}

