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

package contrib.multimodal;

import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.router.util.TravelTime;

import contrib.multimodal.simengine.MultiModalDepartureHandler;
import contrib.multimodal.simengine.MultiModalSimEngine;
import contrib.multimodal.simengine.MultiModalSimEngineFactory;

public class MultimodalQSimFactory implements MobsimFactory {

	private Map<String, TravelTime> multiModalTravelTimes;

	public MultimodalQSimFactory(Map<String, TravelTime> multiModalTravelTimes) {
		this.multiModalTravelTimes = multiModalTravelTimes;
	}

	@Override
	public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
		QSim qSim = (QSim) new QSimFactory().createMobsim(sc, eventsManager);
		MultiModalSimEngine multiModalEngine = new MultiModalSimEngineFactory().createMultiModalSimEngine(qSim, this.multiModalTravelTimes);
		qSim.addMobsimEngine(multiModalEngine);
        MultiModalConfigGroup multiModalConfigGroup = (MultiModalConfigGroup) sc.getConfig().getModule(MultiModalConfigGroup.GROUP_NAME);
        qSim.addDepartureHandler(new MultiModalDepartureHandler(multiModalEngine, multiModalConfigGroup));
		return qSim;
	}

}
