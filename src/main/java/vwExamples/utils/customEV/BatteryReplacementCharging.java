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

import org.matsim.contrib.ev.charging.ChargingPower;
import org.matsim.contrib.ev.charging.ChargingStrategy;
import org.matsim.contrib.ev.fleet.Battery;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;

public class BatteryReplacementCharging implements ChargingPower {
	private final ElectricVehicle electricVehicle;
	private final double timeForBatteryReplacement;
	private Double chargingPowerEquivalent = null;

	public BatteryReplacementCharging(ElectricVehicle electricVehicle, double timeForBatteryReplacement) {
		this.electricVehicle = electricVehicle;
		this.timeForBatteryReplacement = timeForBatteryReplacement;
	}

	@Override
	public double calcChargingPower(Charger charger) {
		if (chargingPowerEquivalent == null) {
			chargingPowerEquivalent = calcRemainingEnergyToCharge() / timeForBatteryReplacement;
		}
		return chargingPowerEquivalent;
	}

	private double calcRemainingEnergyToCharge() {
		Battery b = electricVehicle.getBattery();
		return b.getCapacity() - b.getSoc();
	}

	public static class Strategy implements ChargingStrategy {
		private final ChargingStrategy delegate;

		public Strategy(ChargingStrategy delegate) {
			this.delegate = delegate;
		}

		@Override
		public boolean isChargingCompleted(ElectricVehicle ev) {
			if (delegate.isChargingCompleted(ev)) {
				((BatteryReplacementCharging)ev.getChargingPower()).chargingPowerEquivalent = null;
				return true;
			}
			return false;
		}

		@Override
		public double calcRemainingEnergyToCharge(ElectricVehicle ev) {
			return delegate.calcRemainingEnergyToCharge(ev);
		}
	}
}
