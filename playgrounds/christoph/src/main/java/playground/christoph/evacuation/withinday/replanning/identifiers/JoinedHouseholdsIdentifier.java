/* *********************************************************************** *
 * project: org.matsim.*
 * JoinedHouseholdsIdentifier.java
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

package playground.christoph.evacuation.withinday.replanning.identifiers;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.comparators.PersonAgentComparator;
import org.matsim.core.mobsim.qsim.qnetsimengine.JointDeparture;
import org.matsim.core.mobsim.qsim.qnetsimengine.JointDepartureOrganizer;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.vehicles.Vehicle;
import org.matsim.withinday.mobsim.MobsimDataProvider;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityAgentSelector;

import playground.christoph.evacuation.mobsim.HouseholdDepartureManager;
import playground.christoph.evacuation.mobsim.HouseholdDepartureManager.HouseholdDeparture;
import playground.christoph.evacuation.mobsim.HouseholdDepartureManager.JoinedHouseholdsContext;
import playground.christoph.evacuation.withinday.replanning.utils.HouseholdModeAssignment;
import playground.christoph.evacuation.withinday.replanning.utils.ModeAvailabilityChecker;
import playground.christoph.evacuation.withinday.replanning.utils.SelectHouseholdMeetingPoint;

/**
 *  Define which households will relocate to another (secure!) location
 *  at which time.
 *  
 *  Moreover it is decided which transport mode will be used for the evacuation.
 *  If a car is available, it is used. Otherwise the people will walk.
 *  
 *  @author cdobler
 */
public class JoinedHouseholdsIdentifier extends DuringActivityAgentSelector {

	private static final Logger log = Logger.getLogger(JoinedHouseholdsIdentifier.class);
	
	private final Households households;
	private final ActivityFacilities facilities;
	private final SelectHouseholdMeetingPoint selectHouseholdMeetingPoint;
	private final ModeAvailabilityChecker modeAvailabilityChecker;
	private final JointDepartureOrganizer jointDepartureOrganizer;
	private final MobsimDataProvider mobsimDataProvider;
	private final HouseholdDepartureManager householdDepartureManager;
	private final JoinedHouseholdsContext joinedHouseholdsContext;
	
	private final Map<Id, JointDeparture> jointDepartures;
	
	public JoinedHouseholdsIdentifier(Scenario scenario, SelectHouseholdMeetingPoint selectHouseholdMeetingPoint,
			ModeAvailabilityChecker modeAvailabilityChecker, JointDepartureOrganizer jointDepartureOrganizer,
			MobsimDataProvider mobsimDataProvider, HouseholdDepartureManager householdDepartureManager) {
		this.households = ((ScenarioImpl) scenario).getHouseholds();
		this.facilities = scenario.getActivityFacilities();
		this.selectHouseholdMeetingPoint = selectHouseholdMeetingPoint;
		this.modeAvailabilityChecker = modeAvailabilityChecker;
		this.jointDepartureOrganizer = jointDepartureOrganizer;
		this.mobsimDataProvider = mobsimDataProvider;
		this.householdDepartureManager = householdDepartureManager;
		this.joinedHouseholdsContext = householdDepartureManager.getJoinedHouseholdsContext();
		
		this.jointDepartures = new ConcurrentHashMap<Id, JointDeparture>();
	}

	@Override
	public Set<MobsimAgent> getAgentsToReplan(double time) {
		
		/*
		 * Clear maps for every time step.
		 */
		this.joinedHouseholdsContext.reset();
	
		Set<Id<Person>> agentIds = new HashSet<>();
		
		HouseholdDeparture householdDeparture = null;
		
		while ((householdDeparture = this.householdDepartureManager.peekPlannedDepartureFromQueue()) != null) {
			Id householdId = householdDeparture.getHouseholdId();
			
			/*
			 * If the household departs in the current time step.
			 */
			if (householdDeparture.getDepartureTime() == time) {
				
				Id<ActivityFacility> facilityId = householdDeparture.getFacilityId();
				Id<Link> linkId = this.facilities.getFacilities().get(facilityId).getLinkId();
				Id meetingPointId = selectHouseholdMeetingPoint.selectNextMeetingPoint(householdId);
				this.joinedHouseholdsContext.getHouseholdMeetingPointMap().put(householdId, meetingPointId);
				Household household = households.getHouseholds().get(householdId);

				/*
				 * Store mapping which is read by the replanners
				 */
				HouseholdModeAssignment assignment = modeAvailabilityChecker.getHouseholdModeAssignment(household, facilityId);	
				this.joinedHouseholdsContext.getDriverVehicleMap().putAll(assignment.getDriverVehicleMap());
				this.joinedHouseholdsContext.getTransportModeMap().putAll(assignment.getTransportModeMap());
				
				/*
				 * Create and add joint departures in the departure handler for the mobsim.
				 */
				createJointDepartures(assignment, linkId);
				
				// finally add agents to replanning set
				agentIds.addAll(household.getMemberIds());
				
				// remove entry from the queue ...
				this.householdDepartureManager.pollPlannedDepartureFromQueue();
				
				// ... and add it to the handledDeparturesQueue which is used for a consistency check
				this.householdDepartureManager.addHandledDepartureToQueue(householdDeparture);
			} else break;
		}

		// apply filter to remove agents that should not be replanned
		this.applyFilters(agentIds, time);

		Set<MobsimAgent> agentsToReplan = new TreeSet<MobsimAgent>(new PersonAgentComparator());
		for (Id<Person> agentId : agentIds) agentsToReplan.add(this.mobsimDataProvider.getAgent(agentId));
		
		return agentsToReplan;
	}

	private void createJointDepartures(HouseholdModeAssignment assignment, Id linkId) {
		Set<Id<Person>> driverIds = assignment.getDriverVehicleMap().keySet();
		
		for (Id<Person> driverId : driverIds) {
			Id<Vehicle> vehicleId = assignment.getDriverVehicleMap().get(driverId);
			Set<Id<Person>> passengerIds = new LinkedHashSet<>();
			for (Entry<Id, Id> entry : assignment.getPassengerVehicleMap().entrySet()) {
				if (entry.getValue().equals(vehicleId)) {
					passengerIds.add(entry.getKey());
				}
			}
			
			// create JointDeparture object
			JointDeparture jointDeparture = this.jointDepartureOrganizer.createJointDeparture(linkId, vehicleId, driverId, passengerIds);
			this.jointDepartures.put(driverId, jointDeparture);
			for (Id passengerId :passengerIds) {
				this.jointDepartures.put(passengerId, jointDeparture);
			}
		}
	}
	
	public JoinedHouseholdsContext getJoinedHouseholdsContext() {
		return this.joinedHouseholdsContext;
	}
	
	public JointDepartureOrganizer getJointDepartureOrganizer() {
		return this.jointDepartureOrganizer;
	}
	
	public JointDeparture getJointDeparture(Id agentId) {
		return this.jointDepartures.remove(agentId);
	}
	
	public void incPerformedDepartures(Id householdId) {
		this.householdDepartureManager.getScheduledHouseholdDepartures().get(householdId).incPerformedDepartures();
	}		
}