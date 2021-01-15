package org.matsim.urbanEV;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
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
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.run.ev.RunUrbanEVExample;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.*;

import java.util.*;

public class ChargerSelectionTest {

	Logger logger = Logger.getLogger(ChargerSelectionTest.class);

	@Rule
	public MatsimTestUtils matsimTestUtils = new MatsimTestUtils();

	@Test
	public void testUrbanEVExample() {
		//config. vehicle source = modeVehicleTypeFromData ??


		EvConfigGroup evConfigGroup = new EvConfigGroup();
		evConfigGroup.setVehiclesFile("this is not important because we use standard matsim vehicles");
		evConfigGroup.setTimeProfiles(true);
		evConfigGroup.setChargersFile("chargers.xml");
//		evConfigGroup.setChargersFile("chessboard-chargers-1-plugs-1.xml");
		Config config = ConfigUtils.loadConfig("test/input/chessboard/chessboard-config.xml", evConfigGroup);
		config.network().setInputFile("1pctNetwork.xml");

		//prepare config
		RunUrbanEVExample.prepareConfig(config);
		config.controler().setOutputDirectory(matsimTestUtils.getOutputDirectory());
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		config.controler().setLastIteration(1);

		config.planCalcScore().addModeParams(new PlanCalcScoreConfigGroup.ModeParams(TransportMode.bike));

		//set VehicleSource
		config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);

		//load scenario
		Scenario scenario = ScenarioUtils.loadScenario(config);


		//manually insert car vehicle type with attributes (hbefa technology, initial energy etc....)
		VehiclesFactory vehiclesFactory = scenario.getVehicles().getFactory();

		VehicleType carVehicleType = vehiclesFactory.createVehicleType(Id.create(TransportMode.car, VehicleType.class));
		VehicleUtils.setHbefaTechnology(carVehicleType.getEngineInformation(), "electricity");
		VehicleUtils.setEnergyCapacity(carVehicleType.getEngineInformation(), 10);
		EVUtils.setInitialEnergy(carVehicleType.getEngineInformation(), 5);
		EVUtils.setChargerTypes(carVehicleType.getEngineInformation(), Arrays.asList("a", "b", "default"));

		VehicleType bikeVehicleType = vehiclesFactory.createVehicleType(Id.create(TransportMode.bike, VehicleType.class));
		VehicleUtils.setHbefaTechnology(bikeVehicleType.getEngineInformation(), "electricity");
		VehicleUtils.setEnergyCapacity(bikeVehicleType.getEngineInformation(), 10);
		EVUtils.setInitialEnergy(bikeVehicleType.getEngineInformation(), 4);
		EVUtils.setChargerTypes(bikeVehicleType.getEngineInformation(), Arrays.asList("a", "b", "default"));

		scenario.getVehicles().addVehicleType(carVehicleType);
		scenario.getVehicles().addVehicleType(bikeVehicleType);

		overridePopulation(scenario);

		Map<String, VehicleType> mode2VehicleType = new HashMap<>();
		mode2VehicleType.put(TransportMode.car, carVehicleType);
		mode2VehicleType.put(TransportMode.bike, bikeVehicleType);
		createAndRegisterVehicles(scenario, mode2VehicleType);

		//controler with Urban EV module
		Controler controler = RunUrbanEVExample.prepareControler(scenario);

