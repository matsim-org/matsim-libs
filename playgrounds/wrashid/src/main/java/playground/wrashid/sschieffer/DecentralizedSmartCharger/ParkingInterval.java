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

package playground.wrashid.sschieffer.DecentralizedSmartCharger;

import org.matsim.api.core.v01.Id;


/**
 * extends TimeInterval 
 * additional parameters are the required Charging Duration (and the related charging Schedule)
 * its location (linkId),
 * the available joules to charge according to the deterministic hub load profile during this interval.
 * if the interval is in a system optimal time slot meaning there is available energy to charge
 * (deterministic hub load >0 && joulesInTimeInterval>0), then the boolean parameter optimal is set to true. 
 * 
 * @author Stella
 *
 */
public class ParkingInterval extends TimeInterval {
	
	private double requiredChargingDuration=-1;
	private Id linkId;
	private double joulesInTimeInterval;
	private Schedule parkingChargingSchedule=null;
	
	private Schedule chargingSchedule=null;
	
	private boolean optimal; // true if in optimal loadDistributionInterval
	
	
	
	/**
	 * initialize
	 * @param start
	 * @param end
	 * @param linkId
	 */
	public ParkingInterval(double start, double end, Id linkId){
		super(start, end);
		this.linkId=linkId;
	}
	
	@Override
	public ParkingInterval clone(){
		ParkingInterval clone= new ParkingInterval(getStartTime(), getEndTime(), getLocation());
		if(chargingSchedule!=null){
			clone.setChargingSchedule(chargingSchedule.cloneSchedule());
		}	
		clone.setRequiredChargingDuration(requiredChargingDuration);
		clone.setJoulesInPotentialChargingInterval(joulesInTimeInterval);
		return clone;
	}
	
	
	
	public void setChargingSchedule(Schedule chargingSchedule){
		
		this.chargingSchedule=chargingSchedule;
		
		Schedule newParkingChargingSchedule = new Schedule();
		newParkingChargingSchedule.addTimeInterval(this);
		
		if(chargingSchedule!=null){		
			newParkingChargingSchedule.insertChargingIntervalsIntoParkingIntervalSchedule(chargingSchedule);
			setParkingChargingSchedule(newParkingChargingSchedule);
		}else{
			// can be reset to null, in a rescheduling procedure, if new required charging time is equal to zero in an interval
			setParkingChargingSchedule(newParkingChargingSchedule);
		}
		
	}
	
	
	public void setParkingChargingSchedule(Schedule parkingChargingSchedule){
		this.parkingChargingSchedule=parkingChargingSchedule;
	}
	
	
	
	public Schedule getChargingSchedule(){
		return chargingSchedule;
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
	
	/**
	 * only for print purposes
	 * @return
	 */
	private int getSizeOfChargingSchedule(){
		if(chargingSchedule!=null){
			return chargingSchedule.getNumberOfEntries();
		}else{
			return 0;
		}
	}
	
	
	@Override
	public void printInterval(){
		System.out.println("Parking Interval \t start: "+ this.getStartTime()
				+ "\t  end: "+ this.getEndTime()
				+ "\t  ChargingTime:  " + requiredChargingDuration
				+ "\t  Optimal:  " + optimal
				+ "\t  Joules per Interval:  " + joulesInTimeInterval
				+ "\t  ChargingSchedule of size:  " + getSizeOfChargingSchedule());
	}
}
