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

package org.matsim.contrib.ev.dvrp;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.ev.data.ElectricFleet;
import org.matsim.contrib.ev.data.ElectricVehicle;

/**
 * @author michalm
 */
public class EvDvrpVehicle implements DvrpVehicle {
	static EvDvrpVehicle create(DvrpVehicle vehicle, ElectricFleet evFleet) {
		return new EvDvrpVehicle(vehicle,
				evFleet.getElectricVehicles().get(Id.create(vehicle.getId(), ElectricVehicle.class)));
	}

	private final DvrpVehicle vehicle;
	private final ElectricVehicle electricVehicle;

	public EvDvrpVehicle(DvrpVehicle vehicle, ElectricVehicle electricVehicle) {
		this.vehicle = vehicle;
		this.electricVehicle = electricVehicle;
	}

	public ElectricVehicle getElectricVehicle() {
		return electricVehicle;
	}

	@Override
	public Id<DvrpVehicle> getId() {
		return vehicle.getId();
	}

	@Override
	public Link getStartLink() {
		return vehicle.getStartLink();
	}

	@Override
	public int getCapacity() {
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
}
