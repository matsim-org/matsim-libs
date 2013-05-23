/* *********************************************************************** *
 * project: org.matsim.*
 * DgOagFlight
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
package air.scenario.oag;


/**
 * @author dgrether
 *
 */
public class DgOagFlight {

	private String route;
	private String carrier;
	private String flightDesignator;
	private double departureTime;
	private double duration;
	private String aircraftType;
	private int seatsAvailable;
	private double distance;
	private String originCode;
	private String destinationCode;
	
	public DgOagFlight(String flightDesignator){
		this.flightDesignator = flightDesignator;
	}
	
	/**
	 * as we don't have lines in this context route is a od pair,e.g. FRA_BER
	 */
	public String getRoute() {
		return route;
	}
	
	public void setRoute(String route) {
		this.route = route;
	}
	
	public String getCarrier() {
		return carrier;
	}
	
	public void setCarrier(String carrier) {
		this.carrier = carrier;
	}
	
	public String getFlightDesignator() {
		return flightDesignator;
	}
		
	public double getDepartureTime() {
		return departureTime;
	}
	
	public void setDepartureTime(double departureTime) {
		this.departureTime = departureTime;
	}
	
	public double getScheduledDuration() {
		return duration;
	}
	
	public void setDuration(double duration) {
		this.duration = duration;
	}
	
	public String getAircraftType() {
		return aircraftType;
	}
	
	public void setAircraftType(String aircraftType) {
		this.aircraftType = aircraftType;
	}
	
	public int getSeatsAvailable() {
		return seatsAvailable;
	}
	
	public void setSeatsAvailable(int seatsAvailable) {
		this.seatsAvailable = seatsAvailable;
	}
	
	public double getDistanceKm() {
		return distance;
	}
	
	public void setDistanceKm(double distance) {
		this.distance = distance;
	}

	public void setOriginCode(String origin) {
		this.originCode = origin;
	}
	
	public void setDestinationCode(String dest){
		this.destinationCode = dest;
	}
	
	public String getOriginCode(){
		return this.originCode;
	}
	
	public String getDestinationCode(){
		return this.destinationCode;
	}
	
}
