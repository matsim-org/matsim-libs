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

import org.apache.log4j.Logger;
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
	 * 	- eventsFile: Path to an events-File, e.g. "run.combined.150.events.xml.gz"
	 * 	- networkFile: Path to the network-File used for the simulation resulting in the above events-File, e.g. "multimodalNetwork2030final.xml.gz"
	 * 	- tripFile: Path to the trip-File produced as output, e.g. "trips2030combined.txt"
	 * 		IMPORTANT: tripFile will be overwritten if already existing.
	 * 
	 * The output format is: agentId, tripStartTime, tripStartLink, tripStartLinkXCoord, tripStartLinkYCoord,
	 * tripEndLink, tripEndLinkXCoord, tripEndLinkYCoord, mainMode, tripPurpose.
	 * 
	 * If the agent is stuck, "stuck" is assigned to the trip purpose, end link and end time are taken from the stuckAndAbort event.
	 * 
	 * If a pt trip only contains transit_walk legs, the main mode is transit walk.
	 */
	public static void printTrips(TripHandler tripHandler, Network network, String outFile)
			throws IOException {

		final String header="agentId\tstartTime\tstartLink\tstartXCoord\tstartYCoord\tendTime\tendLink\tendXCoord\tendYCoord\tmode\tpurpose";
		final BufferedWriter out = IOUtils.getBufferedWriter(outFile); // Path to the trip-File produced as output, e.g. "trips2030combined.txt"
		out.write(header);
		out.newLine();
		log.info("Writing trips file...");
		for (Id personId : tripHandler.getStartLink().keySet()) {
			if (!personId.toString().contains("pt")) {
				ArrayList<Id> startLinks = tripHandler.getStartLink().getValues(personId);
				ArrayList<String> modes = tripHandler.getMode().getValues(personId);
				ArrayList<String> purposes = tripHandler.getPurpose().getValues(personId);
				ArrayList<Double> startTimes = tripHandler.getStartTime().getValues(personId);
				ArrayList<Id> endLinks = tripHandler.getEndLink().getValues(personId);
				ArrayList<Double> endTimes = tripHandler.getEndTime().getValues(personId);
	
				for (int i = 0; i < startLinks.size(); i++) {
					if (network.getLinks().get(endLinks.get(i)) != null) {
						out.write(personId
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
							);
						out.newLine();
					}
					// in case there is no end link
					else {
						out.write(personId
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
								);
							out.newLine();
					}
				}
			}
		}
		out.close();
		log.info("Writing trips file...done.");
	}
	
}
