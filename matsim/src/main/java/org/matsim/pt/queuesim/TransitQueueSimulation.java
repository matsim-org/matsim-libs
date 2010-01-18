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

package org.matsim.pt.queuesim;


import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.ptproject.qsim.QueueSimulation;



public class TransitQueueSimulation extends QueueSimulation {

	static final Logger log = Logger.getLogger(TransitQueueSimulation.class);
	
	private TransitQueueSimulationFeature queueSimulationFeature;

	public TransitQueueSimulation(Scenario scenario, EventsManager events) {
		super(scenario, events);
		if (scenario.getConfig().getQSimConfigGroup() == null){
		  scenario.getConfig().setQSimConfigGroup(new QSimConfigGroup());
		}
		installFeature();
		
	}

	private void installFeature() {
		queueSimulationFeature = new TransitQueueSimulationFeature(this);
		super.addFeature(queueSimulationFeature);
		super.addDepartureHandler(queueSimulationFeature);
	}

	public void startOTFServer(String serverName) {
		queueSimulationFeature.startOTFServer(serverName);
	}

	public TransitStopAgentTracker getAgentTracker() {
		return queueSimulationFeature.getAgentTracker();
	}
	
	public void setUseUmlaeufe(boolean useUmlaeufe) {
		queueSimulationFeature.setUseUmlaeufe(useUmlaeufe);
	}
	
	public void setTransitStopHandlerFactory(TransitStopHandlerFactory stopHandlerFactory) {
		queueSimulationFeature.setTransitStopHandlerFactory(stopHandlerFactory);
	}
	
}
