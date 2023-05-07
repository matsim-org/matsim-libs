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

final class TimeSlotComputation{

	private final double travelTimeBinSize;
	private final int numSlots;

	 TimeSlotComputation( final int numSlots, final double travelTimeBinSize ) {
		this.numSlots = numSlots;
		this.travelTimeBinSize = travelTimeBinSize;
	}

	 int getTimeSlotIndex(final double time) {
	    return TimeBinUtils.getTimeBinIndex(time, travelTimeBinSize, numSlots);
	}

 }
