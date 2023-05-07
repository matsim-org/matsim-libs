/* *********************************************************************** *
 * project: org.matsim.*
 * LinearInterpolatingTravelTimeGetter.java
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

class LinearInterpolatingTravelTimeGetter implements TravelTimeGetter {

	private final TimeSlotComputation travelTimeAggregator ;
	private final int numSlots;
	private final double travelTimeBinSize;
	private final double halfBinSize;

	public LinearInterpolatingTravelTimeGetter( int numSlots, double travelTimeBinSize, TimeSlotComputation aggregator ) {
		this.numSlots = numSlots;
		this.travelTimeBinSize = travelTimeBinSize;
		this.halfBinSize = ((double) travelTimeBinSize) / 2;
		this.travelTimeAggregator = aggregator ;
	}

	@Override
	public double getTravelTime(TravelTimeData travelTimeData, double time) {
		final int timeSlot = travelTimeAggregator.getTimeSlotIndex(time);

		// if time is in the first half of the first slot we do not interpolate
		if (time <= halfBinSize) return travelTimeData.getTravelTime(timeSlot, time);

		// if time is in the second half of the last slot we do not interpolate
		else if (time >= numSlots * travelTimeBinSize - halfBinSize) return travelTimeData.getTravelTime(timeSlot, time);


		// time is inbetween, therefore we interpolate
		int firstSlot;
		int secondSlot;

		// if time lies in the first half of the time slot
		if (timeSlot * travelTimeBinSize + halfBinSize > time) {
			firstSlot = timeSlot - 1;
			secondSlot = timeSlot;
		} else {
			firstSlot = timeSlot;
			secondSlot = timeSlot + 1;
		}

		// calculate travel times for both time slots
		double firstTravelTime = travelTimeData.getTravelTime(firstSlot, time);
		double secondTravelTime = travelTimeData.getTravelTime(secondSlot, time);

		// interpolate travel time
		double dx = time - (firstSlot * travelTimeBinSize + halfBinSize);
//		double dy = (secondTravelTime - firstTravelTime) * (travelTimeBinSize - dx) / travelTimeBinSize;
		/*
		 * The line above was a bug: we need to divide the change in travel time (i.e. second minus first travel time) by the change in time (i.e. the time bin size)
		 * to get the gradient of the line between the midpoint of the first interval and the midpoint of the second interval.
		 * Then we have to multiply it by dx to get dy. The last part was wrong (it was multiplied by time bin size minus dx).
		 * The test (TravelTimeCalculatorTest) only tests one value in each time bin -- namely the starting time of each time bin, which is unfortunately
		 * always the midpoint on the interpolated lines, i.e. the only time per time bin where the bug has no influence... theresa, sep'17
		 */
		double dy = (secondTravelTime - firstTravelTime) * dx / travelTimeBinSize;

		return firstTravelTime + dy;
	}

}
