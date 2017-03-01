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

package playground.michalm.ev.discharging;

import com.google.common.base.Predicate;

import playground.michalm.ev.data.ElectricVehicle;

public class OhdeSlaskiAuxEnergyConsumption implements AuxEnergyConsumption {
	public interface TemperatureProvider {
		int getTemperature();
	}

	private static final double a = 1.3;// [W]
	private static final double b = -63.4;// [W]
	private static final double c = 1748.1;// [W]

	// precomputed values
	private static final int minTemp = -20;
	private static final int maxTemp = 40;

	// t - air temp [oC]
	// power - avg power [W]
	private static double calcPower(int t) {
		if (t < minTemp || t > maxTemp) {
			throw new IllegalArgumentException();
		}
		return (a * t + b) * t + c;
	}

	public static OhdeSlaskiAuxEnergyConsumption createConsumptionForFixedTemperatureAndAlwaysOn(ElectricVehicle ev,
			int temperature) {
		return new OhdeSlaskiAuxEnergyConsumption(ev, () -> temperature, (v) -> true);
	}

	private final ElectricVehicle ev;
	private final TemperatureProvider temperatureProvider;
	private final Predicate<ElectricVehicle> isTurnedOn;

	public OhdeSlaskiAuxEnergyConsumption(ElectricVehicle ev, TemperatureProvider temperatureProvider,
			Predicate<ElectricVehicle> isTurnedOn) {
		this.ev = ev;
		this.temperatureProvider = temperatureProvider;
		this.isTurnedOn = isTurnedOn;
	}

	@Override
	public double calcEnergyConsumption(double period) {
		return isTurnedOn.apply(ev) ? calcPower(temperatureProvider.getTemperature()) * period : 0;
	}
}
