/* *********************************************************************** *
 * project: org.matsim.*
 * HouseholdsUtils.java
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

package playground.christoph.evacuation.withinday.replanning.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.mobsim.framework.events.SimulationAfterSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationAfterSimStepListener;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.households.Household;

/**
 * Class that collects information on where the members of a household are located
 * and where they are planning to meet.
 * 
 * Has to be registered as an EventHandler. Registering as SimulationListener is optional
 * (is only required for some statistics).
 * 
 * @author cdobler
 */
public class HouseholdsUtils implements ActivityStartEventHandler, ActivityEndEventHandler, SimulationAfterSimStepListener {

	static final Logger log = Logger.getLogger(HouseholdsUtils.class);
	
	private Scenario scenario;
	private Map<Id, HouseholdInfo> householdInfoMap;		// <householdId, HouseholdInfo>
	private Map<Id, HouseholdInfo> personHouseholdInfoMap;	// <personId, HouseholdInfo> - just performance tuning...
	private Map<Id, Id> personLocationMap;					// <personId, facilityId>

	/* time since last "info" */
	private int infoTime = 0;
	private static final int INFO_PERIOD = 3600;
	
	public HouseholdsUtils(Scenario scenario) {
		this.scenario = scenario;
		
		this.reset(0);
	}

	public Id getMeetingPointId(Id personId) {
		return personHouseholdInfoMap.get(personId).getMeetingPointId();
	}
	
	public void setMeetingPoint(Id householdId, Id facilityId) {
		HouseholdInfo householdInfo = householdInfoMap.get(householdId);
		householdInfo.setMeetingPointId(facilityId);
		
		// reset members at meeting point counter
		householdInfo.resetMembersAtMeetingPoint();
		for (Id personId : householdInfo.household.getMemberIds()) {
			if (personLocationMap.get(personId).equals(facilityId)) {
				householdInfo.addPersonAtMeetingpoint(personId);
			}
		}
	}
	
	public boolean allMembersAtMeetingPoint(Id householdId) {
		return householdInfoMap.get(householdId).allMembersAtMeetingPoint();
	}
	

	@Override
	public void notifySimulationAfterSimStep(SimulationAfterSimStepEvent e) {
		
		if (e.getSimulationTime() >= this.infoTime) {
			this.infoTime += INFO_PERIOD;
			this.printStatistics();
		}
	}
	
	public void printStatistics() {
		int numHouseholds = householdInfoMap.size();
			
		int allAtMeetingPoint = 0;
		for (HouseholdInfo householdInfo : householdInfoMap.values()) {
			if(householdInfo.allMembersAtMeetingPoint()) allAtMeetingPoint++;
		}
			
		log.info("Households at Meeting Point statistics: #total Households=" + numHouseholds
			+ ", Households at Meeting Point=" + allAtMeetingPoint + "(" + ((100.0*allAtMeetingPoint)/numHouseholds) + "%)");
	}
	
	public void printClosingStatistics() {
		
		for (HouseholdInfo householdInfo : householdInfoMap.values()) {
			if(!householdInfo.allMembersAtMeetingPoint()) {
				log.info("household: " + householdInfo.household.getId());
				log.info("meeting point: " + householdInfo.meetingPointId);
				for (Id id : householdInfo.household.getMemberIds()) {
					log.info("member: " + id + ", position: " + this.personLocationMap.get(id));
				}
				log.info("");
			}
		}
	}
	
	@Override
	public void reset(int iteration) {
		personLocationMap = new HashMap<Id, Id>();
		householdInfoMap = new HashMap<Id, HouseholdInfo>();
		personHouseholdInfoMap = new HashMap<Id, HouseholdInfo>();
		
		for(Household household : ((ScenarioImpl) scenario).getHouseholds().getHouseholds().values()) {
			Person person = scenario.getPopulation().getPersons().get(household.getMemberIds().get(0));
			Activity firstActivity = (Activity) person.getSelectedPlan().getPlanElements().get(0);
			
			// if the first Activity is not from type "home" it seems to be a TTA Agent
//			if (!firstActivity.getType().equals("home")) continue;
			
			HouseholdInfo householdInfo = new HouseholdInfo(household);
			householdInfoMap.put(household.getId(), householdInfo);
			
			// By default, all households meet at home, where the first activity in each plan should be located.
			householdInfo.setMeetingPointId(firstActivity.getFacilityId());
			
			for (Id personId : household.getMemberIds()) {
				personHouseholdInfoMap.put(personId, householdInfo);
				householdInfo.addPersonAtMeetingpoint(personId);
				personLocationMap.put(personId, firstActivity.getFacilityId());
			}			
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		HouseholdInfo householdInfo = personHouseholdInfoMap.get(event.getPersonId());
		
		if (householdInfo.getMeetingPointId().equals(event.getFacilityId())) {
			householdInfo.addPersonAtMeetingpoint(event.getPersonId());
		}
		personLocationMap.put(event.getPersonId(), event.getFacilityId());
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		HouseholdInfo householdInfo = personHouseholdInfoMap.get(event.getPersonId());
		householdInfo.removePersonAtMeetingPoint(event.getPersonId());
		personLocationMap.remove(event.getPersonId());
	}
	
	private static class HouseholdInfo {
		private final Household household;
		private final Set<Id> membersAtMeetingPoint;
		private Id meetingPointId;
		
		public HouseholdInfo(Household household) {
			this.household = household;
			membersAtMeetingPoint = new HashSet<Id>();
		}
		
		public boolean addPersonAtMeetingpoint(Id id) {
			return membersAtMeetingPoint.add(id);
		}
		
		public boolean removePersonAtMeetingPoint(Id id) {
			return membersAtMeetingPoint.remove(id);
		}
		
		public void resetMembersAtMeetingPoint() {
			this.membersAtMeetingPoint.clear();
		}
		
		public void setMeetingPointId(Id id) {
			this.meetingPointId = id;
		}
		
		public Id getMeetingPointId() {
			return this.meetingPointId;
		}
		
		public boolean allMembersAtMeetingPoint() {
			return household.getMemberIds().size() == membersAtMeetingPoint.size();
		}
	}
}
