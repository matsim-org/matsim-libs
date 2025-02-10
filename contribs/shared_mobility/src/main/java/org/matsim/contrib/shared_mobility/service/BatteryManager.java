package org.matsim.contrib.shared_mobility.service;

import java.util.HashMap;
import java.util.Map;
import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;

public class BatteryManager {
	private static final double MAX_BATTERY_CAPACITY = 500; // 500 Wh
	private static final double ENERGY_CONSUMPTION_PER_KM = 10; // 10 Wh/km

	private static final Map<Id<Vehicle>, Double> batteryLevels = new HashMap<>();

	// Initialize the battery status of the vehicle
	public static void initializeBattery(Id<Vehicle> vehicleId) {
		batteryLevels.put(vehicleId, MAX_BATTERY_CAPACITY);
	}

	// Check if the vehicle has enough battery for the trip
	public static boolean hasEnoughBattery(Id<Vehicle> vehicleId, double distance) {
		return batteryLevels.getOrDefault(vehicleId, 0.0) >= distance * ENERGY_CONSUMPTION_PER_KM;
	}

	// Consume battery power based on the traveled distance
	public static void consumeBattery(Id<Vehicle> vehicleId, double distance) {
		batteryLevels.put(vehicleId, batteryLevels.get(vehicleId) - (distance * ENERGY_CONSUMPTION_PER_KM));
	}

	// Get the remaining driving range in kilometers
	public static double getRemainingRange(Id<Vehicle> vehicleId) {
		return batteryLevels.getOrDefault(vehicleId, 0.0) / ENERGY_CONSUMPTION_PER_KM;
	}

	// Recharge the battery (e.g., overnight charging)
	public static void recharge(Id<Vehicle> vehicleId) {
		batteryLevels.put(vehicleId, MAX_BATTERY_CAPACITY);
	}
}
