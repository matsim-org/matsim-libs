/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.jbischoff.parking.DynAgent;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dynagent.DriverDynLeg;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.contrib.dynagent.DynAgentLogic;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;

import playground.jbischoff.parking.events.StartParkingSearchEvent;

/**
 * @author  jbischoff
 *
 */
/**
 * @author Joschka
 *
 */
public class DynParkingAgent extends DynAgent {

	private MobsimTimer timer;

	/**
	 * @param id
	 * @param startLinkId
	 * @param events
	 * @param agentLogic
	 */
	public DynParkingAgent(Id<Person> id, Id<Link> startLinkId, EventsManager events, DynAgentLogic agentLogic,
			MobsimTimer timer) {
		super(id, startLinkId, events, agentLogic);
		this.timer = timer;
	}

	@Override
	public void notifyMoveOverNode(Id<Link> newLinkId) {
		super.notifyMoveOverNode(newLinkId);
		if (this.dynLeg instanceof ParkingDynLeg) {
			if (this.getCurrentLinkId().equals(this.dynLeg.getDestinationLinkId())) {
				this.events.processEvent(
						new StartParkingSearchEvent(timer.getTimeOfDay(), getVehicle().getId(), getCurrentLinkId()));
			}
		}
	}

}
