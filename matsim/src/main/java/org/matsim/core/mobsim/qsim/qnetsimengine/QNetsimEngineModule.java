/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * QNetsimEngineModule.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;

public class QNetsimEngineModule {

    public static QNetsimEngine configure(QNetworkFactory networkFactory, QSim qsim, Config config, Scenario scenario, EventsManager eventsManager, MobsimTimer mobsimTimer, AgentCounter agentCounter, InternalInterface internalInterface) {
        QNetsimEngine netsimEngine = new QNetsimEngine(networkFactory, config, scenario, eventsManager, mobsimTimer, agentCounter, internalInterface);
        qsim.addMobsimEngine(netsimEngine);
        qsim.addDepartureHandler(netsimEngine.getDepartureHandler());
        return netsimEngine;
    }

}
