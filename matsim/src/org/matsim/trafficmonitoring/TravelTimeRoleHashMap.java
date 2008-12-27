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

package org.matsim.trafficmonitoring;

import java.util.HashMap;

import org.matsim.network.Link;
import org.matsim.utils.misc.IntegerCache;

public class TravelTimeRoleHashMap implements TravelTimeRole {
	private final HashMap<Integer,TimeStruct> travelTimes;
	
	private final Link link;

	public TravelTimeRoleHashMap(final Link link) {
		this.travelTimes =  new HashMap<Integer,TimeStruct>();
		this.link = link;
		resetTravelTimes();
	}

	public void resetTravelTimes() {
		this.travelTimes.clear();
	}

	public void addTravelTime(final int timeSlice, final double traveltime) {
		TimeStruct curr = this.travelTimes.get(IntegerCache.getInteger(timeSlice));
		if (curr != null ){
			curr.cnt += 1;
			curr.timeSum += traveltime;
		} else {
			this.travelTimes.put(IntegerCache.getInteger(timeSlice), new TimeStruct(traveltime,1));
		}
		
	}

	public double getTravelTime(final int timeSlice, final double now) {

		TimeStruct ts = this.travelTimes.get(IntegerCache.getInteger(timeSlice));
		if (ts == null){
			return this.link.getLength() / this.link.getFreespeed(now);
		}

		return ts.timeSum / ts.cnt;

	}

	private static class TimeStruct {
		public double timeSum;
		public int cnt;
		public TimeStruct(final double timeSum, final int cnt){
			this.cnt = cnt;
			this.timeSum = timeSum;
		}
	}

}
