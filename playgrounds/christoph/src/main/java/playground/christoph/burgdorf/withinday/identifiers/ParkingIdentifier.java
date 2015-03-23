/* *********************************************************************** *
 * project: org.matsim.*
 * ParkingIdentifier.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.burgdorf.withinday.identifiers;

import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.withinday.replanning.identifiers.LeaveLinkIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegAgentSelector;

import playground.christoph.burgdorf.ParkingInfrastructure;

public class ParkingIdentifier extends DuringLegAgentSelector {

	protected LeaveLinkIdentifier leaveLinkIdentifier;
	
	// use the Factory!
	/*package*/ ParkingIdentifier(LeaveLinkIdentifier leaveLinkIdentifier) {
		this.leaveLinkIdentifier = leaveLinkIdentifier;
	}
	
	@Override
	public Set<MobsimAgent> getAgentsToReplan(double time) {

		/* 
		 * Use leave link identifier to identify the agents.
		 * Then select a parking space for them.
		 */
		Set<MobsimAgent> agentsToReplan = leaveLinkIdentifier.getAgentsToReplan(time);
		
		for (MobsimAgent agent : agentsToReplan) {
			Id parkingId = ParkingInfrastructure.selectParking(agent.getCurrentLinkId());
			ParkingInfrastructure.selectedParkings.put(agent.getId(), parkingId);
		}
		
		return agentsToReplan;
	}

}
