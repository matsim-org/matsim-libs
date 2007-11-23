/* *********************************************************************** *
 * project: org.matsim.*
 * ControlVariable.java
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
 * Container class containing the control signal for a certain time instance
 *
 */
public class ControlVariable {
	
//	---------------------- Instance variables -----------------------------
	
	private double splittingRate;
	private double simTime;
	
	/**
	 * Constructor
	 * 
	 * @param splittingRate
	 * 							the control input
	 * @param simTime_s
	 * 							the time instance
	 */
	public ControlVariable(final double splittingRate, final double simTime_s){
		this.splittingRate = splittingRate;
		this.simTime = simTime_s;
	}
	
	/**
	 * Converts the control signal to a <code>String</code> that is returned
	 * 
	 * @return s
	 */
	@Override
	public String toString(){
		String s = Double.toString(this.splittingRate);
		return s;
	}
	
	/**
	 * Returns the time instance of the output
	 * 
	 * @return simTime
	 */
	public double getSimTime(){
		return this.simTime;
	}
	
	/**
	 * Returns the control signal value
	 * 
	 * @return splittingRate
	 */
	public double getValue() {
		return this.splittingRate;
	}

}
