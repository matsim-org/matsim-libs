/* *********************************************************************** *
 * project: org.matsim.*
 * AveragingTravelTimeGetter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.core.trafficmonitoring;

/**
 *  Returns the travel time that is stored in the TravelTimeData objects without modification. 
 *
 */
class AveragingTravelTimeGetter implements TravelTimeGetter {

	private TimeSlotComputation travelTimeAggregator;
	
	public AveragingTravelTimeGetter( TimeSlotComputation travelTimeAggregator ) {
		this.travelTimeAggregator = travelTimeAggregator;		
	}
	
	@Override
	public double getTravelTime(TravelTimeData travelTimeData, double time) {
		final int timeSlot = travelTimeAggregator.getTimeSlotIndex(time);
		return travelTimeData.getTravelTime(timeSlot, time);
	}

}
