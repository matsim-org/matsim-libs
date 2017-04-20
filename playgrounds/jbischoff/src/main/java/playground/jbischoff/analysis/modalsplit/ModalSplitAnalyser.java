/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.jbischoff.analysis.modalsplit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.mutable.MutableInt;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;

import com.google.inject.Inject;

/**
 * @author  jbischoff
 * Counts the trips per iteration per mode.
 * Any activity ending on "interaction" wil be associated with the mode before that word. 
 * While this is not always useful, it provides an easy solution (as long as no one sets any activities with the word interaction :))"
 * 
 * A person id specific filter is also implemented.
 *  
 */
/**
 *
 */
public class ModalSplitAnalyser
		implements ActivityStartEventHandler, ActivityEndEventHandler, PersonDepartureEventHandler {

	private Map<String,MutableInt> tripsPerMode = new HashMap<>();
	private Map<Id<Person>,String> previousActivity = new HashMap<>();
	private Map<Id<Person>,String> previousModes = new HashMap<>();
	private Set<Id<Person>> planAgents = new HashSet<>();
	private Set<String> agentGroups = new HashSet<>();
	private boolean treatPureTransitWalksAsWalk = true;
	
	
	 /**
	 * 
	 */
	@Inject
	public ModalSplitAnalyser(Scenario scenario) {
		this.planAgents = scenario.getPopulation().getPersons().keySet();
	}
	
	/* (non-Javadoc)
	 * @see org.matsim.core.events.handler.EventHandler#reset(int)
	 */
	@Override
	public void reset(int iteration) {
		tripsPerMode.clear();
		previousActivity.clear();
		previousModes.clear();
		
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler#handleEvent(org.matsim.api.core.v01.events.PersonDepartureEvent)
	 */
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		previousModes.put(event.getPersonId(), event.getLegMode());
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.ActivityEndEventHandler#handleEvent(org.matsim.api.core.v01.events.ActivityEndEvent)
	 */
	@Override
	public void handleEvent(ActivityEndEvent event) {
		previousActivity.put(event.getPersonId(), event.getActType());
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.ActivityStartEventHandler#handleEvent(org.matsim.api.core.v01.events.ActivityStartEvent)
	 */
	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (!planAgents.contains(event.getPersonId())) return;
		if (!isRelevantAgent(event.getPersonId())) return;
		
		String type = event.getActType();
		
		if (!isInteractionActivity(type)){
			String previousAct = previousActivity.remove(event.getPersonId());
			String mode = previousModes.remove(event.getPersonId());
			
			if (isInteractionActivity(previousAct)){
				mode = previousAct.replaceAll(" interaction", "");
			} 
			if (mode.equals(TransportMode.transit_walk)){
				if (treatPureTransitWalksAsWalk)
				mode = TransportMode.walk;
			}
			if (!tripsPerMode.containsKey(mode)){
				tripsPerMode.put(mode, new MutableInt());
			}
			tripsPerMode.get(mode).increment();
		}
		
	}
	
	private boolean isInteractionActivity(String act){
		if (act!=null){
		return act.endsWith(" interaction");
		}
		else return false;
	}
	
	public void addAgentGroup(String prefix){
		this.agentGroups.add(prefix);
	}
	
	private boolean isRelevantAgent(Id<Person> personId){
		if (agentGroups.size()==0) return true;
		else {
			String person = personId.toString();
			for (String prefix : agentGroups){
				if (person.startsWith(prefix)) return true;
			}
			return false;
		}
	}
	
	/**
	 * @return the tripsPerMode
	 */
	public Map<String, MutableInt> getTripsPerMode() {
		return tripsPerMode;
	}
	
	/**
	 * @return the treatPureTransitWalksAsWalk
	 */
	public boolean isTreatPureTransitWalksAsWalk() {
		return treatPureTransitWalksAsWalk;
	}
	
	/**
	 * @param treatPureTransitWalksAsWalk the treatPureTransitWalksAsWalk to set
	 */
	public void setTreatPureTransitWalksAsWalk(boolean treatPureTransitWalksAsWalk) {
		this.treatPureTransitWalksAsWalk = treatPureTransitWalksAsWalk;
	}

}
