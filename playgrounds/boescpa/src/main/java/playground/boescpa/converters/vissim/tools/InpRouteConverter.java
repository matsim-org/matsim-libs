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

package playground.boescpa.converters.vissim.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.io.IOUtils;

/**
 * Provides a vissim-inp specific implementation of RouteConverter.
 *
 * @author boescpa
 */
public class InpRouteConverter extends AbstractRouteConverter {

	/**
	 * Parses the provided Vissim-Inp-File (Vissim 5.4 format) and transform the routes into a trips.
	 *
	 * @param path2InpFile Path to a Vissim-Inp-File
	 * @param notUsed
	 * @param notUsedToo
	 * @return
	 */
	@Override
	protected List<Trip> routes2Trips(String path2InpFile, String notUsed, String notUsedToo) {
		final List<Trip> trips = new ArrayList<Trip>();

		// Read inp-routes:
		try {
			final BufferedReader in = IOUtils.getBufferedReader(path2InpFile);
			String line = in.readLine();
			Pattern rdPattern = Pattern.compile("ROUTING_DECISION .*");
			Pattern rPattern = Pattern.compile(" +ROUTE .*");
			Pattern oPattern = Pattern.compile(" +OVER .*");
			Pattern nPattern = Pattern.compile(" *");
			Pattern numPattern = Pattern.compile(" +\\d+.*");
			String routingDecision = "";
			Trip currentTrip = null;
			boolean inTrip = false;
			while (line != null) {
				// ROUTING_DECISION:
				if (rdPattern.matcher(line).matches()) {
					String[] lineVals = line.split(" +");
					routingDecision = lineVals[1];
				}
				// ROUTE:
				if (rPattern.matcher(line).matches()) {
					String[] lineVals = line.split(" +");
					if (currentTrip != null) {
						trips.add(currentTrip);
					}
					currentTrip = new Trip(Id.create(routingDecision + "-" + lineVals[2], Trip.class), 0.0);
					inTrip = true;
				}
				if (inTrip) {
					if (oPattern.matcher(line).matches()) {
						String[] lineVals = line.split(" +");
						for (int i = 2; i < lineVals.length; i++) {
							currentTrip.links.add(Id.create(Long.parseLong(lineVals[i]), Link.class));
						}
					} else if (nPattern.matcher(line).matches()) {
						trips.add(currentTrip);
						currentTrip = null;
						inTrip = false;
					} else if (numPattern.matcher(line).matches()) {
						String[] lineVals = line.split(" +");
						for (int i = 1; i < lineVals.length; i++) {
							currentTrip.links.add(Id.create(Long.parseLong(lineVals[i]), Link.class));
						}
					}
				}
				line = in.readLine();
			}
		}
		catch(IOException e) {
			System.out.println("Reading of " + path2InpFile + " failed.");
			e.printStackTrace();
		}

		return trips;
	}

}
