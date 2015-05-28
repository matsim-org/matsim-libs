/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeRoleArray.java
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

import org.matsim.api.core.v01.network.Link;

/**
 * Implementation of {@link TravelTimeData} that stores the data per time bin
 * in simple arrays. Useful if not too many empty time bins (time bins with 
 * no traffic on a link) exist, so no memory is wasted.
 *
 * @author mrieser
 */
public class TravelTimeDataArray implements TravelTimeData {
	private final double[] timeSum;
	private final int[] timeCnt;
	private final double[] travelTimes;
	private final Link link;

	public TravelTimeDataArray(final Link link, final int numSlots) {
		this.timeSum = new double[numSlots];
		this.timeCnt = new int[numSlots];
		this.travelTimes = new double[numSlots];
		this.link = link;
		resetTravelTimes();
	}

	@Override
	public void resetTravelTimes() {
		for (int i = 0; i < this.timeSum.length; i++) {
			this.timeSum[i] = 0.0;
			this.timeCnt[i] = 0;
			this.travelTimes[i] = -1.0;
		}
	}
	
//	@Override
//	public void resetTravelTime( final int timeSlot ) {
//		this.timeSum[timeSlot] = 0.0;
//		this.timeCnt[timeSlot] = 0;
//		this.travelTimes[timeSlot] = -1.0;
//	}
	
	@Override
	public void setTravelTime( final int timeSlot, final double traveltime ) {
		this.timeSum[timeSlot] = traveltime ;
		this.timeCnt[timeSlot] = 1 ;
		this.travelTimes[timeSlot] = traveltime ; // since this is the only travel time, we do not need to trigger the cache consolidation.
		// if ever some other value is added, the cache is invalidated in addTravelTime. kai/theresa, may'15
	}

	@Override
	public void addTravelTime(final int timeSlot, final double traveltime) {
		double sum = this.timeSum[timeSlot];
		int cnt = this.timeCnt[timeSlot];
		sum += traveltime;
		cnt++;
		this.timeSum[timeSlot] = sum;
		this.timeCnt[timeSlot] = cnt;
		this.travelTimes[timeSlot] = -1.0; // initialize with negative value
	}

	@Override
	public double getTravelTime(final int timeSlot, final double now) {
		double ttime = this.travelTimes[timeSlot];
		if (ttime >= 0.0) return ttime; // negative values are invalid.

		int cnt = this.timeCnt[timeSlot];
		if (cnt == 0) {
			this.travelTimes[timeSlot] = this.link.getLength() / this.link.getFreespeed(now);
			return this.travelTimes[timeSlot];
		}

		double sum = this.timeSum[timeSlot];
		this.travelTimes[timeSlot] = sum / cnt;
		return this.travelTimes[timeSlot];
	}
	

}
