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

package playground.ikaddoura.incidents;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

import org.apache.log4j.Logger;

import playground.ikaddoura.incidents.data.TrafficItem;

/**
* @author ikaddoura
*/

public class TrafficItemWriter {
	private static final Logger log = Logger.getLogger(TrafficItemWriter.class);

	public void writeCSVFile(Collection<TrafficItem> collection, String outputFile) throws IOException {
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
						+ item.getStartTime() + ";"
						+ item.getEndTime() + ";"
						+ item.getStatus() + ";"
						+ item.getTMCAlert().getPhraseCode() + ";"
						+ item.getTMCAlert().getAltertDuration() + ";"
						+ item.getTMCAlert().getUpdateClass() + ";"
						+ item.getTMCAlert().getExtent() + ";"
						+ item.getTMCAlert().getDescription());
				bw.newLine();
			}
			
			log.info("Output written to " + outputFile);
		}
	}
}

