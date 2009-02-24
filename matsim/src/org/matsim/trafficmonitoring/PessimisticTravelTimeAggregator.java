/* *********************************************************************** *
 * project: org.matsim.*
 * ProgressiveTravelTimeCalculator.java
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

public class PessimisticTravelTimeAggregator extends AbstractTravelTimeAggregator {

	public PessimisticTravelTimeAggregator(int travelTimeBinSize, int numSlots) {
		super(travelTimeBinSize, numSlots);
	}

	@Override
	protected void addTravelTime(TravelTimeData travelTimeRole,
			double enterTime, double leaveTime) {

		double ttime = leaveTime - enterTime;
		for (int slot = getTimeSlotIndex(enterTime); slot <= getTimeSlotIndex(leaveTime); slot++ ){
			travelTimeRole.addTravelTime(slot, ttime);
		}
				
	}

	@Override
	public void addStuckEventTravelTime(TravelTimeData travelTimeRole,
			double enterTime, double stuckEventTime) {
		double ttime = Double.POSITIVE_INFINITY;
		for (int slot = getTimeSlotIndex(enterTime); slot <= getTimeSlotIndex(stuckEventTime); slot++ ){
			travelTimeRole.addTravelTime(slot, ttime);
		}
	}
	

}
