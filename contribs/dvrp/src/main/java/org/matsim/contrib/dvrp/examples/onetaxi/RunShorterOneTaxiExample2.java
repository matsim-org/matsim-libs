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
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic.DynActionCreator;
import org.matsim.core.config.*;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import com.google.inject.AbstractModule;


public class RunShorterOneTaxiExample2
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
        controler.addOverridingModule(new DvrpModule("taxi", fleet, new AbstractModule() {
            @Override
            protected void configure()
            {
                bind(VrpOptimizer.class).to(OneTaxiOptimizer.class).asEagerSingleton();
                bind(PassengerRequestCreator.class).to(OneTaxiRequestCreator.class)
                        .asEagerSingleton();
                bind(DynActionCreator.class).to(OneTaxiActionCreator.class).asEagerSingleton();
            }
        }));

        controler.run();
    }
}
