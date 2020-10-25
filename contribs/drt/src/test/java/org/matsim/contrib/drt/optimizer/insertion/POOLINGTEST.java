package org.matsim.contrib.drt.optimizer.insertion;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.matsim.vis.otfvis.OTFVisConfigGroup;

public class POOLINGTEST {
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();


	@Test
	public void testRunDrtExample() {
		PersonEnterDrtVehicleEventHandler handler = setupAndRunScenario(5000,
			5.0,
			400);

		System.out.println(handler.getVehRequestCount());
		Assert.assertEquals(1, handler.getVehRequestCount().size());
		Assert.assertTrue(handler.getVehRequestCount().values().contains(4));

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
				.setMaxTravelTimeBeta(maxTravelTimeBeta));

//		PlanCalcScoreConfigGroup.ActivityParams actParams = new PlanCalcScoreConfigGroup.ActivityParams("dummy2");
//		actParams.setTypicalDuration(3600.);
//		config.planCalcScore().addActivityParams(actParams);

		Controler controler = DrtControlerCreator.createControler(config, false);
		Scenario scenario = controler.getScenario();



		Population population = scenario.getPopulation();
		PopulationFactory pf = population.getFactory();
		ActivityFacilitiesFactory ff = controler.getScenario().getActivityFacilities().getFactory();

		ActivityFacility homeFac1 = ff.createActivityFacility(Id.create("hf1", ActivityFacility.class), Id.createLinkId("360"));
		ActivityFacility homeFac2 = ff.createActivityFacility(Id.create("hf2", ActivityFacility.class), Id.createLinkId("359"));
		ActivityFacility homeFac3 = ff.createActivityFacility(Id.create("hf3", ActivityFacility.class), Id.createLinkId("361"));
		ActivityFacility homeFac4 = ff.createActivityFacility(Id.create("hf4", ActivityFacility.class), Id.createLinkId("353"));

		ActivityFacility workFac1 = ff.createActivityFacility(Id.create("wf1", ActivityFacility.class), Id.createLinkId("285"));
		ActivityFacility workFac2 = ff.createActivityFacility(Id.create("wf2", ActivityFacility.class), Id.createLinkId("239"));
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
//
//		for (ActivityFacility fac : scenario.getActivityFacilities().getFacilities().values()) {
//			ActivityOption option = new ActivityOptionImpl("dummy");
//			fac.addActivityOption(option);
//		}


		Person p1 = makePerson(8 * 3600 + 0.,  homeFac1, workFac1, "1", pf);
		Person p2 = makePerson(8 * 3600 + 10., homeFac2, workFac2, "2", pf);
		Person p3 = makePerson(8 * 3600 + 20., homeFac3, workFac3, "3", pf);
		Person p4 = makePerson(8 * 3600 + 30., homeFac4, workFac4, "4", pf);

//		Person p1 = makePerson(8 * 3600 + 0. , "360", "285", "1", pf);
//		Person p2 = makePerson(8 * 3600 + 10., "359", "239", "2", pf);
//		Person p3 = makePerson(8 * 3600 + 20., "361", "237", "3", pf);
//		Person p4 = makePerson(8 * 3600 + 30., "353", "240", "4", pf);
		population.addPerson(p1);
		population.addPerson(p2);
		population.addPerson(p3);
		population.addPerson(p4);



		EventsManager events = controler.getEvents();
		PersonEnterDrtVehicleEventHandler handler = new PersonEnterDrtVehicleEventHandler();
		events.addHandler(handler);

		new PopulationWriter(population).write("C:\\Users\\jakob\\projects\\matsim-libs\\contribs\\drt\\src\\test\\java\\org\\matsim\\contrib\\drt\\optimizer\\insertion\\revisedPop2.xml");
		new FacilitiesWriter(controler.getScenario().getActivityFacilities()).write("C:\\Users\\jakob\\projects\\matsim-libs\\contribs\\drt\\src\\test\\java\\org\\matsim\\contrib\\drt\\optimizer\\insertion\\facilities.xml");
		new ConfigWriter(config).write("C:\\Users\\jakob\\projects\\matsim-libs\\contribs\\drt\\src\\test\\java\\org\\matsim\\contrib\\drt\\optimizer\\insertion\\revisedConfig2.xml");


		controler.run();

		Provider<TripRouter> tripRouter = controler.getTripRouterProvider();
		TripRouter tripRouter1 = tripRouter.get();
		RoutingModule drt = tripRouter1.getRoutingModule("car");


		List<? extends PlanElement> planElements = drt.calcRoute(homeFac1, homeFac2, 8 * 3600 + 0., p1);

		Leg leg = (Leg)planElements.get(0);
		OptionalTime travelTime = leg.getTravelTime();
		System.out.println("travel time: " + travelTime.seconds());

		System.out.println(planElements);
		return handler;
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

