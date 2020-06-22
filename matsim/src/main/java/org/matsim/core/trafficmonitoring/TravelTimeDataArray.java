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

import java.util.Arrays;

/**
 * Implementation of {@link TravelTimeData} that stores the data per time bin
 * in simple arrays. Useful if not too many empty time bins (time bins with 
 * no traffic on a link) exist, so no memory is wasted.
 *
 * @author mrieser
 */
class TravelTimeDataArray extends TravelTimeData {

	private final short[] timeCnt;
	private final double[] travelTimes;
	private final Link link;

	TravelTimeDataArray(final Link link, final int numSlots) {
		this.timeCnt = new short[numSlots];
		this.travelTimes = new double[numSlots];
		this.link = link;
		resetTravelTimes();
	}

	@Override
	public void resetTravelTimes() {
		Arrays.fill(this.timeCnt, (short) 0);
		Arrays.fill(this.travelTimes, -1.0);
	}

	@Override
	public void setTravelTime( final int timeSlot, final double traveltime ) {
		this.timeCnt[timeSlot] = 1;
		this.travelTimes[timeSlot] = traveltime;
	}

	@Override
	public void addTravelTime(final int timeSlot, final double traveltime) {
		short cnt = this.timeCnt[timeSlot];
		double sum = this.travelTimes[timeSlot] * cnt;

		sum += traveltime;
		cnt++;

		this.travelTimes[timeSlot] = sum / cnt;
		this.timeCnt[timeSlot] = cnt;
	}

	@Override
	public double getTravelTime(final int timeSlot, final double now) {
		double ttime = this.travelTimes[timeSlot];
		if (ttime >= 0.0) return ttime; // negative values are invalid.

		// ttime can only be <0 if it never accumulated anything, i.e. if cnt == 9, so just use freespeed
		double freespeed = this.link.getLength() / this.link.getFreespeed(now);
		this.travelTimes[timeSlot] = freespeed;
		return freespeed;
	}

	/* package-private for debugging */ String cntToString(){
		StringBuilder strb = new StringBuilder().append( "cnt=[ " );
		for( int ii = 0 ; ii < this.timeCnt.length ; ii++ ){
			strb.append( this.timeCnt[ii] ).append( "      " );
		}
		strb.append( "]" );
		return strb.toString();
	}
	/* package-private for debugging */ String ttToString() {
		StringBuilder strb = new StringBuilder().append( "tt=[ " );
		for ( int ii=0 ; ii<this.travelTimes.length ; ii++ ) {
			strb.append( this.travelTimes[ii] ).append( " " );
		}
		strb.append( "]" );
		return strb.toString();
	}

}
