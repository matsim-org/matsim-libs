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

import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.MatsimRandom;
import playground.boescpa.analysis.trips.Trip;
import playground.boescpa.analysis.trips.TripReader;

import java.util.*;

/**
 * WHAT IS IT FOR?
 * WHAT DOES IT?
 *
 * @author boescpa
 */
public class StaticDemandAVImpl extends StaticDemand {
    private static Random random = MatsimRandom.getRandom();

    private final double shareOfOriginalAgentsServedByAV;
    private final String[] modes;

    private final List<Trip> originalDemand;
    private final List<Trip> filteredDemand;
    private final List<Trip> sortedDemand;

    public StaticDemandAVImpl(String pathToTripFile, String[] modes, double shareOfOriginalAgentsServedByAV) {
        this.shareOfOriginalAgentsServedByAV = shareOfOriginalAgentsServedByAV;
        this.modes = modes;
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

	@Override
    public List<Trip> getOriginalDemand() {
        return Collections.unmodifiableList(originalDemand);
    }

	@Override
    public List<Trip> getFilteredDemand() {
        return Collections.unmodifiableList(filteredDemand);
    }

	@Override
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
        List<Trip> modeFilteredTrips = new ArrayList<>();
        // filter mode:
        for (String mode : modes) {
            for (Trip trip : originalDemand) {
                if (mode.equals(trip.mode)) {
                    modeFilteredTrips.add(trip);
                }
            }
        }
        // sample demand via share of agents:
        List<Id> agents = getAllAgents(modeFilteredTrips);
        log.info("Sample demand...");
        long shareToRemove = Math.round(agents.size() * (1- shareOfOriginalAgentsServedByAV));
        for (int i = 0; i < shareToRemove; i++) {
            agents.remove(random.nextInt(agents.size()));
        }
        Set<Id> sampledAgents = new HashSet<>();
        sampledAgents.addAll(agents);
        List<Trip> filteredTrips = new ArrayList<>();
        for (Trip trip : modeFilteredTrips) {
            if (sampledAgents.contains(trip.agentId)) {
                filteredTrips.add(trip);
            }
        }
        log.info("Sample demand... done.");
        return filteredTrips;
    }
}
