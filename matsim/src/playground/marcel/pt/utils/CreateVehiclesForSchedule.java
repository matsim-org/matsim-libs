/* *********************************************************************** *
 * project: org.matsim.*
 * CreateVehiclesForSchedule.java
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

package playground.marcel.pt.utils;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.BasicVehicle;
import org.matsim.vehicles.BasicVehicleCapacity;
import org.matsim.vehicles.BasicVehicleCapacityImpl;
import org.matsim.vehicles.BasicVehicleType;
import org.matsim.vehicles.BasicVehicles;
import org.matsim.vehicles.VehiclesFactory;

/**
 * Creates a vehicle of type "defaultTransitVehicleType" for each departure.
 *
 * @author mrieser
 */
public class CreateVehiclesForSchedule {

	private final TransitSchedule schedule;
	private final BasicVehicles vehicles;

	public CreateVehiclesForSchedule(final TransitSchedule schedule, final BasicVehicles vehicles) {
		this.schedule = schedule;
		this.vehicles = vehicles;
	}

	public void run() {
		VehiclesFactory vb = this.vehicles.getFactory();
		BasicVehicleType vehicleType = vb.createVehicleType(new IdImpl("defaultTransitVehicleType"));
		BasicVehicleCapacity capacity = new BasicVehicleCapacityImpl();
		capacity.setSeats(Integer.valueOf(101));
		capacity.setStandingRoom(Integer.valueOf(0));
		vehicleType.setCapacity(capacity);
		this.vehicles.getVehicleTypes().put(vehicleType.getId(), vehicleType);

		long vehId = 0;
		for (TransitLine line : this.schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (Departure departure : route.getDepartures().values()) {
					BasicVehicle veh = vb.createVehicle(new IdImpl("tr_" + Long.toString(vehId++)), vehicleType);
					this.vehicles.getVehicles().put(veh.getId(), veh);
					departure.setVehicleId(veh.getId());
				}
			}
		}
	}
}
