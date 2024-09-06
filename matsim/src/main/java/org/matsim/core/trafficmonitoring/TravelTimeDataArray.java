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

	/* The original implementation used two arrays to store the data:
	 * an int-array (later replaced with a short-array) to store the count,
	 * and a double-array to store the travel-time. Thus, for each time-bin,
	 * 10 bytes were used. And for each operation, at least 2 memory accesses
	 * had to be performed to access the two arrays.
	 *
	 * The current implementation now uses only 1 array of type long, thus
	 * requiring only 8 bytes per time-bin instead of 10, a reduction of 20%.
	 * For each time-bin, the long-value contains the count as int-value in
	 * the higher 4 bytes, and the travel-time as an int in the lower 4 bytes.
	 * To support fractional values for the travel-time (although this is
	 * very rarely used in MATSim), the (double-value) travel-time is multiplied
	 * by 1000 and then stored as int, supporting millisecond resolution in
	 * the travel time and (2^31 - 1)/1000 = 2147483.647 seconds as maximum
	 * travel time (which is more than 596 hours or 24.8 days).
	 *
	 * The reduced memory usage can lead to fewer CPU cache misses, and as
	 * only one array access is necessary instead of two, routing-performance
	 * improves considerably, especially if used with an efficient routing
	 * implementation like SpeedyALT.
	 *
	 * -mrieser, april 2021
	 */
	private final long[] data;
	private final Link link;

	TravelTimeDataArray(final Link link, final int numSlots) {
		this.data = new long[numSlots];
		this.link = link;
		resetTravelTimes();
	}

	static long encode(int count, double traveltime) {
		long hi = count;
		int lo = (int) (traveltime * 1000);
		long val = hi << 32 | (lo & 0xffffffffL);
		return val;
	}

	static int count(long encoded) {
		return (int) (encoded >>> 32);
	}

	static double traveltime(long encoded) {
		int tt = (int) (encoded);
		return tt / 1000.0;
	}

	@Override
	public void resetTravelTimes() {
		long val = encode(0, -1.0);
		Arrays.fill(this.data, val);
	}

	@Override
	public void setTravelTime( final int timeSlot, final double traveltime ) {
		this.data[timeSlot] = encode(1, traveltime);
	}

	@Override
	public void addTravelTime(final int timeSlot, final double traveltime) {
		long val = this.data[timeSlot];
		int cnt = count(val);
		double tt = traveltime(val);
		double sum = tt * cnt;

		sum += traveltime;
		cnt++;

		this.data[timeSlot] = encode(cnt, sum / cnt);
	}

	@Override
	public double getTravelTime(final int timeSlot, final double now) {
		long val = this.data[timeSlot];
		double ttime = traveltime(val);
		if (ttime >= 0.0) return ttime; // negative values are invalid.

		// ttime can only be <0 if it never accumulated anything, i.e. if cnt == 0, so just use freespeed
		double freespeed = this.link.getLength() / this.link.getFreespeed(now);
		this.data[timeSlot] = encode(0, freespeed);
		return freespeed;
	}

	/* package-private for debugging */ String cntToString(){
		StringBuilder strb = new StringBuilder().append( "cnt=[ " );
		for( int ii = 0 ; ii < this.data.length ; ii++ ){
			strb.append( count(this.data[ii]) ).append( "      " );
		}
		strb.append( "]" );
		return strb.toString();
	}
	/* package-private for debugging */ String ttToString() {
		StringBuilder strb = new StringBuilder().append( "tt=[ " );
		for ( int ii=0 ; ii<this.data.length ; ii++ ) {
			strb.append( traveltime(this.data[ii]) ).append( " " );
		}
		strb.append( "]" );
		return strb.toString();
	}

}
