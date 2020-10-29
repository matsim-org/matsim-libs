package org.matsim.urbanEV;

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
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.run.ev.RunUrbanEVExample;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehiclesFactory;

import java.util.Arrays;

public class ChargerSelectionTest {

	@Rule
	public MatsimTestUtils matsimTestUtils = new MatsimTestUtils();

	@Test
	public void testUrbanEVExample(){
		//config. vehicle source = modeVehicleTypeFromData ??
		EvConfigGroup evConfigGroup = new EvConfigGroup();
		evConfigGroup.setVehiclesFile("this is not important because we use standard matsim vehicles");
		evConfigGroup.setChargersFile("chessboard-chargers-1-plugs-1.xml");
		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("chessboard"), "config.xml"),
				evConfigGroup);

		//prepare config
		RunUrbanEVExample.prepareConfig(config);
		config.controler().setOutputDirectory(matsimTestUtils.getOutputDirectory());
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		config.controler().setLastIteration(1);

		//set VehicleSource
		config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);

		//load scenario
		Scenario scenario = ScenarioUtils.loadScenario(config);

		overridePopulation(scenario);

		//manually insert car vehicle type with attributes (hbefa technology, initial energy etc....)
		VehiclesFactory vehiclesFactory = scenario.getVehicles().getFactory();

		VehicleType carVehicleType = vehiclesFactory.createVehicleType(Id.create(TransportMode.car, VehicleType.class));
		VehicleUtils.setHbefaTechnology(carVehicleType.getEngineInformation(), "electricity");
		VehicleUtils.setEnergyCapacity(carVehicleType.getEngineInformation(), 10);
		EVUtils.setInitialEnergy(carVehicleType.getEngineInformation(), 5);
		EVUtils.setChargerTypes(carVehicleType.getEngineInformation(), Arrays.asList("a", "b", "default"));

//		Vehicle vehicle = vehiclesFactory.createVehicle(Id.createVehicleId("JonasGolf"), carVehicleType);
//		scenario.getVehicles().addVehicle(vehicle);

		scenario.getVehicles().addVehicleType(carVehicleType);

		///controler with Urban EV module
		Controler controler = RunUrbanEVExample.prepareControler(scenario);
		controler.run();
	}

	private void overridePopulation(Scenario scenario){

		//delete all persons that are there already
		scenario.getPopulation().getPersons().clear();

		PopulationFactory factory = scenario.getPopulation().getFactory();
		Person person = factory.createPerson(Id.createPersonId("Jonas' kleiner GeheimAgent"));

		Plan plan = factory.createPlan();

		Activity home1 = factory.createActivityFromLinkId("home", Id.createLinkId("95"));
		home1.setEndTime(8*3600);
		plan.addActivity(home1);
		plan.addLeg(factory.createLeg(TransportMode.car));

		Activity work = factory.createActivityFromLinkId("work", Id.createLinkId("176"));
		work.setEndTime(10*3600);
		plan.addActivity(work);

		plan.addLeg(factory.createLeg(TransportMode.car));

		Activity home2 = factory.createActivityFromLinkId("home", Id.createLinkId("95"));
//		home1.setEndTime(14*3600);
		plan.addActivity(home2);

		person.addPlan(plan);
		scenario.getPopulation().addPerson(person);
	}


}