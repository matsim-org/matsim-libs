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

import java.util.Arrays;
import java.util.List;

public class NetworkTrip {
	final private Id<Person> personId;
	final private double departureTime;
	final private double arrivalTime;
	final private double travelledDistance;
	final private Coord departureLocation;
	final private Coord arrivalLocation;

	private Double validatedTravelTime = null;
	private Double validatedTravelDistance = null;
	private double actualTravelTime;

	NetworkTrip(Id<Person> personId, double departureTime, double arrivalTime, double distance, Coord departureLocation,
	            Coord arrivalLocation) {
		this.personId = personId;
		this.departureTime = departureTime;
		this.arrivalTime = arrivalTime;
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

    public List<String> getTripData() {
        String[] data = new String[]{this.personId.toString(), Double.toString(departureTime),
                Double.toString(departureLocation.getX()), Double.toString(departureLocation.getY()),
                Double.toString(arrivalLocation.getX()), Double.toString(arrivalLocation.getY()),
                Double.toString(actualTravelTime), Double.toString(validatedTravelTime),
                Double.toString(travelledDistance), Double.toString(validatedTravelDistance)};
        return Arrays.asList(data);
    }


    /**
     * This function should not be used in the future! It is a bad design with a bad naming. Please use getTripData instead.
     * */
    @Deprecated
    public String toString(){
//		bw.append("agent;departureTime;fromX;fromY;toX;toY;traveltimeActual;traveltimeValidated;traveledDistance;validatedDistance");
        return (this.personId.toString()+";"+departureTime+";"+departureLocation.getX()+";"+departureLocation.getY()+";"+arrivalLocation.getX()+";"+arrivalLocation.getY()+";"+actualTravelTime+";"+validatedTravelTime+";"+travelledDistance+";"+validatedTravelDistance);
        }

}