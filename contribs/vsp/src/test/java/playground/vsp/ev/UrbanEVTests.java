package playground.vsp.ev;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.ev.charging.ChargingEndEvent;
import org.matsim.contrib.ev.charging.ChargingEndEventHandler;
import org.matsim.contrib.ev.charging.ChargingStartEvent;
import org.matsim.contrib.ev.charging.ChargingStartEventHandler;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

// TODO translate and complete

/**
 * <b>für (jeden) Agenten:</b>		</b>
 * (*) wann wird geladen?	<br>
 * (*) wo wird geladen?	<br>
 * (*) wird geladen?	<br>
 * (*) wie lange wird geladen?	<br>
 * (*) wie oft wird geladen?	<br>
 * ? wird auch bei Fahrzeugwechsel (anderer Mode) geladen?	<br>
 * ? wird auch 3x geladen?	<br>
 * ? gleichzeitiges Laden: werden die Fahrzeuge in der richtigen Reihenfolge ein- und ausgestöpselt? (chargingStart und chargingEndEvents) <br>
 * ? nicht Lader <br>
 * ? zu kurze Ladezeit/falsche Aktivitätentypen <br>
 * <br>
 * <b>für jedes Fahrzeug</b>	<br>
 * (*) wird am richtigen charger geladen (charger type / leistung)?	<br>
 *
 * <b>generell:</b>	<br>
 * Konsistenz zw Plugin and Plugout bzgl <br>
 * ((*) Ort = Link <br>
 * (*) Häufigkeit <br>
 * (*) .. <br>
 **/
public class UrbanEVTests {

	private static UrbanEVTestHandler handler;
	private static Map<Id<Person>, List<Activity>> plannedActivitiesPerPerson;

	@BeforeAll
	public static void run() {
		Scenario scenario = CreateUrbanEVTestScenario.createTestScenario();
		scenario.getConfig().controller().setOutputDirectory("test/output/playground/vsp/ev/UrbanEVTests/");

		scenario.getConfig().controller().setLastIteration(0);

		//modify population
		overridePopulation(scenario);
		plannedActivitiesPerPerson = scenario.getPopulation()
				.getPersons()
				.values()
				.stream()
				.collect(Collectors.toMap(p -> p.getId(),
						p -> TripStructureUtils.getActivities(p.getSelectedPlan(), TripStructureUtils.StageActivityHandling.ExcludeStageActivities)));

		//remove and insert vehicles
		scenario.getVehicles().getVehicles().keySet().forEach(vehicleId -> {
			Id<VehicleType> type = scenario.getVehicles().getVehicles().get(vehicleId).getType().getId();
			scenario.getVehicles().removeVehicle(vehicleId);
			scenario.getVehicles().removeVehicleType(type);
		});
		RunUrbanEVExample.createAndRegisterPersonalCarAndBikeVehicles(scenario);

		//this guy shall start with more energy than the others.
		//		EVUtils.setInitialEnergy(scenario.getVehicles().getVehicleTypes().get(Id.create("Not enough time so charging early", VehicleType.class)).getEngineInformation(),
		//				5.0);

		///controler with Urban EV module
		Controler controler = RunUrbanEVExample.prepareControler(scenario);
		handler = new UrbanEVTestHandler();
		controler.addOverridingModule(new AbstractModule() {
			public void install() {
				this.addEventHandlerBinding().toInstance(handler);
			}
		});
		controler.run();
	}

	@Test
	void testAgentsExecuteSameNumberOfActsAsPlanned() {

		boolean fail = false;
		String personsWithDifferingActCount = "";
		for (Map.Entry<Id<Person>, List<Activity>> person2Acts : plannedActivitiesPerPerson.entrySet()) {

			List<ActivityStartEvent> executedActs = handler.normalActStarts.get(person2Acts.getKey());
			if (executedActs.size() != person2Acts.getValue().size() - 1) { //first act of the day is not started but only ended in qsim
				fail = true;
				personsWithDifferingActCount += "\n"
						+ person2Acts.getKey()
						+ " plans "
						+ person2Acts.getValue().size()
						+ " activities and executes "
						+ executedActs.size()
						+ " activities";
			}
		}
		Assertions.assertFalse(fail,
				"the following persons do not execute the same amount of activities as they plan to:" + personsWithDifferingActCount);
	}

	@Test
	void testCarAndBikeAgent() {
		List<ActivityStartEvent> plugins = this.handler.plugInCntPerPerson.getOrDefault(Id.createPersonId("Charge during leisure + bike"), List.of());
		Assertions.assertEquals(1, plugins.size(), 0);

		//charges at during leisure(12)-bike-leisure(13)-bike-leisure(14)
		ActivityStartEvent pluginActStart2 = plugins.get(0);
		//agent travels 5 links between precedent work activity which end at 11. each link takes 99 seconds
		Assertions.assertEquals(11 * 3600 + 5 * 99, pluginActStart2.getTime(), MatsimTestUtils.EPSILON, "wrong charging start time");
		Assertions.assertEquals("90", pluginActStart2.getLinkId().toString(), "wrong charging start location");

		List<ActivityEndEvent> plugouts = this.handler.plugOutCntPerPerson.getOrDefault(Id.createPersonId("Charge during leisure + bike"), List.of());
		Assertions.assertEquals(1, plugouts.size(), 0);

		ActivityEndEvent plugoutActStart = plugouts.get(0);
		Assertions.assertEquals(14 * 3600, plugoutActStart.getTime(), MatsimTestUtils.EPSILON, "wrong charging end time");
		Assertions.assertEquals("90", plugoutActStart.getLinkId().toString(), "wrong charging end location");
	}

