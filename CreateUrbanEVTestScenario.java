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
import org.matsim.api.core.v01.population.Person;
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

import java.util.*;

class CreateUrbanEVTestScenario {

	static final double CAR_INITIAL_ENERGY = 10.;
	static final double BIKE_INITIAL_ENERGY = 4.;

	static Scenario createTestScenario(){
		EvConfigGroup evConfigGroup = new EvConfigGroup();
		evConfigGroup.setVehiclesFile("this is not important because we use standard matsim vehicles");
		evConfigGroup.setTimeProfiles(true);
		evConfigGroup.setChargersFile("chargers.xml");

		//prepare config
		Config config = ConfigUtils.loadConfig("test/input/chessboard/chessboard-config.xml", evConfigGroup);
		RunUrbanEVExample.prepareConfig(config);
		config.network().setInputFile("1pctNetwork.xml");
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		config.controler().setLastIteration(5);
		config.controler().setWriteEventsInterval(1);
		//set VehicleSource
		config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.fromVehiclesData);
		config.qsim().setEndTime(20*3600);

		//load scenario
		Scenario scenario = ScenarioUtils.loadScenario(config);

		//manually insert car vehicle type with attributes (hbefa technology, initial energy etc....)
		createAndRegisterPersonalCarAndBikeVehicles(scenario);
		return scenario;
	}

	static void createAndRegisterPersonalCarAndBikeVehicles(Scenario scenario){
		VehiclesFactory vehicleFactory = scenario.getVehicles().getFactory();

		for(Person person : scenario.getPopulation().getPersons().values()) {

			VehicleType carVehicleType = vehicleFactory.createVehicleType(Id.create(person.getId().toString(), VehicleType.class)); //TODO should at least have a suffix "_car"
			VehicleUtils.setHbefaTechnology(carVehicleType.getEngineInformation(), "electricity");
			VehicleUtils.setEnergyCapacity(carVehicleType.getEngineInformation(), 10);
			EVUtils.setInitialEnergy(carVehicleType.getEngineInformation(), CAR_INITIAL_ENERGY);
			EVUtils.setChargerTypes(carVehicleType.getEngineInformation(),Arrays.asList("a", "b", "default"));
			scenario.getVehicles().addVehicleType(carVehicleType);
			Vehicle carVehicle = vehicleFactory.createVehicle(VehicleUtils.createVehicleId(person, TransportMode.car), carVehicleType);
			scenario.getVehicles().addVehicle(carVehicle);

			VehicleType bikeVehicleType = vehicleFactory.createVehicleType(Id.create(person.getId().toString() + "_bike", VehicleType.class));
			Vehicle bikeVehicle = vehicleFactory.createVehicle(VehicleUtils.createVehicleId(person, TransportMode.bike), bikeVehicleType);

			scenario.getVehicles().addVehicleType(bikeVehicleType);
			scenario.getVehicles().addVehicle(bikeVehicle);

			Map<String, Id<Vehicle>> mode2Vehicle = new HashMap<>();
			mode2Vehicle.put(TransportMode.car, carVehicle.getId());
			mode2Vehicle.put(TransportMode.bike, bikeVehicle.getId());

			//override the attribute - we assume to need car and bike only
			person.getAttributes().putAttribute("vehicles", Collections.unmodifiableMap(mode2Vehicle));
		}
	}

}
