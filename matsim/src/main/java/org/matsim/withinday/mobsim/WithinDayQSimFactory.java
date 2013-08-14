/* *********************************************************************** *
 * project: org.matsim.*
 * WithinDayQSimFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.withinday.mobsim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimFactory;

public class WithinDayQSimFactory implements MobsimFactory {

	private static final Logger log = Logger.getLogger(WithinDayQSimFactory.class);

	private final WithinDayEngine withinDayEngine;
	private final MobsimFactory delegateFactory;
	
	public WithinDayQSimFactory(WithinDayEngine withinDayEngine) {
		this.withinDayEngine = withinDayEngine;
		this.delegateFactory = new QSimFactory();
	}
	
	public WithinDayQSimFactory(WithinDayEngine withinDayEngine, MobsimFactory delegateFactory) {
		this.withinDayEngine = withinDayEngine;
		this.delegateFactory = delegateFactory;
	}
	
    @Override
	public QSim createMobsim(Scenario sc, EventsManager eventsManager) {
    	
    	Mobsim mobsim = this.delegateFactory.createMobsim(sc, eventsManager);
    	
    	if (mobsim instanceof QSim) {
    		log.info("Adding WithinDayEngine to Mobsim.");
    		((QSim) mobsim).addMobsimEngine(withinDayEngine);
    		return (QSim) mobsim;
    	} else {
    		throw new RuntimeException("Delegate MobsimFactory create a Mobsim from type " + mobsim.getClass().toString() +
    				". Aborting since a Mobsim from type QSim was expexted!");
    	}
	}
}
