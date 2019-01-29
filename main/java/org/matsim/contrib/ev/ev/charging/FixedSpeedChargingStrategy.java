/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.contrib.ev.ev.charging;

import org.matsim.contrib.ev.ev.data.Battery;
import org.matsim.contrib.ev.ev.data.ElectricVehicle;

/**
 * @author michalm
 */
public class FixedSpeedChargingStrategy implements ChargingStrategy {
	private final double chargingPower;
	private final double maxRelativeSoc;

	public FixedSpeedChargingStrategy(double chargingPower) {
		this(chargingPower, 1);
	}

	// e.g. maxRelativeSoc = 0.8 to model linear (fast) charging up to 80% of the battery capacity
	public FixedSpeedChargingStrategy(double chargingPower, double maxRelativeSoc) {
		if (chargingPower <= 0) {
			throw new IllegalArgumentException("chargingPower must be positive");
		}
		if (maxRelativeSoc <= 0 || maxRelativeSoc > 1) {
			throw new IllegalArgumentException("maxRelativeSoc must be in (0,1]");
		}

		this.chargingPower = chargingPower;
		this.maxRelativeSoc = maxRelativeSoc;
	}

	@Override
	public double calcEnergyCharge(ElectricVehicle ev, double chargePeriod) {
		return chargingPower * chargePeriod;
	}

	@Override
	public boolean isChargingCompleted(ElectricVehicle ev) {
		return calcRemainingEnergyToCharge(ev) <= 0;
	}

	@Override
	public double calcRemainingEnergyToCharge(ElectricVehicle ev) {
		Battery b = ev.getBattery();
		return maxRelativeSoc * b.getCapacity() - b.getSoc();
	}

	@Override
	public double calcRemainingTimeToCharge(ElectricVehicle ev) {
		return calcRemainingEnergyToCharge(ev) / chargingPower;
	}
}
