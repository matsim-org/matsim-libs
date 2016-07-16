/* *********************************************************************** *
 * project: org.matsim.*
 * CongestionPerLinkHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.andreas.aas.modules.spatialAveragingLinkDemand;


/**
 * @author benjamin after fhuelsmann
 *
 */

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Network;

public class WeightedDemandPerLinkHandler implements LinkLeaveEventHandler {
	private static final Logger logger = Logger.getLogger(WeightedDemandPerLinkHandler.class);

	Map<Double, Map<Id, Double>> time2LinkIdAndDemand = new HashMap<Double, Map<Id, Double>>();
	private final Network network;
	private final int noOfTimeBins;
	private final double timeBinSize;

	public WeightedDemandPerLinkHandler(Network network, double simulationEndTime, int noOfTimeBins) {
		this.network = network;
		this.noOfTimeBins = noOfTimeBins;
		this.timeBinSize = simulationEndTime / noOfTimeBins;
	}

	public void reset(final int iteration) {
		this.time2LinkIdAndDemand.clear();
		logger.info("Resetting travel demand aggregation to " + this.time2LinkIdAndDemand);
	}

	public void handleEvent(final LinkLeaveEvent event) {
		Id linkId = event.getLinkId();
		double linkLength = this.network.getLinks().get(linkId).getLength();
		double linkLeaveTime = event.getTime();
		double endOfTimeInterval;
		
		for(int i = 0; i < noOfTimeBins; i++){
			if(linkLeaveTime > i * timeBinSize && linkLeaveTime <= (i + 1) * timeBinSize){
				endOfTimeInterval = (i + 1) * timeBinSize;
				Map<Id, Double> linkId2WeightedCounts = new HashMap<Id, Double>();
				double weightedCount = 1. * linkLength;
				
				if(time2LinkIdAndDemand.get(endOfTimeInterval) == null){
					linkId2WeightedCounts.put(linkId, weightedCount);
				} else {
					linkId2WeightedCounts = time2LinkIdAndDemand.get(endOfTimeInterval);
					if(linkId2WeightedCounts.get(linkId) == null){
						linkId2WeightedCounts.put(linkId, weightedCount);
					} else {
						double weightedCountsSoFar = linkId2WeightedCounts.get(linkId);
						double newValue = weightedCountsSoFar + weightedCount;
						linkId2WeightedCounts.put(linkId, newValue);
					}
				}
				time2LinkIdAndDemand.put(endOfTimeInterval, linkId2WeightedCounts);
			}
		}
	}

	public Map<Double, Map<Id, Double>> getDemandPerLinkAndTimeInterval() {
		return this.time2LinkIdAndDemand;
	}

}
