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

package org.matsim.contrib.ev.fleet;

import com.google.common.collect.ImmutableList;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.vehicles.Vehicle;

/**
 * @author Michal Maciejewski (michalm)
 */
public interface ElectricVehicleSpecification extends Identifiable<Vehicle> {
	Vehicle getMatsimVehicle();

	ImmutableList<String> getChargerTypes();

	double getInitialSoc(); //in [0, 1]

	default double getInitialCharge() {
		return getInitialSoc() * getBatteryCapacity();
	}

	double getBatteryCapacity();//[J]
}
