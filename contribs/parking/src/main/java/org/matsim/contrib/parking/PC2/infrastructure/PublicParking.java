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
package org.matsim.contrib.parking.PC2.infrastructure;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.PC2.scoring.ParkingCostModel;
import org.matsim.contrib.parking.lib.DebugLib;

public class PublicParking implements PC2Parking {

	private Id<PC2Parking> id = null;
	private int capacity =0;
	private int availableParking=0;
	private Coord coord=null;
	private ParkingCostModel parkingCostModel=null;
	private String groupName;
	
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
	
	public boolean isParkingAvailable(){
		return availableParking>0;
	}
	
	@Override
	public int getMaximumParkingCapacity(){
		return capacity;
	}
	
	@Override
	public int getAvailableParkingCapacity(){
		return availableParking;
	}
	
	@Override
	public void parkVehicle(){
		if (availableParking>0){
			availableParking--;
		} else {
			DebugLib.stopSystemAndReportInconsistency("trying to park vehicle on full parking - parkingId:" + id + ";" );
		}
	}
	
	@Override
	public void unparkVehicle(){
		if (availableParking<capacity){
			availableParking++;
		} else {
			DebugLib.stopSystemAndReportInconsistency("trying to unpark vehicle from empty parking - parkingId:" + id + "" );
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
