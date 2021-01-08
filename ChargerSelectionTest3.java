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

public class ChargerSelectionTest3 {

	Logger logger = Logger.getLogger(ChargerSelectionTest3.class);

	@Rule
	public MatsimTestUtils matsimTestUtils = new MatsimTestUtils();

	@Test
	public void testUrbanEVExample3() {
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

		overridePopulation3(scenario);

		Map<String, VehicleType> mode2VehicleType = new HashMap<>();
		mode2VehicleType.put(TransportMode.car, carVehicleType);
		mode2VehicleType.put(TransportMode.bike, bikeVehicleType);
		createAndRegisterVehicles(scenario, mode2VehicleType);

		///controler with Urban EV module
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

	private void overridePopulation3(Scenario scenario) {

		//delete all persons that are there already
		scenario.getPopulation().getPersons().clear();

		PopulationFactory factory = scenario.getPopulation().getFactory();
		Person person = factory.createPerson(Id.createPersonId("Charge during leisure + bike"));

		Plan plan = factory.createPlan();

		Activity home = factory.createActivityFromLinkId("home", Id.createLinkId("95"));
		home.setEndTime(8 * 3600);
		plan.addActivity(home);
		plan.addLeg(factory.createLeg(TransportMode.car));

		Activity work = factory.createActivityFromLinkId("work", Id.createLinkId("24"));
		work.setEndTime(10 * 3600);
		plan.addActivity(work);
		plan.addLeg(factory.createLeg(TransportMode.car));

		Activity work2 = factory.createActivityFromLinkId("work", Id.createLinkId("172"));
		work2.setEndTime(11 * 3600);
		plan.addActivity(work2);

		plan.addLeg(factory.createLeg(TransportMode.car));

		Activity leisure = factory.createActivityFromLinkId("leisure", Id.createLinkId("90"));
		leisure.setEndTime(12 * 3600);
		plan.addActivity(leisure);
		plan.addLeg(factory.createLeg(TransportMode.bike));

		Activity leisure2 = factory.createActivityFromLinkId("leisure", Id.createLinkId("89"));
		leisure2.setEndTime(13 * 3600);
		plan.addActivity(leisure2);
		plan.addLeg(factory.createLeg(TransportMode.bike));

		Activity leisure3 = factory.createActivityFromLinkId("leisure", Id.createLinkId("90"));
		leisure3.setEndTime(14 * 3600);
		plan.addActivity(leisure3);
		plan.addLeg(factory.createLeg(TransportMode.car));


		Activity home2 = factory.createActivityFromLinkId("home", Id.createLinkId("95"));
		home2.setEndTime(15 * 3600);
		plan.addActivity(home2);
		person.addPlan(plan);
		person.setSelectedPlan(plan);

		scenario.getPopulation().addPerson(person);

		Person person2 = factory.createPerson(Id.createPersonId("Charger Selection + 20 min shopping"));

		Plan plan2 = factory.createPlan();

		Activity home3 = factory.createActivityFromLinkId("home", Id.createLinkId("91"));
		home3.setEndTime(8 * 3600);
		plan2.addActivity(home3);
		plan2.addLeg(factory.createLeg(TransportMode.car));

		Activity work3 = factory.createActivityFromLinkId("work", Id.createLinkId("175"));
		work3.setEndTime(10 * 3600);
		plan2.addActivity(work3);

		plan2.addLeg(factory.createLeg(TransportMode.car));

		Activity work32 = factory.createActivityFromLinkId("work", Id.createLinkId("60"));
		work32.setEndTime(12 * 3600);
		plan2.addActivity(work32);

		plan2.addLeg(factory.createLeg(TransportMode.car));

		Activity shopping = factory.createActivityFromLinkId("shopping", Id.createLinkId("9"));
		shopping.setStartTime(12 * 3600 +2100);
		shopping.setEndTime(12 * 3600 + 3300);
		plan2.addActivity(shopping);

		plan2.addLeg(factory.createLeg(TransportMode.car));



		Activity home4 = factory.createActivityFromLinkId("home", Id.createLinkId("91"));
		home4.setEndTime(15 * 3600);
		plan2.addActivity(home4);
		person2.addPlan(plan2);
		person2.setSelectedPlan(plan2);

		scenario.getPopulation().addPerson(person2);

		Person person3 = factory.createPerson(Id.createPersonId("Charger Selection long distance leg"));

		Plan plan3 = factory.createPlan();

		Activity home5 = factory.createActivityFromLinkId("home", Id.createLinkId("1"));
		home5.setEndTime(8 * 3600);
		plan3.addActivity(home5);
		plan3.addLeg(factory.createLeg(TransportMode.car));

		Activity work4 = factory.createActivityFromLinkId("work", Id.createLinkId("90"));
		work4.setEndTime(10 * 3600);
		plan3.addActivity(work4);

		plan3.addLeg(factory.createLeg(TransportMode.car));

		Activity work42 = factory.createActivityFromLinkId("work", Id.createLinkId("99"));
		work42.setEndTime(12 * 3600);
		plan3.addActivity(work42);

		plan3.addLeg(factory.createLeg(TransportMode.car));

		Activity home6 = factory.createActivityFromLinkId("home", Id.createLinkId("1"));
		home6.setEndTime(15 * 3600);
		plan3.addActivity(home6);
		person3.addPlan(plan3);
		person3.setSelectedPlan(plan3);

		scenario.getPopulation().addPerson(person3);

		Person person4 = factory.createPerson(Id.createPersonId("Charger Selection + 15 min shopping"));

		Plan plan4 = factory.createPlan();

		Activity home7 = factory.createActivityFromLinkId("home", Id.createLinkId("3"));
		home7.setEndTime(8 * 3600);
		plan4.addActivity(home7);
		plan4.addLeg(factory.createLeg(TransportMode.car));

		Activity work5 = factory.createActivityFromLinkId("work", Id.createLinkId("90"));
		work5.setEndTime(10 * 3600);
		plan4.addActivity(work5);

		plan4.addLeg(factory.createLeg(TransportMode.bike));

		Activity shopping2 = factory.createActivityFromLinkId("shopping", Id.createLinkId("87"));
		shopping.setStartTime(10 * 3600 + 1100);
		shopping2.setEndTime(10 * 3600 + 2000);
		plan4.addActivity(shopping2);

		plan4.addLeg(factory.createLeg(TransportMode.bike));

		Activity work6 = factory.createActivityFromLinkId("work", Id.createLinkId("90"));
		work6.setEndTime(12 * 3600);
		plan4.addActivity(work6);

		plan4.addLeg(factory.createLeg(TransportMode.car));

		Activity work7= factory.createActivityFromLinkId("work", Id.createLinkId("91"));
		work7.setEndTime(14 * 3600);
		plan4.addActivity(work7);

		plan4.addLeg(factory.createLeg(TransportMode.car));


		Activity home8 = factory.createActivityFromLinkId("home", Id.createLinkId("3"));
		home8.setEndTime(15 * 3600);
		plan4.addActivity(home8);
		person4.addPlan(plan4);
		person4.setSelectedPlan(plan4);

		scenario.getPopulation().addPerson(person4);

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


				Assert.assertEquals(4, plugInCntPerPerson.size(), 0);
				Assert.assertEquals(4, plugOutCntPerPerson.size(), 0);
				Assert.assertEquals( plugInCntPerPerson.size(), plugOutCntPerPerson.size());
			}

			this.plugInCntPerPerson.clear();
			this.plugOutCntPerPerson.clear();
		}
	}



}