/* *********************************************************************** *
 * project: org.matsim.*
 * SelectAllAgentsToBeHandled.java
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

package org.matsim.withinday.replanning.identifiers.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.agents.WithinDayAgent;
import org.matsim.withinday.replanning.identifiers.interfaces.AgentsToReplanIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.InitialIdentifier;

public class SelectAllAgentsToBeHandled implements SimulationInitializedListener {

	private static final Logger log = Logger.getLogger(SelectAllAgentsToBeHandled.class);
	
	protected Collection<WithinDayAgent> withinDayAgents;

	protected List<AgentsToReplanIdentifier> identifiers;
	
	public SelectAllAgentsToBeHandled() {
		identifiers = new ArrayList<AgentsToReplanIdentifier>();
	}

	@Override
	public void notifySimulationInitialized(SimulationInitializedEvent e) {
		collectAgents((QSim) e.getQueueSimulation());
		selectHandledAgents();
	}
	
	private void selectHandledAgents() {
		int initialIdentificationCounter = 0;
		int activityIdentificationCounter = 0;
		int legIdentificationCounter = 0;
		int unknownIdentificationCounter = 0;
		
		for (AgentsToReplanIdentifier identifier : identifiers) {
			identifier.setHandledAgent(this.withinDayAgents);
			
			if (identifier instanceof InitialIdentifier) initialIdentificationCounter++;
			else if (identifier instanceof DuringActivityIdentifier) activityIdentificationCounter++;
			else if (identifier instanceof DuringLegIdentifier) legIdentificationCounter++;
			else unknownIdentificationCounter++;
		}

		log.info("Initial Replanning Identifiers have " + initialIdentificationCounter + " registered Agents.");
		log.info("During Activity Replanning Identifiers have " + activityIdentificationCounter + " registered Agents.");
		log.info("During Leg Replanning Identifiers have " + legIdentificationCounter + " registered Agents.");
		log.info("Unknown Replanning Identifiers have " + unknownIdentificationCounter + " registered Agents.");
	}

	private void collectAgents(QSim sim) {
		this.withinDayAgents = new ArrayList<WithinDayAgent>();

		for (MobsimAgent mobsimAgent : sim.getAgents()) {
			if (mobsimAgent instanceof WithinDayAgent) {
				withinDayAgents.add((WithinDayAgent) mobsimAgent);
			} else {
				log.warn("MobsimAgent was expected to be from type WithinDayAgent, but was from type " + mobsimAgent.getClass().toString());
			}
		}
		log.info("Collected " + withinDayAgents.size() + " registered WithinDayAgents.");
	}
	
	public void addIdentifier(AgentsToReplanIdentifier identifier) {
		if (!identifiers.contains(identifier)) identifiers.add(identifier);
	}
	
	public void removeIdentifier(AgentsToReplanIdentifier identifier) {
		identifiers.remove(identifier);
	}
	
}
