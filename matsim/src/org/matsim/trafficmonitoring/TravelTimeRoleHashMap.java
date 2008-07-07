/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeRoleHashMap.java
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

import java.util.HashMap;

import org.matsim.network.Link;
import org.matsim.utils.misc.IntegerCache;

public class TravelTimeRoleHashMap implements TravelTimeRole {
	private final HashMap<Integer,TimeStruct> travelTimes;
	
//	private int currIdx;
//	private int currCnt;
//	private double currTimeSum;
	private final Link link;

	

	public TravelTimeRoleHashMap(final Link link, final int numSlots) {

		this.travelTimes =  new HashMap<Integer,TimeStruct>();
		this.link = link;
		resetTravelTimes();

	}


	public void resetTravelTimes() {
//		this.currCnt = 0;
//		this.currIdx = 0;
//		this.currTimeSum = 0;
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
		
//		if (timeSlice != this.currIdx){
//			changeCurrent(timeSlice);
//
//		}
//		this.currCnt++;
//		this.currTimeSum += traveltime;
	}

//	private void changeCurrent(final int index) {
//		TimeStruct curr = this.travelTimes.get(IntegerCache.getInteger(this.currIdx));
//		// save old
//		if (curr != null ){
//			curr.cnt += this.currCnt;
//			curr.timeSum += this.currTimeSum;
//		} else if (this.currCnt > 0){
//			this.travelTimes.put(IntegerCache.getInteger(this.currIdx), new TimeStruct(this.currTimeSum,this.currCnt));
//
//		}
//
//		// set new
//		this.currIdx = index;
//		curr = this.travelTimes.get(IntegerCache.getInteger(this.currIdx));
//		if (curr == null){
//			this.currCnt = 0;
//			this.currTimeSum = 0;
//		} else {
//			this.currCnt = curr.cnt;
//			this.currTimeSum = curr.timeSum;
//		}
//
//	}

	public double getTravelTime(final int timeSlice, final double now) {

//		if (timeSlice == this.currIdx) {
//			return this.currTimeSum / this.currCnt;
//		}
		TimeStruct ts = this.travelTimes.get(IntegerCache.getInteger(timeSlice));
		if (ts == null){
			return this.link.getLength() / this.link.getFreespeed(now);
		}

		return ts.timeSum / ts.cnt;

	}

	private static class TimeStruct{
		public double timeSum;
		public int cnt;
		public TimeStruct(final double timeSum, final int cnt){
			this.cnt = cnt;
			this.timeSum = timeSum;
		}
	};

}
