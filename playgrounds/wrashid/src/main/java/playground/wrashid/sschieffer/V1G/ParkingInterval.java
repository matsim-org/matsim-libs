/* *********************************************************************** *
 * project: org.matsim.*
 * LoadDistributionInterval.java
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

package playground.wrashid.sschieffer.V1G;

import java.util.LinkedList;

import org.matsim.api.core.v01.Id;

import playground.wrashid.lib.obj.LinkedListValueHashMap;

public class ParkingInterval extends TimeInterval {
	
	private double requiredChargingDuration=-1;
	private Id linkId;
	
	private double joulesInTimeInterval;
	
	
	private Schedule chargingSchedule;
	
	private boolean optimal; // true if in optimal loadDistributionInterval
	
	
	
	
	public ParkingInterval(double start, double end, Id linkId){
		super(start, end);
		this.linkId=linkId;
	}
	
	
	public Id getLocation(){
		return linkId;
	}
	
	
	public double getChargingSpeed(){
		// TODO
		// TODO
		//location--> facility-- charging speed
		return 3500;
		
		
	}
	
	
	public double getJoulesInInterval(){
		return joulesInTimeInterval;
	}
		
	public double getRequiredChargingDuration(){
		return requiredChargingDuration;
	}
	
	public void setRequiredChargingDuration(double rct){
		requiredChargingDuration=rct;
	}
	
	
	public boolean requiresCharging(){
		if(requiredChargingDuration>0){
			return true;
		}else{
			return false;
		}
	}
	
	
	public void setParkingOptimalBoolean(boolean o){
		optimal=o;
	}
	
	public boolean isInSystemOptimalChargingTime(){
		return optimal;
	}
	
	public void setJoulesInPotentialChargingInterval(double d){
		joulesInTimeInterval=d; 
	}
	
	
	@Override
	public void printInterval(){
		System.out.println("Parking Interval \t start: "+ this.getStartTime()+ "\t  end: "+ this.getEndTime()+ "\t  ChargingTime:  " + requiredChargingDuration+ "\t  Optimal:  " + optimal+ "\t  Joules per Interval:  " + joulesInTimeInterval);
	}
}
