/* *********************************************************************** *
 * project: org.matsim.*
 * TransitQueueSimulation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.pt.qsim;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.ptproject.qsim.QSim;



public class TransitQSimulation extends QSim {

	static final Logger log = Logger.getLogger(TransitQSimulation.class);
	
	private TransitQSimFeature transitQueueSimulationFeature;

	public TransitQSimulation(Scenario scenario, EventsManager events) {
		super(scenario, events);
		if (scenario.getConfig().getQSimConfigGroup() == null){
		  scenario.getConfig().setQSimConfigGroup(new QSimConfigGroup());
		}
		installFeatures();
	}

	private void installFeatures() {
		transitQueueSimulationFeature = new TransitQSimFeature(this);
		super.addFeature(transitQueueSimulationFeature);
		super.addDepartureHandler(transitQueueSimulationFeature);
	}

	public TransitStopAgentTracker getAgentTracker() {
		return transitQueueSimulationFeature.getAgentTracker();
	}
	
	public void setUseUmlaeufe(boolean useUmlaeufe) {
		transitQueueSimulationFeature.setUseUmlaeufe(useUmlaeufe);
	}
	
	public void setTransitStopHandlerFactory(TransitStopHandlerFactory stopHandlerFactory) {
		transitQueueSimulationFeature.setTransitStopHandlerFactory(stopHandlerFactory);
	}
	
}
