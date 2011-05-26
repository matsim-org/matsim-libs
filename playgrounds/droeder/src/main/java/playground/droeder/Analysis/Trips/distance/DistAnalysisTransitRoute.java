/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.droeder.Analysis.Trips.distance;

import org.matsim.api.core.v01.Id;

/**
 * @author droeder
 *
 */
public class DistAnalysisTransitRoute {
	
	private Id id;
	private double travelDistance = 0;
	private double trafficPerformance = 0;
	private int transportedPersons = 0;

	/**
	 * @param transitRouteId
	 */
	public DistAnalysisTransitRoute(Id transitRouteId) {
		this.id = transitRouteId;
	}
	
	public void countPassenger(){
		this.transportedPersons++;
	}
	
	/**
	 * @param linkLength
	 * @param nrOfDrivingPassengers
	 */
	public void passedLink(double linkLength, int nrOfDrivingPassengers) {
		this.travelDistance += linkLength;
		this.trafficPerformance += (linkLength * nrOfDrivingPassengers);
	}

	/**
	 * @return the travelDistance
	 */
	public double getTravelDistance() {
		return travelDistance;
	}

	/**
	 * @return the trafficPerformance
	 */
	public double getTrafficPerformance() {
		return trafficPerformance;
	}

	/**
	 * @return the transportedPersons
	 */
	public int getTransportedPersons() {
		return transportedPersons;
	}

	/**
	 * @return the id
	 */
	public Id getId() {
		return id;
	}

	public String toString(boolean header){
		StringBuffer b =  new StringBuffer();
		if(header){
			b.append("RouteId;Distance [m];nr of Pers. * meters ; transported Persons\n");
		}
		b.append(this.id.toString() + ";" + this.travelDistance + ";" + this.trafficPerformance + ";" + this.transportedPersons + "\n");
		return b.toString();
	}
	
	@Override
	public String toString(){
		return this.toString(true);
	}
}
