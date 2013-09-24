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

package playground.dgrether.analysis.simsimanalyser;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;

/**
 * Counts the number of vehicles leaving and entering a link, aggregated into time bins of a specified size.
 *
 * @author tthunig
 */
public class TtVolumesAnalyzer implements LinkLeaveEventHandler, LinkEnterEventHandler {

	private final static Logger log = Logger.getLogger(TtVolumesAnalyzer.class);
	private final int timeBinSize;
	private final int maxTime;
	private final int maxSlotIndex;
	private final Map<Id, int[]> linkLeaveCounts;
	private final Map<Id, int[]> linkEnterCounts;
	

	public TtVolumesAnalyzer(final int timeBinSize, final int maxTime, final Network network) {
		this.timeBinSize = timeBinSize;
		this.maxTime = maxTime;
		this.maxSlotIndex = (this.maxTime/this.timeBinSize) + 1;
		this.linkLeaveCounts = new HashMap<Id, int[]>((int) (network.getLinks().size() * 1.1), 0.95f);
		this.linkEnterCounts = new HashMap<Id, int[]>((int) (network.getLinks().size() * 1.1), 0.95f);
		
		for (Link link : network.getLinks().values()){
			int[] initializeLinkLeaveCounts = new int[this.maxSlotIndex + 1];
			this.linkLeaveCounts.put(link.getId(), initializeLinkLeaveCounts);
			int[] initializeLinkEnterCounts = new int[this.maxSlotIndex + 1];
			this.linkEnterCounts.put(link.getId(), initializeLinkEnterCounts);
		}
		
	}
	
	@Override
	public void handleEvent(final LinkLeaveEvent event) {
		int[] volumes = this.linkLeaveCounts.get(event.getLinkId());
		if (volumes == null) {
			volumes = new int[this.maxSlotIndex + 1]; // initialized to 0 by default, according to JVM specs
			this.linkLeaveCounts.put(event.getLinkId(), volumes);
		}
		int timeslot = getTimeSlotIndex(event.getTime());
		volumes[timeslot]++;
	}

	@Override
	public void handleEvent(final LinkEnterEvent event) {
		int[] volumes = this.linkEnterCounts.get(event.getLinkId());
		if (volumes == null) {
			volumes = new int[this.maxSlotIndex + 1]; // initialized to 0 by default, according to JVM specs
			this.linkEnterCounts.put(event.getLinkId(), volumes);
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

	/**
	 * @param linkId
	 * @return Array containing the number of vehicles leaving the link <code>linkId</code> per time bin,
	 * 		starting with time bin 0 from 0 seconds to (timeBinSize-1)seconds.
	 */
	public int[] getLinkLeaveVolumesForLink(final Id linkId) {
		return this.linkLeaveCounts.get(linkId);
	}
	
	/**
	 * @param linkId
	 * @return Array containing the number of vehicles entering the link <code>linkId</code> per time bin,
	 * 		starting with time bin 0 from 0 seconds to (timeBinSize-1)seconds.
	 */
	public int[] getLinkEnterVolumesForLink(final Id linkId) {
		return this.linkEnterCounts.get(linkId);
	}
	
	/*
	 * This procedure is only working if (hour % timeBinSize == 0)
	 * 
	 * Example: 15 minutes bins
	 *  ___________________
	 * |  0 | 1  | 2  | 3  |
	 * |____|____|____|____|
	 * 0   900 1800  2700 3600
		___________________
	 * | 	  hour 0	   |
	 * |___________________|
	 * 0   				  3600
	 * 
	 * hour 0 = bins 0,1,2,3
	 * hour 1 = bins 4,5,6,7
	 * ...
	 * 
	 * getTimeSlotIndex = (int)time / this.timeBinSize => jumps at 3600.0!
	 * Thus, starting time = (hour = 0) * 3600.0
	 */
	public double[] getLinkLeaveVolumesPerHourForLink(final Id linkId) {
		return getVolumesPerHourForLink(linkId, true);
	}
	
	public double[] getLinkEnterVolumesPerHourForLink(final Id linkId) {
		return getVolumesPerHourForLink(linkId, false);
	}
	
	private double[] getVolumesPerHourForLink(final Id linkId, boolean linkLeave){
		if (3600.0 % this.timeBinSize != 0) log.error("Volumes per hour and per link probably not correct!");
		
		double [] volumes = new double[24];
		for (int hour = 0; hour < 24; hour++) {
			volumes[hour] = 0.0;
		}
		
		int[] volumesForLink;
		if (linkLeave) volumesForLink = this.getLinkLeaveVolumesForLink(linkId);
		else volumesForLink = this.getLinkEnterVolumesForLink(linkId);
		
		if (volumesForLink == null) return volumes;

		int slotsPerHour = (int)(3600.0 / this.timeBinSize);
		for (int hour = 0; hour < 24; hour++) {
			double time = hour * 3600.0;
			for (int i = 0; i < slotsPerHour; i++) {
				volumes[hour] += volumesForLink[this.getTimeSlotIndex(time)];
				time += this.timeBinSize;
			}
		}
		return volumes;
	}
	
//	/**
//	 * @return Set of Strings containing all link ids for which counting-values for link leave events are available.
//	 */
//	public Set<Id> getLinkIds() {
//		return this.linkLeaveCounts.keySet();
//	}

	@Override
	public void reset(final int iteration) {
		this.linkLeaveCounts.clear();
		this.linkEnterCounts.clear();
	}
}
