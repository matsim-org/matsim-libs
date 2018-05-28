/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * SimpleDisruptionEngine.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2018 by the members listed in the COPYING, *
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

package org.matsim.pt.withinday;


import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SimpleDisruptionEngine implements MobsimEngine {

	private static Logger log = Logger.getLogger(SimpleDisruptionEngine.class);
	
	private final WithinDayTransitEngine withinDayTransitEngine;
	private final double disruptionStart;
	
	@SuppressWarnings("unused")
	private InternalInterface internalInterface;
	private boolean triggered;
	
	
	@Inject
	public SimpleDisruptionEngine(Config config, EventsManager eventsManager, WithinDayTransitEngine engine) {
		SimpleDisruptionConfigGroup cfg = ConfigUtils.addOrGetModule(config, SimpleDisruptionConfigGroup.class);
		this.disruptionStart = cfg.getDisruptionStart();
		this.withinDayTransitEngine = engine;
	}
	
	@Override
	public void doSimStep(double time) {
		if (!triggered && time >= disruptionStart) {
			log.info("Disruption is triggered. Running the replanner");
			withinDayTransitEngine.doReplan();
			triggered = true;
		}
	}

	@Override
	public void onPrepareSim() {
		this.triggered = false;
	}

	@Override
	public void afterSim() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}

}
