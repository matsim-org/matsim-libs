/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2022 by the members listed in the COPYING,        *
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

import java.util.Collection;

import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.Vehicle;

public class ElectricVehicleSpecifications {
	public static void setInitialSoc(Vehicle vehicle, double initialSoc) {
		vehicle.getAttributes().putAttribute(ElectricVehicleSpecificationImpl.INITIAL_SOC, initialSoc);
	}

	public static void setChargerTypes(EngineInformation engineInformation, Collection<String> chargerTypes) {
		engineInformation.getAttributes().putAttribute(ElectricVehicleSpecificationImpl.CHARGER_TYPES, chargerTypes);
	}
}
