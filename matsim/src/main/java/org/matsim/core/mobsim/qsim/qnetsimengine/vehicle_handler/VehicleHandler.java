
/* *********************************************************************** *
 * project: org.matsim.*
 * VehicleHandler.java
 *                                                                         *
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
 * *********************************************************************** */

package org.matsim.core.mobsim.qsim.qnetsimengine.vehicle_handler;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;

/**
 * This interface provides functionality to decide what happens if a vehicle
 * interacts with a link in the network simulation.
 *
 * @author Sebastian Hörl <sebastian.hoerl@ivt.baug.ethz.ch>
 */
public interface VehicleHandler {
	/**
	 * Called when a vehicle departs in the network simulation.
	 */
	void handleVehicleDeparture(QVehicle vehicle, Link link);

	/**
	 * Called when a vehicle wants to arrive at a certain link the network
	 * simulation.
	 *
	 * @return Return value defines whether vehicle is (1) currently allowed to arrive
	 * at the link, (2) searches for a parking spot or (3) is not allowed. If not, the link will block until the next query or
	 * until the agent changes its plan dynamically.
	 */
	VehicleArrival handleVehicleArrival(QVehicle vehicle, Link link);

	/**
	 * Called when a vehicle is initially placed on a certain link.
	 */
	void handleInitialVehicleArrival(QVehicle vehicle, Link link);

	enum VehicleArrival {
		ALLOWED, BLOCKED, PARKING
	}
}
