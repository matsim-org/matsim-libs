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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.io.IOUtils;
import playground.boescpa.analysis.spatialCutters.SpatialCutter;
import playground.boescpa.analysis.trips.TripHandler;
import playground.boescpa.analysis.trips.TripProcessor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Extends the abstract TripProcessor with a topdad-specific implementation.
 *
 * @author boescpa
 */
public class TopdadTripProcessor extends TripProcessor {

	private final String valueFile;

	public TopdadTripProcessor(String tripFile, String valueFile, SpatialCutter spatialCutter) {
		super(tripFile, spatialCutter);
		this.valueFile = valueFile;
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
	@Override
	public HashMap<String, Object> analyzeTrips(TripHandler tripData, Network network) {
		HashMap<String, Double> timeMode = new HashMap<String, Double>();
		HashMap<String, Long> distMode = new HashMap<String, Long>();

		log.info("Analyzing trips for topdad...");
		for (Id personId : tripData.getStartLink().keySet()) {
			if (!personId.toString().contains("pt")) {
				ArrayList<Id> startLinks = tripData.getStartLink().getValues(personId);
				ArrayList<String> modes = tripData.getMode().getValues(personId);
				ArrayList<LinkedList<Id>> pathList = tripData.getPath().getValues(personId);
				ArrayList<Double> startTimes = tripData.getStartTime().getValues(personId);
				ArrayList<Id> endLinks = tripData.getEndLink().getValues(personId);
				ArrayList<Double> endTimes = tripData.getEndTime().getValues(personId);

				for (int i = 0; i < startLinks.size(); i++) {
					if (!spatialCutter.spatiallyConsideringTrip(network, startLinks.get(i), endLinks.get(i))) {
						continue;
					}

					if (network.getLinks().get(endLinks.get(i)) != null) {
						String mode = modes.get(i);

						// travel time per mode [minutes]
						double travelTime = calcTravelTime(startTimes.get(i), endTimes.get(i))/60;

						// distance per mode [meters]
						long travelDistance = calcTravelDistance(pathList.get(i), network, startLinks.get(i), endLinks.get(i));

						// store new values
						if (timeMode.containsKey(mode)) {
							travelTime = timeMode.get(mode) + travelTime;
							travelDistance = distMode.get(mode) + travelDistance;
						}
						timeMode.put(mode, travelTime);
						distMode.put(mode, travelDistance);
					}
				}
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
			final BufferedWriter out = IOUtils.getBufferedWriter(valueFile);
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
