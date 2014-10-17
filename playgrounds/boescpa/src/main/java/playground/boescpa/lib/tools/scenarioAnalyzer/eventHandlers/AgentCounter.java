/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.boescpa.lib.tools.scenarioAnalyzer.eventHandlers;

import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import playground.boescpa.lib.tools.scenarioAnalyzer.ScenarioAnalyzer;
import playground.boescpa.lib.tools.scenarioAnalyzer.spatialEventCutters.SpatialEventCutter;

import java.util.HashSet;
import java.util.Set;

/**
 * Counts the number of agents in an events file.
 *
 * @author boescpa
 */
public class AgentCounter implements ScenarioAnalyzerEventHandler, ActivityEndEventHandler {

	private final Set<String> numberOfAgents = new HashSet<>();

	public AgentCounter() {
		this.reset(0);
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		numberOfAgents.add(event.getPersonId().toString());
	}

	@Override
	public String createResults(SpatialEventCutter spatialEventCutter) {
		// todo-boescpa: Add spatial restriction...
		return "Number of Agents: " + this.numberOfAgents.size() + ScenarioAnalyzer.NL;
	}

	@Override
	public void reset(int iteration) {
		numberOfAgents.clear();
	}
}
