/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

import java.util.Optional;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.load.DvrpLoad;
import org.matsim.vehicles.Vehicle;

/**
 * DvrpVehicleSpecification is assumed to be immutable.
 * <p>
 * Its lifespan can span over all iterations, but can be also changed before each iteration.
 * <p>
 * Changing a vehicle specification (e.g. setting a different startLinkId) should be done only "between" iterations
 * by passing a new instance to FleetSpecification.
 *
 * @author Michal Maciejewski (michalm)
 */
public interface DvrpVehicleSpecification extends Identifiable<DvrpVehicle> {
	//provided only if the vehicle specification is created from a corresponding standard matsim vehicle
	//(see FleetModule)
	Optional<Vehicle> getMatsimVehicle();

	/**
	 * @return id of the link where the vehicle stays at the beginning of simulation
	 */
	Id<Link> getStartLinkId();

	/**
	 * @return vehicle capacity
	 */
	DvrpLoad getCapacity();

	/**
	 * @return vehicle operations start time
	 */
	double getServiceBeginTime();

	/**
	 * @return vehicle operations end time
	 */
	double getServiceEndTime();
}
