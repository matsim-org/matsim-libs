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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.events.EventLinkLeave;
import org.matsim.events.handler.EventHandlerLinkLeaveI;
import org.matsim.network.NetworkLayer;

/**
 * Counts the number of vehicles leaving a link, aggregated into time bins of a specified size.
 *
 * @author mrieser
 */
public class VolumesAnalyzer implements EventHandlerLinkLeaveI {

	private final int timeBinSize;
	private final int maxTime;
	private final int maxSlotIndex;
	private final Map<String, int[]> links;

	public VolumesAnalyzer(final int timeBinSize, final int maxTime, final NetworkLayer network) {
		this.timeBinSize = timeBinSize;
		this.maxTime = maxTime;
		this.maxSlotIndex = (this.maxTime/this.timeBinSize) + 1;
		this.links = new HashMap<String, int[]>((int) (network.getLinks().size() * 1.1), 0.95f);
	}

	public void handleEvent(final EventLinkLeave event) {
		String linkid = event.linkId;
		int[] volumes = this.links.get(linkid);
		if (volumes == null) {
			volumes = new int[this.maxSlotIndex + 1]; // initialized to 0 by default, according to JVM specs
			this.links.put(linkid, volumes);
		}
		int timeslot = getTimeSlotIndex(event.time);
		volumes[timeslot]++;
	}

	private int getTimeSlotIndex(final double time) {
		if (time > this.maxTime) {
			return this.maxSlotIndex;
		}
		return ((int)time / this.timeBinSize);
	}

	/**
	 * @param linkId
	 * @return Array containing the number of vehicles leaving the link <code>linkId</code> per time bin,
	 * 		starting with time bin 0 from 0 seconds to (timeBinSize-1)seconds.
	 */
	public int[] getVolumesForLink(final String linkId) {
		return this.links.get(linkId);
	}

	/**
	 * @return Set of Strings containing all link ids for which counting-values are available.
	 */
	public Set<String> getLinkIds() {
		return this.links.keySet();
	}

	public void reset(final int iteration) {
		this.links.clear();
	}
}
