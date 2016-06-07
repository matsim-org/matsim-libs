/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
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
 * *********************************************************************** *
 */

package playground.polettif.publicTransitMapping.hafas.lib;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Different methods for the Schedule creation from HAFAS.
 *
 * @author boescpa
 */
public class HAFASUtils {
	protected static Logger log = Logger.getLogger(HAFASUtils.class);

	public static void removeNonUsedStopFacilities(TransitSchedule schedule) {
		// Collect all used stop facilities:
		Set<Id<TransitStopFacility>> usedStopFacilities = new HashSet<>();
		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (TransitRouteStop stop : route.getStops()) {
					usedStopFacilities.add(stop.getStopFacility().getId());
				}
			}
		}
		// Check all stop facilities if not used:
		Set<TransitStopFacility> unusedStopFacilites = new HashSet<>();
		for (Id<TransitStopFacility> facilityId : schedule.getFacilities().keySet()) {
			if (!usedStopFacilities.contains(facilityId)) {
				unusedStopFacilites.add(schedule.getFacilities().get(facilityId));
			}
		}
		// Remove all stop facilities not used:
		for (TransitStopFacility facility : unusedStopFacilites) {
			schedule.removeStopFacility(facility);
		}
	}

	public static void cleanVehicles(TransitSchedule schedule, Vehicles vehicles) {
		final Set<Id<Vehicle>> usedVehicles = new HashSet<>();
		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (Departure departure : route.getDepartures().values()) {
					usedVehicles.add(departure.getVehicleId());
				}
			}
		}
		final Set<Id<Vehicle>> vehicles2Remove = new HashSet<>();
		for (Id<Vehicle> vehicleId : vehicles.getVehicles().keySet()) {
			if (!usedVehicles.contains(vehicleId)) {
				vehicles2Remove.add(vehicleId);
			}
		}
		for (Id<Vehicle> vehicleId : vehicles2Remove) {
			if (!usedVehicles.contains(vehicleId)) {
				vehicles.removeVehicle(vehicleId);
			}
		}
/*
		final Set<VehicleType> vehicleTypes2Remove = new HashSet<>();
		for(VehicleType vehicleType : vehicles.getVehicleTypes().values()) {
			if(!HafasDefinitions.Vehicles.valueOf(vehicleType.getId().toString()).addToSchedule) {
				vehicleTypes2Remove.add(vehicleType);
			}
		}
		for (VehicleType vehicleType : vehicleTypes2Remove) {
			// TODO implement vehicle removal correctly
//				vehicles.removeVehicleType(vehicleType.getId());
		}
		*/
	}

	private static void writeChangedTimes(List<Double> timeChanges) {
		BufferedWriter bw = null;
		try {
			// here absolute path hard-coded (not very elegant but as it is only for analysis purposes...)
			bw = new BufferedWriter(new FileWriter("c:\\changedTimes.csv"));
			for (double timeDelta : timeChanges) {
				bw.write(timeDelta + "\n");
			}
		} catch (IOException e) {
			throw new RuntimeException("Error while writing changedTimes.csv", e);
		} finally {
			if (bw != null) {
				try { bw.close(); }
				catch (IOException e) { System.out.print("Could not close stream."); }
			}
		}
	}

	@Deprecated
	public static void removeNonUsableVehicles(TransitSchedule schedule, Vehicles vehicles) {
		for(TransitLine line : schedule.getTransitLines().values()) {
			for(TransitRoute route : line.getRoutes().values()) {
//				route.getDepartures().
			}
		}
	}
}
