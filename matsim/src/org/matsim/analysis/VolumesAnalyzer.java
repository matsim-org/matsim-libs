/* *********************************************************************** *
 * project: org.matsim.*
 * VolumesAnalyzer.java
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

package org.matsim.analysis;

import java.util.Set;
import java.util.TreeMap;

import org.matsim.events.EventLinkLeave;
import org.matsim.events.handler.EventHandlerLinkLeaveI;
import org.matsim.network.NetworkLayer;

public class VolumesAnalyzer implements EventHandlerLinkLeaveI {

	private final int timeBinSize;
	private final int maxTime;
	private final int maxSlotIndex;
	private final NetworkLayer network;
	private final TreeMap<String, int[]> links = new TreeMap<String, int[]>();

	public VolumesAnalyzer(final int timeBinSize, final int maxTime, final NetworkLayer network) {
		this.timeBinSize = timeBinSize;
		this.maxTime = maxTime;
		this.network = network;
		this.maxSlotIndex = (this.maxTime/this.timeBinSize) + 1;
	}


	//////////////////////////////////////////////////////////////////////
	// Implementation of EventAlgorithmI
	//////////////////////////////////////////////////////////////////////

	public void handleEvent(final EventLinkLeave event) {
		String linkid = event.linkId;
		int[] volumes = this.links.get(linkid);
		if (volumes == null) {
			volumes = new int[this.maxSlotIndex + 1];
			for (int i = 0; i <= this.maxSlotIndex; i++) {
				volumes[i] = 0;
			}
			this.links.put(linkid, volumes);
		}
		int timeslot = getTimeSlotIndex(event.time);
		volumes[timeslot]++;
	}

	//////////////////////////////////////////////////////////////////////
	// public / private methods
	//////////////////////////////////////////////////////////////////////

	private int getTimeSlotIndex(final double time) {
		if (time > this.maxTime) {
			return this.maxSlotIndex;
		}
		return ((int)time / this.timeBinSize);
	}

	public int[] getVolumesForLink(final String linkid) {
		return this.links.get(linkid);
	}

	public Set<String> getLinkIds() {
		return this.links.keySet();
	}

	public void reset(final int iteration) {
		this.links.clear();
	}
}
