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

import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.util.TravelTime;

public class MultiModalQSimModule {

    private final Config config;
    private final Map<String, TravelTime> multiModalTravelTimes;

    public MultiModalQSimModule(Config config, Map<String, TravelTime> multiModalTravelTimes) {
        this.config = config;
        this.multiModalTravelTimes = multiModalTravelTimes;
    }

    public void configure(QSim qSim) {
        MultiModalConfigGroup multiModalConfigGroup = ConfigUtils.addOrGetModule(this.config, MultiModalConfigGroup.GROUP_NAME, MultiModalConfigGroup.class);
        MultiModalSimEngine multiModalEngine = new MultiModalSimEngine(this.multiModalTravelTimes, multiModalConfigGroup);
        qSim.addMobsimEngine(multiModalEngine);
        qSim.addDepartureHandler(new MultiModalDepartureHandler(multiModalEngine, multiModalConfigGroup));
    }
}