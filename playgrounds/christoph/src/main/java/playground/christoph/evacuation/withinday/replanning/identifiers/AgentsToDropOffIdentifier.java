/* *********************************************************************** *
 * project: org.matsim.*
 * AgentsToDropOffIdentifier.java
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

package playground.christoph.evacuation.withinday.replanning.identifiers;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.mobsim.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.core.mobsim.qsim.comparators.PersonAgentComparator;
import org.matsim.core.mobsim.qsim.qnetsimengine.JointDepartureOrganizer;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.withinday.mobsim.MobsimDataProvider;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;
import org.matsim.withinday.trafficmonitoring.LinkEnteredProvider;

/**
 * Identifies agents that are passengers in a vehicle which has as different
 * destination as they have. The leave the vehicle after it has left the affected
 * area.
 * 
 * So far, agents leave the vehicle at the very first link which is not affected.
 * Therefore, we only have to ensure that they enter only vehicles which have their
 * destination in the secure area. If we keep agents longer in a vehicle, we have to
 * ensure that they leave the vehicle if it arrives at its destination. Otherwise
 * they would wait their forever for another driver. 
 * 
 * @author cdobler
 */
public class AgentsToDropOffIdentifier extends DuringLegIdentifier { 

	private final MobsimDataProvider mobsimDataProvider;
	private final LinkEnteredProvider linkEnteredProvider;
	private final JointDepartureOrganizer jointDepartureOrganizer;
	
	/*package*/ AgentsToDropOffIdentifier(MobsimDataProvider mobsimDataProvider, LinkEnteredProvider linkEnteredProvider,
			JointDepartureOrganizer jointDepartureOrganizer) {
		this.mobsimDataProvider = mobsimDataProvider;
		this.linkEnteredProvider = linkEnteredProvider;
		this.jointDepartureOrganizer = jointDepartureOrganizer;
	}

	public Set<PlanBasedWithinDayAgent> getAgentsToReplan(double time) {
		
		// Get all agents that have just entered a new link.
		Map<Id, Id> linkEnteredAgents = new HashMap<Id, Id>(linkEnteredProvider.getLinkEnteredAgentsInLastTimeStep());	
		
		// Apply filter to remove agents that should not be replanned.
		this.applyFilters(linkEnteredAgents.keySet(), time);
		
		Set<PlanBasedWithinDayAgent> agentsToDropOff = new TreeSet<PlanBasedWithinDayAgent>(new PersonAgentComparator());
		
		Set<Id> agentsLeaveVehicle = new TreeSet<Id>();
		for (Entry<Id, Id> entry : linkEnteredAgents.entrySet()) {
			
			Id driverId = entry.getKey();
			Id linkId = entry.getValue();
			
			QVehicle vehicle = (QVehicle) this.mobsimDataProvider.getDriversVehicle(driverId);
			agentsLeaveVehicle.clear();
			
			// identify passengers that have a different destination than the driver
			MobsimDriverAgent driver = vehicle.getDriver();
			for (PassengerAgent passenger : vehicle.getPassengers()) {
				if (!driver.getDestinationLinkId().equals(passenger.getDestinationLinkId())) {
					agentsLeaveVehicle.add(passenger.getId());
				}
			}
			
			if (agentsLeaveVehicle.size() == 0) continue;
			
			// add driver and remaining agents to replanning set
			agentsToDropOff.add((PlanBasedWithinDayAgent) driver);
			for (Id agentId : agentsLeaveVehicle) agentsToDropOff.add((PlanBasedWithinDayAgent) this.mobsimDataProvider.getAgent(agentId));
			
			/*
			 * Create a JointDeparture where the passenger(s) is(are) dropped off.
			 */
//			Id linkId = driver.getCurrentLinkId();
//			Id driverId = driver.getId();
			Set<Id> remainingPassengers = new LinkedHashSet<Id>();
			for (PassengerAgent passenger : vehicle.getPassengers()) {
				Id passengerId = passenger.getId();
				if (!agentsLeaveVehicle.contains(passengerId)) remainingPassengers.add(passengerId);
			}
			this.jointDepartureOrganizer.createJointDeparture(linkId, vehicle.getId(), driverId, remainingPassengers);
			
			// TODO: assign JointDeparture to agents and update their other joint departures
		}	
		
		return agentsToDropOff;
	}

}
