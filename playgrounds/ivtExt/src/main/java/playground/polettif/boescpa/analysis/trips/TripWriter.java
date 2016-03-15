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

package playground.polettif.boescpa.analysis.trips;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.io.IOUtils;
import playground.polettif.boescpa.analysis.spatialCutters.SpatialCutter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

/**
 * Provides static methods to analyse and postprocess the trips created with
 * the class MainTripCreator.
 * 
 * @author pboesch
 *
 */
public class TripWriter {
	protected static Logger log = Logger.getLogger(TripWriter.class);

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
	public static void writeTrips(TripEventHandler tripHandler, String outputFile) {
        TripWriter.writeTrips(tripHandler.getTrips(), outputFile);
	}

    public static void writeTrips(List<Trip> trips, String outputFile) {
        try {
            final BufferedWriter out = IOUtils.getBufferedWriter(outputFile);
            out.write(Trip.getHeader());
            out.newLine();
            log.info("Writing trips file...");
            for (Trip tempTrip : trips) {
                out.write(tempTrip.toString());
                out.newLine();
            }
            out.close();
            log.info("Writing trips file...done.");
        } catch (IOException e) {
            log.info("Given trip-file-path not valid. Print trips not successfully executed.");
        }
    }
}
