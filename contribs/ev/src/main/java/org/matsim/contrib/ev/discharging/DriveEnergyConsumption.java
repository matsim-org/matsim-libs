/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package org.matsim.contrib.ev.discharging;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.ev.fleet.ElectricVehicle;

/**
 * Net energy taken from the battery to propel the vehicle, i.e. consumed by the motor minus effectively regenerated
 * (returned back to the battery).
 */
public interface DriveEnergyConsumption {
	interface Factory {
		DriveEnergyConsumption create(ElectricVehicle electricVehicle);
	}

	/**
	 * @param link          Link where energy is consumed
	 * @param travelTime    TravelTime spent on link
	 * @param linkEnterTime time of entering link (may be undefined)
	 * @return energy consumed by vehicle on link in J
	 */
	double calcEnergyConsumption(Link link, double travelTime, double linkEnterTime);
}
