/* *********************************************************************** *
 * project: org.matsim.*
 * SelectHandledAgentsByProbability.java
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
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.agents.WithinDayAgent;
import org.matsim.withinday.replanning.identifiers.interfaces.AgentsToReplanIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.InitialIdentifier;

public class SelectHandledAgentsByProbability implements SimulationInitializedListener {

	private static final Logger log = Logger.getLogger(SelectHandledAgentsByProbability.class);
	
	protected Collection<WithinDayAgent> withinDayAgents;

	protected List<Tuple<AgentsToReplanIdentifier, Double>> identifierProbabilities;	// <Identifier, probability for handling an agent>

	public SelectHandledAgentsByProbability() {
		identifierProbabilities = new ArrayList<Tuple<AgentsToReplanIdentifier, Double>>();
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

		Random random = MatsimRandom.getLocalInstance();
		double probability;
		List<WithinDayAgent> agentsToHandle;

		for (Tuple<AgentsToReplanIdentifier, Double> tuple : identifierProbabilities) {
			agentsToHandle = new ArrayList<WithinDayAgent>();

			for (WithinDayAgent withinDayAgent : this.withinDayAgents) {
				probability = random.nextDouble();
				if (probability <= tuple.getSecond()) {
					agentsToHandle.add(withinDayAgent);
					
					if (tuple.getFirst() instanceof InitialIdentifier) initialIdentificationCounter++;
					else if (tuple.getFirst() instanceof DuringActivityIdentifier) activityIdentificationCounter++;
					else if (tuple.getFirst() instanceof DuringLegIdentifier) legIdentificationCounter++;
					else unknownIdentificationCounter++;
				}				
			}
			tuple.getFirst().setHandledAgent(agentsToHandle);
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
	
	public void addIdentifier(AgentsToReplanIdentifier identifier, double probability) { 
		identifierProbabilities.add(new Tuple<AgentsToReplanIdentifier, Double>(identifier, probability));
	}
	
	public void removelIdentifier(AgentsToReplanIdentifier identifier) {
		Iterator<Tuple<AgentsToReplanIdentifier, Double>> iter = identifierProbabilities.iterator();
		while (iter.hasNext()) {
			Tuple<AgentsToReplanIdentifier, Double> tuple = iter.next();
			if (identifier == tuple.getFirst()) iter.remove();
		}
	}
}
