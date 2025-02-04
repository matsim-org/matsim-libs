/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.dvrp.fleet;

import java.util.Objects;
import java.util.Optional;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.load.DvrpLoad;
import org.matsim.contrib.dvrp.load.DvrpLoadFromVehicle;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DvrpVehicleSpecificationWithMatsimVehicle implements DvrpVehicleSpecification {

	public static final String DVRP_MODE = "dvrpMode";
	public static final String START_LINK = "startLink";
	public static final String SERVICE_BEGIN_TIME = "serviceBeginTime";
	public static final String SERVICE_END_TIME = "serviceEndTime";

	public static FleetSpecification createFleetSpecificationFromMatsimVehicles(String mode, Vehicles vehicles, DvrpLoadFromVehicle dvrpLoadFromVehicle) {
		FleetSpecification fleetSpecification = new FleetSpecificationImpl();
		vehicles.getVehicles()
				.values()
				.stream()
				.filter(vehicle -> mode.equals(
						vehicle.getAttributes().getAttribute(DVRP_MODE)))
				.map(vehicle -> new DvrpVehicleSpecificationWithMatsimVehicle(vehicle, dvrpLoadFromVehicle))
				.forEach(fleetSpecification::addVehicleSpecification);
		return fleetSpecification;
	}

	private final Id<DvrpVehicle> id;
	private final Vehicle matsimVehicle;// matsim vehicle is mutable!
	private final Id<Link> startLinkId;
	private final DvrpLoad capacity;

	// time window
	private final double serviceBeginTime;
	private final double serviceEndTime;

	public DvrpVehicleSpecificationWithMatsimVehicle(Vehicle matsimVehicle, DvrpLoadFromVehicle dvrpLoadFromVehicle) {
		id = Objects.requireNonNull(Id.create(matsimVehicle.getId(), DvrpVehicle.class));
		this.matsimVehicle = matsimVehicle;

		this.capacity = dvrpLoadFromVehicle.getLoad(matsimVehicle);

		var attributes = matsimVehicle.getAttributes();
		this.startLinkId = Objects.requireNonNull(Id.createLinkId((String)attributes.getAttribute(START_LINK)));
		this.serviceBeginTime = (double)attributes.getAttribute(SERVICE_BEGIN_TIME);
		this.serviceEndTime = (double)attributes.getAttribute(SERVICE_END_TIME);
	}

	@Override
	public Id<DvrpVehicle> getId() {
		return id;
	}

	@Override
	public Optional<Vehicle> getMatsimVehicle() {
		return Optional.of(matsimVehicle);
	}

	@Override
	public Id<Link> getStartLinkId() {
		return startLinkId;
	}

	@Override
	public DvrpLoad getCapacity() {
		return capacity;
	}

	@Override
	public double getServiceBeginTime() {
		return serviceBeginTime;
	}

	@Override
	public double getServiceEndTime() {
		return serviceEndTime;
	}
}
