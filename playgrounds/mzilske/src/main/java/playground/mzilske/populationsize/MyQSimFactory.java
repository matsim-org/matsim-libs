/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * MyQSimFactory.java
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

package playground.mzilske.populationsize;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.multimodal.simengine.MultiModalQSimModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.router.util.TravelTime;

import java.util.HashMap;

class MyQSimFactory implements MobsimFactory {

    @Inject
    TravelTime carTravelTime;

    @Override
    public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
        QSim qSim = (QSim) QSimUtils.createDefaultQSim(sc, eventsManager);
        HashMap<String, TravelTime> multiModalTravelTimes = new HashMap<>();
        multiModalTravelTimes.put("car", carTravelTime);
        new MultiModalQSimModule(sc.getConfig(), multiModalTravelTimes).configure(qSim);
        return qSim;
    }

}
