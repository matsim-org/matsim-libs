/* *********************************************************************** *
 * project: org.matsim.*
 * TimeBin
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
package playground.vsp.energy.trafficstate;


/**
 * @author dgrether
 *
 */
public class TimeBin {

	private double startTime;
	
	private double endTime; 
	
	private double averageSpeed;
	
	public TimeBin(double startTime, double endTime, double averageSpeed){
		this.startTime = startTime;
		this.endTime = endTime;
		this.averageSpeed = averageSpeed;
	}
	
	public double getStartTime() {
		return startTime;
	}
	
	public double getEndTime() {
		return endTime;
	}

	
	public double getAverageSpeed() {
		return averageSpeed;
	}
	
}
