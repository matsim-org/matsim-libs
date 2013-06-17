/* *********************************************************************** *
 * project: org.matsim.*
 * VolumesAnalyzer.java
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

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;

import playground.christoph.events.EventHandlerInstance;
import playground.christoph.events.MasterEventHandler;

/**
 * Counts the number of vehicles leaving a link, aggregated into time bins of a specified size.
 *
 * @author mrieser
 * @author cdobler
 */
public class VolumesAnalyzer implements MasterEventHandler {

	private final static Logger log = Logger.getLogger(VolumesAnalyzer.class);
	private final int timeBinSize;
	private final int maxTime;
	private final int maxSlotIndex;
	private final ConcurrentMap<Id, int[]> links;

	private final Set<VolumesAnalyzerInstance> instances = new LinkedHashSet<VolumesAnalyzerInstance>();
	
	public VolumesAnalyzer(final int timeBinSize, final int maxTime, final Network network) {
		this.timeBinSize = timeBinSize;
		this.maxTime = maxTime;
		
		this.maxSlotIndex = (this.maxTime/this.timeBinSize) + 1;
		this.links = new ConcurrentHashMap<Id, int[]>((int) (network.getLinks().size() * 1.1), 0.95f);
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
	public int[] getVolumesForLink(final Id linkId) {
		return this.links.get(linkId);
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
	public double[] getVolumesPerHourForLink(final Id linkId) {
		if (3600.0 % this.timeBinSize != 0) log.error("Volumes per hour and per link probably not correct!");
		
		double [] volumes = new double[24];
		for (int hour = 0; hour < 24; hour++) {
			volumes[hour] = 0.0;
		}
		if (this.getVolumesForLink(linkId) == null) return volumes;

		int slotsPerHour = (int)(3600.0 / this.timeBinSize);
		for (int hour = 0; hour < 24; hour++) {
			double time = hour * 3600.0;
			for (int i = 0; i < slotsPerHour; i++) {
				volumes[hour] += this.getVolumesForLink(linkId)[this.getTimeSlotIndex(time)];
				time += this.timeBinSize;
			}
		}
		return volumes;
	}

	/**
	 * @return Set of Strings containing all link ids for which counting-values are available.
	 */
	public Set<Id> getLinkIds() {
		return this.links.keySet();
	}

	@Override
	public void reset(final int iteration) {
		this.links.clear();
		this.instances.clear();
	}

	@Override
	public EventHandlerInstance createInstance() {
		VolumesAnalyzerInstance instance = new VolumesAnalyzerInstance(this.timeBinSize, this.maxTime, this.links);
		instances.add(instance);
		return instance;
	}
	
	@Override
	public void synchronize(double time) {
		for (VolumesAnalyzerInstance instance : instances) instance.synchronize(time);
	}
	
	@Override
	public void finishEventsHandling() {
		// Nothing to do here since all instances share a single ConcurrentMap.
	}
}
