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
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;

public class PassengerDepartureHandler implements DepartureHandler {

	public final static String passengerTransportMode = "ride_passenger";
	
	private final EventsManager eventsManager;
	private final VehiclesTracker vehiclesTracker;
	
	public PassengerDepartureHandler(EventsManager eventsManager, VehiclesTracker vehiclesTracker) {
		this.eventsManager = eventsManager;
		this.vehiclesTracker = vehiclesTracker;
	}
	
	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id linkId) {
		
		if (agent.getMode().equals(passengerTransportMode)) {
			if (agent instanceof MobsimDriverAgent) {
				
				// create PersonEntersVehicleEvent
				Id vehicleId = null;
				Id passengerVehicleId = vehiclesTracker.getPassengersVehicle(agent.getId());
				Id plannedVehicleId = vehiclesTracker.getPlannedPickupVehicles().remove(agent.getId());
				
				if (passengerVehicleId != null && plannedVehicleId != null) {
					throw new RuntimeException("Person was registered as Passenger in a vehicle AND had " +
							"an entry in the planned vehicles map. This should not happen...");
				}
				
				if (plannedVehicleId != null) {
					vehicleId = plannedVehicleId;
					vehiclesTracker.addPassengerToVehicle(agent.getId(), plannedVehicleId);	
				} else if (passengerVehicleId != null) {
					vehicleId = passengerVehicleId;
				} else {
					throw new RuntimeException("Person was registered as Passenger in a vehicle and also " +
							"no entry in the planned vehicles map was found. This should not happen...");
				}

				eventsManager.processEvent(eventsManager.getFactory().createPersonEntersVehicleEvent(now, agent.getId(), vehicleId));
				
				
				return true;
			} else {
				throw new UnsupportedOperationException("Wrong agent type to be a passenger!");
			}
		}
		return false;
	}
}
