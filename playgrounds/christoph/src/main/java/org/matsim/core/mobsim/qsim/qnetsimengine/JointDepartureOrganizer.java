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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;

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
	/*package*/ final Map<Id, Map<Leg, JointDeparture>> scheduledDepartures;	// agentId

	public JointDepartureOrganizer() {
		// needs this to be thread-safe?
		this.scheduledDepartures = new ConcurrentHashMap<Id, Map<Leg, JointDeparture>>();
	}
	
	/*package*/ Map<Leg, JointDeparture> getJointDepartures(Id agentId) {
		return this.scheduledDepartures.get(agentId);
	}
	
	public JointDeparture getJointDepartureForLeg(Id agentId, Leg leg) {
		Map<Leg, JointDeparture> map = this.scheduledDepartures.get(agentId);
		if (map == null) return null;
		else return map.get(leg);
	}
	
	public JointDeparture removeJointDepartureForLeg(Id agentId, Leg leg) {
		Map<Leg, JointDeparture> map = this.scheduledDepartures.get(agentId);
		if (map == null) return null;
		else return map.remove(leg);
	}
	
	public JointDeparture createJointDeparture(Id id, Id linkId, Id vehicleId, Id driverId, 
			Collection<Id> passengerIds) {		
		JointDeparture jointDeparture = new JointDeparture(id, linkId, vehicleId, driverId, passengerIds);
		
		return jointDeparture;
	}
	
	public void assignAgentToJointDeparture(Id agentId, Leg leg, JointDeparture jointDeparture) {
		Map<Leg, JointDeparture> jointDepartures = this.scheduledDepartures.get(agentId);
		if (jointDepartures == null) {
			jointDepartures = new HashMap<Leg, JointDeparture>();
			this.scheduledDepartures.put(agentId, jointDepartures);
		}
		jointDepartures.put(leg, jointDeparture);
	}
	
}