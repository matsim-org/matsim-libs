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

package ch.sbb.matsim.contrib.railsim;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicle;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * Manager for railsim formations information.
 * Maps vehicle IDs to their constituent unit IDs.
 */
@Singleton
public class RailsimFormationsManager {

	private static final Logger log = LogManager.getLogger(RailsimFormationsManager.class);

	private final Map<Id<Vehicle>, List<String>> formations = new ConcurrentHashMap<>();

	@Inject
	public RailsimFormationsManager() {
		// Empty constructor for dependency injection
	}

	/**
	 * Initialize formations from the transit schedule.
	 * This should be called during startup.
	 */
	public void initializeFormations(TransitSchedule schedule) {
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
}
