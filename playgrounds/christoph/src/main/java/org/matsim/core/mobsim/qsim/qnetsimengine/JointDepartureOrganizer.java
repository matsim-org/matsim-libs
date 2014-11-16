/* *********************************************************************** *
 * project: org.matsim.*
 * JointDepartureOrganizer.java
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

package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

public class JointDepartureOrganizer {

	private static Logger log = Logger.getLogger(JointDepartureOrganizer.class);
	
	/*
	 * TODO: 
	 * - use a PriorityQueue for the scheduled departures?
	 * - if yes: for each JointDeparture a departure time has to be defined
	 */
	
	/*
	 * Package protected to allow test cases to check whether all departures have
	 * been processed as expected. 
	 */
	/*package*/ final Map<Id<Person>, Map<Leg, JointDeparture>> scheduledDeparturesMap;	// agentId
	/*package*/ final Map<Id<JointDeparture>, JointDeparture> scheduledDepartures;
	
	private final AtomicInteger jointDepartureCounter = new AtomicInteger(0);
	
	public JointDepartureOrganizer() {
		// needs this to be thread-safe?
		this.scheduledDeparturesMap = new ConcurrentHashMap<>();
		
		/*
		 * This is only to check whether all departures have been processed.
		 * I think this needs to be thread-safe, too since it is accessed
		 * from replanners.
		 */
		this.scheduledDepartures = new ConcurrentHashMap<>();
	}
	
	/*package*/ Map<Leg, JointDeparture> getJointDepartures(Id<Person> agentId) {
		return this.scheduledDeparturesMap.get(agentId);
	}
	
	public JointDeparture getJointDepartureForLeg(Id<Person> agentId, Leg leg) {
		Map<Leg, JointDeparture> map = this.scheduledDeparturesMap.get(agentId);
		if (map == null) return null;
		else return map.get(leg);
	}
	
	public JointDeparture removeJointDepartureForLeg(Id<Person> agentId, Leg leg) {
		Map<Leg, JointDeparture> map = this.scheduledDeparturesMap.get(agentId);
		if (map == null) return null;
		else return map.remove(leg);
	}
	
	/*package*/ boolean removeHandledJointDeparture(JointDeparture jointDeparture) {
		return this.scheduledDepartures.remove(jointDeparture.getId()) != null;
	}
	
	public JointDeparture createJointDeparture(Id<JointDeparture> id, Id<Link> linkId, Id<Vehicle> vehicleId, Id<Person> driverId, 
			Set<Id<Person>> passengerIds) {		
		JointDeparture jointDeparture = new JointDeparture(id, linkId, vehicleId, driverId, passengerIds);
		
		return jointDeparture;
	}
	
	public JointDeparture createJointDeparture(Id<Link> linkId, Id<Vehicle> vehicleId, Id<Person> driverId, 
			Set<Id<Person>> passengerIds) {		
		JointDeparture jointDeparture = new JointDeparture(getNextId(), linkId, vehicleId, driverId, passengerIds);
		
		return jointDeparture;
	}
	
	public Id<JointDeparture> getNextId() {
		return Id.create("id" + jointDepartureCounter.getAndIncrement(), JointDeparture.class);
	}
	
	public void assignAgentToJointDeparture(Id<Person> agentId, Leg leg, JointDeparture jointDeparture) {
		this.scheduledDepartures.put(jointDeparture.getId(), jointDeparture);
		Map<Leg, JointDeparture> jointDepartures = this.scheduledDeparturesMap.get(agentId);
		if (jointDepartures == null) {
			jointDepartures = new HashMap<Leg, JointDeparture>();
			this.scheduledDeparturesMap.put(agentId, jointDepartures);
		}
		jointDepartures.put(leg, jointDeparture);
	}
	
}