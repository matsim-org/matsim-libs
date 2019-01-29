/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractTravelTimeAggregator.java
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

package org.matsim.core.trafficmonitoring;

 abstract class AbstractTravelTimeAggregator {

	private final int travelTimeBinSize;
	private final int numSlots;
	private TravelTimeGetter travelTimeGetter;

	 AbstractTravelTimeAggregator(final int numSlots, final int travelTimeBinSize) {
		this.numSlots = numSlots;
		this.travelTimeBinSize = travelTimeBinSize;

		this.connectTravelTimeGetter(new AveragingTravelTimeGetter()); // by default
	}

	/**
	 * Naming this "connect" instead of "set" since it is a two-way pointer.  kai, aug'13
	 */
	 void connectTravelTimeGetter(final TravelTimeGetter travelTimeGetter) {
		this.travelTimeGetter = travelTimeGetter;
		travelTimeGetter.setTravelTimeAggregator(this);
	}

	 int getTimeSlotIndex(final double time) {
	    return TimeBinUtils.getTimeBinIndex(time, travelTimeBinSize, numSlots);
	}

	 abstract void addTravelTime(TravelTimeData travelTimeRole, double enterTime,
			double leaveTime);

	 void addStuckEventTravelTime(final TravelTimeData travelTimeRole,
			final double enterTime, final double stuckEventTime) {
		//here is the right place to handle StuckEvents (just overwrite this method)
	}

	 double getTravelTime(final TravelTimeData travelTimeRole, final double time) {
		return this.travelTimeGetter.getTravelTime(travelTimeRole, time);
	}

	/*package*/ TravelTimeGetter getTravelTimeGetter() { // for tests
		return this.travelTimeGetter;
	}

}