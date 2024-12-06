/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

import java.util.Collection;

import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;

public interface ChargingLogic {
	interface Factory {
		ChargingLogic create(ChargerSpecification charger);
	}

	void addVehicle(ElectricVehicle ev, ChargingStrategy strategy, double now);

	void addVehicle(ElectricVehicle ev, ChargingStrategy strategy, ChargingListener chargingListener, double now);

	void removeVehicle(ElectricVehicle ev, double now);

	void chargeVehicles(double chargePeriod, double now);

	Collection<ChargingVehicle> getPluggedVehicles();

	Collection<ChargingVehicle> getQueuedVehicles();

	record ChargingVehicle(ElectricVehicle ev, ChargingStrategy strategy) {}
}
