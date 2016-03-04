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

package playground.boescpa.av.staticDemand.bikeSharing;

import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.MatsimRandom;
import playground.boescpa.analysis.trips.Trip;
import playground.boescpa.analysis.trips.TripReader;
import playground.boescpa.av.staticDemand.StaticDemand;

import java.util.*;

/**
 * WHAT IS IT FOR?
 * WHAT DOES IT?
 *
 * @author boescpa
 */
public class StaticDemandBikeImpl extends StaticDemand {
    private static Random random = MatsimRandom.getRandom();

    private final double shareOfOriginalAgentsServedByAV;

    private final List<Trip> originalDemand;
    private final List<Trip> filteredDemand;
    private final List<Trip> sortedDemand;

    public StaticDemandBikeImpl(String pathToTripFile, double shareOfOriginalAgentsServedByAV) {
        this.shareOfOriginalAgentsServedByAV = shareOfOriginalAgentsServedByAV;
        log.info("Read demand...");
        this.originalDemand = TripReader.createTripCollection(pathToTripFile);
        log.info("Read demand... done.");
        log.info("Filter demand...");
        this.filteredDemand = filterTrips(TripReader.createTripCollection(pathToTripFile));
        log.info("Filter demand... done.");
        log.info("Sort demand...");
        this.sortedDemand = new ArrayList<>();
        this.sortedDemand.addAll(this.filteredDemand);
        sortTripsByStartTime(this.sortedDemand);
        log.info("Sort demand... done.");
    }

    public List<Trip> getOriginalDemand() {
        return Collections.unmodifiableList(originalDemand);
    }

    public List<Trip> getFilteredDemand() {
        return Collections.unmodifiableList(filteredDemand);
    }

    public List<Trip> getSortedDemand() {
        return Collections.unmodifiableList(sortedDemand);
    }

    private void sortTripsByStartTime(List<Trip> trips) {
        Collections.sort(trips, new Comparator<Trip>() {
            @Override
            public int compare(Trip o1, Trip o2) {
                return (int) (o2.startTime - o1.startTime);
            }
        });
    }

    /**
     * Filter trips for modes and for number...
     * @return
     */
    private List<Trip> filterTrips(List<Trip> originalDemand) {
        List<Trip> distanceFilteredTrips = new ArrayList<>();
        // filter distance:
		for (Trip trip : originalDemand) {
			if (trip.distance >= Constants.DISTANCE_MIN_REPLACE && trip.distance <= Constants.DISTANCE_MAX_REPLACE) {
				distanceFilteredTrips.add(trip);
			}
		}
        // sample demand via share of agents:
        List<Id> agents = getAllAgents(distanceFilteredTrips);
        log.info("Sample demand...");
        long shareToRemove = Math.round(agents.size() * (1- shareOfOriginalAgentsServedByAV));
        for (int i = 0; i < shareToRemove; i++) {
            agents.remove(random.nextInt(agents.size()));
        }
        Set<Id> sampledAgents = new HashSet<>();
        sampledAgents.addAll(agents);
        List<Trip> filteredTrips = new ArrayList<>();
        for (Trip trip : distanceFilteredTrips) {
            if (sampledAgents.contains(trip.agentId)) {
                filteredTrips.add(trip);
            }
        }
        log.info("Sample demand... done.");
        return filteredTrips;
    }
}
