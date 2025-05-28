/* *********************************************************************** *
 * project: org.matsim.*
 * Main.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.multimodal;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.contrib.multimodal.tools.PrepareMultiModalScenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.PersonVehicles;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/**
 *
 * This class shows an example how to set up a controler and with
 * an initialized multi-modal simulation.
 **
 * @author cdobler
 */
public class RunMultimodalExample {

	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig( args, new MultiModalConfigGroup() ) ;
		Scenario scenario = ScenarioUtils.loadScenario(config);
		PrepareMultiModalScenario.run(scenario);
		Controler controler = new Controler(scenario);

		// After changing to the AccessEgressRouter, we need to make sure, that each person has a vehicle id for each mode:
		// We need to add a dummy vehicle, it however does not affect the results
		Id<VehicleType> typeId = Id.create(1, VehicleType.class);
		controler.getScenario().getVehicles().addVehicleType(VehicleUtils.createVehicleType(typeId));
		controler.getScenario().getVehicles().addVehicle(VehicleUtils.createVehicle(Id.createVehicleId(1), controler.getScenario().getVehicles().getVehicleTypes().get(typeId)));

		PersonVehicles vehicles = new PersonVehicles();
		vehicles.addModeVehicle(TransportMode.car, Id.createVehicleId(1));
		vehicles.addModeVehicle(TransportMode.walk, Id.createVehicleId(1));
		for (Person p : controler.getScenario().getPopulation().getPersons().values()){
			VehicleUtils.insertVehicleIdsIntoPersonAttributes(p, vehicles.getModeVehicles());
		}

		controler.addOverridingModule(new MultiModalModule());
		controler.run();
	}

}
