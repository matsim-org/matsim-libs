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
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;

/**
 * @author Michal Maciejewski (michalm)
 */
public class ChargeUpToMaxSocStrategy implements ChargingStrategy {
	private final ChargerSpecification charger;
	private final double maxSoc;

	public ChargeUpToMaxSocStrategy(ChargerSpecification charger, double maxSoc) {
		if (maxSoc < 0 || maxSoc > 1) {
			throw new IllegalArgumentException();
		}
		this.charger = charger;
		this.maxSoc = maxSoc;
	}

	@Override
	public double calcRemainingEnergyToCharge(ElectricVehicle ev) {
		Battery battery = ev.getBattery();
		return maxSoc * battery.getCapacity() - battery.getCharge();
	}

	@Override
	public double calcRemainingTimeToCharge(ElectricVehicle ev) {
		return ((BatteryCharging)ev.getChargingPower()).calcChargingTime(charger, calcRemainingEnergyToCharge(ev));
	}
}
