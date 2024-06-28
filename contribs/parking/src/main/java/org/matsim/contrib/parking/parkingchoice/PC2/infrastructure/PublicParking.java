/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package org.matsim.contrib.parking.parkingchoice.PC2.infrastructure;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.parkingchoice.PC2.scoring.ParkingCostModel;

public class PublicParking implements PC2Parking {

	private final Id<PC2Parking> id;
	private final int capacity;
	private int availableParking=0;
	private Coord coord=null;
	private ParkingCostModel parkingCostModel=null;
	private final String groupName;

	public PublicParking(Id<PC2Parking> id, int capacity, Coord coord, ParkingCostModel parkingCostModel, String groupName){
		this.id=id;
		this.capacity=capacity;
		resetAvailability();
		this.coord=coord;
		this.parkingCostModel=parkingCostModel;
		this.groupName=groupName;
	}

	@Override
	public Id<PC2Parking> getId(){
		return id;
	}

	@Override
	public double getCost(Id<Person> personId, double arrivalTime, double parkingDurationInSecond){
		return parkingCostModel.calcParkingCost(arrivalTime, parkingDurationInSecond, personId, id);
	}

	@Override
	public Coord getCoordinate(){
		return coord;
	}

//	public boolean isParkingAvailable(){
//		return availableParking>0;
//	}
//
//	@Override
//	public int getMaximumParkingCapacity(){
//		return capacity;
//	}

	@Override
	public int getAvailableParkingCapacity(){
		return availableParking;
	}

	@Override
	public void parkVehicle(){
		if (availableParking>0){
			availableParking--;
		} else {
			throw new Error("system is in inconsistent state: " +
			"trying to park vehicle on full parking - parkingId:" + id + ";" );
		}
	}

	@Override
	public void unparkVehicle(){
		if (availableParking<capacity){
			availableParking++;
		} else {
			throw new Error("system is in inconsistent state: " +
			"trying to unpark vehicle from empty parking - parkingId:" + id + "" );
		}
	}

	@Override
	public String getGroupName() {
		return groupName;
	}

	@Override
	public void resetAvailability() {
		this.availableParking=capacity;
	}
}
