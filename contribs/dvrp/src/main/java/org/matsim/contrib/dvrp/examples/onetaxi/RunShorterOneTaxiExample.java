/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.contrib.dvrp.examples.onetaxi;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.data.FleetImpl;
import org.matsim.contrib.dvrp.data.file.VehicleReader;
import org.matsim.contrib.dvrp.run.BasicDvrpModule;
import org.matsim.core.config.*;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;


public class RunShorterOneTaxiExample
{
    private static final String CONFIG_FILE = "./src/main/resources/one_taxi/shorter_one_taxi_config.xml";
    private static final String VEHICLES_FILE = "./src/main/resources/one_taxi/one_taxi_vehicles.xml";


    public static void main(String... args)
    {
        Config config = ConfigUtils.loadConfig(CONFIG_FILE);
        Scenario scenario = ScenarioUtils.loadScenario(config);

        final FleetImpl fleet = new FleetImpl();
        new VehicleReader(scenario.getNetwork(), fleet).readFile(VEHICLES_FILE);

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new BasicDvrpModule(//
                "taxi", // departures of the "taxi" mode will be handled
                fleet, // taxi fleet that will serve requests
                OneTaxiOptimizer.class, // optimizer that dispatches taxis
                OneTaxiRequestCreator.class, // converts departures of the "taxi" mode into taxi requests
                OneTaxiActionCreator.class)); // converts scheduled tasks into simulated actions (legs and activities)

        controler.run();
    }
}
