/* *********************************************************************** *
 * project: org.matsim.*
 * DgOagLine
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package air.scenario.oag;

public class DgOagLine{

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