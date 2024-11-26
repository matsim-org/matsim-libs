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
import org.matsim.vehicles.Vehicle;

/**
 * @author Michal Maciejewski (michalm)
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class ChargeUpToMaxSocStrategy implements ChargingStrategy {
	static public final String MAXIMUM_SOC_VEHICLE_ATTRIBUTE = "maximumSoc";

	private final ElectricVehicle ev;
	private final ChargerSpecification charger;
	private final double maxSoc;

	public ChargeUpToMaxSocStrategy(ChargerSpecification charger, ElectricVehicle ev, double maxSoc) {
		if (maxSoc < 0 || maxSoc > 1) {
			throw new IllegalArgumentException();
		}
		this.charger = charger;
		this.maxSoc = maxSoc;
		this.ev = ev;
	}

	@Override
	public double calcRemainingEnergyToCharge() {
		Battery battery = ev.getBattery();
		return maxSoc * battery.getCapacity() - battery.getCharge();
	}

	@Override
	public double calcRemainingTimeToCharge() {
		return ((BatteryCharging)ev.getChargingPower()).calcChargingTime(charger, calcRemainingEnergyToCharge());
	}

	@Override
	public boolean isChargingCompleted() {
		return calcRemainingEnergyToCharge() <= 0;
	}

	static public class Factory implements ChargingStrategy.Factory {
		private final double maxSoc;

		public Factory(double maxSoc) {
			this.maxSoc = maxSoc;
		}		

		@Override
		public ChargingStrategy createStrategy(ChargerSpecification charger, ElectricVehicle ev) {
			double vehicleMaximumSoc = maxSoc;

			Vehicle vehicle = ev.getVehicleSpecification().getMatsimVehicle();
			if (vehicle != null) {
				Double value = (Double) vehicle.getAttributes().getAttribute(MAXIMUM_SOC_VEHICLE_ATTRIBUTE);
				
				if (value != null) {
					vehicleMaximumSoc = value;
				}
			}
			
			return new ChargeUpToMaxSocStrategy(charger, ev, vehicleMaximumSoc);
		}
	}
}
