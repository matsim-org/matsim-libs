/* *********************************************************************** *
 * project: org.matsim.*
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
 * *********************************************************************** */

package playground.boescpa.topdad.postprocessing;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.io.IOUtils;

/**
 * Provides static methods to analyse and postprocess the trips created with
 * the class MainTripCreator.
 * 
 * @author pboesch
 *
 */
public final class TripProcessing {
	
	private static Logger log = Logger.getLogger(TripProcessing.class);
	
	private TripProcessing() {}
	
	/**
	 * Creates a text file containing all trips based on a given events file.
	 * 
	 * The inputs are:
	 * 	- tripData: A TripHandler containing the trips read from an events file
	 * 	- network: The network used for the simulation
	 * 	- outFile: Path to the File where the trips will be stored
	 * 		IMPORTANT: outFile will be overwritten if already existing.
	 * 
	 * The output format is: agentId, tripStartTime, tripStartLink, tripStartLinkXCoord, tripStartLinkYCoord,
	 * tripEndLink, tripEndLinkXCoord, tripEndLinkYCoord, mainMode, tripPurpose.
	 * 
	 * If the agent is stuck, "stuck" is assigned to the trip purpose, end link and end time are taken from the stuckAndAbort event.
	 * 
	 * If a pt trip only contains transit_walk legs, the main mode is transit walk.
	 */
	public static void printTrips(TripHandler tripHandler, Network network, String outFile) {
		try {
			final String header="agentId\tstartTime\tstartLink\tstartXCoord\tstartYCoord\tendTime\tendLink\tendXCoord\tendYCoord\tmode\tpurpose\ttime\tdistance";
			final BufferedWriter out = IOUtils.getBufferedWriter(outFile); // Path to the trip-File produced as output, e.g. "trips2030combined.txt"
			int incognitoPersonId = 0;
			out.write(header);
			out.newLine();
			log.info("Writing trips file...");
			for (Id personId : tripHandler.getStartLink().keySet()) {
				if (!personId.toString().contains("pt")) {
					incognitoPersonId++;
					
					ArrayList<Id> startLinks = tripHandler.getStartLink().getValues(personId);
					ArrayList<String> modes = tripHandler.getMode().getValues(personId);
					ArrayList<String> purposes = tripHandler.getPurpose().getValues(personId);
					ArrayList<Double> startTimes = tripHandler.getStartTime().getValues(personId);
					ArrayList<Id> endLinks = tripHandler.getEndLink().getValues(personId);
					ArrayList<Double> endTimes = tripHandler.getEndTime().getValues(personId);
					ArrayList<LinkedList<Id>> pathList = tripHandler.getPath().getValues(personId);
					
					for (int i = 0; i < startLinks.size(); i++) {
						// travel time per mode [minutes]
						double travelTime = calcTravelTime(startTimes.get(i), endTimes.get(i))/60;
						// distance per mode [meters]
						long travelDistance = calcTravelDistance(pathList.get(i), network, startLinks.get(i), endLinks.get(i));
						
						if (network.getLinks().get(endLinks.get(i)) != null) {
							out.write(incognitoPersonId
								+ "\t"
								+ startTimes.get(i)
								+ "\t"
								+ startLinks.get(i)
								+ "\t"
								+ network.getLinks().get(startLinks.get(i)).getCoord().getX()
								+ "\t"
								+ network.getLinks().get(startLinks.get(i)).getCoord().getY()
								+ "\t"
								+ endTimes.get(i)
								+ "\t"
								+ endLinks.get(i)
								+ "\t"
								+ network.getLinks().get(endLinks.get(i)).getCoord().getX()
								+ "\t"
								+ network.getLinks().get(endLinks.get(i)).getCoord().getY()
								+ "\t"
								+ modes.get(i)
								+ "\t"
								+ purposes.get(i)
								+ "\t"
								+ String.valueOf(travelTime)
								+ "\t"
								+ String.valueOf(travelDistance)
								);
							out.newLine();
						}
						// in case there is no end link
						else {
							out.write(incognitoPersonId
									+ "\t"
									+ startTimes.get(i)
									+ "\t"
									+ startLinks.get(i)
									+ "\t"
									+ network.getLinks().get(startLinks.get(i)).getCoord().getX()
									+ "\t"
									+ network.getLinks().get(startLinks.get(i)).getCoord().getY()
									+ "\t"
									+ endTimes.get(i)
									+ "\t"
									+ endLinks.get(i)
									+ "\t"
									+ "null"
									+ "\t"
									+ "null"
									+ "\t"
									+ modes.get(i)
									+ "\t"
									+ purposes.get(i)
									+ "\t"
									+ String.valueOf(travelTime)
									+ "\t"
									+ String.valueOf(travelDistance)
									);
								out.newLine();
						}
					}
				}
			}
			out.close();
			log.info("Writing trips file...done.");
		} catch (IOException e) {
			log.info("Given trip-file-path not valid. Print trips not successfully executed.");
		}
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
	public static HashMap<String, Double[]> analyzeTripsTopdad(TripHandler tripData, Network network, String outFile) {
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
			final BufferedWriter out = IOUtils.getBufferedWriter(outFile);
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
		HashMap<String, Double[]> result = new HashMap<String, Double[]>();
		for (String mode : distMode.keySet()) {
			Double[] val = {timeMode.get(mode), (double)distMode.get(mode)};
			result.put(mode, val);
		}
		return result;
	}
	
	/**
	 * Calculates the travel time for a given trip.
	 * 
	 * If no path is provided (path == null), the euclidian distance between the start and the end link is returned.
	 * If the trip wasn't finished (endLink == null), the path length is not calculated (return 0).
	 * 
	 * @param path		of the trip 
	 * @param network	of the simulation
	 * @param startLink	of the trip
	 * @param endLink	of the trip
	 * @return	path length of the trip [m]
	 */
	private static int calcTravelDistance(LinkedList<Id> path, Network network, Id startLink, Id endLink) {
		// If the trip wasn't finished (endLink == null), the path length is not calculated.
		if (endLink == null) {
			return 0;
		}
		
		int travelDistance = 0;
		if (path.size() > 0) {
			// if a path was recorded, use the actual path for travel-distance calculation
			for (Id linkId : path) {
				travelDistance += network.getLinks().get(linkId).getLength();
			}
		} else {
			// if no path available, use euclidian distance as estimation for travel-distance
			Coord coordsStartLink = network.getLinks().get(startLink).getCoord();
			Coord coordsEndLink = network.getLinks().get(endLink).getCoord(); 
			travelDistance += (long) Math.sqrt(
					((coordsEndLink.getX() - coordsStartLink.getX())*(coordsEndLink.getX() - coordsStartLink.getX()))
					+ ((coordsEndLink.getY() - coordsStartLink.getY())*(coordsEndLink.getY() - coordsStartLink.getY()))); 
		}
		return travelDistance;
	}

	/**
	 * Calculates the travel time for a given trip.
	 * 
	 * @param startTime	of the trip [sec]
	 * @param endTime	of the trip [sec]
	 * @return	total travel time of the trip [sec]
	 */
	private static double calcTravelTime(Double startTime, Double endTime) {
		return endTime - startTime;
	}
	
}
