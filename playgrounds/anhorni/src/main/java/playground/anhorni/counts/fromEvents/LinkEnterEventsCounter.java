/* *********************************************************************** *
 * project: org.matsim.*
 * VolumesCounter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.anhorni.counts.fromEvents;

import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;

/**
 * A very simple algorithm just counting how many vehicles enter a link for a given time bin
 *
 * @author anhorni
 */
public class LinkEnterEventsCounter implements LinkEnterEventHandler {
	
	private final TreeMap<Integer, Links> timeBins = new TreeMap<Integer, Links>();
	private double binSizeInMinutes;
	private final static Logger log = Logger.getLogger(LinkEnterEventsCounter.class);
	
	public LinkEnterEventsCounter(double binSizeInMinutes) {
		this.binSizeInMinutes = binSizeInMinutes;
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		int index = (int)((event.getTime() / 60.0) / (this.binSizeInMinutes));
		if (this.timeBins.get(index) == null) this.timeBins.put(index, new Links());
		this.timeBins.get(index).incrementLinkEnterCount(event.getLinkId(), 1.0);
	}

	@Override
	public void reset(int iteration) {
		this.timeBins.clear();
	}
	
	public double getLinkEnterCount(Id id, double hour) {
		int index = (int)(hour * 60.0 / (this.binSizeInMinutes));
		if (this.timeBins.get(index) == null) return 0.0;
		return this.timeBins.get(index).getLinkEnterCount(id);
	}

}
