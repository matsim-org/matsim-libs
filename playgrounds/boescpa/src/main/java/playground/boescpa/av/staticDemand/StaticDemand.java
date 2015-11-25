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
import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.MatsimRandom;
import playground.boescpa.analysis.trips.tripReader.Trip;

import java.util.*;

/**
 * WHAT IS IT FOR?
 * WHAT DOES IT?
 *
 * @author boescpa
 */
public class StaticDemand {
    private static Logger log = Logger.getLogger(StaticDemand.class);
    private static Random random = MatsimRandom.getRandom();

    private final double shareOfOriginalAgentsServedByAV;
    private final String[] modes;

    private final List<Trip> originalDemand;
    private final List<Trip> filteredDemand;
    private final List<Trip> sortedDemand;

    public StaticDemand(String pathToTripFile, String[] modes, double shareOfOriginalAgentsServedByAV) {
        this.shareOfOriginalAgentsServedByAV = shareOfOriginalAgentsServedByAV;
        this.modes = modes;
        log.info("Read demand...");
        final Map<Long, Trip> tripsOriginal = Trip.createTripCollection(pathToTripFile);
        this.originalDemand = new ArrayList<>();
        this.originalDemand.addAll(tripsOriginal.values());
        log.info("Read demand... done.");
        log.info("Filter demand...");
        this.filteredDemand = filterTrips(tripsOriginal);
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
     * @param trips
     * @return
     */
    private List<Trip> filterTrips(Map<Long, Trip> trips) {
        List<Trip> modeFilteredTrips = new ArrayList<>();
        // filter mode:
        for (String mode : modes) {
            for (Trip trip : trips.values()) {
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

    protected static List<Id> getAllAgents(List<Trip> modeFilteredTrips) {
        log.info("Filter demand for agents...");
        List<Id> agents = new ArrayList<>();
        Set<Id> agentsSet = new HashSet<>();
        for (Trip trip : modeFilteredTrips) {
            if (!agentsSet.contains(trip.agentId)) {
                agentsSet.add(trip.agentId);
                agents.add(trip.agentId);
            }
        }
        log.info("Filter demand for agents... done.");
        log.info("Total " + agents.size() + " driving agents found.");
        return agents;
    }
}
