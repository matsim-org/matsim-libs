/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.analysis.vsp.traveltimedistance;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public class CarTrip {
	final private Id<Person> personId;
	final private double departureTime;
	final private double arrivalTime;
	final private double travelledDistance;
	final private Coord departureLocation;
	final private Coord arrivalLocation;
	
	private Double validatedTravelTime = null;
	private Double validatedTravelDistance = null;
	private double actualTravelTime;

	CarTrip(Id<Person> personId, double departureTime, double arrivaldTime, double distance, Coord departureLocation,
			Coord arrivalLocation) {
		this.personId = personId;
		this.departureTime = departureTime;
		this.arrivalTime = arrivaldTime;
		this.departureLocation = departureLocation;
		this.arrivalLocation = arrivalLocation;
		this.travelledDistance = distance;
		
	}

	
	public Id<Person> getPersonId() {
		return personId;
	}

	public double getDepartureTime() {
		return departureTime;
	}

	public double getArrivalTime() {
		return arrivalTime;
	}

	public Coord getDepartureLocation() {
		return departureLocation;
	}

	public Coord getArrivalLocation() {
		return arrivalLocation;
	}
	/**
	 * @param actualTravelTime the actualTravelTime to set
	 */
	public void setActualTravelTime(double actualTravelTime) {
		this.actualTravelTime = actualTravelTime;
	}
	/**
	 * @return the actualTravelTime
	 */
	public double getActualTravelTime() {
		return actualTravelTime;
	}
	/**
	 * @param validatedTravelTime the validatedTravelTime to set
	 */
	public void setValidatedTravelTime(double validatedTravelTime) {
		this.validatedTravelTime = validatedTravelTime;
	}
	/**
	 * @return the validatedTravelTime
	 */
	public Double getValidatedTravelTime() {
		return validatedTravelTime;
	}
	
	public Double getValidatedTravelDistance() {
		return validatedTravelDistance;
	}


	public void setValidatedTravelDistance(Double validatedTravelDistance) {
		this.validatedTravelDistance = validatedTravelDistance;
	}


	public double getTravelledDistance() {
		return travelledDistance;
	}


	public String toString(){
//		bw.append("agent;departureTime;fromX;fromY;toX;toY;traveltimeActual;traveltimeValidated;traveledDistance;validatedDistance");
		return (this.personId.toString()+";"+departureTime+";"+departureLocation.getX()+";"+departureLocation.getY()+";"+arrivalLocation.getX()+";"+arrivalLocation.getY()+";"+actualTravelTime+";"+validatedTravelTime+";"+travelledDistance+";"+validatedTravelDistance);
	}

}