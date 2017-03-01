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

package playground.michalm.ev.data;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;

public class EvDataImpl implements EvData {
	private final Map<Id<Charger>, Charger> chargers = new LinkedHashMap<>();
	private final Map<Id<Vehicle>, ElectricVehicle> eVehicles = new LinkedHashMap<>();

	@Override
	public Map<Id<Charger>, Charger> getChargers() {
		return Collections.unmodifiableMap(chargers);
	}

	@Override
	public void addCharger(Charger charger) {
		chargers.put(charger.getId(), charger);
	}

	@Override
	public Map<Id<Vehicle>, ElectricVehicle> getElectricVehicles() {
		return Collections.unmodifiableMap(eVehicles);
	}

	@Override
	public void addElectricVehicle(Id<Vehicle> vehicleId, ElectricVehicle ev) {
		eVehicles.put(vehicleId, ev);
	}

	@Override
	public void clearQueuesAndResetBatteries() {
		for (ElectricVehicle ev : eVehicles.values()) {
			ev.getBattery().resetSoc();
		}

		for (Charger c : chargers.values()) {
			c.resetLogic();
		}
	}
}