	@Test
	void testTripleCharger() {
		List<ActivityStartEvent> plugins = this.handler.plugInCntPerPerson.getOrDefault(Id.createPersonId("Triple Charger"), List.of());
		Assertions.assertEquals(plugins.size(), 3., 0);
		ActivityStartEvent pluginActStart = plugins.get(0);
		Assertions.assertEquals(1490d, pluginActStart.getTime(), MatsimTestUtils.EPSILON, "wrong charging start time");
		Assertions.assertEquals("90", pluginActStart.getLinkId().toString(), "wrong charging start location");

		ActivityStartEvent pluginActStart2 = plugins.get(1);
		Assertions.assertEquals(25580d, pluginActStart2.getTime(), MatsimTestUtils.EPSILON, "wrong charging start time");
		Assertions.assertEquals("90", pluginActStart2.getLinkId().toString(), "wrong charging start location");

		ActivityStartEvent pluginActStart3 = plugins.get(2);
		Assertions.assertEquals(45724d, pluginActStart3.getTime(), MatsimTestUtils.EPSILON, "wrong charging start time");
		Assertions.assertEquals("95", pluginActStart3.getLinkId().toString(), "wrong charging start location");

		List<ActivityEndEvent> plugouts = this.handler.plugOutCntPerPerson.getOrDefault(Id.createPersonId("Triple Charger"), List.of());
		Assertions.assertEquals(plugouts.size(), 2, 0);
		ActivityEndEvent plugoutActStart = plugouts.get(0);
		Assertions.assertEquals(3179d, plugoutActStart.getTime(), MatsimTestUtils.EPSILON, "wrong charging end time");
		Assertions.assertEquals("90", plugoutActStart.getLinkId().toString(), "wrong charging end location");

		ActivityEndEvent plugoutActStart2 = plugouts.get(1);
		Assertions.assertEquals(26608d, plugoutActStart2.getTime(), MatsimTestUtils.EPSILON, "wrong charging end time");
		Assertions.assertEquals("90", plugoutActStart2.getLinkId().toString(), "wrong charging end location");
	}

	@Test
	void testChargerSelectionShopping() {
		List<ActivityStartEvent> plugins = this.handler.plugInCntPerPerson.getOrDefault(Id.createPersonId("Charging during shopping"), List.of());
		Assertions.assertEquals(1, plugins.size(), 0);

		ActivityStartEvent pluginActStart = plugins.get(0);
		//starts at 10am at work and travels 8 links à 99s
		Assertions.assertEquals(10 * 3600 + 8 * 99, pluginActStart.getTime(), MatsimTestUtils.EPSILON, "wrong charging start time");
		Assertions.assertEquals("172", pluginActStart.getLinkId().toString(), "wrong charging start location");

		List<ActivityEndEvent> plugouts = this.handler.plugOutCntPerPerson.getOrDefault(Id.createPersonId("Charging during shopping"), List.of());
		Assertions.assertEquals(1, plugouts.size(), 0);
		ActivityEndEvent plugoutActStart = plugouts.get(0);
		Assertions.assertEquals(11 * 3600 + 23 * 60 + 27, plugoutActStart.getTime(), MatsimTestUtils.EPSILON, "wrong charging end time");
		Assertions.assertEquals("172", plugoutActStart.getLinkId().toString(), "wrong charging end location");

	}

	@Test
	void testLongDistance() {
		List<ActivityStartEvent> plugins = this.handler.plugInCntPerPerson.getOrDefault(Id.createPersonId("Charger Selection long distance leg"),
				List.of());
		Assertions.assertEquals(1, plugins.size(), 0);
		ActivityStartEvent pluginActStart = plugins.get(0);

		//starts at 8 am and travels 19 links à 99s + 3s waiting time to enter traffic
		Assertions.assertEquals(8 * 3600 + 19 * 99 + 3, pluginActStart.getTime(), MatsimTestUtils.EPSILON, "wrong charging start time");
		Assertions.assertEquals("89", pluginActStart.getLinkId().toString(), "wrong charging start location");

		List<ActivityEndEvent> plugouts = this.handler.plugOutCntPerPerson.getOrDefault(Id.createPersonId("Charger Selection long distance leg"),
				List.of());
		Assertions.assertEquals(1, plugouts.size(), 0);
		ActivityEndEvent plugoutActStart = plugouts.get(0);
		//needs to walk for 26 minutes
		Assertions.assertEquals(10 * 3600 + 26 * 60, plugoutActStart.getTime(), MatsimTestUtils.EPSILON, "wrong charging end time");
		Assertions.assertEquals("89", plugoutActStart.getLinkId().toString(), "wrong charging end location");
	}

	@Test
	void testTwin() {
		List<ActivityStartEvent> plugins = this.handler.plugInCntPerPerson.getOrDefault(Id.createPersonId("Charger Selection long distance twin"),
				List.of());
		Assertions.assertEquals(1, plugins.size(), 0);
		ActivityStartEvent pluginActStart = plugins.get(0);

		//starts at 8:00:40 am and travels 19 links à 99s
		Assertions.assertEquals(8 * 3605 + 19 * 99, pluginActStart.getTime(), MatsimTestUtils.EPSILON, "wrong charging start time");
		Assertions.assertEquals("89", pluginActStart.getLinkId().toString(), "wrong charging start location");

		List<ActivityEndEvent> plugouts = this.handler.plugOutCntPerPerson.getOrDefault(Id.createPersonId("Charger Selection long distance twin"),
				List.of());
		Assertions.assertEquals(1, plugouts.size(), 0);
		ActivityEndEvent plugoutActStart = plugouts.get(0);
		Assertions.assertEquals(10 * 3600, plugoutActStart.getTime(), MatsimTestUtils.EPSILON, "wrong charging end time");
		Assertions.assertEquals("89", plugoutActStart.getLinkId().toString(), "wrong charging end location");
	}

