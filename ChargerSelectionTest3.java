package org.matsim.urbanEV;

import gnu.trove.map.hash.THashMap;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.ev.EvConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.run.ev.RunUrbanEVExample;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.*;

import java.util.Arrays;
import java.util.HashMap;

public class ChargerSelectionTest3 {

	@Rule
	public MatsimTestUtils matsimTestUtils = new MatsimTestUtils();

	@Test
	public void testUrbanEVExample3() {
		//config. vehicle source = modeVehicleTypeFromData ??
		EvConfigGroup evConfigGroup = new EvConfigGroup();
		evConfigGroup.setVehiclesFile("this is not important because we use standard matsim vehicles");
		evConfigGroup.setTimeProfiles(true);
		evConfigGroup.setChargersFile("C:/Users/admin/Desktop/chargers.xml");
		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("chessboard"), "config.xml"),
				evConfigGroup);
		config.network().setInputFile("C:/Users/admin/IdeaProjects/matsim-berlin/test/input/1%network.xml");

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

		overridePopulation3(scenario);

		//manually insert car vehicle type with attributes (hbefa technology, initial energy etc....)
		VehiclesFactory vehiclesFactory = scenario.getVehicles().getFactory();

		VehicleType carVehicleType = vehiclesFactory.createVehicleType(Id.create(TransportMode.car, VehicleType.class));
		VehicleUtils.setHbefaTechnology(carVehicleType.getEngineInformation(), "electricity");
		VehicleUtils.setEnergyCapacity(carVehicleType.getEngineInformation(), 10);
		EVUtils.setInitialEnergy(carVehicleType.getEngineInformation(), 4);
		EVUtils.setChargerTypes(carVehicleType.getEngineInformation(), Arrays.asList("a", "b", "default"));

		VehicleType bikeVehicleType = vehiclesFactory.createVehicleType(Id.create(TransportMode.bike, VehicleType.class));
		VehicleUtils.setHbefaTechnology(carVehicleType.getEngineInformation(), "electricity");
		VehicleUtils.setEnergyCapacity(carVehicleType.getEngineInformation(), 10);
		EVUtils.setInitialEnergy(carVehicleType.getEngineInformation(), 4);
		EVUtils.setChargerTypes(carVehicleType.getEngineInformation(), Arrays.asList("a", "b", "default"));




		Vehicle vehicle = vehiclesFactory.createVehicle(Id.createVehicleId("Bike"), bikeVehicleType);
		scenario.getVehicles().addVehicle(vehicle);


		scenario.getVehicles().addVehicleType(carVehicleType);
		scenario.getVehicles().addVehicleType(bikeVehicleType);


		///controler with Urban EV module
		Controler controler = RunUrbanEVExample.prepareControler(scenario);
		controler.run();
	}

	private void overridePopulation3(Scenario scenario) {



		//delete all persons that are there already
		scenario.getPopulation().getPersons().clear();

		PopulationFactory factory = scenario.getPopulation().getFactory();
		Person person = factory.createPerson(Id.createPersonId("Jonas' kleiner GeheimAgent"));


		HashMap<String, Id<Vehicle>> modetoVehicle = new HashMap<String, Id<Vehicle>>();
		//VehicleUtils.createVehicle(Id.create(<"bike">, VehicleType.class));

	//	Vehicles vehicles = VehicleUtils.createVehiclesContainer();
	//	modetoVehicle.put(TransportMode.bike, vehicles.getvehicleId());
		//VehicleUtils.insertVehicleIdsIntoAttributes(person, modetoVehicle);
		Plan plan = factory.createPlan();


		Activity home = factory.createActivityFromLinkId("home", Id.createLinkId("95"));
		home.setEndTime(8 * 3600);
		plan.addActivity(home);
		plan.addLeg(factory.createLeg(TransportMode.car));

		Activity work = factory.createActivityFromLinkId("work", Id.createLinkId("176"));
		work.setEndTime(10 * 3600);
		plan.addActivity(work);

		plan.addLeg(factory.createLeg(TransportMode.car));

		Activity leisure = factory.createActivityFromLinkId("leisure", Id.createLinkId("90"));
		leisure.setEndTime(11 * 3600);
		plan.addActivity(leisure);
		plan.addLeg(factory.createLeg(TransportMode.bike));

		Activity home2 = factory.createActivityFromLinkId("home", Id.createLinkId("95"));
		home2.setEndTime(14 * 3600);
		plan.addActivity(home2);


		person.addPlan(plan);
		scenario.getPopulation().addPerson(person);

		Person person2 = factory.createPerson(Id.createPersonId("Jonas' kleiner GeheimAgent2"));

		Plan plan2 = factory.createPlan();

		Activity home3 = factory.createActivityFromLinkId("home", Id.createLinkId("91"));
		home3.setEndTime(8 * 3600);
		plan2.addActivity(home3);
		plan2.addLeg(factory.createLeg(TransportMode.car));

		Activity work2 = factory.createActivityFromLinkId("work", Id.createLinkId("175"));
		work2.setEndTime(10 * 3600);
		plan2.addActivity(work2);

		plan2.addLeg(factory.createLeg(TransportMode.car));

		Activity home4 = factory.createActivityFromLinkId("home", Id.createLinkId("91"));
		home4.setEndTime(14 * 3600);
		plan2.addActivity(home4);
		person2.addPlan(plan2);

		scenario.getPopulation().addPerson(person2);

		//HashMap<String, Id<Vehicle>> modetoVehicle = new HashMap<String, Id<Vehicle>>();
		//for (Person p : scenario.getPopulation().getPersons().values()) {

		//	modetoVehicle = new HashMap<>();
			//HashMap<String, Id<Vehicle>> modetoVehicle = new HashMap<>();
		//	modetoVehicle.put(TransportMode.bike, Id.createVehicleId(TransportMode.bike + person.getId().toString()));
		//	VehicleUtils.insertVehicleIdsIntoAttributes(person, modetoVehicle);
		//}




	}
}