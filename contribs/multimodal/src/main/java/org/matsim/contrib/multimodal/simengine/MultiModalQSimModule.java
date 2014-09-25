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

import org.apache.log4j.Logger;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.util.TravelTime;

import java.util.Map;

public class MultiModalQSimModule {

    private static Logger log = Logger.getLogger(MultiModalQSimModule.class);

    private final Config config;
    private final Map<String, TravelTime> multiModalTravelTimes;

    public MultiModalQSimModule(Config config, Map<String, TravelTime> multiModalTravelTimes) {
        this.config = config;
        this.multiModalTravelTimes = multiModalTravelTimes;
    }

    public void configure(QSim qSim) {
        MultiModalConfigGroup multiModalConfigGroup = ConfigUtils.addOrGetModule(this.config, MultiModalConfigGroup.GROUP_NAME, MultiModalConfigGroup.class);
        MultiModalSimEngine multiModalEngine;
        if (multiModalConfigGroup.getNumberOfThreads() > 1) {
            multiModalEngine = new ParallelMultiModalSimEngine(this.multiModalTravelTimes, multiModalConfigGroup);
            log.info("Using ParallelMultiModalSimEngine with " + multiModalConfigGroup.getNumberOfThreads() + " threads.");
        }
        else {
            multiModalEngine = new MultiModalSimEngine(this.multiModalTravelTimes);
        }
        qSim.addMobsimEngine(multiModalEngine);
        qSim.addDepartureHandler(new MultiModalDepartureHandler(multiModalEngine, multiModalConfigGroup));
    }

}
