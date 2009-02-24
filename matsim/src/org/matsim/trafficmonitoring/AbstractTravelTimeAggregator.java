/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeAggregator.java
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

package org.matsim.trafficmonitoring;

public abstract class AbstractTravelTimeAggregator {

	private final int travelTimeBinSize;
	private final int numSlots;


	public AbstractTravelTimeAggregator(int numSlots, int travelTimeBinSize) {
		this.numSlots = numSlots;
		this.travelTimeBinSize = travelTimeBinSize;
	}
	
	
	protected int getTimeSlotIndex(final double time) {
		int slice = ((int) time)/this.travelTimeBinSize;
		if (slice >= this.numSlots) slice = this.numSlots - 1;
		return slice;
	}


	protected abstract void addTravelTime(TravelTimeData travelTimeRole, double enterTime,
			double leaveTime);


	public void addStuckEventTravelTime(TravelTimeData travelTimeRole,
			double enterTime, double stuckEventTime) {
		//here is the right place to handle StuckEvents (just overwrite this method) 
	}
	

	protected double getTravelTime(TravelTimeData travelTimeRole, double time) {
		final int timeSlot = getTimeSlotIndex(time);
		return travelTimeRole.getTravelTime(timeSlot, time);
	}



		

}
