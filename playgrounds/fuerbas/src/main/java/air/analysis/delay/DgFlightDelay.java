/* *********************************************************************** *
 * project: org.matsim.*
 * DgFlightDelay
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
package air.analysis.delay;

import air.scenario.oag.DgOagFlight;


/**
 * @author dgrether
 *
 */
public class DgFlightDelay {


	private DgOagFlight flight;
	private Double departureDelay = null;
	private Double arrivalDelay = null;
	private double actualDepartureTime;
	private double actualArrivalTime;

	public DgFlightDelay(DgOagFlight flight) {
		this.flight = flight;
	}
	
	public void setDepartureDelay(Double departureDelay){
		this.departureDelay = departureDelay;
	}
	
	public void setArrivalDelay(Double arrivalDelay){
		this.arrivalDelay  = arrivalDelay;
	}

	
	public DgOagFlight getFlight() {
		return flight;
	}

	
	public Double getDepartureDelay() {
		return departureDelay;
	}

	
	public Double getArrivalDelay() {
		return arrivalDelay;
	}

	public void setActualDepartureTime(double timeSec) {
		this.actualDepartureTime = timeSec;
	}

	public void setActualArrivalTime(double timeSec) {
		this.actualArrivalTime = timeSec;
	}

	
	public double getActualDepartureTime() {
		return actualDepartureTime;
	}

	
	public double getActualArrivalTime() {
		return actualArrivalTime;
	}

}
