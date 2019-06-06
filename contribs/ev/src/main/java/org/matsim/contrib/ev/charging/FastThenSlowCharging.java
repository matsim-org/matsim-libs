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

package org.matsim.contrib.ev.charging;/*
 * created by jbischoff, 16.11.2018
 *
 * This charging strategy mimics the typical behavior at fast-chargers:
 * Up to 50%, full power (or up to 1.75* C) is applied, up to
 * 75% SOC, a maximum of 1.25 * C is applied. Untill full, maximum power is 0.5*C.
 * C == battery capacity.
 * This charging behavior is based on research conducted at LTH / University of Lund
 */

import org.matsim.contrib.ev.fleet.Battery;
import org.matsim.contrib.ev.fleet.ElectricVehicle;

public class FastThenSlowCharging implements ChargingStrategy {

	private final double chargingPower;

	public FastThenSlowCharging(double chargingPower) {
		if (chargingPower <= 0) {
			throw new IllegalArgumentException("chargingPower must be positive");
		}
		this.chargingPower = chargingPower;
	}

	@Override
	public double calcEnergyCharge(ElectricVehicle ev, double chargePeriod) {
		Battery b = ev.getBattery();
		double relativeSoc = b.getSoc() / b.getCapacity();
		double c = b.getCapacity() / 3600;
		double currentPower;
		if (relativeSoc <= 0.5) {
			currentPower = Math.min(chargingPower, 1.75 * c);
		} else if (relativeSoc <= 0.75) {
			currentPower = Math.min(chargingPower, 1.25 * c);
		} else {
			currentPower = Math.min(chargingPower, 0.5 * c);
		}
		return currentPower * chargePeriod;
	}

	@Override
	public double calcRemainingEnergyToCharge(ElectricVehicle ev) {
		Battery b = ev.getBattery();
		return b.getCapacity() - b.getSoc();
	}

	@Override
	public double calcRemainingTimeToCharge(ElectricVehicle ev) {
		return calcRemainingEnergyToCharge(ev) / chargingPower;//TODO should consider variable charging speed
	}
}
