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

package playground.boescpa.av.staticDemand;

import org.apache.log4j.Logger;
import playground.boescpa.lib.tools.tripReader.Trip;

import java.util.*;

/**
 * WHAT IS IT FOR?
 * WHAT DOES IT?
 *
 * @author boescpa
 */
public class StaticDemand {
	private static Logger log = Logger.getLogger(StaticDemand.class);

	private final List<Trip> filteredDemand;
	private final List<Trip> sortedDemand;

	public StaticDemand(String pathToTripFile, String[] modes) {
		log.info("Read demand...");
		final Map<Long, Trip> tripsOriginal = Trip.createTripCollection(pathToTripFile);
		log.info("Read demand... done.");
		log.info("Filter demand...");
		this.filteredDemand = filterTrips(tripsOriginal, modes);
		log.info("Filter demand... done.");
		log.info("Sort demand...");
		this.sortedDemand = new ArrayList<>();
		this.sortedDemand.addAll(this.filteredDemand);
		sortTripsByStartTime(this.sortedDemand);
		log.info("Sort demand... done.");
	}

	public List<Trip> getFilteredDemand() {
		return Collections.unmodifiableList(filteredDemand);
	}

	public List<Trip> getSortedDemand() {
		return Collections.unmodifiableList(sortedDemand);
	}

	private void sortTripsByStartTime(List<Trip> trips) {
		trips.sort(new Comparator<Trip>() {
			@Override
			public int compare(Trip o1, Trip o2) {
				return (int) (o2.startTime - o1.startTime);
			}
		});
	}

	private List<Trip> filterTrips(Map<Long, Trip> trips, String[] modes) {
		List<Trip> filteredTrips = new ArrayList<>();
		for (String mode : modes) {
			for (Trip trip : trips.values()) {
				if (mode.equals(trip.mode)) {
					filteredTrips.add(trip);
				}
			}
		}
		return filteredTrips;
	}
}
