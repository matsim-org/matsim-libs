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

package org.matsim.vsp.ev.charging;

import org.matsim.vsp.ev.data.Battery;
import org.matsim.vsp.ev.data.Charger;
import org.matsim.vsp.ev.data.ElectricVehicle;

/**
 * @author michalm
 */
public class FixedSpeedChargingStrategy implements ChargingStrategy {
	private final double effectivePower;

	public FixedSpeedChargingStrategy(Charger charger, double chargingSpeedFactor) {
		this.effectivePower = chargingSpeedFactor * charger.getPower();
	}

	@Override
	public double calcEnergyCharge(ElectricVehicle ev, double chargePeriod) {
		return effectivePower * chargePeriod;
	}

	@Override
	public boolean isChargingCompleted(ElectricVehicle ev) {
		return calcRemainingEnergyToCharge(ev) <= 0;
	}

	@Override
	public double calcRemainingEnergyToCharge(ElectricVehicle ev) {
		Battery b = ev.getBattery();
		return b.getCapacity() - b.getSoc();
	}

	@Override
	public double calcRemainingTimeToCharge(ElectricVehicle ev) {
		return calcRemainingEnergyToCharge(ev) / effectivePower;
	}
}
