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

import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.VrpSimEngine;
import org.matsim.contrib.dvrp.data.MatsimVrpData;
import org.matsim.contrib.dvrp.data.network.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.run.*;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils.TravelTimeSource;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.util.*;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.vis.otfvis.*;

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.network.VrpGraph;


public class OneTaxiLauncher
{
    /*package*/final String dirName;
    /*package*/final String netFileName;
    /*package*/final String plansFileName;
    /*package*/final String depotsFileName;
    /*package*/final boolean otfVis;

    /*package*/final Scenario scenario;


    /*package*/public OneTaxiLauncher()
    {
        dirName = "./src/main/resources/";
        netFileName = dirName + "network.xml";
        plansFileName = dirName + "population.xml";
        depotsFileName = dirName + "1_depot_1_taxi.xml";

        otfVis = true;//or false -- turning ON/OFF visualization

        scenario = VrpLauncherUtils.initScenario(netFileName, plansFileName);
    }


    /*package*/void go()
    {
        TravelTime travelTime = new FreeSpeedTravelTime();
        TravelDisutility travelDisutility = new TimeAsTravelDisutility(travelTime);

        VrpGraph graph = VrpLauncherUtils.initVrpGraph(scenario,
                TravelTimeSource.FREE_FLOW_SPEED, travelTime, travelDisutility);

        VrpData vrpData = VrpLauncherUtils.initVrpData(scenario, graph, depotsFileName);
        MatsimVrpData data = new MatsimVrpData(vrpData, scenario);
        OneTaxiOptimizer optimizer = new OneTaxiOptimizer(vrpData);

        QSim qSim = VrpLauncherUtils.initQSim(scenario);
        VrpSimEngine vrpSimEngine = VrpLauncherUtils.initVrpSimEngine(qSim, data, optimizer);
        VrpLauncherUtils.initAgentSources(qSim, data, vrpSimEngine, new OneTaxiActionCreator(
                vrpSimEngine));
        VrpLauncherUtils.initDepartureHandler(qSim, data, vrpSimEngine, new OneTaxiRequestCreator(
                vrpData), OneTaxiRequestCreator.MODE);

        EventsManager events = qSim.getEventsManager();

        if (otfVis) { // OFTVis visualization
            ConfigUtils.addOrGetModule(scenario.getConfig(), OTFVisConfigGroup.GROUP_NAME,
                    OTFVisConfigGroup.class).setDrawNonMovingItems(true);
            OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(scenario.getConfig(),
                    scenario, qSim.getEventsManager(), qSim);
            OTFClientLive.run(scenario.getConfig(), server);
        }

        qSim.run();
        events.finishProcessing();
    }


    public static void main(String... args)
        throws IOException
    {
        OneTaxiLauncher launcher = new OneTaxiLauncher();
        launcher.go();
    }
}
