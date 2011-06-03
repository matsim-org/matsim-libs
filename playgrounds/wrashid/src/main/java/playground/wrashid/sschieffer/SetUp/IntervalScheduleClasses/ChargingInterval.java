/* *********************************************************************** *
 * project: org.matsim.*
 * ChargingInterval.java
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
package playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses;


/**
 * extends TimeInterval
 * TimeSlot designated for Charging
 * @author Stella
 *
 */
public class ChargingInterval extends TimeInterval {
// might be unnecessary
	
	private boolean battery;
	
	public ChargingInterval(double start, double end){
		super(start,end);
		battery=true;
	}
	
	@Override
	public ChargingInterval clone(){
		ChargingInterval c= new ChargingInterval(getStartTime(), getEndTime());
		
		return c;
		
	}
	
	
	/**
	 * 
	 * @param start
	 * @param end
	 * @param fromBatteryYesNo
	 */
	public ChargingInterval(double start, double end, boolean fromBatteryYesNo){
		super(start,end);
		battery=fromBatteryYesNo;
	}
	
	
	public boolean isFromBattery(){
		return battery;
	}
}
