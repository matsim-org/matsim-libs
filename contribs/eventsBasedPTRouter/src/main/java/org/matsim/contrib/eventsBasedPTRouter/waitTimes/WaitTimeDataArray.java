/* *********************************************************************** *
 * project: org.matsim.*
 * WaitTimeCalculator.java
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

package org.matsim.contrib.eventsBasedPTRouter.waitTimes;

/**
 * Array implementation of the structure for saving wait times
 * 
 * @author sergioo
 */

public class WaitTimeDataArray implements WaitTimeData {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	//Attributes
	private double[] waitTimes;
	private int[] numTimes;

	//Constructors
	public WaitTimeDataArray(int numSlots) {
		waitTimes = new double[numSlots];
		numTimes = new int[numSlots];
		resetWaitTimes();
	}

	//Methods
	@Override
	public void resetWaitTimes() {
		for(int i=0; i<waitTimes.length; i++) {
			waitTimes[i] = 0;
			numTimes[i] = 0;
		}
	}
	@Override
	public synchronized void addWaitTime(int timeSlot, double waitTime) {
		waitTimes[timeSlot] = (waitTimes[timeSlot]*numTimes[timeSlot]+waitTime)/++numTimes[timeSlot];
	}
	@Override
	public double getWaitTime(int timeSlot) {
		return waitTimes[timeSlot<waitTimes.length?timeSlot:(waitTimes.length-1)];
	}
	@Override
	public int getNumData(int timeSlot) {
		return numTimes[timeSlot<waitTimes.length?timeSlot:(waitTimes.length-1)];
	}

}
