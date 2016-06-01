/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.incidents.io;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import playground.ikaddoura.incidents.data.NetworkIncident;
import playground.ikaddoura.incidents.data.TrafficItem;

/**
* @author ikaddoura
*/

public class Incident2CSVWriter {
	private static final Logger log = Logger.getLogger(Incident2CSVWriter.class);

	public static void writeTrafficItems(Collection<TrafficItem> collection, String outputFile) throws IOException {
		try ( BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile)) ) {
			
			bw.write("Traffic Item ID;Original Traffic Item ID;Download Time;"
					+ "Origin Location ID;Origin X;Origin Y;Origin Description; Origin Country Code;"
					+ "To Location ID;To X;To Y;To Description;To Country Code;"
					+ "Start Time;End Time;Status;"
					+ "TMC Alert Code;TMC Alert Duration;Update Class;Extent;TMC Alert Description(s)");
			bw.newLine();

			for (TrafficItem item : collection) {
				bw.write(item.getId() + ";"
						+ item.getOriginalId() + ";"
						+ item.getDownloadTime() + ";"
						+ item.getOrigin().getLocationId() + ";"
						+ item.getOrigin().getLongitude() + ";"
						+ item.getOrigin().getLatitude() + ";"
						+ item.getOrigin().getDescription() + ";"
						+ item.getOrigin().getCountryCode() + ";"
						+ item.getTo().getLocationId() + ";"
						+ item.getTo().getLongitude() + ";"
						+ item.getTo().getLatitude() + ";"
						+ item.getTo().getDescription() + ";"
						+ item.getTo().getCountryCode() + ";"
						+ item.getStartDateTime() + ";"
						+ item.getEndDateTime() + ";"
						+ item.getStatus() + ";"
						+ item.getTMCAlert().getPhraseCode() + ";"
						+ item.getTMCAlert().getAltertDuration() + ";"
						+ item.getTMCAlert().getUpdateClass() + ";"
						+ item.getTMCAlert().getExtent() + ";"
						+ item.getTMCAlert().getDescription());
				bw.newLine();
			}
			log.info("Traffic items written to " + outputFile);
			bw.close();
		}
	}

	public static void writeProcessedNetworkIncidents(Map<Id<Link>, List<NetworkIncident>> linkId2processedIncidentsCurrentDay, String outputFile) throws IOException {
		try ( BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile)) ) {
			
			bw.write("Link Id;Incident Id;Start Time; End Time;Original Capacity;"
					+ "Original Freespeed;Original Number Of Lanes;Incident Capacity;Incident Freespeed; Incident Number Of Lanes");
			bw.newLine();
			
			for (Id<Link> linkId : linkId2processedIncidentsCurrentDay.keySet()) {
				for (NetworkIncident incident : linkId2processedIncidentsCurrentDay.get(linkId)) {
					bw.write(incident.getLink().getId().toString() + ";"
							+ incident.getId() + ";"
							+ incident.getStartTime() + ";"
							+ incident.getEndTime() + ";"
							+ incident.getLink().getCapacity()
							+ ";" + incident.getLink().getFreespeed()
							+ ";" + incident.getLink().getNumberOfLanes()
							+ ";" + incident.getIncidentLink().getCapacity()
							+ ";" + incident.getIncidentLink().getFreespeed()
							+ ";" + incident.getIncidentLink().getNumberOfLanes());
					bw.newLine();
				}
			}
			bw.close();
		}
		log.info("Traffic incidents written to " + outputFile);
	}
}

