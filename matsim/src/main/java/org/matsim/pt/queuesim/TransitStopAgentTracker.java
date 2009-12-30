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

package org.matsim.pt.queuesim;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.transitSchedule.api.TransitStopFacility;


public class TransitStopAgentTracker {

	private final Map<TransitStopFacility, List<PassengerAgent>> agentsAtStops = new HashMap<TransitStopFacility, List<PassengerAgent>>();

	public void addAgentToStop(final PassengerAgent agent, final TransitStopFacility stop) {
		if (stop == null) {
			throw new NullPointerException("stop must not be null.");
		}
		List<PassengerAgent> agents = this.agentsAtStops.get(stop);
		if (agents == null) {
			agents = new LinkedList<PassengerAgent>();
			this.agentsAtStops.put(stop, agents);
		}
		agents.add(agent);
	}

	public void removeAgentFromStop(final PassengerAgent agent, final TransitStopFacility stop) {
		if (stop == null) {
			throw new NullPointerException("stop must not be null.");
		}
		List<PassengerAgent> agents = this.agentsAtStops.get(stop);
		if (agents != null) {
			agents.remove(agent);
		}
	}

	public List<PassengerAgent> getAgentsAtStop(final TransitStopFacility stop) {
		List<PassengerAgent> agents = this.agentsAtStops.get(stop);
		if (agents == null) {
			return Collections.emptyList();
		}
		return Collections.unmodifiableList(agents);
	}
}
