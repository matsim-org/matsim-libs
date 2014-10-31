/* *********************************************************************** *
 * project: org.matsim.*
 * CAWalkerDepatureHandler.java
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

package playground.gregor.casim.simulation;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;

import playground.gregor.casim.simulation.physics.CALink;
import playground.gregor.casim.simulation.physics.CAVehicle;
import playground.gregor.sim2d_v4.scenario.TransportMode;

public class CAWalkerDepatureHandler implements DepartureHandler {
	
	private static final String transportMode = TransportMode.walkca;
	private final CAEngine engine;

	public CAWalkerDepatureHandler(CAEngine caEngine) {
		this.engine = caEngine;
	}

	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id linkId) {
		if (agent.getMode().equals(transportMode)) {
			if ( agent instanceof MobsimDriverAgent ) {
				handleCarDeparture(now, (MobsimDriverAgent)agent, linkId);
				return true;
			} else {
				throw new UnsupportedOperationException("wrong agent type to depart on a network mode");
			}
		} 
		return false;
	}

	private void handleCarDeparture(double now, MobsimDriverAgent agent,
			Id linkId) {
//		CALink link = this.engine.getCANetwork().getCALink(linkId);
//		Id vehicleId = agent.getPlannedVehicleId() ;
//		CAVehicle veh = new CAVehicle(vehicleId,agent,linkId,link);
//		link.letAgentDepart(veh);
		
	}

}
