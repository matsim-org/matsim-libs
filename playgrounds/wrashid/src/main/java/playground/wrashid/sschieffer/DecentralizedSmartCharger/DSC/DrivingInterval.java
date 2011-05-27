/* *********************************************************************** *
 * project: org.matsim.*
 * DrivingInterval.java
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
package playground.wrashid.sschieffer.DecentralizedSmartCharger.DSC;



/**
 * extends TimeInterval
 * extra parameters are the total consumption during the interval for driving.
 * 
 * the variables extraConsumption [J] and timeEngine [s] are adjusted during the optimization
 * if the energy is taken from a source other than the electric battery.
 * 
 * 
 *  		
 * @author Stella
 *
 */
public class DrivingInterval extends TimeInterval {
	
	double consumptionFromBattery; // consumption from battery
	
	double extraConsumption=0; // consumption from engine
	double timeEngine=0;
	
	boolean extra=false;
	
	
	/**
	 * Driving Interval with start, end and consumption in Joules
	 * @param start - startTime
	 * @param end - End Time
	 * @param consumption - Consumption in Joule
	 */
	public  DrivingInterval(double start, double end, double consumption){
		super(start, end);
		
		this.consumptionFromBattery=consumption;
	}
	
	@Override
	public DrivingInterval clone(){
		DrivingInterval d = new DrivingInterval(getStartTime(), getEndTime(), getBatteryConsumption());
		d.setValueForExtraConsumption(extraConsumption);
		d.setValueForExtraTime(timeEngine);
		return d;
	}
	
	
	private void setValueForExtraConsumption(double extra){
		this.extraConsumption=extra;
	}
	
	private void setValueForExtraTime(double extra){
		this.timeEngine=extra;
	}
	
	public double getBatteryConsumption(){
		return consumptionFromBattery;
	}
	
	
	
	/**
	 * reduces current consumption by extraC
	 * and sets extraConsumption equal extraC
	 * 
	 * @param extraC extra consumption not from electric battery
	 * @param extraTime extra time required prior to this driving interval to charge the necessary missing consumption
	 */
	public void setExtraConsumption(double extraC, double extraTime){
		consumptionFromBattery=consumptionFromBattery-extraC;
		extraConsumption+=extraC;
		timeEngine=extraTime;
		
		extra=true;
	}
	
	
	public boolean hasExtraConsumption(){
		return extra;
	}
	
	
	public double getExtraConsumption(){
		return extraConsumption;
	}
	
	
	public double getEngineTime(){
		return timeEngine;
	}
	
	 @Override
	public void printInterval(){
		System.out.println("Driving Interval \t  start: "+ this.getStartTime()+ "\t  end: "+ this.getEndTime()+ "\t  consumption: " + getBatteryConsumption());
	}
	
}
