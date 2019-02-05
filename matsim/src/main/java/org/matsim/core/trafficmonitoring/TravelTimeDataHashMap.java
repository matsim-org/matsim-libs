/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeRoleHashMap.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.misc.IntegerCache;

/**
 * Implementation of {@link TravelTimeData} that stores the travel time data in
 * a HashMap. This has the advantage that no memory is used if no vehicle travels
 * over a link, and thus no data is available to be stored. Especially useful with
 * short time bins as there the probability to have empty time bins is higher.
 *
 * @author mrieser
 * @author glaemmel
 */
public class TravelTimeDataHashMap extends TravelTimeData {
	private final Map<Integer,TimeStruct> travelTimes;
	
	protected final Link link;

	public TravelTimeDataHashMap(final Link link) {
		this.travelTimes = new ConcurrentHashMap<>();
		this.link = link;
//		resetTravelTimes();
	}

	@Override
	public void resetTravelTimes() {
		this.travelTimes.clear();
	}
	
	@Override
	public void setTravelTime( final int timeSlice, final double traveltime ) {
		TimeStruct curr = this.travelTimes.get(IntegerCache.getInteger(timeSlice));
		if (curr != null) {
			curr.cnt = 1;
			curr.timeSum = traveltime;
		} else {
			this.travelTimes.put(IntegerCache.getInteger(timeSlice), new TimeStruct(traveltime,1));
		}
	}
	
	@Override
	public void addTravelTime(final int timeSlice, final double traveltime) {
		TimeStruct curr = this.travelTimes.get(IntegerCache.getInteger(timeSlice));
		if (curr != null) {
			curr.cnt += 1;
			curr.timeSum += traveltime;
		} else {
			this.travelTimes.put(IntegerCache.getInteger(timeSlice), new TimeStruct(traveltime,1));
		}
	}

	@Override
	public double getTravelTime(final int timeSlice, final double now) {

		TimeStruct ts = this.travelTimes.get(IntegerCache.getInteger(timeSlice));
		if (ts == null) {
			Link r = ((Link)this.link);
			return NetworkUtils.getFreespeedTravelTime(r, now) ;
		}
		return ts.timeSum / ts.cnt;
	}

	private static class TimeStruct {
		public double timeSum;
		public int cnt;
		public TimeStruct(final double timeSum, final int cnt) {
			this.cnt = cnt;
			this.timeSum = timeSum;
		}
	}

}
