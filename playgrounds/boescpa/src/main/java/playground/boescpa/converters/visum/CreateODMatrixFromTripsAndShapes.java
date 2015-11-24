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

package playground.boescpa.converters.visum;

import org.apache.log4j.Logger;
import playground.boescpa.converters.visum.obj.VisumTrip;
import playground.boescpa.analysis.trips.tripReader.Trip;

import java.io.IOException;
import java.util.HashMap;

/**
 * Provides the main method to create OD-matrices for visum
 * from a given trip file and given shp-files (zones, borders, farOuts).
 *
 * @author boescpa
 */
public class CreateODMatrixFromTripsAndShapes {
	
	private static Logger log = Logger.getLogger(CreateODMatrixFromTripsAndShapes.class);
	private static HashMap<Long, VisumTrip> tripCollection;
		
	public static void main(String[] args) {
		
		String sourceTripFile = args[0]; // Path to source-File, e.g. "Trips.txt";
		String shpZones = args[1]; // Path to Shape-File with Zones, e.g. "Zones.shp", and according, additional files;
		String shpBorders = args[2]; // Path to Shape-File with Borders (centroids), e.g. "Borders.shp", and according, additional files; 
		String shpFarOuts = args[3]; // Path to Shape-File with FarOuts (centroids), e.g. "FarOut.shp", and according, additional files;
		String targetODFile = args[4]; // Path to target-OD-File in the Visum-o-Format, e.g. "odFile" (IMPORTANT: No file-ending needed, will be added...);
		int time = Integer.parseInt(args[5]); // Beginning of the 24-hour hour, for which OD-matrix. E.g. if from 17.00 to 18.00 then argument: "17". If full 24h, then "-1" for the hour.;
		
		log.info("Reading trip file...");
		HashMap<Long, Trip> tempTripCollection = Trip.createTripCollection(sourceTripFile);
		for (Long tripId : tempTripCollection.keySet()) {
			tripCollection.put(tripId, new VisumTrip(tempTripCollection.get(tripId).toString().split("\t")));
		}
		log.info("Reading trip file...done.");
		
		String[] modes = {"", "car", "bike", "pt", "walk", "transit_walk"};
		for (String mode : modes) {
			log.info("Mode " + mode + "...");
			log.info("Load zones...");
			ODMatrix odMatrix = new ODMatrix(tripCollection, time, mode);
			odMatrix.loadZones(shpZones);
			odMatrix.loadBorders(shpBorders);
			odMatrix.loadFarOut(shpFarOuts);
			log.info("Load zones...done.");
			
			log.info("Create ODMatrix...");
			odMatrix.createODMatrix();
			log.info("Create ODMatrix...done.");
			
			log.info("Write Visum-o-File...");
			try {
				odMatrix.createVisumOFile(targetODFile);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			log.info("Write Visum-o-File...done.");
			log.info("Mode " + mode + "...done.");
		}
		log.info("Finished creating ODMatrix without errors.");
	}
}
