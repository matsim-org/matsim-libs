/* *********************************************************************** *
 * project: org.matsim.*
 * FlightODRelation
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

public class FlightODRelation {
	
	private String fromAirportCode;
	private String toAirportCode;
	private Double numberOfTrips = null;

	public FlightODRelation(String fromAirportCode, String toAirportCode, Double numberOfTrips){
		this.fromAirportCode = fromAirportCode;
		this.toAirportCode = toAirportCode;
		this.numberOfTrips = numberOfTrips;
	}

	
	public String getFromAirportCode() {
		return fromAirportCode;
	}

	
	public String getToAirportCode() {
		return toAirportCode;
	}

	
	public Double getNumberOfTrips() {
		return numberOfTrips;
	}


	public void setNumberOfTrips(Double i) {
		this.numberOfTrips = i;
	}
	
}