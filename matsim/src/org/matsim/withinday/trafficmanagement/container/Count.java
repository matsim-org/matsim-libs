/* *********************************************************************** *
 * project: org.matsim.*
 * Count.java
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
 * Container class containing the number of agents presently traveling on one route
 * from one of the signLinks to one of the directionLinks a certain time instance
 *
 */
public class Count {
	
//	----------------- Instance variables ------------------------------
	
	private double value;
	private int simTime;
	
	
	/**
	 * Constructor
	 * 
	 * @param value
	 * 					the number of agents on the route
	 * @param simTime_s
	 * 					the time instance
	 */
	public Count(double value, int simTime_s){
		this.value = value;
		this.simTime = simTime_s;
	}
	
	/**
	 * Converts the number of agents to a <code>String</code> that is returned
	 * 
	 * @return s
	 */
	public String toString(){
		String s = Double.toString(value);
		return s;
	}
	
	/**
	 * Returns the time instance 
	 * 
	 * @return simTime
	 */
	public int getSimTime(){
		return simTime;
	}
	
	/**
	 * Returns the number of agents on the route
	 * 
	 * @return value
	 */
	public double getValue(){
		return value;
	}

}
