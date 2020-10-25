package org.matsim.contrib.drt.optimizer.insertion;

import java.net.URL;
import java.util.*;

import com.google.inject.Provider;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.drt.routing.DrtRouteCreator;
import org.matsim.contrib.drt.routing.DrtRoutingModuleTest;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEventHandler;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.examples.ExamplesUtils;
import org.matsim.facilities.*;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.vehicles.Vehicle;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

public class POOLINGTEST {

	Controler controler;

	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();



	@Test
	public void testRunDrtExample() {
		PersonEnterDrtVehicleEventHandler handler = setupAndRunScenario(5000,
			1.0,
			0.); // 116 does not work! 120 does work!

		System.out.println(handler.getVehRequestCount());
		Assert.assertEquals("There should only be one vehicle used" , 1, handler.getVehRequestCount().size());
		Assert.assertTrue("One Vehicle should have been requested twice", handler.getVehRequestCount().values().contains(2));;

//		Provider<TripRouter> tripRouter = controler.getTripRouterProvider();
//		TripRouter tripRouter1 = tripRouter.get();
////		RoutingModule drt = tripRouter1.getRoutingModule("car");
//
////		tripRouter1.calcRoute("drt", )
////		ArrayList<ActivityFacility> route =(ArrayList<ActivityFacility>) Arrays.asList(homeFac1, homeFac2,workFac1,workFac2);
//
//		Scenario scenario = controler.getScenario();
//		Map<Id<ActivityFacility>, ? extends ActivityFacility> facilities = scenario.getActivityFacilities().getFacilities();
//		ActivityFacility homeFac1 = facilities.get(Id.create("hf1", ActivityFacility.class));
//		ActivityFacility homeFac2 = facilities.get(Id.create("hf2", ActivityFacility.class));
//		ActivityFacility workFac1 = facilities.get(Id.create("wf1", ActivityFacility.class));
//		ActivityFacility workFac2 = facilities.get(Id.create("wf2", ActivityFacility.class));
//
//		Person p1 = scenario.getPopulation().getPersons().get(Id.createPersonId("1"));
//		ActivityFacility vehLoc = scenario.getActivityFacilities().getFactory().createActivityFacility(Id.create("vehLoc", ActivityFacility.class), Id.createLinkId(385));
////		Double r1 = getTT(new ActivityFacility[]{vehLoc, homeFac1, homeFac2, workFac1, workFac2}, p1, tripRouter1);
////		Double r2 = getTT(new ActivityFacility[]{vehLoc, homeFac1, homeFac2, workFac2, workFac1}, p1, tripRouter1);
////		Double r3 = getTT(new ActivityFacility[]{vehLoc, homeFac2, homeFac1, workFac1, workFac2}, p1, tripRouter1);
////		Double r4 = getTT(new ActivityFacility[]{vehLoc, homeFac2, homeFac1, workFac2, workFac1}, p1, tripRouter1);
//		Double usual1 = getTT(facilities.get(homeFac1), facilities.get(workFac1), p1, tripRouter1);
//		Double usual2 = getTT(facilities.get(homeFac2), facilities.get(workFac2), p1, tripRouter1);
//
//		System.out.println("usual travel time - p1: " + usual1);
//
//		System.out.println("usual travel time - p2: " + usual2);

//		Leg h1_h2 = (Leg) tripRouter1.calcRoute("car",facilities.get("hf1"), facilities.get("hf2"), 8 * 3600 + 0., p1).get(0);
//		Double tt_h1_h2 = h1_h2.getTravelTime().seconds();
//		System.out.println("h1_h2: " + tt_h1_h2);
//		Double tt_vehLoc_h1 = getTT(vehLoc, facilities.get("hf1"), p1, tripRouter1);
//		System.out.println("vehLoc_h1: " + tt_vehLoc_h1);
//
//		Double tt_vehLoc_h2 = getTT(vehLoc, facilities.get("hf2"), p1, tripRouter1);
//		System.out.println("vehLoc_h2: " + tt_vehLoc_h2);


//		Double deltaPool = Double.max(tt_vehLoc_h1, tt_vehLoc_h2) + tt_h1_h2 + tt_w1_w2 + 4*60.;


	}

	private PersonEnterDrtVehicleEventHandler setupAndRunScenario(double maxWaitTime, double maxTravelTimeAlpha, double maxTravelTimeBeta) {
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_drt_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
			new OTFVisConfigGroup());

		config.plans().setInputFile(null);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());