	@Test
	void testDoubleCharger() {
		List<ActivityStartEvent> plugins = this.handler.plugInCntPerPerson.getOrDefault(Id.createPersonId("Double Charger"), List.of());
		Assertions.assertEquals(1, plugins.size(), 0);
		ActivityStartEvent pluginActStart = plugins.get(0);

		//starts at 6 am and travels 17 links à 99s
		Assertions.assertEquals(6 * 3600 + 17 * 99, pluginActStart.getTime(), MatsimTestUtils.EPSILON, "wrong charging start time");
		Assertions.assertEquals("90", pluginActStart.getLinkId().toString(), "wrong charging start location");

		List<ActivityEndEvent> plugouts = this.handler.plugOutCntPerPerson.getOrDefault(Id.createPersonId("Double Charger"), List.of());
		Assertions.assertEquals(1, plugouts.size(), 0);
		ActivityEndEvent plugoutActStart = plugouts.get(0);
		Assertions.assertEquals(28783d, plugoutActStart.getTime(), MatsimTestUtils.EPSILON, "wrong charging end time");
		Assertions.assertEquals("90", plugoutActStart.getLinkId().toString(), "wrong charging end location");
	}

	@Test
	void testNotEnoughTimeCharger() {
		//TODO this test succeeds if the corresponding agents is deleted
		List<ActivityStartEvent> plugins = this.handler.plugInCntPerPerson.getOrDefault(Id.createPersonId("Not enough time so no charge"), List.of());
		Assertions.assertTrue(plugins.isEmpty());

		List<ActivityEndEvent> plugouts = this.handler.plugOutCntPerPerson.getOrDefault(Id.createPersonId("Not enough time so no charge"), List.of());
		Assertions.assertTrue(plugins.isEmpty());
	}

	@Test
	void testEarlyCharger() {
		//this guy starts with more energy than the others, exceeds the threshold at the 3rd leg but can only charge during first non-home-act. charge is lasting long enough so no additional charge is needed

		List<ActivityStartEvent> plugins = this.handler.plugInCntPerPerson.getOrDefault(Id.createPersonId("Not enough time so charging early"),
				List.of());
		Assertions.assertEquals(1, plugins.size(), 0);
		ActivityStartEvent pluginActStart = plugins.get(0);

		//starts at 6 am and travels 18 links à 99s + 3s waiting time to enter traffic
		Assertions.assertEquals(6 * 3600 + 18 * 99 + 3, pluginActStart.getTime(), MatsimTestUtils.EPSILON, "wrong charging start time");
		Assertions.assertEquals("90", pluginActStart.getLinkId().toString(), "wrong charging start location");

		List<ActivityEndEvent> plugouts = this.handler.plugOutCntPerPerson.getOrDefault(Id.createPersonId("Not enough time so charging early"),
				List.of());
		Assertions.assertEquals(1, plugouts.size(), 0);
		ActivityEndEvent plugoutActStart = plugouts.get(0);
		Assertions.assertEquals(7 * 3600 + 27 * 60 + 49, plugoutActStart.getTime(), MatsimTestUtils.EPSILON, "wrong charging end time");
		Assertions.assertEquals("90", plugoutActStart.getLinkId().toString(), "wrong charging end location");
	}

	@Test
	void testHomeCharger() {
		List<ActivityStartEvent> plugins = this.handler.plugInCntPerPerson.getOrDefault(Id.createPersonId("Home Charger"), List.of());
		Assertions.assertEquals(1, plugins.size(), 0);
		ActivityStartEvent pluginActStart = plugins.get(0);

		//starts return to home trip at 8am and travels 10 links à 99s + 3s waiting time
		Assertions.assertEquals(8 * 3600 + 10 * 99, pluginActStart.getTime(), MatsimTestUtils.EPSILON, "wrong charging start time");
		Assertions.assertEquals(pluginActStart.getLinkId().toString(), "95", "wrong charging start location");

		List<ActivityEndEvent> plugouts = this.handler.plugOutCntPerPerson.getOrDefault(Id.createPersonId("Home Charger"), List.of());
		Assertions.assertTrue(plugouts.isEmpty(), "Home charger should not have a plug out interaction");
	}

	@Test
	void testNoRoundTripSoNoHomeCharge() {
		//TODO this test succeeds if the corresponding agents is deleted
		List<ActivityStartEvent> plugins = this.handler.plugInCntPerPerson.getOrDefault(Id.createPersonId("No Round Trip So No Home Charge"),
				List.of());

		Assertions.assertTrue(plugins.isEmpty());

		List<ActivityEndEvent> plugouts = this.handler.plugOutCntPerPerson.getOrDefault(Id.createPersonId("No Round Trip So No Home Charge"),
				List.of());
		Assertions.assertTrue(plugouts.isEmpty(), "Home charger should not have a plug out interaction");
	}

