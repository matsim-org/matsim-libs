/* *********************************************************************** *
 * project: org.matsim.*
 * AgentsInEvacuationAreaActivityCounter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.analysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.facilities.ActivityFacility;

import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataProvider;
import playground.christoph.evacuation.mobsim.decisionmodel.EvacuationDecisionModel.Participating;

/**
 * Counts the number of agents (not) at home within an evacuated area.
 * 
 * @author cdobler
 */
public class AgentsInEvacuationAreaActivityCounter implements ActivityStartEventHandler, ActivityEndEventHandler,
		PersonStuckEventHandler, IterationEndsListener, MobsimInitializedListener {

	private static final Logger log = Logger.getLogger(AgentsInEvacuationAreaActivityCounter.class);

//	protected int binSize = 300; // by default 5 minutes
	protected int binSize = 60; // by default 1 minute
	protected int nofBins = 36 * 3600 / binSize; // by default 36 hours
	protected int currentBin = 0; // current time bin slot

	protected final Scenario scenario;
	protected final CoordAnalyzer coordAnalyzer;
	protected final DecisionDataProvider decisionDataProvider;
	protected final double scaleFactor;
		
	protected Set<Id> activityAgentsInEvacuationAreaAtHome;
	protected Set<Id> activityAgentsInEvacuationAreaNotAtHome;
	protected Map<Id, Id> homeFacilities;
	protected Map<Integer, Set<Id>> activityBinsUndefinedAtHome;
	protected Map<Integer, Set<Id>> activityBinsUndefinedNotAtHome;
	
	protected int[] activityBins;
	protected int[] activityBinsParticipatingAtHome;
	protected int[] activityBinsParticipatingNotAtHome;
	protected int[] activityBinsNotParticipatingAtHome;
	protected int[] activityBinsNotParticipatingNotAtHome;

	public AgentsInEvacuationAreaActivityCounter(Scenario scenario, CoordAnalyzer coordAnalyzer,
			DecisionDataProvider decisionDataProvider, double scaleFactor) {
		this.scenario = scenario;
		this.coordAnalyzer = coordAnalyzer;
		this.scaleFactor = scaleFactor;
		this.decisionDataProvider = decisionDataProvider;
		
		init();
	}

	private void init() {
		homeFacilities = new HashMap<Id, Id>();
		activityAgentsInEvacuationAreaAtHome = new HashSet<Id>();
		activityAgentsInEvacuationAreaNotAtHome = new HashSet<Id>();
		activityBinsUndefinedAtHome = new HashMap<Integer, Set<Id>>();
		activityBinsUndefinedNotAtHome = new HashMap<Integer, Set<Id>>();
		activityBins = new int[nofBins];
		activityBinsParticipatingAtHome = new int[nofBins];
		activityBinsParticipatingNotAtHome = new int[nofBins];
		activityBinsNotParticipatingAtHome = new int[nofBins];
		activityBinsNotParticipatingNotAtHome = new int[nofBins];
	}

	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		for (Person person : scenario.getPopulation().getPersons().values()) {

			if (person.getSelectedPlan().getPlanElements().size() == 0) continue;
			else {
				Activity activity = (Activity) person.getSelectedPlan().getPlanElements().get(0);

				boolean isAffected = false;
				if (activity.getFacilityId() != null) {
					ActivityFacility facility = scenario.getActivityFacilities().getFacilities().get(activity.getFacilityId());
					isAffected = this.coordAnalyzer.isFacilityAffected(facility);
					
					// we assume that each agents start its day at home
					homeFacilities.put(person.getId(), facility.getId());
				} else {
					Link link = scenario.getNetwork().getLinks().get(activity.getLinkId());
					isAffected = this.coordAnalyzer.isLinkAffected(link);
					log.warn("No facility defined in activity - taking coordinate from activity...");
				}

				if (isAffected) activityAgentsInEvacuationAreaAtHome.add(person.getId());
			}
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		updateBinData(event.getTime());

		boolean isAffected = false;
		if (event.getFacilityId() != null) {
			ActivityFacility facility = scenario.getActivityFacilities().getFacilities().get(event.getFacilityId());
			isAffected = this.coordAnalyzer.isFacilityAffected(facility);
		} else {
			Link link = scenario.getNetwork().getLinks().get(event.getLinkId());
			isAffected = this.coordAnalyzer.isLinkAffected(link);
			log.warn("No facilityId given - using link coordinates!");
		}

		if (isAffected) {
			if (event.getFacilityId().equals(homeFacilities.get(event.getPersonId()))) {
				activityAgentsInEvacuationAreaAtHome.add(event.getPersonId());
			} else activityAgentsInEvacuationAreaNotAtHome.add(event.getPersonId());
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		updateBinData(event.getTime());

		if (event.getFacilityId().equals(homeFacilities.get(event.getPersonId()))) {
			activityAgentsInEvacuationAreaAtHome.remove(event.getPersonId());
		} else activityAgentsInEvacuationAreaNotAtHome.remove(event.getPersonId());
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		updateBinData(event.getTime());
	}

	@Override
	public void reset(int iteration) {
		currentBin = 0;
		activityAgentsInEvacuationAreaAtHome.clear();
		activityAgentsInEvacuationAreaNotAtHome.clear();
		activityBinsUndefinedAtHome.clear();
		activityBinsUndefinedNotAtHome.clear();

		activityBins = new int[nofBins];
		activityBinsParticipatingAtHome = new int[nofBins];
		activityBinsParticipatingNotAtHome = new int[nofBins];
		activityBinsNotParticipatingAtHome = new int[nofBins];
		activityBinsNotParticipatingNotAtHome = new int[nofBins];
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		
		// ensure, that the last bin is written
		updateBinData(currentBin + 1);
		
		// now process data that has been collected before agents have decided whether to evacuate or not
		postProcessUndefinedAgents();
		
		// debug
		for (Id id : activityAgentsInEvacuationAreaNotAtHome) {
			Participating participating = this.decisionDataProvider.getPersonDecisionData(id).getParticipating();
			if (participating == Participating.TRUE) {
				log.warn("Person " + id.toString() + " is still inside the evacuation area but should have left.");
			} else if (participating == Participating.FALSE) {
				log.warn("Person " + id.toString() + " is still inside the evacuation area but has not returned home yet.");
			} else {
				log.warn("Person " + id.toString() + " is still inside the evacuation area and has an undefined evacuation participation state.");
			}
		}
		
		String fileName = null;

		AgentsInEvacuationAreaActivityWriter writer = new AgentsInEvacuationAreaActivityWriter(this.binSize, event.getIteration());

		/*
		 * write text files
		 */
		fileName = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "activitiyAgentsInEvacuationArea.txt");
		writer.write(fileName, activityBins, activityBinsParticipatingAtHome, activityBinsParticipatingNotAtHome, 
				activityBinsNotParticipatingAtHome, activityBinsNotParticipatingNotAtHome);
		
		/*
		 * write activity performing graphs
		 */
		fileName = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "activityAgentsInEvacuationArea.png");
		writer.writeGraphic(fileName, activityBins, activityBinsParticipatingAtHome, activityBinsParticipatingNotAtHome, 
				activityBinsNotParticipatingAtHome, activityBinsNotParticipatingNotAtHome);
	}

	private int getBinIndex(final double time) {
		int bin = (int) (time / this.binSize);
		if (bin >= this.nofBins) {
			return this.nofBins;
		}
		return bin;
	}

	/*
	 * If the given time is in a later time bin, we can calculate the values for
	 * the past time bins.
	 */
	private void updateBinData(double time) {
		int binIndex = getBinIndex(time);
		if (binIndex > currentBin) {

			int participatingActivityAgentsAtHome = 0;
			int notParticipatingActivityAgentsAtHome = 0;
			Set<Id> undefinedActivityAgentsInEvacuationAreaAtHome = new HashSet<Id>();
			for (Id activityAgentId : activityAgentsInEvacuationAreaAtHome) {
				Participating participating = this.decisionDataProvider.getPersonDecisionData(activityAgentId).getParticipating();
				if (participating == Participating.TRUE) participatingActivityAgentsAtHome++;
				else if (participating == Participating.FALSE) notParticipatingActivityAgentsAtHome++;
				else if (participating == Participating.UNDEFINED) undefinedActivityAgentsInEvacuationAreaAtHome.add(activityAgentId);
				else throw new RuntimeException("Unexpected participation state found: " + participating.toString());
			}
			
			int participatingActivityAgentsNotAtHome = 0;
			int notParticipatingActivityAgentsNotAtHome = 0;
			Set<Id> undefinedActivityAgentsInEvacuationAreaNotAtHome = new HashSet<Id>();
			for (Id activityAgentId : activityAgentsInEvacuationAreaNotAtHome) {
				Participating participating = this.decisionDataProvider.getPersonDecisionData(activityAgentId).getParticipating();
				if (participating == Participating.TRUE) participatingActivityAgentsNotAtHome++;
				else if (participating == Participating.FALSE) notParticipatingActivityAgentsNotAtHome++;
				else if (participating == Participating.UNDEFINED) undefinedActivityAgentsInEvacuationAreaNotAtHome.add(activityAgentId);
				else throw new RuntimeException("Unexpected participation state found: " + participating.toString());
			}
			
			// for all not processed past bins
			for (int index = currentBin; index < binIndex; index++) {
				int activityAgentsInEvacuationArea = activityAgentsInEvacuationAreaAtHome.size() + activityAgentsInEvacuationAreaNotAtHome.size();
				
				activityBins[index] = (int) Math.round(activityAgentsInEvacuationArea * scaleFactor);
				activityBinsParticipatingAtHome[index] = (int) Math.round(participatingActivityAgentsAtHome * scaleFactor);
				activityBinsParticipatingNotAtHome[index] = (int) Math.round(participatingActivityAgentsNotAtHome * scaleFactor);
				activityBinsNotParticipatingAtHome[index] = (int) Math.round(notParticipatingActivityAgentsAtHome * scaleFactor);
				activityBinsNotParticipatingNotAtHome[index] = (int) Math.round(notParticipatingActivityAgentsNotAtHome * scaleFactor);
				
				activityBinsUndefinedAtHome.put(index, undefinedActivityAgentsInEvacuationAreaAtHome);
				activityBinsUndefinedNotAtHome.put(index, undefinedActivityAgentsInEvacuationAreaNotAtHome);
			}
		}

		currentBin = binIndex;
	}
	
	private void postProcessUndefinedAgents() {
		
		Set<Integer> bins = activityBinsUndefinedAtHome.keySet();
		for (int index : bins) {
			
			int participatingActivityAgentsAtHome = 0;
			int notParticipatingActivityAgentsAtHome = 0;
			for (Id activityAgentId : activityBinsUndefinedAtHome.get(index)) {			
				Participating participating = this.decisionDataProvider.getPersonDecisionData(activityAgentId).getParticipating();
				if (participating == Participating.TRUE) participatingActivityAgentsAtHome++;
				else if (participating == Participating.FALSE) notParticipatingActivityAgentsAtHome++;
				else if (participating == Participating.UNDEFINED) {
					throw new RuntimeException("Participation state still UNDEFINED: " + activityAgentId.toString());
				}
				else throw new RuntimeException("Unexpected participation state found: " + participating.toString());
			}
			
			int participatingActivityAgentsNotAtHome = 0;
			int notParticipatingActivityAgentsNotAtHome = 0;
			for (Id activityAgentId : activityBinsUndefinedNotAtHome.get(index)) {			
				Participating participating = this.decisionDataProvider.getPersonDecisionData(activityAgentId).getParticipating();
				if (participating == Participating.TRUE) participatingActivityAgentsNotAtHome++;
				else if (participating == Participating.FALSE) notParticipatingActivityAgentsNotAtHome++;
				else if (participating == Participating.UNDEFINED) {
					throw new RuntimeException("Participation state still UNDEFINED: " + activityAgentId.toString());
				}
				else throw new RuntimeException("Unexpected participation state found: " + participating.toString());
			}
			
			// update activity bin arrays
			activityBinsParticipatingAtHome[index] = activityBinsParticipatingAtHome[index] + (int) Math.round(participatingActivityAgentsAtHome * scaleFactor);
			activityBinsParticipatingNotAtHome[index] = activityBinsParticipatingNotAtHome[index] + (int) Math.round(participatingActivityAgentsNotAtHome * scaleFactor);
			activityBinsNotParticipatingAtHome[index] = activityBinsNotParticipatingAtHome[index] + (int) Math.round(notParticipatingActivityAgentsAtHome * scaleFactor);
			activityBinsNotParticipatingNotAtHome[index] = activityBinsNotParticipatingNotAtHome[index] +(int) Math.round(notParticipatingActivityAgentsNotAtHome * scaleFactor);			
		}
	}
}