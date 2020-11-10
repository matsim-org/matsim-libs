/* *********************************************************************** *
 * project: org.matsim.*
 * MyControler1.java
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

package org.matsim.codeexamples.config;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.Map;


/**
 * runs a mobsim and writes events output.  See the config file for configuration details.
 *
 * @author nagel
 */
public final class RunFromConfigfileExample {

	public static void main(final String[] args) {

		Config config;
		if (args != null && args.length >= 1) {
			config = ConfigUtils.loadConfig(args); // note that this may process command line options
		} else {
			config = ConfigUtils.loadConfig("scenarios/equil/config.xml");
		}

		Scenario scenario = ScenarioUtils.loadScenario(config);

		// if vehicles are not auto generated but explicitly loaded from a vehicles file, vehicles have to be attached to agents.
		if (config.qsim().getVehiclesSource().equals(QSimConfigGroup.VehiclesSource.fromVehiclesData)) {

			// Sets the network mode for bicycles correctly
			Id<VehicleType> bicycle = Id.create("bicycle", VehicleType.class);
			if (scenario.getVehicles().getVehicleTypes().containsKey(bicycle)) {
				scenario.getVehicles().getVehicleTypes().get(bicycle).setNetworkMode("bicycle");
			}

			for (Person person : scenario.getPopulation().getPersons().values()) {
				Vehicle vehicle = scenario.getVehicles().getVehicles().get(Id.createVehicleId(person.getId()));
				VehicleUtils.insertVehicleIdsIntoAttributes(person, Map.of(vehicle.getType().getNetworkMode(), vehicle.getId()));
			}
		}

		Controler controler = new Controler(scenario);

		controler.run();
	}

}
