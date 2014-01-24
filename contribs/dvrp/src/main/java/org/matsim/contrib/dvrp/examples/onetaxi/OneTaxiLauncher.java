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
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.router.*;
import org.matsim.contrib.dvrp.run.*;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils.TravelTimeSource;
import org.matsim.contrib.dynagent.run.DynAgentLauncherUtils;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.util.*;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;


public class OneTaxiLauncher
{
    private final String dirName;
    private final String netFileName;
    private final String plansFileName;
    private final String depotsFileName;
    private final boolean otfVis;

    private final Scenario scenario;


    public OneTaxiLauncher()
    {
        dirName = "./src/main/resources/";
        netFileName = dirName + "network.xml";
        plansFileName = dirName + "population.xml";
        depotsFileName = dirName + "1_depot_1_taxi.xml";

        otfVis = true;//or false -- turning ON/OFF visualization

        scenario = VrpLauncherUtils.initScenario(netFileName, plansFileName);
    }


    public void go()
    {
        TravelTime travelTime = new FreeSpeedTravelTime();
        TravelDisutility travelDisutility = new TimeAsTravelDisutility(travelTime);

        VrpPathCalculator calculator = VrpLauncherUtils.initVrpPathCalculator(scenario,
                TravelTimeSource.FREE_FLOW_SPEED, travelTime, travelDisutility);

        VrpData vrpData = VrpLauncherUtils.initVrpData(scenario, depotsFileName);

        OneTaxiOptimizer optimizer = new OneTaxiOptimizer(vrpData, calculator);

        QSim qSim = DynAgentLauncherUtils.initQSim(scenario);

        MatsimVrpData data = new MatsimVrpData(vrpData, scenario, qSim.getSimTimer());

        PassengerEngine passengerEngine = VrpLauncherUtils.initPassengerEngine(
                OneTaxiRequestCreator.MODE, new OneTaxiRequestCreator(), optimizer, data, qSim);

        VrpLauncherUtils.initAgentSources(qSim, data, optimizer, new OneTaxiActionCreator(
                passengerEngine));

        EventsManager events = qSim.getEventsManager();

        if (otfVis) { // OFTVis visualization
            DynAgentLauncherUtils.runOTFVis(qSim, true);
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
