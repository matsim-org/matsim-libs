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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.temperature.TemperatureService;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public final class OhdeSlaskiAuxEnergyConsumption implements AuxEnergyConsumption {
	private static final double a = 1.3;// [W]
	private static final double b = -63.4;// [W]
	private static final double c = 1748.1;// [W]

	// precomputed values
	private static final int MIN_TEMP = -20;
	private static final int MAX_TEMP = 40;

	// temp - air temp [oC]
	// power - avg power [W]
	private static double calcPower(double temp) {
		Preconditions.checkArgument(temp >= MIN_TEMP && temp <= MAX_TEMP, "temperature outside allowed range: %s",
				temp);
		return (a * temp + b) * temp + c;
	}

	private final TemperatureService temperatureService;

	OhdeSlaskiAuxEnergyConsumption(TemperatureService temperatureService) {
		this.temperatureService = temperatureService;
	}

	@Override
	public double calcEnergyConsumption(double beginTime, double duration, Id<Link> linkId) {
		return calcPower(temperatureService.getCurrentTemperature(linkId)) * duration;
	}

	public static class Factory implements AuxEnergyConsumption.Factory {
		private final TemperatureService temperatureService;

		@Inject
		Factory(TemperatureService temperatureService) {
			this.temperatureService = temperatureService;
		}

		@Override
		public AuxEnergyConsumption create(ElectricVehicle electricVehicle) {
			return new OhdeSlaskiAuxEnergyConsumption(temperatureService);
		}
	}
}