//		config.facilities().setInputFile("");


		MultiModeDrtConfigGroup mm = ConfigUtils.addOrGetModule( config, MultiModeDrtConfigGroup.class );
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


		EventsManager events = controler.getEvents();
		PersonEnterDrtVehicleEventHandler handler = new PersonEnterDrtVehicleEventHandler();
		events.addHandler(handler);

//		new PopulationWriter(population).write("C:\\Users\\jakob\\projects\\matsim-libs\\contribs\\drt\\src\\test\\java\\org\\matsim\\contrib\\drt\\optimizer\\insertion\\revisedPop2.xml");
//		new FacilitiesWriter(controler.getScenario().getActivityFacilities()).write("C:\\Users\\jakob\\projects\\matsim-libs\\contribs\\drt\\src\\test\\java\\org\\matsim\\contrib\\drt\\optimizer\\insertion\\facilities.xml");
//		new ConfigWriter(config).write("C:\\Users\\jakob\\projects\\matsim-libs\\contribs\\drt\\src\\test\\java\\org\\matsim\\contrib\\drt\\optimizer\\insertion\\revisedConfig2.xml");

		controler.run();

//		Provider<TripRouter> tripRouter = controler.getTripRouterProvider();
//		TripRouter tripRouter1 = tripRouter.get();
////		RoutingModule drt = tripRouter1.getRoutingModule("car");
//
////		tripRouter1.calcRoute("drt", )
////		ArrayList<ActivityFacility> route =(ArrayList<ActivityFacility>) Arrays.asList(homeFac1, homeFac2,workFac1,workFac2);
//
//		ActivityFacility vehLoc = ff.createActivityFacility(Id.create("vehLoc", ActivityFacility.class), Id.createLinkId(385));
//		Double r1 = getTT(new ActivityFacility[]{vehLoc, homeFac1, homeFac2, workFac1, workFac2}, p1, tripRouter1);
//		Double r2 = getTT(new ActivityFacility[]{vehLoc, homeFac1, homeFac2, workFac2, workFac1}, p1, tripRouter1);
//		Double r3 = getTT(new ActivityFacility[]{vehLoc, homeFac2, homeFac1, workFac1, workFac2}, p1, tripRouter1);
//		Double r4 = getTT(new ActivityFacility[]{vehLoc, homeFac2, homeFac1, workFac2, workFac1}, p1, tripRouter1);
//		Double usual1 = getTT(homeFac1, workFac1, p1, tripRouter1);
//		Double usual2 = getTT(homeFac2, workFac2, p1, tripRouter1);
//
//		System.out.println(r1 + " , " + r2 + " , " + r3 + " , " + r4);
//
//		System.out.println("usual travel time - p1: " + usual1);
//
//		System.out.println("usual travel time - p2: " + usual2);
//
//		Leg h1_h2 = (Leg) tripRouter1.calcRoute("car",homeFac1, homeFac2, 8 * 3600 + 0., p2).get(0);
//		Double tt_h1_h2 = h1_h2.getTravelTime().seconds();
//		System.out.println("h1_h2: " + tt_h1_h2);
//
//		Double tt_w1_w2 = getTT(workFac1, workFac2, p2, tripRouter1);
//		System.out.println("w1_w2: " + tt_w1_w2);
//
//		Double tt_vehLoc_h1 = getTT(vehLoc, homeFac1, p2, tripRouter1);
//		System.out.println("vehLoc_h1: " + tt_vehLoc_h1);
//
//		Double tt_vehLoc_h2 = getTT(vehLoc, homeFac2, p2, tripRouter1);
//		System.out.println("vehLoc_h2: " + tt_vehLoc_h2);
//
//
//		Double deltaPool = Double.max(tt_vehLoc_h1, tt_vehLoc_h2) + tt_h1_h2 + tt_w1_w2 + 4*60.;


		return handler;
	}

	private Double getTT(ActivityFacility homeFac1, ActivityFacility workFac1, Person p1, TripRouter router) {
		Leg p1Usual = (Leg) router.calcRoute("car", homeFac1, workFac1, 8 * 3600 + 0., p1).get(0);
		return p1Usual.getTravelTime().seconds();
	}


	private Double getTT(ActivityFacility[] facilities, Person p1, TripRouter router) {
		Double tt = 0.;
		for (int i = 0; i < facilities.length - 1; i++) {
			tt += getTT(facilities[i], facilities[i + 1], p1, router);
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

}

