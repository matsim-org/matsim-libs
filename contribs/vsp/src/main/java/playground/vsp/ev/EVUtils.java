/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.vsp.ev;

import java.util.Collection;

import org.matsim.contrib.ev.fleet.ElectricVehicleSpecificationImpl;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.Vehicle;

import com.google.common.collect.ImmutableList;

public class EVUtils {
	/**
	 * @param vehicle
	 * @return the initial energy in kWh
	 */
	static Double getInitialEnergy(Vehicle vehicle) {
		return (Double)vehicle.getAttributes()
				.getAttribute(ElectricVehicleSpecificationImpl.INITIAL_ENERGY_kWh);
	}

	/**
	 * @param vehicle
	 * @param initialEnergyInKWh initial energy [kWh]
	 */
	public static void setInitialEnergy(Vehicle vehicle, double initialEnergyInKWh) {
		vehicle.getAttributes()
				.putAttribute(ElectricVehicleSpecificationImpl.INITIAL_ENERGY_kWh, initialEnergyInKWh);
	}

	static ImmutableList<String> getChargerTypes(EngineInformation engineInformation) {
		return ImmutableList.copyOf((Collection<String>)engineInformation.getAttributes()
				.getAttribute(ElectricVehicleSpecificationImpl.CHARGER_TYPES));
	}

	public static void setChargerTypes(EngineInformation engineInformation, Collection<String> chargerTypes) {
		engineInformation.getAttributes()
				.putAttribute(ElectricVehicleSpecificationImpl.CHARGER_TYPES, chargerTypes);
	}
}
