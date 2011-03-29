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
package playground.wrashid.sschieffer.DecentralizedSmartCharger;



public class DrivingInterval extends TimeInterval {
	
	double consumption;
	
	double extraConsumption=0;
	double timeEngine=0;
	
	boolean extra=false;
	
	public  DrivingInterval(double start, double end, double consumption){
		super(start, end);
		
		this.consumption=consumption;
	}
	
	
	public double getConsumption(){
		return consumption;
	}
	
	
	
	/**
	 * reduces current consumption by extraC
	 * and sets extraConsumption equal extraC
	 * 
	 * @param extraC
	 */
	public void setExtraConsumption(double extraC, double extraTime){
		consumption=consumption-extraC;
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
		System.out.println("Driving Interval \t  start: "+ this.getStartTime()+ "\t  end: "+ this.getEndTime()+ "\t  consumption: " + getConsumption());
	}
	
}
