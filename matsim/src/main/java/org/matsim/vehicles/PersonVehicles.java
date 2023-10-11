package org.matsim.vehicles;

import org.matsim.api.core.v01.Id;

import java.util.HashMap;
import java.util.Map;

/**
 * Container class to store mode specific vehicles and types.
 */
public final class PersonVehicles {

    private final Map<String, Id<Vehicle>> modeVehicles;

	private final Map<String, Id<VehicleType>> modeVehicleTypes;

	public PersonVehicles() {
		this.modeVehicles = new HashMap<>();
		this.modeVehicleTypes = new HashMap<>();
	}

	PersonVehicles(Map<String, Id<Vehicle>> modeVehicles) {
		this.modeVehicles = modeVehicles;
		this.modeVehicleTypes = new HashMap<>();
	}

	PersonVehicles(Map<String, Id<Vehicle>> modeVehicles, Map<String, Id<VehicleType>> modeVehicleTypes) {
		this.modeVehicles =modeVehicles;
		this.modeVehicleTypes = modeVehicleTypes;
	}

	public void addModeVehicle(String mode, Id<Vehicle> vehicle) {
        modeVehicles.put(mode, vehicle);
    }

	public void addModeVehicleType(String mode, Id<VehicleType> vehicleType) {
		modeVehicleTypes.put(mode, vehicleType);
	}

    public void addModeVehicleList(Map<String, Id<Vehicle>> list) {
        modeVehicles.putAll(list);
    }

	public void addModeVehicleTypes(Map<String, Id<VehicleType>> list) {
		modeVehicleTypes.putAll(list);
	}
    public void addModeVehicleIfAbsent(String mode, Id<Vehicle> vehicleId) {
        modeVehicles.putIfAbsent(mode, vehicleId);
    }

    public void addModeVehicleListIfAbsent(Map<String, Id<Vehicle>> list) {
        for (Map.Entry<String, Id<Vehicle>> entry: list.entrySet()) {
            modeVehicles.putIfAbsent(entry.getKey(), entry.getValue());
        }
    }

    public Map<String, Id<Vehicle>> getModeVehicles() {
        return modeVehicles;
    }

    public Id<Vehicle> getVehicle(String mode) {
        return modeVehicles.get(mode);
    }
}
