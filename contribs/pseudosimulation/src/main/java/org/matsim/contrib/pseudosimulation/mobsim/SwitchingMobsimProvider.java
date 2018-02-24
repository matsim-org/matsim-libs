/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * SwitchingMobsimProvider.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
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

package org.matsim.contrib.pseudosimulation.mobsim;

import com.google.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.pseudosimulation.RunPSim;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.jdeqsim.JDEQSimConfigGroup;
import org.matsim.core.mobsim.jdeqsim.JDEQSimulation;
import org.matsim.core.mobsim.qsim.ActiveQSimBridge;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;

import javax.inject.Inject;

public class SwitchingMobsimProvider implements Provider<Mobsim> {

    private Config config;
    private Scenario scenario;
    private EventsManager eventsManager;
    private RunPSim.MobSimSwitcher mobSimSwitcher;
    private MobsimTimer mobsimTimer;
    private AgentCounter agentCounter;
    private ActiveQSimBridge activeQSimBridge;

    @Inject
    SwitchingMobsimProvider(Config config, Scenario scenario, EventsManager eventsManager, RunPSim.MobSimSwitcher mobSimSwitcher, AgentCounter agentCounter, MobsimTimer mobsimTimer, ActiveQSimBridge activeQSimBridge) {
        this.config = config;
        this.scenario = scenario;
        this.eventsManager = eventsManager;
        this.mobSimSwitcher = mobSimSwitcher;
        this.agentCounter = agentCounter;
        this.mobsimTimer = mobsimTimer;
        this.activeQSimBridge = activeQSimBridge;
    }

    @Override
    public Mobsim get() {
        String mobsim = config.controler().getMobsim();
        if (mobSimSwitcher.isQSimIteration()) {
            if (mobsim.equals("jdeqsim")) {
                return new JDEQSimulation(ConfigUtils.addOrGetModule(scenario.getConfig(), JDEQSimConfigGroup.NAME, JDEQSimConfigGroup.class), scenario, eventsManager);
            } else {
                return QSimUtils.createDefaultQSim(scenario, eventsManager, mobsimTimer, agentCounter, activeQSimBridge);
            }
        } else {
            return mobSimSwitcher.getpSimFactory().get();
        }
    }

}
