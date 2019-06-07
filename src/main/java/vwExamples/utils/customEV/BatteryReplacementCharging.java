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

import org.matsim.contrib.ev.charging.BatteryCharging;
import org.matsim.contrib.ev.charging.ChargingStrategy;
import org.matsim.contrib.ev.fleet.Battery;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;

public class BatteryReplacementCharging implements BatteryCharging {
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

	@Override
	public double calcChargingTime(Charger charger, double energy) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	public static class Strategy implements ChargingStrategy {
		private final Charger charger;
		private final ChargingStrategy delegate;

		public Strategy(Charger charger, ChargingStrategy delegate) {
			this.charger = charger;
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

		@Override
		public double calcRemainingTimeToCharge(ElectricVehicle ev) {
			return ((BatteryReplacementCharging)ev.getChargingPower()).calcChargingTime(charger,
					delegate.calcRemainingEnergyToCharge(ev));
		}
	}
}
