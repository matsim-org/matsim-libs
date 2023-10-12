package org.matsim.vehicles;

import org.matsim.api.core.v01.Id;

import java.util.HashMap;
import java.util.Map;

public class PersonVehicles {

    Map<String, Id<Vehicle>> modeVehicles;

    public PersonVehicles() {
        modeVehicles = new HashMap<>();
    }

    public PersonVehicles(Map<String, Id<Vehicle>> list) {
        modeVehicles = list;
    }

    public void addModeVehicle(String mode, Id<Vehicle> vehicle) {
        modeVehicles.put(mode, vehicle);
    }
    public void addModeVehicleList(Map<String, Id<Vehicle>> list) {
        modeVehicles.putAll(list);
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
