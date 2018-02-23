/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * MultiModalQSimModule.java
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

package org.matsim.contrib.multimodal.simengine;

import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.router.util.TravelTime;

public class MultiModalQSimModule {

    private final Config config;
    private final Map<String, TravelTime> multiModalTravelTimes;
    
    private final EventsManager eventsManager;
    private final Scenario scenario;
    private final AgentCounter agentCounter;
    private final MobsimTimer mobsimTimer;

    public MultiModalQSimModule(Config config, Map<String, TravelTime> multiModalTravelTimes,
    		EventsManager eventsManager, Scenario scenario, AgentCounter agentCounter, MobsimTimer mobsimTimer) {
        this.config = config;
        this.multiModalTravelTimes = multiModalTravelTimes;
        
    	this.eventsManager = eventsManager;
    	this.scenario = scenario;
    	this.agentCounter = agentCounter;
    	this.mobsimTimer = mobsimTimer;
    }

    public void configure(QSim qSim) {
        MultiModalConfigGroup multiModalConfigGroup = ConfigUtils.addOrGetModule(this.config, MultiModalConfigGroup.GROUP_NAME, MultiModalConfigGroup.class);
        MultiModalSimEngine multiModalEngine = new MultiModalSimEngine(this.multiModalTravelTimes, multiModalConfigGroup, eventsManager, scenario, agentCounter, mobsimTimer);
        qSim.addMobsimEngine(multiModalEngine);
        qSim.addDepartureHandler(new MultiModalDepartureHandler(multiModalEngine, multiModalConfigGroup));
    }
}