		UrbanEVTestHandler handler = new UrbanEVTestHandler();

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
//				this.addEventHandlerBinding().toInstance(handler);
			}
		});

		controler.run();

	}

	private void overridePopulation(Scenario scenario) {

		//delete all persons that are there already
		scenario.getPopulation().getPersons().clear();

		PopulationFactory factory = scenario.getPopulation().getFactory();
		Person person = factory.createPerson(Id.createPersonId("Charge during leisure + bike"));

		Plan plan = factory.createPlan();

		Activity home1 = factory.createActivityFromLinkId("home", Id.createLinkId("95"));
		home1.setEndTime(8 * 3600);
		plan.addActivity(home1);

		plan.addLeg(factory.createLeg(TransportMode.car));

		Activity work1 = factory.createActivityFromLinkId("work", Id.createLinkId("24"));
		work1.setEndTime(10 * 3600);
		plan.addActivity(work1);

		plan.addLeg(factory.createLeg(TransportMode.car));

		Activity work12 = factory.createActivityFromLinkId("work", Id.createLinkId("172"));
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


		Activity home12 = factory.createActivityFromLinkId("home", Id.createLinkId("95"));
		home12.setEndTime(15 * 3600);
		plan.addActivity(home12);
		person.addPlan(plan);
		person.setSelectedPlan(plan);

		scenario.getPopulation().addPerson(person);

	/*	Person person2 = factory.createPerson(Id.createPersonId("Charger Selection + 20 min shopping"));

		Plan plan2 = factory.createPlan();

		Activity home21 = factory.createActivityFromLinkId("home", Id.createLinkId("91"));
		home21.setEndTime(8 * 3600);
		plan2.addActivity(home21);
		plan2.addLeg(factory.createLeg(TransportMode.car));

		Activity work21 = factory.createActivityFromLinkId("work", Id.createLinkId("175"));
		work21.setEndTime(10 * 3600);
		plan2.addActivity(work21);

		plan2.addLeg(factory.createLeg(TransportMode.car));

		Activity work22 = factory.createActivityFromLinkId("work", Id.createLinkId("60"));
		work22.setEndTime(12 * 3600);
		plan2.addActivity(work22);

		plan2.addLeg(factory.createLeg(TransportMode.car));

		Activity shopping21 = factory.createActivityFromLinkId("shopping", Id.createLinkId("9"));
		shopping21.setStartTime(12 * 3600 +2100);
		shopping21.setEndTime(12 * 3600 + 3300);
		plan2.addActivity(shopping21);

		plan2.addLeg(factory.createLeg(TransportMode.car));



		Activity home22 = factory.createActivityFromLinkId("home", Id.createLinkId("91"));
		home22.setEndTime(15 * 3600);
		plan2.addActivity(home22);
		person2.addPlan(plan2);
		person2.setSelectedPlan(plan2);

		scenario.getPopulation().addPerson(person2);

		Person person3 = factory.createPerson(Id.createPersonId("Charger Selection long distance leg"));

		Plan plan3 = factory.createPlan();

		Activity home31 = factory.createActivityFromLinkId("home", Id.createLinkId("1"));
		home31.setEndTime(8 * 3600);
		plan3.addActivity(home31);
		plan3.addLeg(factory.createLeg(TransportMode.car));

		Activity work31 = factory.createActivityFromLinkId("work", Id.createLinkId("90"));
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

		Person person4 = factory.createPerson(Id.createPersonId("Charger 15 min shopping"));

		Plan plan4 = factory.createPlan();

		Activity home41 = factory.createActivityFromLinkId("home", Id.createLinkId("3"));
		home41.setEndTime(8 * 3600);
		plan4.addActivity(home41);
		plan4.addLeg(factory.createLeg(TransportMode.car));

		Activity work41 = factory.createActivityFromLinkId("work", Id.createLinkId("90"));
		work41.setEndTime(10 * 3600);
		plan4.addActivity(work41);

		plan4.addLeg(factory.createLeg(TransportMode.bike));

		Activity shopping41 = factory.createActivityFromLinkId("shopping", Id.createLinkId("87"));
		shopping41.setStartTime(10 * 3600 + 1100);
		shopping41.setEndTime(10 * 3600 + 2000);
		plan4.addActivity(shopping41);

		plan4.addLeg(factory.createLeg(TransportMode.bike));

		Activity work42 = factory.createActivityFromLinkId("work", Id.createLinkId("90"));
		work42.setEndTime(12 * 3600);
		plan4.addActivity(work42);

		plan4.addLeg(factory.createLeg(TransportMode.car));

		Activity work43= factory.createActivityFromLinkId("work", Id.createLinkId("91"));
		work43.setEndTime(14 * 3600);
		plan4.addActivity(work43);

		plan4.addLeg(factory.createLeg(TransportMode.car));


		Activity home42 = factory.createActivityFromLinkId("home", Id.createLinkId("3"));
		home42.setEndTime(15 * 3600);
		plan4.addActivity(home42);
		person4.addPlan(plan4);
		person4.setSelectedPlan(plan4);

		scenario.getPopulation().addPerson(person4);

		Person person5 = factory.createPerson(Id.createPersonId("Triple Charger"));

		Plan plan5 = factory.createPlan();

		Activity home51 = factory.createActivityFromLinkId("home", Id.createLinkId("1"));
		home51.setEndTime(1 * 3600);
		plan5.addActivity(home51);

		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity work51 = factory.createActivityFromLinkId("work", Id.createLinkId("9"));
		work51.setEndTime(2 * 3600);
		plan5.addActivity(work51);

		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity work52 = factory.createActivityFromLinkId("work", Id.createLinkId("90"));
		work52.setEndTime(3 * 3600);
		plan5.addActivity(work52);

		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity work53 = factory.createActivityFromLinkId("home", Id.createLinkId("99"));
		work53.setEndTime(4 * 3600);
		plan5.addActivity(work53);

		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity home52 = factory.createActivityFromLinkId("work", Id.createLinkId("1"));
		home52.setEndTime(5 * 3600);
		plan5.addActivity(home52);

		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity work54 = factory.createActivityFromLinkId("work", Id.createLinkId("9"));
		work54.setEndTime(6 * 3600);
		plan5.addActivity(work54);

		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity work55 = factory.createActivityFromLinkId("work", Id.createLinkId("90"));
		work55.setEndTime(7 * 3600);
		plan5.addActivity(work55);

		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity work56 = factory.createActivityFromLinkId("home", Id.createLinkId("99"));
		work56.setEndTime(8 * 3600);
		plan5.addActivity(work56);

		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity home53 = factory.createActivityFromLinkId("work", Id.createLinkId("1"));
		home53.setEndTime(9 * 3600);
		plan5.addActivity(home53);

		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity work57 = factory.createActivityFromLinkId("work", Id.createLinkId("9"));
		work57.setEndTime(10 * 3600);
		plan5.addActivity(work57);

		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity work58 = factory.createActivityFromLinkId("work", Id.createLinkId("90"));
		work58.setEndTime(11 * 3600);
		plan5.addActivity(work58);

		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity work59 = factory.createActivityFromLinkId("home", Id.createLinkId("99"));
		work59.setEndTime(12 * 3600);
		plan5.addActivity(work59);
		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity home54 = factory.createActivityFromLinkId("work", Id.createLinkId("1"));
		home54.setEndTime(13 * 3600);
		plan5.addActivity(home54);

		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity work510 = factory.createActivityFromLinkId("work", Id.createLinkId("9"));
		work510.setEndTime(14 * 3600);
		plan5.addActivity(work510);

		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity work511 = factory.createActivityFromLinkId("work", Id.createLinkId("90"));
		work511.setEndTime(15 * 3600);
		plan5.addActivity(work511);

		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity work512 = factory.createActivityFromLinkId("home", Id.createLinkId("99"));
		work512.setEndTime(16 * 3600);
		plan5.addActivity(work512);
		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity home55 = factory.createActivityFromLinkId("work", Id.createLinkId("1"));
		home55.setEndTime(17 * 3600);
		plan5.addActivity(home55);




		person5.addPlan(plan5);
		person5.setSelectedPlan(plan5);

		scenario.getPopulation().addPerson(person5);
*/


	}

	private void createAndRegisterVehicles(Scenario scenario, Map<String, VehicleType> mode2VehicleType){
		VehiclesFactory vFactory = scenario.getVehicles().getFactory();
		for(Person person : scenario.getPopulation().getPersons().values()) {
			Map<String,Id<Vehicle>> mode2VehicleId = new HashMap<>();
			for (String mode : mode2VehicleType.keySet()) {
				Id<Vehicle> vehicleId = VehicleUtils.createVehicleId(person, mode);
				Vehicle vehicle = vFactory.createVehicle(vehicleId, mode2VehicleType.get(mode));
				scenario.getVehicles().addVehicle(vehicle);
				mode2VehicleId.put(mode, vehicleId);
			}
			VehicleUtils.insertVehicleIdsIntoAttributes(person, mode2VehicleId);//probably unnecessary
		}
	}

	private class UrbanEVTestHandler implements ActivityStartEventHandler, ActivityEndEventHandler {

		private Map<Id<Person>, Double> plugOutCntPerPerson = new HashMap<>();
		private Map<Id<Person>, Double> plugInCntPerPerson = new HashMap<>();
		private Map<Id<Person>, Double> BikeCntPerPerson = new HashMap<>();


		@Override
		public void handleEvent(ActivityEndEvent event) {
			if( event.getActType().contains(UrbanVehicleChargingHandler.PLUGOUT_INTERACTION) ){
				this.plugOutCntPerPerson.compute(event.getPersonId(), (person,count) -> count == null ? 1 : count + 1);
			}
		}

		@Override
		public void handleEvent(ActivityStartEvent event) {
			if( event.getActType().contains(UrbanVehicleChargingHandler.PLUGIN_INTERACTION) ){
				this.plugInCntPerPerson.compute(event.getPersonId(), (person,count) -> count == null ? 1 : count + 1);

			}
		}


		@Override
		public void reset(int iteration) {
			if(iteration > 0){
				System.out.println("ITERATION = " + iteration);

				Assert.assertTrue(plugInCntPerPerson.containsKey(Id.createPersonId("Charger Selection long distance leg")));
				Assert.assertTrue(plugOutCntPerPerson.containsKey(Id.createPersonId("Charger Selection long distance leg")));

				for (Id<Person> personId : plugInCntPerPerson.keySet()) {
					Assert.assertTrue(plugInCntPerPerson.get(personId) == 1);
					Assert.assertTrue(plugInCntPerPerson.get(personId) == plugOutCntPerPerson.get(personId));

				}

//				plugIns.forEach(person -> {
//					if(Collections.frequency(plugIns, person) != Collections.frequency(plugOuts, person)){
//						logger.fatal(" in iteration " + (iteration -1)  + ", person " + person + " starts loading " + Collections.frequency(plugIns, person) + " times and ends loading " + Collections.frequency(plugOuts, person) + "times");
//						throw new RuntimeException(" in iteration " + (iteration -1) + ", person " + person + " starts loading " + Collections.frequency(plugIns, person) + " times and ends loading " + Collections.frequency(plugOuts, person) + "times");
//					}
//				});


				Assert.assertEquals(6, plugInCntPerPerson.size(), 0);
				Assert.assertEquals(6, plugOutCntPerPerson.size(), 0);
				Assert.assertEquals(4, BikeCntPerPerson.size(), 0);
				Assert.assertEquals( plugInCntPerPerson.size(), plugOutCntPerPerson.size());

			}

			this.plugInCntPerPerson.clear();
			this.plugOutCntPerPerson.clear();
		}
	}



}