/* *********************************************************************** *
 * project: org.matsim.*
 * MultimodalQSimFactory.java
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

package org.matsim.contrib.multimodal;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.multimodal.simengine.MultiModalQSimModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.router.util.TravelTime;

import java.util.Map;

public class MultimodalQSimFactory implements MobsimFactory {

	private final Map<String, TravelTime> multiModalTravelTimes;
	private final MobsimFactory delegateFactory;
	
	public MultimodalQSimFactory(Map<String, TravelTime> multiModalTravelTimes) {
		// use a QSimFactory as default delegate
		this(multiModalTravelTimes, QSimFactory.createQSimFactory());
	}

	public MultimodalQSimFactory(Map<String, TravelTime> multiModalTravelTimes, MobsimFactory mobsimFactory) {
		this.multiModalTravelTimes = multiModalTravelTimes;
		this.delegateFactory = mobsimFactory;
	}
	
	@Override
	public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
		QSim qSim = (QSim) this.delegateFactory.createMobsim(sc, eventsManager);
        new MultiModalQSimModule(sc.getConfig(), this.multiModalTravelTimes).configure(qSim);
        return qSim;
	}

}
