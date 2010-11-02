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

	public void getLowTariffIntervals(){
		// TODO: think about this (what should be return type, or should this be solved differently?)
		
		// e.g. return double[]
	}
	
	
	// TODO: add methods for low and high tariff prices...
	
	
	
	
	/*
	 * in first iteration the result is same as getBaseLoadCurve.
	 * 
	 */
	public void getTotalLoadInPreviousIteration(){
		// TODO: think about this (what should be return type, or should this be solved differently?)
		
		// e.g. 15min bins (double[])
	}
	
	/*
	 * remains constant during simulation.
	 */
	public void getBaseLoadCurve(){
		// TODO: think about return type, e.g. double array
		
		
		// E.g. get one from A:\data\matsim\input\runRW1005\base load (e.g. home)
	}
	
	public void getAdaptedProbabilityCurve(Id agentId){
		// TODO: implement method.
	}
	
	public  LinkedListValueHashMap<Id, Double> getEnergyConsumptionOfLegs(){
		// TODO: rw
		return null;
	}
	
	public LinkedListValueHashMap<Id, ParkingInterval> getParkingTimeIntervals(){
		// TODO: rw
		
		
		
		
		return null;
	}
	
	public double getSOCOfAgent(Id agentId, double time){
		
		// TODO: will find out...
		
		return -1.0;
	}
	
	
	
	
}
