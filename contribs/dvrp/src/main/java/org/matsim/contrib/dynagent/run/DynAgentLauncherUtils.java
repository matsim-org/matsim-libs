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

package org.matsim.contrib.dynagent.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineModule;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.OTFVisConfigGroup.ColoringScheme;
import org.matsim.vis.otfvis.OnTheFlyServer;


public class DynAgentLauncherUtils
{
    public static QSim initQSim(Scenario scenario)
    {
        EventsManager events = EventsUtils.createEventsManager();
        QSim qSim = new QSim(scenario, events);

        DynActivityEngine dynActivityEngine = new DynActivityEngine(events, qSim.getAgentCounter());
        qSim.addMobsimEngine(dynActivityEngine);
        qSim.addActivityHandler(dynActivityEngine);

        QNetsimEngineModule.configure(qSim);

        qSim.addMobsimEngine(new TeleportationEngine(scenario, events));
        
        if (scenario.getConfig().network().isTimeVariantNetwork()) {
            qSim.addMobsimEngine(new NetworkChangeEventsEngine());
        }

        return qSim;
    }


    public static void runOTFVis(QSim qSim, boolean drawNonMovingItems)
    {
        runOTFVis(qSim, drawNonMovingItems, ColoringScheme.standard);
    }


    public static void runOTFVis(QSim qSim, boolean drawNonMovingItems,
            ColoringScheme coloringScheme)
    {
        Config config = qSim.getScenario().getConfig();

        OTFVisConfigGroup configGroup = ConfigUtils.addOrGetModule(config,
                OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class);
        configGroup.setDrawNonMovingItems(drawNonMovingItems);
        configGroup.setColoringScheme(coloringScheme);

        OnTheFlyServer server = OTFVis.startServerAndRegisterWithQSim(config, qSim.getScenario(),
                qSim.getEventsManager(), qSim);
        OTFClientLive.run(config, server);

    }
}
