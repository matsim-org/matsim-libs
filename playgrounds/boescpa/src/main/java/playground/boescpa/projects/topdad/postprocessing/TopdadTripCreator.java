/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.boescpa.projects.topdad.postprocessing;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.io.IOUtils;
import playground.boescpa.analysis.spatialCutters.CirclePointCutter;
import playground.boescpa.analysis.trips.EventsToTrips;
import playground.boescpa.analysis.trips.SpatialTripCutter;
import playground.boescpa.analysis.trips.Trip;
import playground.boescpa.analysis.trips.TripWriter;
import playground.boescpa.lib.tools.NetworkUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * Creates and evaluates trips for topdad-postprocessing.
 *
 * @author boescpa
 */
public class TopdadTripCreator {
    private static Logger log = Logger.getLogger(TopdadTripCreator.class);

    public static List<Trip> trips;
    public static HashMap<String, Object> travelTimesAndDistances;

    public static void main(String[] args) {
        String eventsFile = args[0]; // Path to an events-File
        String networkFile = args[1]; // Path to the network-File used for the simulation resulting in the above events-File
        String tripFile = args[2]; // Path to the trip-File produced as output
        String valueFile = args[3]; // Path to the value-File produced as output, e.g. "vals2030combined.txt"
        int zoneRadius = Integer.valueOf(args[4]);

        Network network = NetworkUtils.readNetwork(networkFile);
        SpatialTripCutter spatialTripCutter = new SpatialTripCutter(new CirclePointCutter(zoneRadius, 683518.0, 246836.0), network);
        trips = EventsToTrips.createTripsFromEvents(eventsFile, network);
        trips = EventsToTrips.filterTrips(trips, spatialTripCutter);
        travelTimesAndDistances = calcTravelTimeAndDistance(trips, valueFile);
        new TripWriter().writeTrips(trips, tripFile);
    }

    /**
     * Calculates the total travel distance and travel time per mode
     * for a given population based on a given events file.
     *
     * The inputs are:
     * 	- tripData: A TripHandler containing the trips read from an events file
     * 	- network: The network used for the simulation
     * 	- outFile: Path to the File where the calculated values will be written
     * 		IMPORTANT: outFile will be overwritten if already existing.
     *
     * If an agent doesn't finish its trip (endLink = null), this trip is not considered
     * for the total travel distances and times.
     *
     * If an agent is a pt-driver ("pt" part of id), the agent is not considered in the calculation.
     *
     * @return HashMap with (String, key) mode and (Double[], value) [time,distance] per mode
     */
    public static HashMap<String, Object> calcTravelTimeAndDistance(List<Trip> trips, String pathToValueFile) {
        HashMap<String, Double> timeMode = new HashMap<>();
        HashMap<String, Double> distMode = new HashMap<>();

        log.info("Analyzing trips for topdad...");
        for (Trip tempTrip : trips) {
            if (tempTrip.endLinkId != null) {
                String mode = tempTrip.mode;

                // travel time per mode [minutes]
                double travelTime = tempTrip.duration/60;

                // distance per mode [meters]
                double travelDistance = tempTrip.distance;

                // store new values
                if (timeMode.containsKey(mode)) {
                    travelTime = timeMode.get(mode) + travelTime;
                    travelDistance = distMode.get(mode) + travelDistance;
                }
                timeMode.put(mode, travelTime);
                distMode.put(mode, travelDistance);
            }
        }

        // ----------- Write Output -----------
        // Write logging:
        log.info("Travel times per mode:");
        for (String mode : timeMode.keySet()) {
            log.info("Mode " + mode + ": " + String.valueOf(timeMode.get(mode)) + " min");
        }
        log.info("Travel distances per mode:");
        for (String mode : distMode.keySet()) {
            log.info("Mode " + mode + ": " + String.valueOf(distMode.get(mode)) + " m");
        }
        // Write to file:
        try {
            final BufferedWriter out = IOUtils.getBufferedWriter(pathToValueFile);
            out.write("Travel times per mode:"); out.newLine();
            for (String mode : timeMode.keySet()) {
                out.write(" - Mode " + mode + ": " + String.valueOf(timeMode.get(mode)) + " min");
                out.newLine();
            }
            out.write("Travel distances per mode:"); out.newLine();
            for (String mode : distMode.keySet()) {
                out.write(" - Mode " + mode + ": " + String.valueOf(distMode.get(mode)) + " m");
                out.newLine();
            }
            out.close();
        } catch (IOException e) {
            log.info("IOException. Could not write topdad-analysis summary to file.");
        }

        log.info("Analyzing trips for topdad...done.");
        HashMap<String, Object> result = new HashMap<String, Object>();
        for (String mode : distMode.keySet()) {
            Double[] val = {timeMode.get(mode), (double)distMode.get(mode)};
            result.put(mode, val);
        }
        return result;
    }

}
