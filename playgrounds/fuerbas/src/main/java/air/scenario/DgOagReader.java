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

	private String originCountry;
	private String destinationCountry;
	private String originAirport;
	private String destinationAirport;
	private String carrier;
	private String flightNumber;
	private double flightDistanceMiles;
	private double minutes;
	private double hours;
	private double departureTime;
	private double stops;
	private String fullRouting;
	private String aircraftType;
	private int seats;
	private char[] daysOfOperation;
	private boolean codeShareFlight;
	
	public DgOagLine(String[] lineEntries) {
		this.originCountry = lineEntries[6].trim().toUpperCase();
		this.destinationCountry = lineEntries[9].trim().toUpperCase();
		this.originAirport = lineEntries[4].trim();
		this.destinationAirport = lineEntries[7].trim();
		this.carrier = lineEntries[0].trim();
		this.flightNumber = lineEntries[1].replaceAll(" ", "0");
		this.flightDistanceMiles = Double.parseDouble(lineEntries[42]);
		String m = lineEntries[13].substring(3);
		this.minutes = Double.parseDouble(m);
		String h = lineEntries[13].substring(0, 3);
		this.hours = Double.parseDouble(h);
		this.departureTime = Double.parseDouble(lineEntries[10].substring(2)) * 60
				+ Double.parseDouble(lineEntries[10].substring(0, 2)) * 3600;
		this.stops = Double.parseDouble(lineEntries[15]);
		this.fullRouting = lineEntries[40];
		this.aircraftType = lineEntries[21].trim();
		this.seats = Integer.parseInt(lineEntries[23]);
		this.daysOfOperation = this.createDaysOfOperation(lineEntries[14]);
		this.codeShareFlight  = this.createIsCodeshareFlight(lineEntries);
	}
	
	private boolean createIsCodeshareFlight(String[] lineEntries){
		// filter codeshare flights (see WW_DBF_With_Frequency.DOC from OAG input data)
		// either "operating marker" set, "shared airline designator" not set or "duplicate" not set
		return ! (lineEntries[47].contains("O") || lineEntries[43].equalsIgnoreCase("")
				|| lineEntries[49].equalsIgnoreCase(""));
	}
	
	private char[] createDaysOfOperation(String doo){
		doo = doo.replace(" ", "");
		char[] opsDays = doo.toCharArray();
		return opsDays;
	}
	
	public String getOriginCountry(){
		return this.originCountry;
	}
	
	public String getDestinationCountry(){
		return this.destinationCountry;
	}
	
	public String getOriginAirport(){
		return this.originAirport;
	}

	public String getDestinationAirport(){
		return this.destinationAirport;
	}
	
	public String getCarrier(){
		return this.carrier;
	}
	
	public String getFlightNumber(){
		return this.flightNumber;
	}
	
	public double getFlightDistanceMiles(){
		return this.flightDistanceMiles;
	}
	
	public double getFlightDistanceKm(){
		return this.getFlightDistanceMiles() * 1.609344; // statute miles to kilometers
	}
	
	public double getMinutes(){
		return minutes;
	}
	
	public double getHours(){
		return hours;
	}
	
	public double getDepartureTimeSeconds(){
		return this.departureTime;
	}
	
	public double getStops(){
		return this.stops;
	}
	
	public String getFullRouting(){
		return fullRouting;
	}
	
	public String getAircraftType(){
		return this.aircraftType;
	}
	
	public double getFlightDurationSeconds(){
		return getHours() * 3600.0 + getMinutes() * 60.0;
	}
	
	public Integer getSeatsAvailable(){
		return this.seats;
	}
	
	public char[] getDaysOfOperation(){
		return this.daysOfOperation;
	}
	
	public boolean isCodeshareFlight(){
		return this.codeShareFlight;
	}
	
	
}


