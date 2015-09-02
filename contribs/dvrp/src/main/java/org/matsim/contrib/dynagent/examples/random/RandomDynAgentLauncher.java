/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dynagent.examples.random;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dynagent.run.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup.ColoringScheme;


public class RandomDynAgentLauncher
{
    private final String dir;
    private final String netFile;
    private final boolean otfVis;
    private final int agentCount;


    public RandomDynAgentLauncher(boolean otfVis)
    {
        this.otfVis = otfVis;

        dir = "./src/main/resources/";
        netFile = dir + "grid_network.xml";

        agentCount = 100;
    }


    public void go()
    {
        Scenario scenario = ScenarioUtils.createScenario(DynConfigUtils.createConfig());
        new MatsimNetworkReader(scenario).readFile(netFile);

        QSim qSim = DynAgentLauncherUtils.initQSim(scenario);
        qSim.addAgentSource(new RandomDynAgentSource(qSim, agentCount));

        EventsManager events = qSim.getEventsManager();

        if (otfVis) { // OTFVis visualization
            DynAgentLauncherUtils.runOTFVis(qSim, true, ColoringScheme.byId);
        }

        qSim.run();
        events.finishProcessing();
    }


    public static void main(String[] args)
    {
        new RandomDynAgentLauncher(true).go();
    }
}
