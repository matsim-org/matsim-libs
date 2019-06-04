/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

import java.util.function.DoubleSupplier;

import org.matsim.contrib.ev.discharging.AuxEnergyConsumption;
import org.matsim.contrib.ev.discharging.OhdeSlaskiAuxEnergyConsumption;
import org.matsim.contrib.ev.fleet.ElectricVehicle;

public class DvrpAuxConsumptionFactory implements AuxEnergyConsumption.Factory {
	private final DoubleSupplier temperatureProvider;

	public DvrpAuxConsumptionFactory(DoubleSupplier temperatureProvider) {
		this.temperatureProvider = temperatureProvider;
	}

	@Override
	public AuxEnergyConsumption create(ElectricVehicle electricVehicle) {
		return new OhdeSlaskiAuxEnergyConsumption(temperatureProvider);
	}
}
