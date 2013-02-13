/* *********************************************************************** *
 * project: org.matsim.*
 * LinkFilterFactory.java
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

public class LinkFilterFactory implements AgentFilterFactory, MobsimInitializedListener {

	private final Map<Id, MobsimAgent> agents;
	private final Set<Id> links;
	
	public LinkFilterFactory(Set<Id> links) {
		this.links = links;
		this.agents = new HashMap<Id, MobsimAgent>();
	}
	
	@Override
	public LinkFilter createAgentFilter() {
		return new LinkFilter(this.agents, this.links);
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
