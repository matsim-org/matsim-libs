/* *********************************************************************** *
 * project: org.matsim.*
 * DgOagReader
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
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;


/**
 * @author dgrether
 *
 */
public class DgOagReader {

	private static final Logger log = Logger.getLogger(DgOagReader.class);
	
	public List<DgOagLine> readOagLines(String inputOagFile) throws Exception {
		List<DgOagLine> ret = new ArrayList<DgOagLine>();
		BufferedReader br = new BufferedReader(new FileReader(new File(inputOagFile)));
		int lines = 0;
		while (br.ready()) {
			String oneLine = br.readLine();
			lines++;
			String[] lineEntries = new String[81];
			lineEntries = oneLine.split(",");
			if (lines > 1) {
				for (int jj = 0; jj < 81; jj++) {
					lineEntries[jj] = lineEntries[jj].replaceAll("\"", "");
				}

				DgOagLine l = new DgOagLine(lineEntries);
				ret.add(l);
			}
			if (lines % 10000 == 0){
				log.info("Read " + lines +  " lines of oag data...");
			}
		}
		log.info("Anzahl der Zeilen mit Flügen: " + (lines - 1));
		return ret;
	}

	
}


class DgOagLine{

	private String[] lineEntries;

	public DgOagLine(String[] lineEntries) {
		this.lineEntries = lineEntries;
	}
	
	public String getOriginCountry(){
		return lineEntries[6];
	}
	
	public String getDestinationCountry(){
		return lineEntries[9];
	}
	
	public String getOriginAirport(){
		return lineEntries[4];
	}

	public String getDestinationAirport(){
		return lineEntries[7];
	}
	
	public String getCarrier(){
		return lineEntries[0];
	}
	
	public String getFlightNumber(){
		return lineEntries[1].replaceAll(" ", "0");
	}
	
	public double getFlightDistanceMiles(){
		return Double.parseDouble(lineEntries[42]);
	}
	
	public double getFlightDistanceKm(){
		return this.getFlightDistanceMiles() * 1.609344; // statute miles to kilometers
	}
	
	public double getMinutes(){
		String minutes = lineEntries[13].substring(3);
		return Double.parseDouble(minutes);
	}
	
	public double getHours(){
		String hours = lineEntries[13].substring(0, 3);
		return Double.parseDouble(hours);
	}
	
	public double getDepartureTimeSeconds(){
		return Double.parseDouble(lineEntries[10].substring(2)) * 60
				+ Double.parseDouble(lineEntries[10].substring(0, 2)) * 3600;
	}
	
	public double getStops(){
		return Double.parseDouble(lineEntries[15]);
	}
	
	public String getFullRouting(){
		return lineEntries[40];
	}
	
	public String getAircraftType(){
		return lineEntries[21];
	}
	
	public double getFlightDurationSeconds(){
		return getHours() * 3600.0 + getMinutes() * 60.0;
	}
	
	public Integer getSeatsAvailable(){
		return Integer.parseInt(lineEntries[23]);
	}
	
	public char[] getDaysOfOperation(){
		String daysOfOperation = lineEntries[14];
		daysOfOperation = daysOfOperation.replace(" ", "");
		char[] opsDays = daysOfOperation.toCharArray();
		return opsDays;
	}
	
	public boolean isCodeshareFlight(){
		// filter codeshare flights (see WW_DBF_With_Frequency.DOC from OAG input data)
		// either "operating marker" set, "shared airline designator" not set or "duplicate" not set
		return ! (lineEntries[47].contains("O") || lineEntries[43].equalsIgnoreCase("")
				|| lineEntries[49].equalsIgnoreCase(""));
	}
	
}


