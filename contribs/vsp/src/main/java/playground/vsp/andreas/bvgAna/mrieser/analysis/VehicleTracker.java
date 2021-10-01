/* *********************************************************************** *
 * project: org.matsim.*
 * VehicleTracker.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.vsp.andreas.bvgAna.mrieser.analysis;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

/**
 * Tracks at which facility a vehicle is currently located. If a vehicle departs at a facility,
 * no information about its location can be returned until the vehicle arrives at some other
 * facility.
 *
 * @author mrieser
 */
public class VehicleTracker implements VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler {

	private final Map<Id<Vehicle>, Id<TransitStopFacility>> vehicleFacilityMap = new HashMap<>();
	
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		this.vehicleFacilityMap.put(event.getVehicleId(), event.getFacilityId());
	}

	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		this.vehicleFacilityMap.remove(event.getVehicleId());
		
	}
	
	public void reset(int iteration) {
		this.vehicleFacilityMap.clear();
	}

	/**
	 * @param vehicleId
	 * @return the id of the facility where the specified vehicle is currently located, 
	 * <code>null</code> if the vehicle is currently at no known location.
	 */
	public Id<TransitStopFacility> getFacilityIdForVehicle(final Id<Vehicle> vehicleId) {
		return this.vehicleFacilityMap.get(vehicleId);
	}
	
}
