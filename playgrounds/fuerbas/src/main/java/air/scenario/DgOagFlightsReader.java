/* *********************************************************************** *
 * project: org.matsim.*
 * DgOagFlightsReader
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
package air.scenario;

import java.io.BufferedReader;
import java.io.IOException;

import org.matsim.core.utils.io.IOUtils;


/**
 * @author dgrether
 *
 */
public class DgOagFlightsReader {
	
	private DgOagFlightsData data;

	public DgOagFlightsReader(DgOagFlightsData data){
		this.data = data;
	}

	public void readFile(String filename){
		BufferedReader reader = IOUtils.getBufferedReader(filename);
		try {
			String line = reader.readLine();
			while (line != null) {
				String[] e = line.split("\t"); //rename to entries
				DgOagFlight flight = new DgOagFlight(e[3]);
				flight.setRoute(e[0]);
				flight.setOriginCode(e[0].split("_")[0]);
				flight.setDestinationCode(e[0].split("_")[1]);
				
				String carrier = e[1].split("_")[2];
				flight.setCarrier(carrier);
				flight.setDepartureTime(Double.parseDouble(e[4]));
				flight.setDuration(Double.parseDouble(e[5]));
				flight.setAircraftType(e[6]);
				flight.setSeatsAvailable(Integer.parseInt(e[7]));
				flight.setDistanceKm(Double.parseDouble(e[8]));
				this.data.addFlight(flight);
				line = reader.readLine();
			}
		
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		
	}
	
}
