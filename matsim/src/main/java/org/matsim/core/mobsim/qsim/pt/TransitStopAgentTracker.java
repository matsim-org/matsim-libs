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

package org.matsim.core.mobsim.qsim.pt;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;

/**
 * @author mrieser
 */
public class TransitStopAgentTracker {

	private final Map<Id, List<PTPassengerAgent>> agentsAtStops = new HashMap<Id, List<PTPassengerAgent>>();

	public void addAgentToStop(final PTPassengerAgent agent, final Id stopId) {
		if (stopId == null) {
			throw new NullPointerException("stop must not be null.");
		}
		List<PTPassengerAgent> agents = this.agentsAtStops.get(stopId);
		if (agents == null) {
			agents = new LinkedList<PTPassengerAgent>();
			this.agentsAtStops.put(stopId, agents);
		}
		agents.add(agent);
	}

	public void removeAgentFromStop(final PTPassengerAgent agent, final Id stopId) {
		if (stopId == null) {
			throw new NullPointerException("stopId must not be null.");
		}
		List<PTPassengerAgent> agents = this.agentsAtStops.get(stopId);
		if (agents != null) {
			agents.remove(agent);
		}
	}

	public List<PTPassengerAgent> getAgentsAtStop(final Id stopId) {
		List<PTPassengerAgent> agents = this.agentsAtStops.get(stopId);
		if (agents == null) {
			return Collections.emptyList();
		}
		return Collections.unmodifiableList(agents);
	}

	public Map<Id, List<PTPassengerAgent>> getAgentsAtStop() {
		return this.agentsAtStops;
	}
}
