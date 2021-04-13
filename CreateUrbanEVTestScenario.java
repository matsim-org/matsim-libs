/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.urbanEV;

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
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.run.ev.RunUrbanEVExample;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehiclesFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

class CreateUrbanEVTestScenario {

	static Scenario createTestScenario(){
		//config. vehicle source = modeVehicleTypeFromData ??

		EvConfigGroup evConfigGroup = new EvConfigGroup();
		evConfigGroup.setVehiclesFile("this is not important because we use standard matsim vehicles");
		evConfigGroup.setTimeProfiles(true);
		evConfigGroup.setChargersFile("chargers.xml");
		Config config = ConfigUtils.loadConfig("test/input/chessboard/chessboard-config.xml", evConfigGroup);
		config.network().setInputFile("1pctNetwork.xml");


		//prepare config
		RunUrbanEVExample.prepareConfig(config);
		config.controler().setOutputDirectory("test/output/urbanEV");
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

		VehicleType bikeVehicleType = vehiclesFactory.createVehicleType(Id.create(TransportMode.bike, VehicleType.class));
		VehicleUtils.setHbefaTechnology(bikeVehicleType.getEngineInformation(), "electricity");
		VehicleUtils.setEnergyCapacity(bikeVehicleType.getEngineInformation(), 10);
		EVUtils.setInitialEnergy(bikeVehicleType.getEngineInformation(), 4);
		EVUtils.setChargerTypes(bikeVehicleType.getEngineInformation(), Arrays.asList("a", "b", "default"));

		scenario.getVehicles().addVehicleType(carVehicleType);
		scenario.getVehicles().addVehicleType(bikeVehicleType);

		Map<String, VehicleType> mode2VehicleType = new HashMap<>();
		mode2VehicleType.put(TransportMode.car, carVehicleType);
		mode2VehicleType.put(TransportMode.bike, bikeVehicleType);
		createAndRegisterVehicles(scenario, mode2VehicleType);

		return scenario;
	}

	private static void overridePopulation(Scenario scenario) {

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


		Person person2 = factory.createPerson(Id.createPersonId("Charging during shopping"));

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
		shopping21.setMaximumDuration(1200);

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

		Person person4 = factory.createPerson(Id.createPersonId("Charger Selection long distance twin"));

		Plan plan4 = factory.createPlan();

		Activity home41 = factory.createActivityFromLinkId("home", Id.createLinkId("1"));
		home41.setEndTime(8 * 3605);
		plan4.addActivity(home41);
		plan4.addLeg(factory.createLeg(TransportMode.car));

		Activity work41 = factory.createActivityFromLinkId("work", Id.createLinkId("90"));
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

		Person person5 = factory.createPerson(Id.createPersonId("Triple Charger"));

		Plan plan5 = factory.createPlan();

		Activity home51 = factory.createActivityFromLinkId("home", Id.createLinkId("1"));
		home51.setMaximumDuration(1*1200);
		plan5.addActivity(home51);

		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity work51 = factory.createActivityFromLinkId("work", Id.createLinkId("90"));
		work51.setMaximumDuration(1*1200);
		plan5.addActivity(work51);

		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity work52 = factory.createActivityFromLinkId("work", Id.createLinkId("1"));
		work52.setMaximumDuration(1*1200);
		plan5.addActivity(work52);

		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity work53 = factory.createActivityFromLinkId("home", Id.createLinkId("90"));
		work53.setMaximumDuration(1*1200);
		plan5.addActivity(work53);

		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity home52 = factory.createActivityFromLinkId("work", Id.createLinkId("1"));
		home52.setMaximumDuration(1*1200);
		plan5.addActivity(home52);

		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity work54 = factory.createActivityFromLinkId("work", Id.createLinkId("90"));
		work54.setMaximumDuration(1*1200);
		plan5.addActivity(work54);

		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity work55 = factory.createActivityFromLinkId("work", Id.createLinkId("1"));
		work55.setMaximumDuration(1*1200);
		plan5.addActivity(work55);

		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity work56 = factory.createActivityFromLinkId("home", Id.createLinkId("90"));
		work56.setMaximumDuration(1*1200);
		plan5.addActivity(work56);

		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity home53 = factory.createActivityFromLinkId("work", Id.createLinkId("1"));
		home53.setMaximumDuration(1*1200);
		plan5.addActivity(home53);

		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity work57 = factory.createActivityFromLinkId("work", Id.createLinkId("90"));
		work57.setMaximumDuration(1*1200);
		plan5.addActivity(work57);

		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity work58 = factory.createActivityFromLinkId("work", Id.createLinkId("1"));
		work58.setMaximumDuration(1*1200);
		plan5.addActivity(work58);

		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity work59 = factory.createActivityFromLinkId("home", Id.createLinkId("90"));
		work59.setMaximumDuration(1*1200);
		plan5.addActivity(work59);
		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity home54 = factory.createActivityFromLinkId("work", Id.createLinkId("1"));
		home54.setMaximumDuration(1*1200);
		plan5.addActivity(home54);

		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity work510 = factory.createActivityFromLinkId("work", Id.createLinkId("90"));
		work510.setMaximumDuration(1*1200);
		plan5.addActivity(work510);

		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity work511 = factory.createActivityFromLinkId("work", Id.createLinkId("1"));
		work511.setMaximumDuration(1*1200);
		plan5.addActivity(work511);

		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity work512 = factory.createActivityFromLinkId("home", Id.createLinkId("90"));
		work512.setMaximumDuration(1*1200);
		plan5.addActivity(work512);
		plan5.addLeg(factory.createLeg(TransportMode.car));

		Activity home55 = factory.createActivityFromLinkId("work", Id.createLinkId("1"));
		home55.setMaximumDuration(1*1200);
		plan5.addActivity(home55);




		person5.addPlan(plan5);
		person5.setSelectedPlan(plan5);

		scenario.getPopulation().addPerson(person5);

		Person person6= factory.createPerson(Id.createPersonId("Double Charger"));

		Plan plan6 = factory.createPlan();

		Activity home61 = factory.createActivityFromLinkId("home", Id.createLinkId("2"));
		home61.setEndTime(6*3600);
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
		work64.setMaximumDuration(1200);
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

		Person person7= factory.createPerson(Id.createPersonId("Not enough time Charger"));

		Plan plan7 = factory.createPlan();

		Activity home71 = factory.createActivityFromLinkId("home", Id.createLinkId("1"));
		home71.setEndTime(6*3600);
		plan7.addActivity(home71);
		plan7.addLeg(factory.createLeg(TransportMode.car));

		Activity work71 = factory.createActivityFromLinkId("work", Id.createLinkId("90"));
		work71.setMaximumDuration(1200);
		plan7.addActivity(work61);

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

	private static void createAndRegisterVehicles(Scenario scenario, Map<String, VehicleType> mode2VehicleType){
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

}
