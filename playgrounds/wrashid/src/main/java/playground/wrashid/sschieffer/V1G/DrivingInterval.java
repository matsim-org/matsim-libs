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
package playground.wrashid.sschieffer.V1G;



public class DrivingInterval extends TimeInterval {
	
	double consumption;
	
	DrivingInterval(double start, double end, double consumption){
		super(start, end);
		
		this.consumption=consumption;
	}
	
	
	double getConsumption(){
		return consumption;
	}
	
	 @Override
	public void printInterval(){
		System.out.println("Driving Interval \t  start: "+ this.getStartTime()+ "\t  end: "+ this.getEndTime()+ "\t  consumption: " + getConsumption());
	}
	
}
