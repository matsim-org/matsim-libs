/* *********************************************************************** *
 * project: org.matsim.*
 * DgAirportsReader
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
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;


/**
 * @author dgrether
 *
 */
public class DgAirportsReader {

	public Map<String, Coord> loadAirportCoordinates(String inputfile) throws Exception {
		Map<String, Coord> availableAirportCoordinates = new HashMap<String, Coord>();
		BufferedReader brAirports = new BufferedReader(new FileReader(new File(inputfile)));
		while (brAirports.ready()) {
			String line = brAirports.readLine();
			String[] entries = line.split("\t");
			String airportCode = entries[0];
			String xCoord = entries[1];
			String yCoord = entries[2];
			availableAirportCoordinates.put(airportCode, new CoordImpl(xCoord, yCoord));
		}
		brAirports.close();
		return availableAirportCoordinates;
	}
	
}
