/* *********************************************************************** *
 * project: org.matsim.*
 * TransitStop.java
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

package playground.marcel.pt.integration;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.core.api.facilities.ActivityFacility;
import org.matsim.core.mobsim.queuesim.DriverAgent;

public class TransitStopAgentTracker {

	private final Map<ActivityFacility, List<DriverAgent>> agentsAtStops = new HashMap<ActivityFacility, List<DriverAgent>>();
	private final List<DriverAgent> emptyList = new LinkedList<DriverAgent>();
	
	public void addAgentToStop(final DriverAgent agent, final ActivityFacility stop) {
		List<DriverAgent> agents = this.agentsAtStops.get(stop);
		if (agents == null) {
			agents = new LinkedList<DriverAgent>();
			this.agentsAtStops.put(stop, agents);
		}
		agents.add(agent);
	}
	
	public List<DriverAgent> getAgentsAtStop(final ActivityFacility stop) {
		List<DriverAgent> agents = this.agentsAtStops.get(stop);
		if (agents == null) {
			return this.emptyList;
		}
		return agents;
	}
}
