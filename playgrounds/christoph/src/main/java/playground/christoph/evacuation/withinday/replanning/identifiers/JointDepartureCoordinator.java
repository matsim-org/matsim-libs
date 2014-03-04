/* *********************************************************************** *
 * project: org.matsim.*
 * JointDepartureCoordinator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.withinday.mobsim.MobsimDataProvider;

/**
 * Checks whether an AgentsToDropOffIdentifier or an AgentsToPickupIdentifier have
 * scheduled a replanning for an agent in the current time step. This ensures
 * that an agent is not identified by both in one time step which would make
 * the replanning much more complex (they would have to communicate and create a
 * single JointDeparture object).
 * 
 * @author cdobler
 */
public class JointDepartureCoordinator { 

	private final MobsimDataProvider mobsimDataProvider;
	private AgentsToPickupIdentifier pickupIdentifier;
	private AgentsToDropOffIdentifier dropoffIdentifier;

	public JointDepartureCoordinator(MobsimDataProvider mobsimDataProvider) {
		this.mobsimDataProvider = mobsimDataProvider;
	}
	
	public void setAgentsToPickupIdentifier(AgentsToPickupIdentifier pickupIdentifier) {
		this.pickupIdentifier = pickupIdentifier;
	}
	
	public void setAgentsToDropOffIdentifier(AgentsToDropOffIdentifier dropoffIdentifier) {
		this.dropoffIdentifier = dropoffIdentifier;
	}
	
	public boolean isJointDepartureScheduled(Id agentId) {
		
		// check whether the agent should be replanned in the current time step
		if (this.pickupIdentifier.isJointDepartureScheduled(agentId)) return true;
		if (this.dropoffIdentifier.isJointDepartureScheduled(agentId)) return true;
		
		// check whether the agent has already been replanned and has scheduled joint departure
		MobsimAgent driver = this.mobsimDataProvider.getAgent(agentId);
		Leg currentLeg = WithinDayAgentUtils.getModifiableCurrentLeg(driver);
		if (this.pickupIdentifier.getJointDepartureOrganizer().getJointDepartureForLeg(agentId, currentLeg) != null) return true;

		return false;
	}
}