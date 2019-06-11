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

package org.matsim.contrib.ev.charging;
/*
 * created by jbischoff, 16.11.2018
 *
 * This charging model mimics the typical behavior at fast-chargers:
 * Up to 50%, full power (or up to 1.75* C) is applied, up to
 * 75% SOC, a maximum of 1.25 * C is applied. Until full, maximum power is 0.5*C.
 * C == battery capacity.
 * This charging behavior is based on research conducted at LTH / University of Lund
 */

import org.matsim.contrib.ev.fleet.Battery;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;

public class FastThenSlowCharging implements BatteryCharging {
	private final ElectricVehicle electricVehicle;

	public FastThenSlowCharging(ElectricVehicle electricVehicle) {
		this.electricVehicle = electricVehicle;
	}


	public double calcChargingPower(double maxPower) {
		Battery b = electricVehicle.getBattery();
		double relativeSoc = b.getSoc() / b.getCapacity();
		double c = b.getCapacity() / 3600;
		if (relativeSoc <= 0.5) {
			return Math.min(maxPower, 1.75 * c);
		} else if (relativeSoc <= 0.75) {
			return Math.min(maxPower, 1.25 * c);
		} else {
			return Math.min(maxPower, 0.5 * c);
		}
	}


	@Override
	public double calcChargingTime(Charger charger, double energy) {
		if (energy < 0) {
			throw new IllegalArgumentException("Energy must be positive");
		}

		Battery b = electricVehicle.getBattery();
		double startSoc = b.getSoc();
		double endSoc = startSoc + energy;
		if (endSoc > b.getCapacity()) {
			throw new IllegalArgumentException("End SOC must not be greater than 100%");
		}

		double threshold1 = 0.5 * b.getCapacity();
		double threshold2 = 0.75 * b.getCapacity();
		double c = b.getCapacity() / 3600;

		double energyA = startSoc >= threshold1 ? 0 : Math.min(threshold1, endSoc) - startSoc;
		double timeA = energyA / Math.min(charger.getPower(), 1.75 * c);

		double energyB = startSoc >= threshold2 || endSoc <= threshold1 ?
				0 :
				Math.min(threshold2, endSoc) - Math.max(threshold1, startSoc);
		double timeB = energyB / Math.min(charger.getPower(), 1.25 * c);

		double energyC = endSoc <= threshold2 ? 0 : endSoc - Math.max(threshold2, startSoc);
		double timeC = energyC / Math.min(charger.getPower(), 0.5 * c);

		return timeA + timeB + timeC;
	}


	@Override
	public double calcChargingPower(Charger charger) {
		return calcChargingPower(charger.getPower());
	}
}
