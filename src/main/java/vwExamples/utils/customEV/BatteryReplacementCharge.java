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

package vwExamples.utils.customEV;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.ev.charging.ChargingStrategy;
import org.matsim.contrib.ev.fleet.Battery;
import org.matsim.contrib.ev.fleet.ElectricVehicle;

public class BatteryReplacementCharge implements ChargingStrategy {
	private final double timeForBatteryReplacement;
	// private final double effectiveChargingPower;
	Map<Id<ElectricVehicle>, Double> chargingVehicleMap = new HashMap<>();

	public BatteryReplacementCharge(double timeForBatteryReplacement) {
		this.timeForBatteryReplacement = timeForBatteryReplacement;
	}

	@Override
	public double calcChargingPower(ElectricVehicle ev) {
		return chargingVehicleMap.computeIfAbsent(ev.getId(),
				id -> (calcRemainingEnergyToCharge(ev) / 3600) / (timeForBatteryReplacement / 3600));
	}

	@Override
	public boolean isChargingCompleted(ElectricVehicle ev) {
		boolean finishedCharging = ChargingStrategy.super.isChargingCompleted(ev);
		if (finishedCharging) {
			chargingVehicleMap.remove(ev.getId());
		}
		return finishedCharging;
	}

	@Override
	public double calcRemainingEnergyToCharge(ElectricVehicle ev) {
		Battery b = ev.getBattery();
		return b.getCapacity() - b.getSoc();
	}

	@Override
	public double calcRemainingTimeToCharge(ElectricVehicle ev) {
		// Required Charging Time is a constant
		return this.timeForBatteryReplacement;
	}
}
