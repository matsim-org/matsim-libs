/* *********************************************************************** *
 * project: org.matsim.*
 * ReplanningFlagInitializer.java
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

package org.matsim.withinday.controller;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.withinday.mobsim.ReplanningManager;
import org.matsim.withinday.mobsim.WithinDayPersonAgent;

public class ReplanningFlagInitializer implements SimulationInitializedListener {

	private static final Logger log = Logger.getLogger(ReplanningFlagInitializer.class);
	
	protected ReplanningManager replanningManager;
	protected Map<Id, WithinDayPersonAgent> withinDayPersonAgents;

	protected Map<Id, Double> initialReplannerProbabilities;	// <replannerId, probability for replanning>
	protected Map<Id, Double> duringActivityReplannerProbabilities;	// <replannerId, probability for replanning>
	protected Map<Id, Double> duringLegReplannerProbabilities;	// <replannerId, probability for replanning>

	// TODO: find a better name! 
	public ReplanningFlagInitializer(ReplanningManager replanningManager) {
		this.replanningManager = replanningManager;
		
		initialReplannerProbabilities = new TreeMap<Id, Double>();
		duringActivityReplannerProbabilities = new TreeMap<Id, Double>();
		duringLegReplannerProbabilities = new TreeMap<Id, Double>();
	}

	@Override
	public void notifySimulationInitialized(SimulationInitializedEvent e) {
		collectAgents((QSim) e.getQueueSimulation());
		setReplanningFlags();
	}
	
	private void setReplanningFlags() {
		int noReplanningCounter = 0;
		int initialReplanningCounter = 0;
		int actEndReplanningCounter = 0;
		int leaveLinkReplanningCounter = 0;

		Random random = MatsimRandom.getLocalInstance();

		for (WithinDayPersonAgent withinDayPersonAgent : this.withinDayPersonAgents.values()) {
			double probability;
			boolean noReplanning = true;

			// initial replanning
			for (Entry<Id, Double> entry : initialReplannerProbabilities.entrySet()) {
				probability = random.nextDouble();
				if (probability > entry.getValue());
				else {
					withinDayPersonAgent.getReplannerAdministrator().addWithinDayReplanner(entry.getKey());
					noReplanning = false;
					initialReplanningCounter++;
				}
			}

			// during activity replanning
			for (Entry<Id, Double> entry : duringActivityReplannerProbabilities.entrySet()) {
				probability = random.nextDouble();
				if (probability > entry.getValue());
				else {
					withinDayPersonAgent.getReplannerAdministrator().addWithinDayReplanner(entry.getKey());
					noReplanning = false;
					actEndReplanningCounter++;
				}				
			}

			// during link replanning
			for (Entry<Id, Double> entry : duringLegReplannerProbabilities.entrySet()) {
				probability = random.nextDouble();
				if (probability > entry.getValue());
				else {
					withinDayPersonAgent.getReplannerAdministrator().addWithinDayReplanner(entry.getKey());
					noReplanning = false;
					leaveLinkReplanningCounter++;
				}				
			}

			// if non of the replanning modules was activated
			if (noReplanning) noReplanningCounter++;

			// (de)activate replanning if it is not used
			if (initialReplanningCounter == 0) this.replanningManager.doInitialReplanning(false);
			else this.replanningManager.doInitialReplanning(true);

			if (actEndReplanningCounter == 0) this.replanningManager.doActEndReplanning(false);
			else this.replanningManager.doActEndReplanning(true);

			if (leaveLinkReplanningCounter == 0) this.replanningManager.doLeaveLinkReplanning(false);
			else this.replanningManager.doLeaveLinkReplanning(true);
		}

		/*
		 * How to print replanning statistics?
		 * - Count per replanner?
		 * - Count per replanning type?
		 */
//		log.info("Initial Replanning Probability: " + withinDayControler.pInitialReplanning);
//		log.info("Act End Replanning Probability: " + withinDayControler.pActEndReplanning);
//		log.info("Leave Link Replanning Probability: " + withinDayControler.pLeaveLinkReplanning);

//		double numPersons = withinDayControler.getPopulation().getPersons().size();
//		log.info(noReplanningCounter + " persons don't replan their Plans ("+ noReplanningCounter / numPersons * 100.0 + "%)");
//		log.info(initialReplanningCounter + " persons replan their plans initially (" + initialReplanningCounter / numPersons * 100.0 + "%)");
//		log.info(actEndReplanningCounter + " persons replan their plans after an activity (" + actEndReplanningCounter / numPersons * 100.0 + "%)");
//		log.info(leaveLinkReplanningCounter + " persons replan their plans at each node (" + leaveLinkReplanningCounter / numPersons * 100.0 + "%)");
	}

	private void collectAgents(QSim sim) {
		this.withinDayPersonAgents = new TreeMap<Id, WithinDayPersonAgent>();

		for (MobsimAgent mobsimAgent : sim.getAgents()) {
			if (mobsimAgent instanceof PersonAgent) {
				PersonAgent personAgent = (PersonAgent) mobsimAgent;
				withinDayPersonAgents.put(personAgent.getId(), (WithinDayPersonAgent) personAgent);
			} else {
				log.warn("MobsimAgent was expected to be from type PersonAgent, but was from type " + mobsimAgent.getClass().toString());
			}
		}
	}
	
	public void addInitialReplanner(Id withinDayReplannerId, double probability) { 
		initialReplannerProbabilities.put(withinDayReplannerId, probability);
	}
	public void removeInitialReplanner(Id withinDayReplannerId) {
		initialReplannerProbabilities.remove(withinDayReplannerId);
	}
	
	public void addDuringActivityReplanner(Id withinDayReplannerId, double probability) { 
		duringActivityReplannerProbabilities.put(withinDayReplannerId, probability);
	}
	public void removeDuringActivityReplanner(Id withinDayReplannerId) {
		duringActivityReplannerProbabilities.remove(withinDayReplannerId);
	}
	
	public void addDuringLegReplanner(Id withinDayReplannerId, double probability) { 
		duringLegReplannerProbabilities.put(withinDayReplannerId, probability);
	}
	public void removeDuringLegReplanner(Id withinDayReplannerId) {
		duringLegReplannerProbabilities.remove(withinDayReplannerId);
	}

}
