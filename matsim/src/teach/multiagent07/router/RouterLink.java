/* *********************************************************************** *
 * project: org.matsim.*
 * RouterLink.java
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

package teach.multiagent07.router;

import java.util.HashMap;

import teach.multiagent07.net.CALink;

public class RouterLink extends CALink {

	public RouterLink(String id) {
		super(id);
		// TODO Auto-generated constructor stub
	}

	private HashMap<Integer, Long> timeSum_ = null;	// map<timeslot-index, sum-of-travel-times>
	private HashMap<Integer, Integer> timeCnt_ = null;		// map<timeslot-index, count-of-travel-times>
	private int timeslice_ = 1000;	// default timeslot-duration: 15 minutes
	
	private int getTimeSlotIndex(int time) {
		int slice = (int)Math.floor(time/timeslice_);
		return slice;
	}
	
	public void build() {
		super.build();
		resetTravelTimes();
	}

	public void resetTravelTimes() {
		int nofSlots = (int)((27*3600)/timeslice_);	// default number of slots
		timeSum_ = new HashMap<Integer, Long>(nofSlots);
		timeCnt_ = new HashMap<Integer, Integer>(nofSlots);
	}
	
	public void addTravelTime(int now, int traveltime) {
		Integer index = new Integer(getTimeSlotIndex(now));
		Long sum = timeSum_.get(index);
		Integer cnt = timeCnt_.get(index);
		if (null == sum) {
			sum = new Long(traveltime);
			cnt = new Integer(1);
		} else {
			sum = new Long(sum.longValue() + traveltime);
			cnt = new Integer(cnt.intValue() + 1);
		}
		timeSum_.put(index, sum);
		timeCnt_.put(index, cnt);
	}
	
	public double getTravelTime(double now) {
		Integer index = new Integer(getTimeSlotIndex((int)now));
		Long sum = timeSum_.get(index);
		if (null == sum) {
			// we do not yet have any information about the speed at this time
			// assume free speed
			return getLength() / getFreespeed();
		} else {
			Integer cnt = timeCnt_.get(index);
			return sum.longValue() / cnt.intValue();
		}
	}
	
	public void printTravelTimeDump() {
		System.out.println("Link " + getId() + " from node " + getFromNode().getId() + " to node " + getToNode().getId());
		System.out.println("  " + timeSum_);
		System.out.println("  " + timeCnt_);
	}

}
