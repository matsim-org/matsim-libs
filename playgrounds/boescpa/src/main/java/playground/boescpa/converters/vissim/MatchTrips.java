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

package playground.boescpa.converters.vissim;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.IOUtils;
import playground.boescpa.converters.vissim.tools.TripMatcher;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;

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

		// Read trips
		HashMap<Id, Long[]> msTrips = MapRoutes.readRoutes(path2MsRoutes);
		//HashMap<Id, Long[]> amTrips = MapRoutes.readRoutes(path2AmRoutes);
		HashMap<Id, Long[]> inpTrips = MapRoutes.readRoutes(path2InpRoutes);

		// Match trips
		ConvEvents2Anm.TripMatcher tripMatcher = new TripMatcher();
		HashMap<Id, Integer> results = tripMatcher.matchTrips(msTrips, inpTrips);

		writeTripDemands(results, path2WriteDemands);
	}

	private static void writeTripDemands(HashMap<Id, Integer> results, String path2WriteDemands) {
		try {
			final String header = "RouteId, Demand";
			final BufferedWriter out = IOUtils.getBufferedWriter(path2WriteDemands);
			out.write(header); out.newLine();
			for (Id routeId : results.keySet()) {
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

	public static HashMap<Id, Integer> readTripDemands(String path2DemandFile) {
		HashMap<Id, Integer> demands = new HashMap<Id, Integer>();
		try {
			final BufferedReader in = IOUtils.getBufferedReader(path2DemandFile);
			in.readLine(); // header
			String line = in.readLine();
			while (line != null) {
				String[] route = line.split(delimiter);
				Id routeId = new IdImpl(route[0]);
				int routeDemand = Integer.parseInt(route[1]);
				demands.put(routeId, routeDemand);
				line = in.readLine();
			}
		} catch (IOException e) {
			System.out.println("Reading of " + path2DemandFile + " failed.");
			e.printStackTrace();
		}
		return demands;
	}

}
