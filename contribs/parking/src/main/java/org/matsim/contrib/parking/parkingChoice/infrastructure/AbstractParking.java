/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.contrib.parking.parkingChoice.infrastructure;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.parkingChoice.infrastructure.cost.CostCalculator;

/**
 * 
 * 
 * 
 * 
 * @author rashid_waraich
 *
 */
public abstract class AbstractParking implements Parking{
	private Id id = null;
	private int capacity =0;
	private int availableParking=0;
	private Coord coord=null;
	private CostCalculator costCalculator=null;
	
	public abstract boolean isAllowedToUseParking(Id agentId, Id actFacilityId, String actType);
	
	public Id getId(){
		return id;
	}
	
	public double getCost(Id agentId, double arrivalTime, double parkingDurationInSecond){
		return costCalculator.getCost(agentId, arrivalTime, parkingDurationInSecond);
	}
	
	public Coord getCoordinate(){
		return coord;
	}
	
	public boolean isParkingAvailable(){
		return availableParking>0;
	}
	
	public int getMaximumParkingCapacity(){
		return capacity;
	}
	
	public int getAvailableParkingCapacity(){
		return availableParking;
	}
	
	public void parkVehicle(Id agentId){
		if (availableParking>0){
			availableParking--;
		} else {
			DebugLib.stopSystemAndReportInconsistency("trying to park vehicle on full parking - parkingId:" + id + ";agentId:" + agentId);
		}
	}
	
	public void unparkVehicle(Id agentId){
		if (availableParking<capacity){
			availableParking++;
		} else {
			DebugLib.stopSystemAndReportInconsistency("trying to unpark vehicle from empty parking - parkingId:" + id + ";agentId:" + agentId);
		}
	}
	
}