	@Test
	void testDoubleChargerHomeCharger() {
		List<ActivityStartEvent> plugins = this.handler.plugInCntPerPerson.getOrDefault(Id.createPersonId("Double Charger Home Charger"), List.of());
		Assertions.assertEquals(plugins.size(), 2, 0);
		ActivityStartEvent pluginActStart = plugins.get(0);
		Assertions.assertEquals(5 * 3600 + 13 * 99, pluginActStart.getTime(), MatsimTestUtils.EPSILON, "wrong charging start time");
		Assertions.assertEquals("95", pluginActStart.getLinkId().toString(), "wrong charging start location");

		//drives back home at 17 pm and travels 18 links
		ActivityStartEvent pluginActStart2 = plugins.get(1);
		Assertions.assertEquals(17 * 3600 + 18 * 99, pluginActStart2.getTime(), MatsimTestUtils.EPSILON, "wrong charging start time");
		Assertions.assertEquals("90", pluginActStart2.getLinkId().toString(), "wrong charging start location");

		List<ActivityEndEvent> plugouts = this.handler.plugOutCntPerPerson.getOrDefault(Id.createPersonId("Double Charger Home Charger"), List.of());
		ActivityEndEvent plugoutActStart = plugouts.get(0);
		Assertions.assertEquals(33242d, plugoutActStart.getTime(), MatsimTestUtils.EPSILON, "wrong charging end time");
		Assertions.assertEquals("95", plugoutActStart.getLinkId().toString(), "wrong charging end location");
		Assertions.assertEquals(1, plugouts.size(), "Should plug out exactly once (as second plugin is for home charge)");
	}

