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

package org.matsim.contrib.ev.charging;

import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;

/**
 * @author michalm
 */
public class FixedSpeedChargingStrategy implements ChargingPower {
	private final double maxPower;

	/**
	 * @param electricVehicle
	 * @param relativeSpeed   in C, where 1 C = full recharge in 1 hour
	 */
	public FixedSpeedChargingStrategy(ElectricVehicle electricVehicle, double relativeSpeed) {
		double c = electricVehicle.getBattery().getCapacity() / 3600.;
		maxPower = relativeSpeed * c;
	}

	@Override
	public double calcChargingPower(Charger charger) {
		return Math.min(maxPower, charger.getPower());
	}
}
