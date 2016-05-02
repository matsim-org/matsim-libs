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

package playground.boescpa.converters.visum.preprocessing;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import playground.boescpa.analysis.spatialCutters.SHPFileCutter;
import playground.boescpa.analysis.trips.*;
import playground.boescpa.lib.tools.NetworkUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Creates and prepares trips for visum-conversion.
 *
 * @author boescpa
 */
public class VisumTripCreator {

    public static List<Trip> trips;
    public static List<Id<Person>> failedAgents;

    public static void main(String[] args) {
		String eventsFile = args[0]; // Path to an events-File
		String networkFile = args[1]; // Path to the network-File used for the simulation resulting in the above events-File
		String tripFile = args[2]; // Path to the trip-File produced as output
		String shpFile = args[3]; // Path to a shp-File which defines the considered area.

        Network network = NetworkUtils.readNetwork(networkFile);
        SpatialTripCutter spatialTripCutter = new SpatialTripCutter(new SHPFileCutter(shpFile), network);
        trips = new EventsToTrips(network).createTripsFromEvents(eventsFile);
        trips = TripFilter.spatialTripFilter(trips, spatialTripCutter);
        trips = TripFilter.removeUnfinishedTrips(trips, failedAgents);
        TripWriter.writeTrips(trips, tripFile);
	}


}
