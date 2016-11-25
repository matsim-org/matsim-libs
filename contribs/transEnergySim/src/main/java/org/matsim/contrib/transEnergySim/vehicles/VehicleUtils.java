/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.transEnergySim.vehicles;

import java.util.ArrayList;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.parkingchoice.lib.GeneralLib;
import org.matsim.contrib.transEnergySim.analysis.energyConsumption.EnergyConsumptionLogRow;
import org.matsim.contrib.transEnergySim.vehicles.api.BatteryElectricVehicle;
import org.matsim.contrib.transEnergySim.vehicles.api.Vehicle;

public class VehicleUtils {

	/**
	 * Prints all electric vehicles (the drivers agentId), which ran out of
	 * battery.
	 * 
	 * @param vehicles
	 */
	public static void printToConsoleVehiclesWhichRanOutOfBattery(
			HashMap<Id<Vehicle>, Vehicle> vehicles) {
		System.out.println("agentId");

		for (Id<Vehicle> vehicleId : vehicles.keySet()) {
			Vehicle vehicle = vehicles.get(vehicleId);

			if (vehicle instanceof BatteryElectricVehicle) {
				BatteryElectricVehicle bev = (BatteryElectricVehicle) vehicle;

				if (bev.didVehicleRunOutOfBattery()) {
					System.out.println(vehicleId);
				}
			}
		}
	}

	public static void writeToFile(HashMap<Id<Vehicle>, Vehicle> vehicles,
			String outputFile) {
		ArrayList<String> list = new ArrayList<String>();
		list.add("agentId");
		for (Id<Vehicle> vehicleId : vehicles.keySet()) {
			Vehicle vehicle = vehicles.get(vehicleId);
			if (vehicle instanceof BatteryElectricVehicle) {
				BatteryElectricVehicle bev = (BatteryElectricVehicle) vehicle;
				if (bev.didVehicleRunOutOfBattery()) {
					list.add(vehicleId.toString());
				}
			}
		}
		GeneralLib.writeList(list, outputFile);
	}

}
