/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * WithinDayTransitQSimFactory.java
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

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class WithinDayTransitQSimFactory implements Provider<Mobsim> {

	
	private final Scenario scenario;
	private final EventsManager eventsManager;
	//private final WithinDayTransitEngine withinDayTransitEngine; 
	private final WithinDayTransitEngine simpleDisruptionEngine;
	
	@Inject
	public WithinDayTransitQSimFactory(final Scenario scenario, final EventsManager eventsManager, /*WithinDayTransitEngine withinDay,*/ WithinDayTransitEngine disruption) {
		this.scenario = scenario;
		this.eventsManager = eventsManager;
		//this.withinDayTransitEngine = withinDay;
		this.simpleDisruptionEngine = disruption;
	}
	
	
	@Override
	public Mobsim get() {
		QSim mobsim = QSimUtils.createDefaultQSim(scenario, eventsManager);
		//mobsim.addMobsimEngine(withinDayTransitEngine);
		mobsim.addMobsimEngine(simpleDisruptionEngine);
		return mobsim;
		
	}

	
	
}
