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

package playground.boescpa.analysis.trips;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.io.IOUtils;
import playground.boescpa.analysis.spatialCutters.NoCutter;
import playground.boescpa.analysis.spatialCutters.SpatialCutter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Provides static methods to analyse and postprocess the trips created with
 * the class MainTripCreator.
 * 
 * @author pboesch
 *
 */
public abstract class TripProcessor {
	
	protected static Logger log = Logger.getLogger(TripProcessor.class);
	private final String outputFile;
	protected SpatialTripCutter spatialCutter;

	public TripProcessor(String outputFile) {
		this.outputFile = outputFile; // Path to the trip-File produced as output, e.g. "trips2030combined.txt"
		this.spatialCutter = new SpatialTripCutter(new NoCutter());
	}

	public TripProcessor(String outputFile, SpatialCutter spatialCutter)	{
		this(outputFile);
		this.spatialCutter = new SpatialTripCutter(spatialCutter);
	}

	/**
	 * Sets the strategy according to which the trips are spatially cut
	 * before any further processing.
	 * 
	 * Currently known implemented strategies:
	 * 	NoCutter			[DEFAULT] All trips are processed, no spatial cut. ("argument" is not used)
	 * 	ShpFileCutter		The trips are cut according to a provided shp-file. Needs the shp-file as an extra argument (Path as a String).
	 * 	CirclePointCutter	Only the trips within a circle around Bellevue are processed. Needs the radius of the circle as an extra argument.
	 * 
	 * @param spatialCutter
	 */
	public void setSpatialCutter(SpatialCutter spatialCutter) {
		this.spatialCutter = new SpatialTripCutter(spatialCutter);
	}

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
	public void printTrips(TripHandler tripHandler, Network network) {
		try {
			final String header="agentId\tstartTime\tstartLink\tstartXCoord\tstartYCoord\tendTime\tendLink\tendXCoord\tendYCoord\tmode\tpurpose\tduration\tdistance";
			final BufferedWriter out = IOUtils.getBufferedWriter(outputFile);
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
						if (!spatialCutter.spatiallyConsideringTrip(network, startLinks.get(i), endLinks.get(i))) {
							continue;
						}
						
						if (network.getLinks().get(endLinks.get(i)) != null) {
							// travel time per mode [minutes]
							double travelTime = calcTravelTime(startTimes.get(i), endTimes.get(i))/60;
							// distance per mode [meters]
							long travelDistance = calcTravelDistance(pathList.get(i), network, startLinks.get(i), endLinks.get(i));
							
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
									+ "null"
									+ "\t"
									+ "null"
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
	public static int calcTravelDistance(LinkedList<Id> path, Network network, Id startLink, Id endLink) {
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
			travelDistance += (int) Math.sqrt(
					((coordsEndLink.getX() - coordsStartLink.getX())*(coordsEndLink.getX() - coordsStartLink.getX()))
					+ ((coordsEndLink.getY() - coordsStartLink.getY())*(coordsEndLink.getY() - coordsStartLink.getY())));
			// and scale it with a factor to account for non-euclidian detours on "real" path
			travelDistance *= 1.51; // 1.51 was estimated by comparing euclidian and non-euclidian distances for a large scenario (ToPDAd)...
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
	public static double calcTravelTime(Double startTime, Double endTime) {
		return endTime - startTime;
	}

	public abstract HashMap<String, Object> analyzeTrips(TripHandler tripHandler, Network network);
}
