/* *********************************************************************** *
 * project: org.matsim.*
 * HouseholdsTracker.java
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

package playground.christoph.evacuation.mobsim;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
import org.matsim.core.api.experimental.events.PersonLeavesVehicleEvent;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.households.Household;
import org.matsim.households.Households;

import playground.christoph.evacuation.mobsim.Tracker.Position;

public class HouseholdsTracker extends AgentsTracker implements MobsimAfterSimStepListener {

	private static final Logger log = Logger.getLogger(HouseholdsTracker.class);
	
	/* time since last "info" */
	private int infoTime = 0;
	private static final int INFO_PERIOD = 3600;
	
	private Set<Id> joinedInLastTimeStep = new LinkedHashSet<Id>();
	private Set<Id> updatedInLastTimeStep = new LinkedHashSet<Id>();
	private Set<Id> updatedInCurrentTimeStep = new LinkedHashSet<Id>();
	private final Set<Id> joinedHouseholds = new HashSet<Id>();
	
	private final Map<Id, Id> personHouseholdMap;
	private final Map<Id, HouseholdPosition> householdPositions;
	
	public HouseholdsTracker(Scenario scenario) {
		super(scenario);
		
		this.personHouseholdMap = new HashMap<Id, Id>();
		this.householdPositions = new HashMap<Id, HouseholdPosition>();
	}
	
	public Id getPersonsHouseholdId(Id personId) {
		return this.personHouseholdMap.get(personId);
	}
	
	public HouseholdPosition getHouseholdPosition(Id householdId) {
		return this.householdPositions.get(householdId);
	}
	
	public Set<Id> getJoinedHouseholds() {
		return Collections.unmodifiableSet(this.joinedHouseholds);
	}
	
	public Set<Id> getHouseholdsJoinedInLastTimeStep() {
		return Collections.unmodifiableSet(this.joinedInLastTimeStep);
	}
	
	public Set<Id> getHouseholdsUpdatedInLastTimeStep() {
		return Collections.unmodifiableSet(this.updatedInLastTimeStep);
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		super.handleEvent(event);
		updatedInCurrentTimeStep.add(this.personHouseholdMap.get(event.getPersonId()));
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		super.handleEvent(event);
		updatedInCurrentTimeStep.add(this.personHouseholdMap.get(event.getPersonId()));
	}
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		super.handleEvent(event);
		updatedInCurrentTimeStep.add(this.personHouseholdMap.get(event.getPersonId()));
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		super.handleEvent(event);
		updatedInCurrentTimeStep.add(this.personHouseholdMap.get(event.getPersonId()));
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		super.handleEvent(event);
		updatedInCurrentTimeStep.add(this.personHouseholdMap.get(event.getPersonId()));
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		super.handleEvent(event);
		updatedInCurrentTimeStep.add(this.personHouseholdMap.get(event.getPersonId()));
	}
	
	@Override
	public void handleEvent(ActivityStartEvent event) {
		super.handleEvent(event);
		updatedInCurrentTimeStep.add(this.personHouseholdMap.get(event.getPersonId()));
	}
	
	@Override
	public void handleEvent(ActivityEndEvent event) {
		super.handleEvent(event);
		updatedInCurrentTimeStep.add(this.personHouseholdMap.get(event.getPersonId()));
	}

	@Override
	public void reset(int iteration) {
		super.reset(iteration);
		this.updatedInCurrentTimeStep.clear();
		this.updatedInLastTimeStep.clear();
		this.personHouseholdMap.clear();
		this.householdPositions.clear();
	}
	
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		super.notifyMobsimInitialized(e);
		
		Households households = ((ScenarioImpl) scenario).getHouseholds();
		for (Household household : households.getHouseholds().values()) {
			
			HouseholdPosition householdPosition = new HouseholdPosition();			
			for (Id personId : household.getMemberIds()) {
				personHouseholdMap.put(personId, household.getId());
				
				AgentPosition agentPosition = this.getAgentPosition(personId);
				householdPosition.addAgentPosition(agentPosition);
			}
			householdPosition.update();
			
			if (householdPosition.isHouseholdJoined()) this.joinedHouseholds.add(household.getId());
			
			// only observe household if its member size is > 0
			if (household.getMemberIds().size() > 0) householdPositions.put(household.getId(), householdPosition);
		}
	}
	
	@Override
	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
		
		this.joinedInLastTimeStep = new LinkedHashSet<Id>();
		
		for (Id householdId : this.updatedInCurrentTimeStep) {
			HouseholdPosition householdPosition = householdPositions.get(householdId);
			householdPosition.update();
			if (householdPosition.isHouseholdJoined()) {
				/*
				 * If the set already contains the household, the "add" method returns false.
				 * If the household was not joined, it is added to the joinedInLastTimeStep set.
				 */
				boolean wasJoined = !this.joinedHouseholds.add(householdId);
				if(!wasJoined) this.joinedInLastTimeStep.add(householdId);
			}
			else this.joinedHouseholds.remove(householdId);
		}
		
		this.updatedInLastTimeStep = this.updatedInCurrentTimeStep;
		this.updatedInCurrentTimeStep = new LinkedHashSet<Id>();
				
		if (e.getSimulationTime() >= this.infoTime) {
			this.infoTime += INFO_PERIOD;
			this.printStatistics();
		}
	}
	
	public void printStatistics() {
		int numHouseholds = householdPositions.size();
		
		int split = 0;
		int joined = 0;
		int joinedOnLink = 0;
		int joinedInVehicles = 0;
		int joinedInFacility = 0;
		int joinedUndefined = 0;
		for (HouseholdPosition householdPosition : householdPositions.values()) {
			if (householdPosition.isHouseholdJoined()) {
				joined++;
				if (householdPosition.getPositionType() == Position.LINK) {
					joinedOnLink++;
				} else if (householdPosition.getPositionType() == Position.VEHICLE) {
					joinedInVehicles++;
				} else if (householdPosition.getPositionType() == Position.FACILITY) {
					joinedInFacility++;
				} else joinedUndefined++;
			} else split++;			
		}
		
		DecimalFormat df = new DecimalFormat("#.##");
		log.info("Households Statistics: # total Households=" + numHouseholds
			+ ", # total joined Households=" + joined + "(" + df.format((100.0*joined)/numHouseholds) + "%)"
			+ ", # on Link joined Households=" + joinedOnLink + "(" + df.format((100.0*joinedOnLink)/numHouseholds) + "%)"
			+ ", # in Vehicle joined Households=" + joinedInVehicles + "(" + df.format((100.0*joinedInVehicles)/numHouseholds) + "%)"
			+ ", # in Facility joined Households=" + joinedInFacility + "(" + df.format((100.0*joinedInFacility)/numHouseholds) + "%)"
			+ ", # undefined joined Households=" + joinedUndefined + "(" + df.format((100.0*joinedUndefined)/numHouseholds) + "%)"
			+ ", # split Households=" + split + "(" + df.format((100.0*split)/numHouseholds) + "%)");
	}

}