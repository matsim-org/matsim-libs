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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

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
	/*package*/ final Map<Id, List<JointDeparture>> scheduledDepartures;	// agentId

	public JointDepartureOrganizer() {
		// needs this to be thread-safe?
		this.scheduledDepartures = new ConcurrentHashMap<Id, List<JointDeparture>>();
	}
	
	public List<JointDeparture> getJointDepartures(Id agentId) {
		return this.scheduledDepartures.get(agentId);
	}
	
	public JointDeparture createJointDeparture(Id id, Id linkId, Id vehicleId, Id driverId, 
			Collection<Id> passengerIds) {		
		JointDeparture jointDeparture = new JointDeparture(id, linkId, vehicleId, driverId, passengerIds);
		
		this.assignAgentToJointDeparture(driverId, jointDeparture);
		for (Id passengerId : passengerIds) this.assignAgentToJointDeparture(passengerId, jointDeparture);
		
		return jointDeparture;
	}
	
	/*package*/ void assignAgentToJointDeparture(Id agentId, JointDeparture jointDeparture) {
		List<JointDeparture> jointDepartures = this.scheduledDepartures.get(agentId);
		if (jointDepartures == null) {
			jointDepartures = new ArrayList<JointDeparture>();
			this.scheduledDepartures.put(agentId, jointDepartures);
		}
		jointDepartures.add(jointDeparture);
	}
	
	/*package*/ JointDeparture getJointDeparture(Id agentId) {
		List<JointDeparture> jointDepartures = scheduledDepartures.get(agentId);
		if (jointDepartures == null || jointDepartures.size() == 0) return null;
		
		/*
		 * Return the first jointDeparture from the list which has not been processed.
		 */
		JointDeparture jointDeparture;
		while (true) {
			jointDeparture = jointDepartures.remove(0);
			if (jointDeparture.isDeparted()) {
				log.warn("Seems that agent " + agentId + 
						" has missed departure " + jointDeparture.getId().toString() + 
						" with vehicle " + jointDeparture.getVehicleId().toString() +
						" on link " + jointDeparture.getLinkId().toString());
				
				// Return null if no non-departed JointDeparture is left.
				if (jointDepartures.size() == 0) return null;
			}
			else return jointDeparture;
		}
	}
	
}