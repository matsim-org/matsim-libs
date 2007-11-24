/* *********************************************************************** *
 * project: org.matsim.*
 * NashTime.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.withinday.trafficmanagement.container;

/**
 * Container class containing the two output signals for one time instance
 */
public class NashTime {
	
//	------------------ Instance variables -----------------------------------
	
	private double time;
	private double timeExp;
	private int simTime;
	
	
	/**
	 * Contructor.
	 * 
	 * @param time
	 * 					the predictive output
	 * @param timeExp
	 * 					the reactive output
	 * @param simTime_s
	 * 					the time instance of the output
	 */
	public NashTime(double time, double timeExp, int simTime_s){
		this.time = time;
		this.timeExp = timeExp;
		this.simTime = simTime_s;
	}
	
	/**
	 * Converts the predictive output to a <code>String</code> that is returned
	 * 
	 * @return s
	 */
	@Override
	public String toString(){
		String s = Double.toString(time);
		return s;
	}
	
	/**
	 * Returns the time instance of the output
	 * 
	 * @return simTime
	 */
	public int getSimTime(){
		return simTime;
	}
	
	/**
	 * Returns the predictive output
	 * 
	 * @return time
	 */
	public double getValue() {
		return time;
	}
	
	/**
	 * Returns the reactive output
	 * 
	 * @return timeExp
	 */
	public double getValueExp() {
		return timeExp;
	}

}
