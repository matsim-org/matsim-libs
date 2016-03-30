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

package playground.polettif.boescpa.converters.osm.scheduleCreator.hafasCreator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Different methods for the Schedule creation from HAFAS.
 *
 * @author boescpa
 */
public class HAFASUtils {
	protected static Logger log = Logger.getLogger(HAFASUtils.class);

	protected static void removeNonUsedStopFacilities(TransitSchedule schedule) {
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

	protected static void cleanVehicles(TransitSchedule schedule, Vehicles vehicles) {
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
	}

	protected static void cleanDepartures(TransitSchedule schedule) {
		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				final Set<Double> departureTimes = new HashSet<>();
				final List<Departure> departuresToRemove = new ArrayList<>();
				for (Departure departure : route.getDepartures().values()) {
					double dt = departure.getDepartureTime();
					if (departureTimes.contains(dt)) {
						departuresToRemove.add(departure);
					} else {
						departureTimes.add(dt);
					}
				}
				for (Departure departure2Remove : departuresToRemove) {
					route.removeDeparture(departure2Remove);
				}
			}
		}
	}

	protected static void uniteSameRoutesWithJustDifferentDepartures(TransitSchedule schedule) {
		long totalNumberOfDepartures = 0;
		long departuresWithChangedSchedules = 0;
		long totalNumberOfStops = 0;
		long stopsWithChangedTimes = 0;
		double changedTotalTimeAtStops = 0.;
		List<Double> timeChanges = new ArrayList<>();
		for (TransitLine line : schedule.getTransitLines().values()) {
			// Collect all route profiles
			final Map<String, List<TransitRoute>> routeProfiles = new HashMap<>();
			for (TransitRoute route : line.getRoutes().values()) {
				totalNumberOfDepartures += route.getDepartures().size();
				totalNumberOfStops += route.getDepartures().size() * route.getStops().size();
				String routeProfile = route.getStops().get(0).getStopFacility().getId().toString();
				for (int i = 1; i < route.getStops().size(); i++) {
					//routeProfile = routeProfile + "-" + route.getStops().get(i).toString() + ":" + route.getStops().get(i).getDepartureOffset();
					routeProfile = routeProfile + "-" + route.getStops().get(i).getStopFacility().getId().toString();
				}
				List profiles = routeProfiles.get(routeProfile);
				if (profiles == null) {
					profiles = new ArrayList();
					routeProfiles.put(routeProfile, profiles);
				}
				profiles.add(route);
			}
			// Check profiles and if the same, add latter to former.
			for (List<TransitRoute> routesToUnite : routeProfiles.values()) {
				TransitRoute finalRoute = routesToUnite.get(0);
				for (int i = 1; i < routesToUnite.size(); i++) {
					TransitRoute routeToAdd = routesToUnite.get(i);
					// unite departures
					for (Departure departure : routeToAdd.getDepartures().values()) {
						finalRoute.addDeparture(departure);
					}
					line.removeRoute(routeToAdd);
					// make analysis
					int numberOfDepartures = routeToAdd.getDepartures().size();
					boolean departureWithChangedDepartureTimes = false;
					for (int j = 0; j < finalRoute.getStops().size(); j++) {
						double changedTotalTimeAtStop =
								Math.abs(finalRoute.getStops().get(j).getArrivalOffset() - routeToAdd.getStops().get(j).getArrivalOffset())
										+ Math.abs(finalRoute.getStops().get(j).getDepartureOffset() - routeToAdd.getStops().get(j).getDepartureOffset());
						if (changedTotalTimeAtStop > 0) {
							stopsWithChangedTimes += numberOfDepartures;
							changedTotalTimeAtStops += changedTotalTimeAtStop*numberOfDepartures;
							for (int k = 0; k < numberOfDepartures; k++) {
								timeChanges.add(changedTotalTimeAtStop);
							}
							departureWithChangedDepartureTimes = true;
						}
					}
					if (departureWithChangedDepartureTimes) {
						departuresWithChangedSchedules += numberOfDepartures;
					}
				}
			}
		}
		log.info("LINE UNIFICATION: Total Number of Departures: " + totalNumberOfDepartures);
		log.info("LINE UNIFICATION: Number of Departures with changed schedule: " + departuresWithChangedSchedules);
		log.info("LINE UNIFICATION: Total Number of Stops: " + totalNumberOfStops);
		log.info("LINE UNIFICATION: Number of Stops with changed departure or arrival times: " + stopsWithChangedTimes);
		log.info("LINE UNIFICATION: Total time difference caused by changed departure or arrival times: " + changedTotalTimeAtStops);
		log.info("LINE UNIFICATION: Average time difference caused by changed times: " + (changedTotalTimeAtStops/stopsWithChangedTimes));
		log.info("LINE UNIFICATION: Average time difference over all stops caused by changed times: " +
				(changedTotalTimeAtStops/totalNumberOfStops));
		//writeChangedTimes(timeChanges);
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
}
