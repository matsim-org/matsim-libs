/* *********************************************************************** *
 * project: org.matsim.*
 * VolumesAnalyzerInstance.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.events.implementations;

import java.util.concurrent.ConcurrentMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;

import playground.christoph.events.EventHandlerInstance;

/**
 * Counts the number of vehicles leaving a link, aggregated into time bins of a specified size.
 *
 * @author mrieser
 * @author cdobler
 */
public class VolumesAnalyzerInstance implements EventHandlerInstance, LinkLeaveEventHandler {

	private final int timeBinSize;
	private final int maxTime;
	private final int maxSlotIndex;
	private final ConcurrentMap<Id, int[]> links;

	public VolumesAnalyzerInstance(final int timeBinSize, final int maxTime, final ConcurrentMap<Id, int[]> links) {
		this.timeBinSize = timeBinSize;
		this.maxTime = maxTime;
		this.maxSlotIndex = (this.maxTime/this.timeBinSize) + 1;
		this.links = links;
	}

	@Override
	public void handleEvent(final LinkLeaveEvent event) {
		int[] volumes = this.links.get(event.getLinkId());
		if (volumes == null) {
			volumes = new int[this.maxSlotIndex + 1]; // initialized to 0 by default, according to JVM specs
			this.links.put(event.getLinkId(), volumes);
		}
		int timeslot = getTimeSlotIndex(event.getTime());
		volumes[timeslot]++;
	}

	private int getTimeSlotIndex(final double time) {
		if (time > this.maxTime) {
			return this.maxSlotIndex;
		}
		return ((int)time / this.timeBinSize);
	}

	@Override
	public void reset(final int iteration) {
		// nothing to do here
	}
	
	@Override
	public void synchronize(double time) {
		// nothing to do here
	}
}
