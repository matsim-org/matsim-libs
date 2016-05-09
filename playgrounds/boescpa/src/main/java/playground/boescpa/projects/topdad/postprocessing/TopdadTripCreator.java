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

import org.matsim.api.core.v01.network.Network;
import playground.boescpa.analysis.spatialCutters.CirclePointCutter;
import playground.boescpa.analysis.trips.*;
import playground.boescpa.analysis.trips.tripAnalysis.TravelTimesAndDistances;
import playground.boescpa.lib.tools.NetworkUtils;

import java.util.HashMap;
import java.util.List;

/**
 * Creates and evaluates trips for topdad-postprocessing.
 *
 * @author boescpa
 */
public class TopdadTripCreator {

    public static List<Trip> trips;
    public static HashMap<String, Double[]> travelTimesAndDistances;

    public static void main(String[] args) {
        String eventsFile = args[0]; // Path to an events-File
        String networkFile = args[1]; // Path to the network-File used for the simulation resulting in the above events-File
        String tripFile = args[2]; // Path to the trip-File produced as output
        String valueFile = args[3]; // Path to the value-File produced as output, e.g. "vals2030combined.txt"
        int zoneRadius = Integer.valueOf(args[4]);

        Network network = NetworkUtils.readNetwork(networkFile);
        SpatialTripCutter spatialTripCutter = new SpatialTripCutter(new CirclePointCutter(zoneRadius, 683518.0, 246836.0), network);
        trips = new EventsToTrips(network).createTripsFromEvents(eventsFile);
        trips = TripFilter.spatialTripFilter(trips, spatialTripCutter);
        travelTimesAndDistances = TravelTimesAndDistances.calcTravelTimeAndDistance(trips, valueFile);
        TripWriter.writeTrips(trips, tripFile);
    }

}