	private static void overridePopulation(Scenario scenario) {

		//delete all persons that are there already
		scenario.getPopulation().getPersons().clear();

		PopulationFactory factory = scenario.getPopulation().getFactory();

		{
			Person person = factory.createPerson(Id.createPersonId("Charge during leisure + bike"));

			Plan plan = factory.createPlan();

			Activity home1 = factory.createActivityFromLinkId("home", Id.createLinkId("19"));
			home1.setEndTime(8 * 3600);
			plan.addActivity(home1);

			plan.addLeg(factory.createLeg(TransportMode.car));

			Activity work1 = factory.createActivityFromLinkId("work", Id.createLinkId("24"));
			work1.setEndTime(10 * 3600);
			plan.addActivity(work1);

			plan.addLeg(factory.createLeg(TransportMode.car));

			Activity work12 = factory.createActivityFromLinkId("work", Id.createLinkId("176"));
			work12.setEndTime(11 * 3600);
			plan.addActivity(work12);

			plan.addLeg(factory.createLeg(TransportMode.car));

			Activity leisure1 = factory.createActivityFromLinkId("leisure", Id.createLinkId("90"));
			leisure1.setEndTime(12 * 3600);
			plan.addActivity(leisure1);

			plan.addLeg(factory.createLeg(TransportMode.bike));

			Activity leisure12 = factory.createActivityFromLinkId("leisure", Id.createLinkId("89"));
			leisure12.setEndTime(13 * 3600);
			plan.addActivity(leisure12);

			plan.addLeg(factory.createLeg(TransportMode.bike));

			Activity leisure13 = factory.createActivityFromLinkId("leisure", Id.createLinkId("90"));
			leisure13.setEndTime(14 * 3600);
			plan.addActivity(leisure13);

			plan.addLeg(factory.createLeg(TransportMode.car));

			Activity home12 = factory.createActivityFromLinkId("home", Id.createLinkId("19"));
			home12.setEndTime(15 * 3600);
			plan.addActivity(home12);
			person.addPlan(plan);
			person.setSelectedPlan(plan);

			scenario.getPopulation().addPerson(person);
		}

		{
			Person person2 = factory.createPerson(Id.createPersonId("Charging during shopping"));

			Plan plan2 = factory.createPlan();

			Activity home21 = factory.createActivityFromLinkId("home", Id.createLinkId("1"));
			home21.setEndTime(8 * 3600);
			plan2.addActivity(home21);
			plan2.addLeg(factory.createLeg(TransportMode.car));

			Activity work21 = factory.createActivityFromLinkId("work", Id.createLinkId("176"));
			work21.setEndTime(10 * 3600);
			plan2.addActivity(work21);

			plan2.addLeg(factory.createLeg(TransportMode.car));

			//			Activity work22 = factory.createActivityFromLinkId("work", Id.createLinkId("60"));
			//			work22.setEndTime(12 * 3600);
			//			plan2.addActivity(work22);
			//
			//			plan2.addLeg(factory.createLeg(TransportMode.car));

			Activity shopping21 = factory.createActivityFromLinkId("shopping", Id.createLinkId("9"));
			shopping21.setMaximumDuration(1200);

			plan2.addActivity(shopping21);

			plan2.addLeg(factory.createLeg(TransportMode.car));

			Activity work23 = factory.createActivityFromLinkId("work", Id.createLinkId("5"));
			work23.setEndTime(13 * 3600);
			plan2.addActivity(work23);

			plan2.addLeg(factory.createLeg(TransportMode.car));

			Activity home22 = factory.createActivityFromLinkId("home", Id.createLinkId("1"));
			home22.setEndTime(15 * 3600);
			plan2.addActivity(home22);
			person2.addPlan(plan2);
			person2.setSelectedPlan(plan2);

			scenario.getPopulation().addPerson(person2);
		}

		{
			Person person3 = factory.createPerson(Id.createPersonId("Charger Selection long distance leg"));

			Plan plan3 = factory.createPlan();

			Activity home31 = factory.createActivityFromLinkId("home", Id.createLinkId("1"));
			home31.setEndTime(8 * 3600);
			plan3.addActivity(home31);
			plan3.addLeg(factory.createLeg(TransportMode.car));

			Activity work31 = factory.createActivityFromLinkId("work", Id.createLinkId("170"));
			work31.setEndTime(10 * 3600);
			plan3.addActivity(work31);

			plan3.addLeg(factory.createLeg(TransportMode.car));

			Activity work32 = factory.createActivityFromLinkId("work", Id.createLinkId("99"));
			work32.setEndTime(12 * 3600);
			plan3.addActivity(work32);

			plan3.addLeg(factory.createLeg(TransportMode.car));

			Activity home32 = factory.createActivityFromLinkId("home", Id.createLinkId("1"));
			home32.setEndTime(15 * 3600);
			plan3.addActivity(home32);
			person3.addPlan(plan3);
			person3.setSelectedPlan(plan3);

			scenario.getPopulation().addPerson(person3);
		}

		{
			Person person4 = factory.createPerson(Id.createPersonId("Charger Selection long distance twin"));

			Plan plan4 = factory.createPlan();

			Activity home41 = factory.createActivityFromLinkId("home", Id.createLinkId("1"));
			home41.setEndTime(8 * 3605);
			plan4.addActivity(home41);
			plan4.addLeg(factory.createLeg(TransportMode.car));

			Activity work41 = factory.createActivityFromLinkId("work", Id.createLinkId("89"));
			work41.setEndTime(10 * 3600);
			plan4.addActivity(work41);

			plan4.addLeg(factory.createLeg(TransportMode.car));

			Activity work42 = factory.createActivityFromLinkId("work", Id.createLinkId("99"));
			work42.setEndTime(12 * 3600);
			plan4.addActivity(work42);

			plan4.addLeg(factory.createLeg(TransportMode.car));

			Activity home42 = factory.createActivityFromLinkId("home", Id.createLinkId("1"));
			home42.setEndTime(15 * 3600);
			plan4.addActivity(home42);
			person4.addPlan(plan4);
			person4.setSelectedPlan(plan4);

			scenario.getPopulation().addPerson(person4);
		}

		{

			Person person5 = factory.createPerson(Id.createPersonId("Triple Charger"));

			Plan plan5 = factory.createPlan();

			Activity home51 = factory.createActivityFromLinkId("home", Id.createLinkId("95"));
			home51.setEndTime(5);
			plan5.addActivity(home51);

			plan5.addLeg(factory.createLeg(TransportMode.car));

			Activity work51 = factory.createActivityFromLinkId("work", Id.createLinkId("90"));
			work51.setMaximumDuration(1 * 1800);
			plan5.addActivity(work51);

			plan5.addLeg(factory.createLeg(TransportMode.car));

			Activity work52 = factory.createActivityFromLinkId("work", Id.createLinkId("95"));
			work52.setMaximumDuration(1 * 1800);
			plan5.addActivity(work52);

			plan5.addLeg(factory.createLeg(TransportMode.car));

			Activity work53 = factory.createActivityFromLinkId("work", Id.createLinkId("90"));
			work53.setMaximumDuration(1 * 1800);
			plan5.addActivity(work53);

			plan5.addLeg(factory.createLeg(TransportMode.car));

			Activity home52 = factory.createActivityFromLinkId("work", Id.createLinkId("95"));
			home52.setMaximumDuration(1 * 1800);
			plan5.addActivity(home52);

			plan5.addLeg(factory.createLeg(TransportMode.car));

			Activity work54 = factory.createActivityFromLinkId("work", Id.createLinkId("90"));
			work54.setMaximumDuration(1 * 1800);
			plan5.addActivity(work54);

			plan5.addLeg(factory.createLeg(TransportMode.car));

			Activity work55 = factory.createActivityFromLinkId("work", Id.createLinkId("95"));
			work55.setMaximumDuration(1 * 1800);
			plan5.addActivity(work55);

			plan5.addLeg(factory.createLeg(TransportMode.car));

			Activity work56 = factory.createActivityFromLinkId("work", Id.createLinkId("90"));
			work56.setMaximumDuration(1 * 1800);
			plan5.addActivity(work56);

			plan5.addLeg(factory.createLeg(TransportMode.car));

			Activity home53 = factory.createActivityFromLinkId("work", Id.createLinkId("95"));
			home53.setMaximumDuration(1 * 1800);
			plan5.addActivity(home53);

			plan5.addLeg(factory.createLeg(TransportMode.car));

			Activity work57 = factory.createActivityFromLinkId("work", Id.createLinkId("90"));
			work57.setMaximumDuration(1 * 1800);
			plan5.addActivity(work57);

			plan5.addLeg(factory.createLeg(TransportMode.car));

			Activity work58 = factory.createActivityFromLinkId("work", Id.createLinkId("95"));
			work58.setMaximumDuration(1 * 1800);
			plan5.addActivity(work58);

			plan5.addLeg(factory.createLeg(TransportMode.car));

			Activity work59 = factory.createActivityFromLinkId("work", Id.createLinkId("90"));
			work59.setMaximumDuration(1 * 1800);
			plan5.addActivity(work59);
			plan5.addLeg(factory.createLeg(TransportMode.car));

			Activity home54 = factory.createActivityFromLinkId("work", Id.createLinkId("95"));
			home54.setMaximumDuration(1 * 1800);
			plan5.addActivity(home54);

			plan5.addLeg(factory.createLeg(TransportMode.car));

			Activity work510 = factory.createActivityFromLinkId("work", Id.createLinkId("90"));
			work510.setMaximumDuration(1 * 1800);
			plan5.addActivity(work510);

			plan5.addLeg(factory.createLeg(TransportMode.car));

			Activity work511 = factory.createActivityFromLinkId("work", Id.createLinkId("95"));
			work511.setMaximumDuration(1 * 1800);
			plan5.addActivity(work511);

			plan5.addLeg(factory.createLeg(TransportMode.car));

			Activity work512 = factory.createActivityFromLinkId("work", Id.createLinkId("90"));
			work512.setMaximumDuration(1 * 1800);
			plan5.addActivity(work512);
			plan5.addLeg(factory.createLeg(TransportMode.car));

			Activity home55 = factory.createActivityFromLinkId("home", Id.createLinkId("95"));
			home55.setMaximumDuration(1 * 1800);
			plan5.addActivity(home55);

			person5.addPlan(plan5);
			person5.setSelectedPlan(plan5);

			scenario.getPopulation().addPerson(person5);
		}

		{

			Person person6 = factory.createPerson(Id.createPersonId("Double Charger"));

			Plan plan6 = factory.createPlan();

			Activity home61 = factory.createActivityFromLinkId("home", Id.createLinkId("2"));
			home61.setEndTime(6 * 3600);
			plan6.addActivity(home61);
			plan6.addLeg(factory.createLeg(TransportMode.car));

			Activity work61 = factory.createActivityFromLinkId("work", Id.createLinkId("179"));
			work61.setMaximumDuration(1200);
			plan6.addActivity(work61);

			plan6.addLeg(factory.createLeg(TransportMode.car));

			Activity work62 = factory.createActivityFromLinkId("work", Id.createLinkId("2"));
			work62.setMaximumDuration(1200);
			plan6.addActivity(work62);

			plan6.addLeg(factory.createLeg(TransportMode.car));

			Activity work63 = factory.createActivityFromLinkId("work", Id.createLinkId("179"));
			work63.setMaximumDuration(1200);
			plan6.addActivity(work63);

			plan6.addLeg(factory.createLeg(TransportMode.car));

			Activity work64 = factory.createActivityFromLinkId("work", Id.createLinkId("2"));
			work64.setEndTime(12 * 3600);
			plan6.addActivity(work64);

			plan6.addLeg(factory.createLeg(TransportMode.car));

			Activity work65 = factory.createActivityFromLinkId("work", Id.createLinkId("179"));
			work65.setMaximumDuration(1200);
			plan6.addActivity(work65);

			plan6.addLeg(factory.createLeg(TransportMode.car));

			Activity home62 = factory.createActivityFromLinkId("home", Id.createLinkId("2"));
			home62.setMaximumDuration(1200);
			plan6.addActivity(home62);

			person6.addPlan(plan6);
			person6.setSelectedPlan(plan6);
			scenario.getPopulation().addPerson(person6);
		}

		{

			Person person7 = factory.createPerson(Id.createPersonId("Not enough time so no charge"));

			Plan plan7 = factory.createPlan();

			Activity home71 = factory.createActivityFromLinkId("home", Id.createLinkId("1"));
			home71.setEndTime(6 * 3600);
			plan7.addActivity(home71);
			plan7.addLeg(factory.createLeg(TransportMode.car));

			Activity work71 = factory.createActivityFromLinkId("work", Id.createLinkId("90"));
			work71.setMaximumDuration(1140);
			plan7.addActivity(work71);

			plan7.addLeg(factory.createLeg(TransportMode.car));

			Activity work72 = factory.createActivityFromLinkId("work", Id.createLinkId("99"));
			work72.setMaximumDuration(1200);
			plan7.addActivity(work72);

			plan7.addLeg(factory.createLeg(TransportMode.car));

			Activity work73 = factory.createActivityFromLinkId("work", Id.createLinkId("92"));
			work73.setMaximumDuration(1140);
			plan7.addActivity(work73);

			plan7.addLeg(factory.createLeg(TransportMode.car));

			Activity work74 = factory.createActivityFromLinkId("work", Id.createLinkId("75"));
			work74.setMaximumDuration(1200);
			plan7.addActivity(work74);

			plan7.addLeg(factory.createLeg(TransportMode.car));

			Activity work75 = factory.createActivityFromLinkId("work", Id.createLinkId("179"));
			work75.setMaximumDuration(1200);
			plan7.addActivity(work75);

			plan7.addLeg(factory.createLeg(TransportMode.car));

			Activity home72 = factory.createActivityFromLinkId("home", Id.createLinkId("2"));
			home72.setMaximumDuration(1200);
			plan7.addActivity(home72);

			person7.addPlan(plan7);
			person7.setSelectedPlan(plan7);
			scenario.getPopulation().addPerson(person7);
		}

		{

			Person person7 = factory.createPerson(Id.createPersonId("Not enough time so charging early"));

			Plan plan7 = factory.createPlan();

			Activity home71 = factory.createActivityFromLinkId("home", Id.createLinkId("1"));
			home71.setEndTime(6 * 3600);
			plan7.addActivity(home71);
			plan7.addLeg(factory.createLeg(TransportMode.car));

			Activity work71 = factory.createActivityFromLinkId("work", Id.createLinkId("90"));
			work71.setMaximumDuration(3600);
			plan7.addActivity(work71);

			plan7.addLeg(factory.createLeg(TransportMode.car));

			Activity work72 = factory.createActivityFromLinkId("work", Id.createLinkId("99"));
			work72.setMaximumDuration(1140);
			plan7.addActivity(work72);

			plan7.addLeg(factory.createLeg(TransportMode.car));

			Activity work73 = factory.createActivityFromLinkId("work", Id.createLinkId("92"));
			work73.setMaximumDuration(2400);
			plan7.addActivity(work73);

			plan7.addLeg(factory.createLeg(TransportMode.car));

			Activity work74 = factory.createActivityFromLinkId("work", Id.createLinkId("75"));
			work74.setMaximumDuration(1200);
			plan7.addActivity(work74);

			plan7.addLeg(factory.createLeg(TransportMode.car));

			Activity work75 = factory.createActivityFromLinkId("work", Id.createLinkId("179"));
			work75.setMaximumDuration(1200);
			plan7.addActivity(work75);

			plan7.addLeg(factory.createLeg(TransportMode.car));

			Activity home72 = factory.createActivityFromLinkId("home", Id.createLinkId("2"));
			home72.setMaximumDuration(1200);
			plan7.addActivity(home72);

			person7.addPlan(plan7);
			person7.setSelectedPlan(plan7);
			scenario.getPopulation().addPerson(person7);
		}

		{
			Person person8 = factory.createPerson(Id.createPersonId("Home Charger"));
			Plan plan8 = factory.createPlan();

			Activity home8 = factory.createActivityFromLinkId("home", Id.createLinkId("95"));
			home8.setEndTime(6 * 3600);
			plan8.addActivity(home8);

			plan8.addLeg(factory.createLeg(TransportMode.car));

			Activity work8 = factory.createActivityFromLinkId("work", Id.createLinkId("24"));
			work8.setEndTime(8 * 3600);
			plan8.addActivity(work8);

			plan8.addLeg(factory.createLeg(TransportMode.car));

			Activity home81 = factory.createActivityFromLinkId("home", Id.createLinkId("95"));
			home81.setMaximumDuration(1200);
			plan8.addActivity(home81);

			plan8.addLeg(factory.createLeg(TransportMode.bike));

			Activity leisure81 = factory.createActivityFromLinkId("leisure", Id.createLinkId("11"));
			leisure81.setMaximumDuration(1200);
			plan8.addActivity(leisure81);
			plan8.addLeg(factory.createLeg(TransportMode.bike));

			Activity leisure82 = factory.createActivityFromLinkId("leisure", Id.createLinkId("95"));
			leisure82.setMaximumDuration(1200);
			plan8.addActivity(leisure82);

			person8.addPlan(plan8);
			person8.setSelectedPlan(plan8);
			scenario.getPopulation().addPerson(person8);
		}

		{
			Person personWithOpenSubtour = factory.createPerson(Id.createPersonId("No Round Trip So No Home Charge"));
			Plan plan8 = factory.createPlan();

			Activity home8 = factory.createActivityFromLinkId("home",
					Id.createLinkId("94")); //will return home to link 95 later (so no round trip, i.e. subtour, so no charge
			home8.setEndTime(12 * 3600);
			plan8.addActivity(home8);

			plan8.addLeg(factory.createLeg(TransportMode.car));

			Activity work8 = factory.createActivityFromLinkId("work", Id.createLinkId("24"));
			work8.setEndTime(14 * 3600);
			plan8.addActivity(work8);

			plan8.addLeg(factory.createLeg(TransportMode.car));

			Activity home81 = factory.createActivityFromLinkId("home", Id.createLinkId("95"));
			home81.setMaximumDuration(1200);
			plan8.addActivity(home81);

			plan8.addLeg(factory.createLeg(TransportMode.bike));

			Activity leisure81 = factory.createActivityFromLinkId("leisure", Id.createLinkId("11"));
			leisure81.setMaximumDuration(1200);
			plan8.addActivity(leisure81);
			plan8.addLeg(factory.createLeg(TransportMode.bike));

			Activity leisure82 = factory.createActivityFromLinkId("leisure", Id.createLinkId("95"));
			leisure82.setMaximumDuration(1200);
			plan8.addActivity(leisure82);

			personWithOpenSubtour.addPlan(plan8);
			personWithOpenSubtour.setSelectedPlan(plan8);
			scenario.getPopulation().addPerson(personWithOpenSubtour);
		}

		{

			Person person9 = factory.createPerson(Id.createPersonId("Double Charger Home Charger"));
			Plan plan9 = factory.createPlan();

			Activity home91 = factory.createActivityFromLinkId("home", Id.createLinkId("90"));
			home91.setEndTime(5 * 3600);
			plan9.addActivity(home91);

			plan9.addLeg(factory.createLeg(TransportMode.car));

			Activity work91 = factory.createActivityFromLinkId("work", Id.createLinkId("1"));
			work91.setMaximumDuration(1 * 1200);
			plan9.addActivity(work91);

			plan9.addLeg(factory.createLeg(TransportMode.car));

			Activity work92 = factory.createActivityFromLinkId("work", Id.createLinkId("90"));
			work92.setMaximumDuration(1 * 1200);
			plan9.addActivity(work92);

			plan9.addLeg(factory.createLeg(TransportMode.car));

			Activity work93 = factory.createActivityFromLinkId("work", Id.createLinkId("80"));
			work93.setMaximumDuration(1 * 1200);
			plan9.addActivity(work93);

			plan9.addLeg(factory.createLeg(TransportMode.car));

			Activity work94 = factory.createActivityFromLinkId("work", Id.createLinkId("1"));
			work94.setMaximumDuration(1 * 1200);
			plan9.addActivity(work94);

			plan9.addLeg(factory.createLeg(TransportMode.car));

			Activity work95 = factory.createActivityFromLinkId("work", Id.createLinkId("80"));
			work95.setMaximumDuration(1 * 1200);
			plan9.addActivity(work95);

			plan9.addLeg(factory.createLeg(TransportMode.car));

			Activity work96 = factory.createActivityFromLinkId("work", Id.createLinkId("1"));
			work96.setEndTime(17 * 3600);
			plan9.addActivity(work96);

			plan9.addLeg(factory.createLeg(TransportMode.car));

			Activity home92 = factory.createActivityFromLinkId("home", Id.createLinkId("90"));
			home92.setEndTime(18 * 3600);
			plan9.addActivity(home92);

			plan9.addLeg(factory.createLeg(TransportMode.bike));

			Activity leisure91 = factory.createActivityFromLinkId("leisure", Id.createLinkId("5"));
			leisure91.setMaximumDuration(1200);
			plan9.addActivity(leisure91);

			plan9.addLeg(factory.createLeg(TransportMode.bike));

			Activity leisure92 = factory.createActivityFromLinkId("leisure", Id.createLinkId("91"));
			leisure92.setMaximumDuration(1200);
			plan9.addActivity(leisure92);

			person9.addPlan(plan9);
			person9.setSelectedPlan(plan9);
			scenario.getPopulation().addPerson(person9);
		}
	}

