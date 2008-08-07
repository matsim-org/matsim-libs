/* *********************************************************************** *
 * project: org.matsim.*
 * LinkQueueStats.java
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

package org.matsim.events.algorithms;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.matsim.events.LinkEnterEnter;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.handler.LinkEnterEventHandler;
import org.matsim.events.handler.LinkLeaveEventHandler;
import org.matsim.utils.misc.Time;

/**
 * Counts the number of vehicles on a link. It can output a list of times and
 * corresponding numbers of vehicles on a link at that time.
 *
 * @author mrieser
 */
public class LinkQueueStats implements LinkEnterEventHandler, LinkLeaveEventHandler {

	private String linkId;
	private SortedSet<Double> enterTimes = new TreeSet<Double>();
	private SortedSet<Double> leaveTimes = new TreeSet<Double>();

	public LinkQueueStats(String linkId) {
		this.linkId = linkId;
	}

	public void handleEvent(LinkEnterEnter event) {
		if (event.linkId.equals(this.linkId)) {
			enterTimes.add(event.time);
		}
	}

	public void handleEvent(LinkLeaveEvent event) {
		if (event.linkId.equals(this.linkId)) {
			leaveTimes.add(event.time);
		}
	}

	public void reset(int iteration) {
		enterTimes.clear();
		leaveTimes.clear();
	}

	public void dumpStats(Writer out) throws IOException {
		double time = 0;
		int count = 0;

		Iterator<Double> enterIter = enterTimes.iterator();
		Iterator<Double> leaveIter = leaveTimes.iterator();
		if (!enterIter.hasNext()) {
			// there seems to be no information...
			return;
		}
		Double enterTime = enterIter.next();
		Double leaveTime = leaveIter.next();
		while (enterTime != null || leaveTime != null) {
			if (enterTime == null) {
				double dLeaveTime = leaveTime.doubleValue();
				// decrease
				if (time != dLeaveTime) {
					dumpLine(out, time, count);
					time = dLeaveTime;
				}
				count--;
				leaveTime = leaveIter.hasNext() ? leaveIter.next() : null;
			} else if (leaveTime == null) {
				double dEnterTime = enterTime.doubleValue();
				// increase
				if (time != dEnterTime) {
					dumpLine(out, time, count);
					time = dEnterTime;
				}
				count++;
				enterTime = enterIter.hasNext() ? enterIter.next() : null;
			} else {
				double dLeaveTime = leaveTime.doubleValue();
				double dEnterTime = enterTime.doubleValue();
				if (dLeaveTime <= dEnterTime) {
					// decrease
					if (time != dLeaveTime) {
						dumpLine(out, time, count);
						time = dLeaveTime;
					}
					count--;
					leaveTime = leaveIter.hasNext() ? leaveIter.next() : null;
				} else {
					// leaveTime > enterTime
					// increase
					if (time != dEnterTime) {
						dumpLine(out, time, count);
						time = dEnterTime;
					}
					count++;
					enterTime = enterIter.hasNext() ? enterIter.next() : null;
				}
			}
		}

	}

	private void dumpLine(Writer out, double time, int count) throws IOException {
		out.write(Time.writeTime(time, Time.TIMEFORMAT_HHMMSS) + "\t" + time + "\t" + count + "\n");
	}

}
