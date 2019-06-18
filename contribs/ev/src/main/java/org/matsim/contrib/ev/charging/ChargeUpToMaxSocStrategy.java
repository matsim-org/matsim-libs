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

package org.matsim.contrib.ev.charging;

import org.matsim.contrib.ev.fleet.Battery;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;

/**
 * @author Michal Maciejewski (michalm)
 */
public class ChargeUpToMaxSocStrategy implements ChargingStrategy {
	private final Charger charger;
	private final double maxRelativeSoc;

	public ChargeUpToMaxSocStrategy(Charger charger, double maxRelativeSoc) {
		if (maxRelativeSoc < 0 || maxRelativeSoc > 1) {
			throw new IllegalArgumentException();
		}
		this.charger = charger;
		this.maxRelativeSoc = maxRelativeSoc;
	}

	@Override
	public double calcRemainingEnergyToCharge(ElectricVehicle ev) {
		Battery battery = ev.getBattery();
		return maxRelativeSoc * battery.getCapacity() - battery.getSoc();
	}

	@Override
	public double calcRemainingTimeToCharge(ElectricVehicle ev) {
		return ((BatteryCharging)ev.getChargingPower()).calcChargingTime(charger, calcRemainingEnergyToCharge(ev));
	}
}
