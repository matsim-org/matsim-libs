/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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

package ch.sbb.matsim.contrib.railsim.qsimengine;

import ch.sbb.matsim.contrib.railsim.RailsimUtils;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

import java.util.*;

/**
 * Manager active trains and can access the state of all trains in the simulation.
 */
@Singleton
public class TrainManager {

	private static final Logger log = LogManager.getLogger(TrainManager.class);

	/**
	 * Current active trains in the simulation.
	 */
	private final Map<Id<Vehicle>, TrainState> activeTrains = new LinkedHashMap<>();

	/**
	 * Maps vehicle IDs to their constituent unit IDs.
	 */
	private final Map<Id<Vehicle>, List<String>> formations = new LinkedHashMap<>();

	/**
	 * Maps vehicles to related vehicles that contain the same units.
	 */
	private final Map<Id<Vehicle>, List<Id<Vehicle>>> relatedVehicles = new LinkedHashMap<>();

	@Inject
	public TrainManager(Scenario scenario) {
		initializeFormations(scenario.getTransitSchedule());
		initializeRelatedVehicles(scenario.getTransitVehicles());
	}

	/**
	 * Empty constructor, not using formations.
	 */
	TrainManager() {
	}

	Collection<TrainState> getActiveTrains() {
		return activeTrains.values();
	}

	/**
	 * Remove state and return if it was removed.
	 */
	boolean removeActiveTrain(TrainState state) {
		return activeTrains.remove(state.driver.getVehicle().getId()) != null;
	}

	/**
	 * Return current train state for one specific vehicle.
	 */
	public TrainPosition getActiveTrain(Id<Vehicle> id) {
		return activeTrains.get(id);
	}

	/**
	 * Add an active train to the simulation.
	 */
	void addActiveTrain(TrainState state) {
		activeTrains.put(state.driver.getVehicle().getId(), state);
	}

