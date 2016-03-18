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

package playground.polettif.boescpa.analysis.scenarioAnalyzer.eventHandlers;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.network.Network;
import playground.polettif.boescpa.analysis.scenarioAnalyzer.ScenarioAnalyzer;
import playground.polettif.boescpa.analysis.spatialCutters.SpatialCutter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Counts the number of agents in an events file.
 *
 * @author boescpa
 */
public class AgentCounter extends ScenarioAnalyzerEventHandler implements ActivityEndEventHandler {

	private final List<String[]> agentsAndLinks = new ArrayList<>();
	private final Network network;

	public AgentCounter(Network network) {
		this.network = network;
		this.reset(0);
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (!event.getPersonId().toString().contains("pt")) {
			agentsAndLinks.add(new String[]{event.getPersonId().toString(), event.getLinkId().toString()});
		}
	}

	@Override
	public String createResults(SpatialCutter spatialEventCutter, int scaleFactor) {
		Set<String> agents = new HashSet<>();
		for (String[] vals : agentsAndLinks) {
			if (spatialEventCutter.spatiallyConsideringLink(network.getLinks().get(Id.createLinkId(vals[1])))) {
				agents.add(vals[0]);
			}
		}
		return "Number of Agents: " + (scaleFactor * agents.size()) + ScenarioAnalyzer.NL;
	}

	@Override
	public void reset(int iteration) {
		agentsAndLinks.clear();
	}
}
