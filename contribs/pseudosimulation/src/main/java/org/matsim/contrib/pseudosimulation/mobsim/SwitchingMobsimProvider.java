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

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.pseudosimulation.MobSimSwitcher;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.jdeqsim.JDEQSimConfigGroup;
import org.matsim.core.mobsim.jdeqsim.JDEQSimulation;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.mobsim.qsim.QSimProvider;

import com.google.inject.Inject;
import com.google.inject.Provider;


public class SwitchingMobsimProvider implements Provider<Mobsim> {

    @Inject private Config config;
    @Inject private Scenario scenario;
    @Inject private EventsManager eventsManager;
    @Inject private MobSimSwitcher mobSimSwitcher;
    @Inject private PSimProvider pSimProvider;
    @Inject private QSimProvider qsimProvider;


    @Override
    public Mobsim get() {
        String mobsim = config.controler().getMobsim();
        if (mobSimSwitcher.isQSimIteration()) {
            if (mobsim.equals("jdeqsim")) {
                return new JDEQSimulation(ConfigUtils.addOrGetModule(scenario.getConfig(), JDEQSimConfigGroup.NAME, JDEQSimConfigGroup.class), scenario, eventsManager);
            } else {
            	return qsimProvider.get();
            }
        } else {
            return pSimProvider.get();
        }
    }

}
