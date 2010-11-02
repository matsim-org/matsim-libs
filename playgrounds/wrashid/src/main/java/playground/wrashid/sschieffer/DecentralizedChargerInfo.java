/* *********************************************************************** *
 * project: org.matsim.*
 * DecentralizedChargerInfo.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.wrashid.sschieffer;

import org.matsim.api.core.v01.Id;

import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingInterval;
import playground.wrashid.lib.obj.LinkedListValueHashMap;

public class DecentralizedChargerInfo {

	public void getLowTariffIntervals(double parkingStartTime, double parkingEndTime){
		// TODO: think about this (what should be return type, or should this be solved differently?)
	}
	
	public void getBaseLoadCurve(){
		// TODO: think about return type, e.g. double array
	}
	
	public void getAdaptedProbabilityCurve(Id agentId){
		// TODO: implement method.
	}
	
	public  LinkedListValueHashMap<Id, Double> getEnergyConsumptionOfLegs(){
		return null;
	}
	
	public LinkedListValueHashMap<Id, ParkingInterval> getParkingTimeIntervals(){
		return null;
	}
	
	public double getSOCOfAgent(Id agentId, double time){
		return -1.0;
	}
	
	
	
	
}
