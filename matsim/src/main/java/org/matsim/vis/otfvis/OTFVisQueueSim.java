/* *********************************************************************** *
 * project: org.matsim.*
 * OnTheFlyQueueSimQuad.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.vis.otfvis;


import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.ptproject.qsim.QueueSimulation;
import org.matsim.vis.otfvis.server.OnTheFlyServer;


public class OTFVisQueueSim extends QueueSimulation {

	private final OTFVisQueueSimFeature queueSimulationFeature;

	public OTFVisQueueSim(Scenario scenario, EventsManager events) {
		super(scenario, events);
		queueSimulationFeature = new OTFVisQueueSimFeature(this);
		super.addFeature(queueSimulationFeature);
	}

	public void setServer(OnTheFlyServer server) {
		queueSimulationFeature.setServer(server);
	}

	public void setVisualizeTeleportedAgents(boolean active) {
		queueSimulationFeature.setVisualizeTeleportedAgents(active);
	}

	public OTFVisQueueSimFeature getQueueSimulationFeature() {
		return queueSimulationFeature;
	}
	
}

