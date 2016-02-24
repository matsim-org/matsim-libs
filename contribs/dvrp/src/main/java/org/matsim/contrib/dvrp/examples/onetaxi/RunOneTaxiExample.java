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
import org.matsim.contrib.dvrp.MatsimVrpContextImpl;
import org.matsim.contrib.dvrp.data.VrpData;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.run.VrpLauncherUtils;
import org.matsim.contrib.dynagent.run.DynAgentLauncherUtils;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.vis.otfvis.OTFVisConfigGroup.ColoringScheme;


public class RunOneTaxiExample
{
    private final String dir;
    private final String netFile;
    private final String plansFile;
    private final String vehiclesFile;
    private final boolean otfVis;


    public RunOneTaxiExample(boolean otfVis)
    {
        this.otfVis = otfVis;

        dir = "./src/main/resources/";
        netFile = dir + "grid_network.xml";
        plansFile = dir + "one_taxi/one_taxi_population.xml";
        vehiclesFile = dir + "one_taxi/one_taxi_vehicles.xml";
    }


    public void go()
    {
        MatsimVrpContextImpl context = new MatsimVrpContextImpl();

        Scenario scenario = VrpLauncherUtils.initScenario(netFile, plansFile);
        context.setScenario(scenario);

        VrpData vrpData = VrpLauncherUtils.initVrpData(context, vehiclesFile);
        context.setVrpData(vrpData);

        OneTaxiOptimizer optimizer = new OneTaxiOptimizer(context);

        QSim qSim = DynAgentLauncherUtils.initQSim(scenario);
        context.setMobsimTimer(qSim.getSimTimer());

        PassengerEngine passengerEngine = VrpLauncherUtils.initPassengerEngine("taxi",
                new OneTaxiRequestCreator(), optimizer, context, qSim);

        VrpLauncherUtils.initAgentSources(qSim, context, optimizer,
                new OneTaxiActionCreator(passengerEngine, qSim.getSimTimer()));

        EventsManager events = qSim.getEventsManager();

        if (otfVis) { // OFTVis visualization
            DynAgentLauncherUtils.runOTFVis(qSim, true, ColoringScheme.taxicab);
        }

        qSim.run();
        events.finishProcessing();
    }


    public static void main(String... args)
    {
        new RunOneTaxiExample(true).go();
    }
}
