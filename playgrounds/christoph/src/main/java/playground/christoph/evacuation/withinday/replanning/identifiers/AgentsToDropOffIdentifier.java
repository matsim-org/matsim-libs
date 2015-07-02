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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.mobsim.qsim.qnetsimengine.JointDeparture;
import org.matsim.core.mobsim.qsim.qnetsimengine.JointDepartureOrganizer;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.withinday.mobsim.MobsimDataProvider;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegAgentSelector;
import org.matsim.withinday.trafficmonitoring.LinkEnteredProvider;
import playground.christoph.tools.PersonAgentComparator;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

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
 * We also have to ensure, that passengers leave the vehicle of the driver stops for
 * an activity different than pick-up or drop-off. Otherwise they also might wait
 * forever in the car.
 * 
 * @author cdobler
 */
public class AgentsToDropOffIdentifier extends DuringLegAgentSelector { 

	private final MobsimDataProvider mobsimDataProvider;
	private final LinkEnteredProvider linkEnteredProvider;
	private final JointDepartureOrganizer jointDepartureOrganizer;
	private final JointDepartureCoordinator jointDepartureCoordinator;
	
	private final Map<Id, JointDeparture> jointDepartures;
	
	/*package*/ AgentsToDropOffIdentifier(MobsimDataProvider mobsimDataProvider, LinkEnteredProvider linkEnteredProvider,
			JointDepartureOrganizer jointDepartureOrganizer, JointDepartureCoordinator jointDepartureCoordinator) {
		this.mobsimDataProvider = mobsimDataProvider;
		this.linkEnteredProvider = linkEnteredProvider;
		this.jointDepartureOrganizer = jointDepartureOrganizer;
		this.jointDepartureCoordinator = jointDepartureCoordinator;
		
		this.jointDepartures = new ConcurrentHashMap<Id, JointDeparture>();
	}

	@Override
	public Set<MobsimAgent> getAgentsToReplan(double time) {
		
		// Get all agents that have just entered a new link.
//		Map<Id, Id> linkEnteredAgents = new HashMap<Id, Id>(this.linkEnteredProvider.getLinkEnteredAgentsInLastTimeStep());
		
		// Apply filter to remove agents that should not be replanned.
//		this.applyFilters(this.linkEnteredAgents.keySet(), time);
		
		Set<MobsimAgent> agentsToDropOff = new TreeSet<MobsimAgent>(new PersonAgentComparator());
		
		Set<Id> agentsLeaveVehicle = new TreeSet<Id>();
//		for (Entry<Id, Id> entry : linkEnteredAgents.entrySet()) {
		for (Entry<Id<Person>, Id<Link>> entry : this.linkEnteredProvider.getLinkEnteredAgentsInLastTimeStep().entrySet()) {
						
			Id<Person> driverId = entry.getKey();
			Id<Link> linkId = entry.getValue();
			
			// if the filters do not include the agent skip it
			if (!this.applyFilters(driverId, time)) continue;
			
			/*
			 * Check whether the driver has already scheduled a JointDeparture on the current link;
			 */
			if (this.jointDepartureCoordinator.isJointDepartureScheduled(driverId)) continue;
			
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
			agentsToDropOff.add(driver);
			for (Id<Person> agentId : agentsLeaveVehicle) agentsToDropOff.add(this.mobsimDataProvider.getAgent(agentId));
			
			/*
			 * Create a JointDeparture where the passenger(s) is(are) dropped off.
			 */
//			Id linkId = driver.getCurrentLinkId();
//			Id driverId = driver.getId();
			Set<Id<Person>> remainingPassengers = new LinkedHashSet<>();
			for (PassengerAgent passenger : vehicle.getPassengers()) {
				Id<Person> passengerId = passenger.getId();
				if (!agentsLeaveVehicle.contains(passengerId)) remainingPassengers.add(passengerId);
			}
			JointDeparture jointDeparture = this.jointDepartureOrganizer.createJointDeparture(linkId, vehicle.getId(), driverId, remainingPassengers);
			
			this.jointDepartures.put(driverId, jointDeparture);
//			for (Id passengerId :remainingPassengers) {
//				this.jointDepartures.put(passengerId, jointDeparture);
//			}
			
			// TODO: assign JointDeparture to agents and update their other joint departures
		}	
		
		return agentsToDropOff;
	}
	
	public JointDepartureOrganizer getJointDepartureOrganizer() {
		return this.jointDepartureOrganizer;
	}
	
	public JointDeparture getJointDeparture(Id agentId) {
		return this.jointDepartures.remove(agentId);
	}
	
	/*package*/ boolean isJointDepartureScheduled(Id agentId) {
		return this.jointDepartures.containsKey(agentId);
	}
}
