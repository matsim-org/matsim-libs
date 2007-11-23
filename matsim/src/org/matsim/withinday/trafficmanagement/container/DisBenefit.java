/* *********************************************************************** *
 * project: org.matsim.*
 * DisBenefit.java
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
 * Container class containing the d(t) output signal
 */
public class DisBenefit {
	
//	------------------ Instance variables -----------------------------------
	
	private double dis;
	private double simTime;
	
	
	/**
	 * Contructor.
	 * 
	 * @param dis
	 * 					the value
	 * @param simTime_s
	 * 					the time instance of the output
	 */
	public DisBenefit(final double dis, final double simTime_s){
		this.dis = dis;
		this.simTime = simTime_s;
	}
	
	/**
	 * Converts the dis value to a <code>String</code> that is returned
	 * 
	 * @return s
	 */
	@Override
	public String toString(){
		String s = Double.toString(this.dis);
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
	 * Returns the predictive output
	 * 
	 * @return time
	 */
	public double getValue() {
		return this.dis;
	}
}
