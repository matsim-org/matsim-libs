package org.matsim.vehicles;

import org.matsim.api.core.v01.Id;

import java.util.HashMap;
import java.util.Map;

/**
 * Container class to store mode specific person vehicle types.
 */
public final class PersonVehicleTypes {

	private final Map<String, Id<VehicleType>> modeVehicleTypes = new HashMap<>();

	public PersonVehicleTypes() {
	}

	public void addModeVehicleType(String mode, Id<VehicleType> vehicleType) {
		modeVehicleTypes.put(mode, vehicleType);
	}

	public Id<VehicleType> getVehicleType(String mode) {
		return modeVehicleTypes.get(mode);
	}

	public Map<String, Id<VehicleType>> getModeVehicleTypes() {
		return modeVehicleTypes;
	}

	public void putModeVehicleTypes(Map<String, Id<VehicleType>> vehicleTypes) {
		modeVehicleTypes.putAll(vehicleTypes);
	}
}
