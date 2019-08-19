/* *********************************************************************** *
 * project: matsim
 * VehicleUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.vehicles;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;

import java.util.HashMap;
import java.util.Map;


/**
 * @author nagel
 *
 */
public class VehicleUtils {

	private static final VehicleType DEFAULT_VEHICLE_TYPE = VehicleUtils.getFactory().createVehicleType(Id.create("defaultVehicleType", VehicleType.class));
	private static final String VEHICLE_ATTRIBUTE_KEY = "vehicles";

	static {
		VehicleCapacityImpl capacity = new VehicleCapacityImpl();
		capacity.setSeats(4);
		DEFAULT_VEHICLE_TYPE.setCapacity(capacity);
	}

	public static VehiclesFactory getFactory() {
		return new VehiclesFactoryImpl();
	}

	public static Vehicles createVehiclesContainer() {
		return new VehiclesImpl();
	}

	public static VehicleType getDefaultVehicleType() {
		return DEFAULT_VEHICLE_TYPE;
	}

	/**
	 * Creates a vehicle id based on the person and the mode
	 * <p>
	 * If config.qsim().getVehicleSource() is "defaultVehicle" or "fromVehiclesData" the function returns a VehicleId which
	 * has the same value as the person's id
	 * <p>
	 * If config.qsim().getVehicleSource() is "modeVehicleTypesFromVehiclesData", the returned id is a combination of
	 * the person's id and the supplied mode. E.g. "person1_car
	 *
	 * @param person The person which owns the vehicle
	 * @param mode   The mode this vehicle is for
	 * @return a VehicleId
	 */
	public static Id<Vehicle> createVehicleId(Person person, String mode, Config config) {

		if (config.qsim().getUsePersonIdForMissingVehicleId()) {
			switch (config.qsim().getVehiclesSource()) {
				case defaultVehicle:
				case fromVehiclesData:
					return Id.createVehicleId(person.getId());
				case modeVehicleTypesFromVehiclesData:
					return Id.createVehicleId(person.getId().toString() + "_" + mode);
			}
		}
		throw new RuntimeException("not implemented");
	}

	/**
	 * Retrieves a vehicleId from the person's attributes.
	 *
	 * @return the vehicleId of the person's vehicle for the specified mode
	 * @throws RuntimeException In case no vehicleIds were set or in case no vehicleId was set for the specified mode
	 */
	public static Id<Vehicle> getVehicleId(Person person, String mode) {
		Map<String, Id<Vehicle>> vehicleIds = (Map<String, Id<Vehicle>>) person.getAttributes().getAttribute(VehicleUtils.VEHICLE_ATTRIBUTE_KEY);
		if (vehicleIds == null || !vehicleIds.containsKey(mode)) {
			throw new RuntimeException("Could not retrieve vehicle id from person: " + person.getId().toString() + " for mode: " + mode);
		}
		return vehicleIds.get(mode);
	}

	public static void insertVehicleIdIntoAttributes(Person person, String mode, Id<Vehicle> vehicleId) {
		Object attr = person.getAttributes().getAttribute("vehicles");
		Map<String, Id<Vehicle>> map = attr == null ? new HashMap<>() : ((Map<String, Id<Vehicle>>) attr);

		map.put(mode, vehicleId);
		person.getAttributes().putAttribute("vehicles", map);
	}
}
