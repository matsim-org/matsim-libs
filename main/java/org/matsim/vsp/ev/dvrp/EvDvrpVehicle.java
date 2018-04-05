/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.vsp.ev.dvrp;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.vsp.ev.data.ElectricFleet;
import org.matsim.vsp.ev.data.ElectricVehicle;

/**
 * @author michalm
 */
public class EvDvrpVehicle implements Vehicle {
	static EvDvrpVehicle create(Vehicle vehicle, ElectricFleet evFleet) {
		return new EvDvrpVehicle(vehicle, evFleet.getElectricVehicles().get(vehicle.getId()));
	}

	private final Vehicle vehicle;
	private final ElectricVehicle electricVehicle;

	public EvDvrpVehicle(Vehicle vehicle, ElectricVehicle electricVehicle) {
		this.vehicle = vehicle;
		this.electricVehicle = electricVehicle;
	}

	public ElectricVehicle getElectricVehicle() {
		return electricVehicle;
	}

	@Override
	public Id<Vehicle> getId() {
		return vehicle.getId();
	}

	@Override
	public Link getStartLink() {
		return vehicle.getStartLink();
	}

	@Override
	public double getCapacity() {
		return vehicle.getCapacity();
	}

	@Override
	public double getServiceBeginTime() {
		return vehicle.getServiceBeginTime();
	}

	@Override
	public double getServiceEndTime() {
		return vehicle.getServiceEndTime();
	}

	@Override
	public Schedule getSchedule() {
		return vehicle.getSchedule();
	}

	@Override
	public void setStartLink(Link link) {
		vehicle.setStartLink(link);
	}

	@Override
	public void resetSchedule() {
		vehicle.resetSchedule();
	}
}
