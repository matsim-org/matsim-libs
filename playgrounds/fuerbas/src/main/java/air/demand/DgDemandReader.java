/* *********************************************************************** *
 * project: org.matsim.*
 * DgPopulationGenerator
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package air.demand;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;


/**
 * @author dgrether
 *
 */
public class DgDemandReader {

	private static final Logger log = Logger.getLogger(DgDemandReader.class);

	public List<FlightODRelation> readFile(String oddemand) throws IOException {
		List<FlightODRelation> result = new ArrayList<FlightODRelation>();
		BufferedReader br = IOUtils.getBufferedReader(oddemand);
		String headerLine = br.readLine();
		String[] header = headerLine.split(",");
		String line = br.readLine();
		while (line != null) {
			log.debug(line);
			String[] entries = line.split(",");
			String fromAirportCode = entries[0].trim();
			for (int i = 1; i < entries.length; i++) {
				String toAirportCode = header[i].trim();
				String tripsString = entries[i].trim();
				tripsString = tripsString.replace(" ", "");
				if (tripsString.compareTo("-") != 0) {
					int trips = Integer.parseInt(tripsString);
					result.add(new FlightODRelation(fromAirportCode, toAirportCode, Double.valueOf(trips)));
					log.info("Read " + trips + " from " + fromAirportCode + " to " + toAirportCode);
				}
				else {
					result.add(new FlightODRelation(fromAirportCode, toAirportCode, null));
				}
			}
			line = br.readLine();
		}
		br.close();
		return result;
	}

}
