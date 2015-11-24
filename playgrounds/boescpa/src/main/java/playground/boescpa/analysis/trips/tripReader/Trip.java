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

package playground.boescpa.analysis.trips.tripReader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

/**
 * Provides a representation of trips.
 *
 * @author boescpa
 */
public class Trip {
    private static Logger log = Logger.getLogger(Trip.class);

	// TODO-boescpa Write tests...

	private static Long tripIdCounter = (long)0;
    private static boolean alreadyWarned = false;

	public final long tripId;
	public final Id agentId;
	public final double startTime;
	public final Id startLinkId;
	public final double startXCoord;
	public final double startYCoord;
	public final double endTime;
	public final Id endLinkId;
	public final double endXCoord;
	public final double endYCoord;
	public final String mode;
	public final String purpose;
	public final double duration;
	public final long distance;

	public Trip(Id agentId,
				double startTime, Id startLinkId, double startXCoord, double startYCoord,
				double endTime, Id endLinkId, double endXCoord, double endYCoord,
				String mode, String purpose, double duration, long distance) {

		tripIdCounter++;
		tripId = tripIdCounter;

		this.agentId = agentId;
		this.startTime = startTime;
		this.startLinkId = startLinkId;
		this.startXCoord = startXCoord;
		this.startYCoord = startYCoord;
		this.endTime = endTime;
		this.endLinkId = endLinkId;
		this.endXCoord = endXCoord;
		this.endYCoord = endYCoord;
		this.mode = mode;
		this.purpose = purpose;
		this.duration = duration;
		this.distance = distance;
	}



	public static String getHeader() {
		return "tripId\tagentId\tstartTime\tstartLink\tstartXCoord\tstartYCoord\tendTime\tendLink\tendXCoord\tendYCoord\tmode\tpurpose\tduration\tdistance";
	}

	@Override
	public String toString() {
		return tripId + "\t" + agentId + "\t" + startTime + "\t" + startLinkId + "\t" + startXCoord + "\t"
				+ startYCoord + "\t" + endTime + "\t" + endLinkId + "\t" + endXCoord + "\t" + endYCoord
				+ "\t" + mode + "\t" + purpose + "\t" + duration + "\t" + distance;
	}

	/**
	 * Reads a trip file into the memory and provides the trips in the form of a HashMap.
	 *
	 * @param sourceTripFile
	 * @return HashMap<Long,Trip> - Key: TripId, Value: Trip
	 */
	public static HashMap<Long,Trip> createTripCollection(String sourceTripFile) {
		HashMap<Long,Trip> tripCollection = new HashMap<Long, Trip>();
		FileReader reader;
		try {
			reader = new FileReader(sourceTripFile);
			BufferedReader readsLines = new BufferedReader(reader);
			String newLine = readsLines.readLine(); // Header is read...
			newLine = readsLines.readLine();
			while (newLine != null) {
                try {
                    String[] tripLine = newLine.split("\t");
                    Trip trip = new Trip(
                            Id.create(tripLine[0], Person.class), //agentId
                            Double.parseDouble(tripLine[1]), // startTime
                            Id.create(tripLine[2], Link.class), // startLinkId
                            Double.parseDouble(tripLine[3]), // startXCoord
                            Double.parseDouble(tripLine[4]), // startYCoord
                            Double.parseDouble(tripLine[5]), // endTime
                            Id.create(tripLine[6], Link.class), // endLinkId
                            Double.parseDouble(tripLine[7]), // endXCoord
                            Double.parseDouble(tripLine[8]), // endYCoord
                            tripLine[9], // mode
                            tripLine[10], // purpose
                            Double.parseDouble(tripLine[11]), // duration
                            Long.parseLong(tripLine[12])); // distance
                    tripCollection.put(trip.tripId, trip);
                } catch (Exception e) {
                    if (!alreadyWarned) {
                        log.error("Trip file contains entries that can not be read.");
                        alreadyWarned = true;
                    }
                }
				newLine = readsLines.readLine();
			}
			readsLines.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return tripCollection;
	}
}
