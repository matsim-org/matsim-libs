/* *********************************************************************** *
 * project: org.matsim.*
 * TransportModeFilterFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.withinday.replanning.identifiers.filter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.withinday.replanning.identifiers.interfaces.AgentFilterFactory;

public class TransportModeFilterFactory implements AgentFilterFactory, MobsimInitializedListener {

	private final Map<Id, MobsimAgent> agents;
	private final Set<String> modes;
	
	public TransportModeFilterFactory(Set<String> modes) {
		this.modes = modes;
		this.agents = new HashMap<Id, MobsimAgent>();
	}
	
	@Override
	public TransportModeFilter createAgentFilter() {
		return new TransportModeFilter(this.agents, this.modes);
	}

	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		this.agents.clear();
		
		QSim qSim = (QSim) e.getQueueSimulation();
		for (MobsimAgent mobsimAgent : qSim.getAgents()) {
			this.agents.put(mobsimAgent.getId(), mobsimAgent);
		}
	}

}
