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
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.data.file.VehicleReader;
import org.matsim.contrib.dvrp.router.DynRoutingModule;
import org.matsim.contrib.dvrp.run.VrpQSimConfigConsistencyChecker;
import org.matsim.contrib.dynagent.run.DynQSimModule;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.*;
import org.matsim.core.controler.*;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;


public class RunOneTaxiExample
{
    public static final String MODE = "taxi";


    public static void run(boolean otfvis)
    {
        String configFile = "./src/main/resources/one_taxi/one_taxi_config.xml";
        run(configFile, otfvis);
    }


    public static void run(String configFile, boolean otfvis)
    {
        Config config = ConfigUtils.loadConfig(configFile, new OTFVisConfigGroup());
        config.addConfigConsistencyChecker(new VrpQSimConfigConsistencyChecker());
        config.checkConsistency();

        Scenario scenario = ScenarioUtils.loadScenario(config);

        final VrpData vrpData = new VrpDataImpl();
        String taxisFile = config.getModule("one_taxi").getValue("taxisFile");
        new VehicleReader(scenario.getNetwork(), vrpData).parse(taxisFile);

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new AbstractModule() {
            public void install()
            {
                addRoutingModuleBinding(MODE).toInstance(new DynRoutingModule(MODE));
                bind(VrpData.class).toInstance(vrpData);
            }
        });
        controler.addOverridingModule(new DynQSimModule<>(OneTaxiQSimProvider.class));

        if (otfvis) {
            controler.addOverridingModule(new OTFVisLiveModule());
        }

        controler.run();
    }


    public static void main(String... args)
    {
        run(true);
    }
}
