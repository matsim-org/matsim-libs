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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.withinday.replanning.identifiers.filter.CollectionAgentFilter;
import org.matsim.withinday.replanning.identifiers.filter.CollectionAgentFilterFactory;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityAgentSelector;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegAgentSelector;
import org.matsim.withinday.replanning.identifiers.interfaces.AgentSelector;
import org.matsim.withinday.replanning.identifiers.interfaces.InitialIdentifier;

public class SelectHandledAgentsByProbability implements MobsimInitializedListener {

	private static final Logger log = Logger.getLogger(SelectHandledAgentsByProbability.class);
	
	protected Collection<Id<Person>> withinDayAgents;

	protected List<Tuple<AgentSelector, Double>> identifierProbabilities;	// <Identifier, probability for handling an agent>

	public SelectHandledAgentsByProbability() {
		identifierProbabilities = new ArrayList<Tuple<AgentSelector, Double>>();
	}
	
	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
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
		Set<Id<Person>> agentsToHandle;

		for (Tuple<AgentSelector, Double> tuple : identifierProbabilities) {
			agentsToHandle = new LinkedHashSet<Id<Person>>();

			for (Id<Person> agentId : this.withinDayAgents) {
				probability = random.nextDouble();
				if (probability <= tuple.getSecond()) {
					agentsToHandle.add(agentId);
					
					if (tuple.getFirst() instanceof InitialIdentifier) initialIdentificationCounter++;
					else if (tuple.getFirst() instanceof DuringActivityAgentSelector) activityIdentificationCounter++;
					else if (tuple.getFirst() instanceof DuringLegAgentSelector) legIdentificationCounter++;
					else unknownIdentificationCounter++;
				}				
			}
			
			CollectionAgentFilter filter = new CollectionAgentFilterFactory(agentsToHandle).createAgentFilter();
			tuple.getFirst().addAgentFilter(filter);
		}

		log.info("Initial Replanning Identifiers have " + initialIdentificationCounter + " registered Agents.");
		log.info("During Activity Replanning Identifiers have " + activityIdentificationCounter + " registered Agents.");
		log.info("During Leg Replanning Identifiers have " + legIdentificationCounter + " registered Agents.");
		log.info("Unknown Replanning Identifiers have " + unknownIdentificationCounter + " registered Agents.");
	}

	private void collectAgents(QSim sim) {
		this.withinDayAgents = new ArrayList<>();

		for (MobsimAgent mobsimAgent : sim.getAgents()) {
			Logger.getLogger(this.getClass()).fatal("WithinDayAgent is no longer") ;
			System.exit(-1) ;
//			if (mobsimAgent instanceof PlanBasedWithinDayAgent) {
//				withinDayAgents.add(mobsimAgent.getId());
//			} else {
//				log.warn("MobsimAgent was expected to be from type WithinDayAgent, but was from type " + mobsimAgent.getClass().toString());
//			}
		}
		log.info("Collected " + withinDayAgents.size() + " registered WithinDayAgents.");
	}
	
	public void addIdentifier(AgentSelector identifier, double probability) { 
		identifierProbabilities.add(new Tuple<AgentSelector, Double>(identifier, probability));
	}
	
	public void removeIdentifier(AgentSelector identifier) {
		Iterator<Tuple<AgentSelector, Double>> iter = identifierProbabilities.iterator();
		while (iter.hasNext()) {
			Tuple<AgentSelector, Double> tuple = iter.next();
			if (identifier == tuple.getFirst()) iter.remove();
		}
	}

}