	/**
	 * Initialize formations from the transit schedule.
	 * This should be called during startup.
	 */
	private void initializeFormations(TransitSchedule schedule) {
		formations.clear();

		int formationsProcessed = 0;

		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (Departure departure : route.getDepartures().values()) {

					List<String> formation = RailsimUtils.getFormation(departure);
					Id<Vehicle> vehicleId = departure.getVehicleId();

					// Empty and single formations are not saved
					if (!formation.isEmpty() && !formation.getFirst().equals(vehicleId.toString())) {
						// Store the formation for the vehicle used by this departure
						formations.put(vehicleId, formation);
						formationsProcessed++;
					}
				}
			}
		}

		log.info("Initialized {} formations for vehicles", formationsProcessed);
	}

	/**
	 * Initialize the related vehicles map based on shared units in formations.
	 * Vehicles are considered related if they:
	 * 1. Share any unit IDs in their formations, OR
	 * 2. A vehicle's ID appears as a unit in another vehicle's formation, OR
	 * 3. A vehicle's formation contains other vehicle IDs as units, OR
	 * 4. Vehicle IDs appear together as units in the same formation
	 */
	private void initializeRelatedVehicles(Vehicles transitVehicles) {
		relatedVehicles.clear();

		// First, create a mapping from unit IDs to vehicles that contain them
		Map<String, List<Id<Vehicle>>> unitToVehicles = new LinkedHashMap<>();

		for (Map.Entry<Id<Vehicle>, List<String>> entry : formations.entrySet()) {
			Id<Vehicle> vehicleId = entry.getKey();
			List<String> formation = entry.getValue();

			for (String unitId : formation) {
				unitToVehicles.computeIfAbsent(unitId, k -> new ArrayList<>()).add(vehicleId);
			}
		}

		// Process all vehicles in the transit vehicles collection
		for (Vehicle vehicle : transitVehicles.getVehicles().values()) {
			Id<Vehicle> vehicleId = vehicle.getId();

			// Skip vehicles with null IDs
			if (vehicleId == null) {
				continue;
			}

			List<Id<Vehicle>> related = new ArrayList<>();

			// Case 1: If this vehicle has a formation, find vehicles that share units
			if (formations.containsKey(vehicleId)) {
				List<String> formation = formations.get(vehicleId);

				for (Map.Entry<Id<Vehicle>, List<String>> otherEntry : formations.entrySet()) {
					Id<Vehicle> otherVehicleId = otherEntry.getKey();
					List<String> otherFormation = otherEntry.getValue();

					// Skip self
					if (vehicleId.equals(otherVehicleId)) {
						continue;
					}

					// Check if formations share any units
					if (formation.stream().anyMatch(otherFormation::contains)) {
						related.add(otherVehicleId);
					}
				}
			}

			// Case 2: If this vehicle's ID appears as a unit in other formations
			List<Id<Vehicle>> vehiclesContainingThisUnit = unitToVehicles.get(vehicleId.toString());
			if (vehiclesContainingThisUnit != null) {
				for (Id<Vehicle> containingVehicle : vehiclesContainingThisUnit) {
					if (!vehicleId.equals(containingVehicle)) {
						related.add(containingVehicle);
					}
				}
			}

			// Case 3: If this vehicle's formation contains other vehicle IDs as units
			if (formations.containsKey(vehicleId)) {
				List<String> formation = formations.get(vehicleId);
				for (String unitId : formation) {
					// Check if this unit ID corresponds to a vehicle ID
					Id<Vehicle> unitAsVehicleId = Id.create(unitId, Vehicle.class);
					if (transitVehicles.getVehicles().containsKey(unitAsVehicleId)) {
						related.add(unitAsVehicleId);
					}
				}
			}

			// Case 4: If this vehicle appears as a unit in a formation, relate it to other units in the same formation
			for (Map.Entry<Id<Vehicle>, List<String>> formationEntry : formations.entrySet()) {
				List<String> formation = formationEntry.getValue();
				if (formation.contains(vehicleId.toString())) {
					// This vehicle appears as a unit in this formation
					for (String unitId : formation) {
						Id<Vehicle> unitAsVehicleId = Id.create(unitId, Vehicle.class);
						// Add other units in the same formation as related (excluding self)
						if (!vehicleId.equals(unitAsVehicleId) && transitVehicles.getVehicles().containsKey(unitAsVehicleId)) {
							related.add(unitAsVehicleId);
						}
					}
				}
			}

			// Only store if there are related vehicles
			if (!related.isEmpty()) {
				relatedVehicles.put(vehicleId, related);
			}
		}

		log.info("Initialized {} related vehicles for vehicles", relatedVehicles.size());
	}

	/**
	 * Get the formation (list of unit IDs) for a given vehicle ID.
	 * @param vehicleId the vehicle ID
	 * @return list of unit IDs, or empty list if no formation exists
	 */
	public List<String> getFormation(Id<Vehicle> vehicleId) {
		return formations.getOrDefault(vehicleId, Collections.emptyList());
	}

	/**
	 * Check if a vehicle has a formation.
	 * @param vehicleId the vehicle ID
	 * @return true if the vehicle has a formation
	 */
	public boolean hasFormation(Id<Vehicle> vehicleId) {
		return formations.containsKey(vehicleId) && !formations.get(vehicleId).isEmpty();
	}

	/**
	 * Get all formations as an unmodifiable map.
	 * @return map of vehicle ID to unit IDs
	 */
	public Map<Id<Vehicle>, List<String>> getAllFormations() {
		return Collections.unmodifiableMap(formations);
	}

	/**
	 * Get the number of formations currently stored.
	 * @return number of formations
	 */
	public int getFormationCount() {
		return formations.size();
	}

	/**
	 * Return list of vehicle related. See {@link #initializeRelatedVehicles(Vehicles)}.
	 */
	public List<Id<Vehicle>> getRelatedVehicles(Id<Vehicle> vehicleId) {
		return relatedVehicles.getOrDefault(vehicleId, Collections.emptyList());
	}

	/**
	 * Remove active trains.
	 */
	public void clear() {
		activeTrains.clear();
	}
}
