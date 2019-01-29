/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

import org.matsim.contrib.ev.data.ElectricVehicle;

import java.util.function.DoubleSupplier;
import java.util.function.Predicate;

public class OhdeSlaskiAuxEnergyConsumption implements AuxEnergyConsumption {
	private static final double a = 1.3;// [W]
	private static final double b = -63.4;// [W]
	private static final double c = 1748.1;// [W]

	// precomputed values
	private static final int minTemp = -20;
	private static final int maxTemp = 40;

	// temp - air temp [oC]
	// power - avg power [W]
	private static double calcPower(double temp) {
		if (temp < minTemp || temp > maxTemp) {
			throw new IllegalArgumentException();
		}
		return (a * temp + b) * temp + c;
	}

	public static OhdeSlaskiAuxEnergyConsumption createConsumptionForFixedTemperatureAndAlwaysOn(ElectricVehicle ev,
			int temperature) {
		return new OhdeSlaskiAuxEnergyConsumption(ev, () -> temperature, v -> true);
	}

	private final ElectricVehicle ev;
	private final DoubleSupplier temperatureProvider;
	private final Predicate<ElectricVehicle> isTurnedOn;

	public OhdeSlaskiAuxEnergyConsumption(ElectricVehicle ev, DoubleSupplier temperatureProvider,
			Predicate<ElectricVehicle> isTurnedOn) {
		this.ev = ev;
		this.temperatureProvider = temperatureProvider;
		this.isTurnedOn = isTurnedOn;
	}

	@Override
	public double calcEnergyConsumption(double period) {
		return isTurnedOn.test(ev) ? calcPower(temperatureProvider.getAsDouble()) * period : 0;
	}
}
