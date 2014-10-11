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

package playground.boescpa.converters.vissim.mains;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;

import playground.boescpa.converters.vissim.ConvEvents;
import playground.boescpa.converters.vissim.tools.AbstractRouteConverter.Trip;
import playground.boescpa.converters.vissim.tools.TripMatcher;

/**
 * Matches the routes provided and creates a demand file.
 *
 * @author boescpa
 */
public class MatchTrips {

	private static final String delimiter = ", ";

	public static void main(String[] args) {
		String path2MsRoutes = args[0];
		String path2AmRoutes = args[1];
		String path2WriteDemands = args[2];
		String path2InpRoutes = args[3];

		for (int i = 0; i < 31; i++) {
			// Read trips
			HashMap<Id<Trip>, Long[]> msTrips = MapRoutes.readRoutes(ConvEvents.insertVersNumInFilepath(path2MsRoutes,i));
			//HashMap<Id, Long[]> amTrips = MapRoutes.readRoutes(path2AmRoutes);
			HashMap<Id<Trip>, Long[]> inpTrips = MapRoutes.readRoutes(path2InpRoutes);

			// Match trips
			ConvEvents.TripMatcher tripMatcher = new TripMatcher();
			HashMap<Id<Trip>, Integer> results = tripMatcher.matchTrips(msTrips, inpTrips);

			writeTripDemands(results, ConvEvents.insertVersNumInFilepath(path2WriteDemands,i));
		}
	}

	private static void writeTripDemands(HashMap<Id<Trip>, Integer> results, String path2WriteDemands) {
		try {
			final String header = "RouteId, Demand";
			final BufferedWriter out = IOUtils.getBufferedWriter(path2WriteDemands);
			out.write(header); out.newLine();
			for (Id<Trip> routeId : results.keySet()) {
				String line = routeId.toString();
				line = line + delimiter + results.get(routeId).toString();
				out.write(line); out.newLine();
			}
			out.close();
		} catch (IOException e) {
			System.out.println("Writing of " + path2WriteDemands + " failed.");
			e.printStackTrace();
		}
	}

	public static HashMap<Id<Trip>, Integer> readTripDemands(String path2DemandFile) {
		HashMap<Id<Trip>, Integer> demands = new HashMap<>();
		try {
			final BufferedReader in = IOUtils.getBufferedReader(path2DemandFile);
			in.readLine(); // header
			String line = in.readLine();
			while (line != null) {
				String[] route = line.split(delimiter);
				String routeId = route[0];
				int routeDemand = Integer.parseInt(route[1]);
				demands.put(Id.create(routeId, Trip.class), routeDemand);
				line = in.readLine();
			}
		} catch (IOException e) {
			System.out.println("Reading of " + path2DemandFile + " failed.");
			e.printStackTrace();
		}
		return demands;
	}

}
