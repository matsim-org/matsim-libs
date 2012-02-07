/* *********************************************************************** *
 * project: org.matsim.*
 * PassengerDepartureHandler.java
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

package playground.christoph.evacuation.mobsim;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.ptproject.qsim.interfaces.DepartureHandler;

public class PassengerDepartureHandler implements DepartureHandler {

	public final static String passengerTransportMode = "ride_passenger";
	
	private final EventsManager eventsManager;
	private final PassengerTracker passengerTracker;
	
	public PassengerDepartureHandler(EventsManager eventsManager, PassengerTracker passengerTracker) {
		this.eventsManager = eventsManager;
		this.passengerTracker = passengerTracker;
	}
	
	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id linkId) {
		if (agent.getMode().equals(passengerTransportMode)) {
			if (agent instanceof MobsimDriverAgent) {
				
				// create PersonEntersVehicleEvent
				Id vehicleId = passengerTracker.getPassengersVehicle(agent.getId());
				eventsManager.processEvent(eventsManager.getFactory().createPersonEntersVehicleEvent(now, agent.getId(), vehicleId));
				
				passengerTracker.addEnrouteAgent(agent);
				
				return true;
			} else {
				throw new UnsupportedOperationException("Wrong agent type to be a passenger!");
			}
		}
		return false;
	}
}