	//	-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

	/**
	 * tests (every iteration) <br>
	 * * location consistency between plugin and plugout <br>
	 * * nrOfPlugins == nrOfPlugouts for each person <br>
	 * * nr of all charging persons <br>
	 * * consistency of chargers between charging start and charging end <br>
	 */
	private static class UrbanEVTestHandler
			implements ActivityStartEventHandler, ActivityEndEventHandler, ChargingStartEventHandler, ChargingEndEventHandler {

		private Map<Id<Person>, List<ActivityEndEvent>> plugOutCntPerPerson = new HashMap<>();
		private Map<Id<Person>, List<ActivityStartEvent>> plugInCntPerPerson = new HashMap<>();
		private Map<Id<Person>, List<ActivityStartEvent>> normalActStarts = new HashMap<>();
		private Map<Id<Vehicle>, List<ChargingStartEvent>> chargingStarts = new HashMap<>();
		private Map<Id<Vehicle>, List<ChargingEndEvent>> chargingEnds = new HashMap<>();

		@Override
		public void handleEvent(ActivityStartEvent event) {
			if (event.getActType().contains( UrbanEVModule.PLUGIN_INTERACTION )) {
				compute(plugInCntPerPerson, event);
			} else if (!TripStructureUtils.isStageActivityType(event.getActType())) {
				compute(normalActStarts, event);
			}
		}

		@Override
		public void handleEvent(ActivityEndEvent event) {
			if (event.getActType().contains( UrbanEVModule.PLUGOUT_INTERACTION )) {
				plugOutCntPerPerson.compute(event.getPersonId(), (person, list) -> {
					if (list == null)
						list = new ArrayList<>();
					list.add(event);
					return list;
				});

				ActivityStartEvent correspondingPlugin = this.plugInCntPerPerson.get(event.getPersonId())
						.get(this.plugInCntPerPerson.get(event.getPersonId()).size() - 1);
				Assertions.assertEquals(correspondingPlugin.getLinkId(),
						event.getLinkId(),
						"plugin and plugout location seem not to match. event=" + event);
			}
		}

		@Override
		public void reset(int iteration) {
			if (iteration > 0) {
				System.out.println("ITERATION = " + iteration);

				//TODO move the following assert statements out of the simulation loop? Or do we want to explicitly check these _every_ iteration?

				//				Assert.assertEquals("there should be 9 people plugging in in this test", 9, plugInCntPerPerson.size(), 0);
				//				Assert.assertEquals("there should be 8 people plugging out this test", 9, plugOutCntPerPerson.size(), 0);
				//	The number of plug in and outs is not equal anymore since we added homecharging
				//				Assert.assertEquals( plugInCntPerPerson.size(), plugOutCntPerPerson.size()); //not necessary

			}

			this.plugInCntPerPerson.clear();
			this.plugOutCntPerPerson.clear();
			this.normalActStarts.clear();
		}

		@Override
		public void handleEvent(ChargingEndEvent event) {
			this.chargingEnds.compute(event.getVehicleId(), (person, list) -> {
				if (list == null)
					list = new ArrayList<>();
				list.add(event);
				return list;
			});

			ChargingStartEvent correspondingStart = this.chargingStarts.get(event.getVehicleId())
					.get(this.chargingStarts.get(event.getVehicleId()).size() - 1);
			Assertions.assertEquals(correspondingStart.getChargerId(), event.getChargerId(), "chargingEnd and chargingStart do not seem not to take place at the same charger. event=" + event);
		}

		@Override
		public void handleEvent(ChargingStartEvent event) {
			this.chargingStarts.compute(event.getVehicleId(), (person, list) -> {
				if (list == null)
					list = new ArrayList<>();
				list.add(event);
				return list;
			});
		}
	}

	private static void compute(Map<Id<Person>, List<ActivityStartEvent>> map, ActivityStartEvent event) {
		map.compute(event.getPersonId(), (person, list) -> {
			if (list == null)
				list = new ArrayList<>();
			list.add(event);
			return list;
		});
	}

}
