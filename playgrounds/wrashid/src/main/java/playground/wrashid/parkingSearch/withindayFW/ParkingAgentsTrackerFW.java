/* *********************************************************************** *
 * project: org.matsim.*
 * ParkingAgentsTracker.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.wrashid.parkingSearch.withindayFW;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.ExperimentalBasicWithindayAgent;

public class ParkingAgentsTrackerFW implements MobsimInitializedListener {

	private final Map<Id, ExperimentalBasicWithindayAgent> agents;
	
	public ParkingAgentsTrackerFW() {
		this.agents = new HashMap<Id, ExperimentalBasicWithindayAgent>();
	}
	
	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		for (MobsimAgent agent : ((QSim) e.getQueueSimulation()).getAgents()) {
			this.agents.put(agent.getId(), (ExperimentalBasicWithindayAgent) agent);
		}
	}

}
