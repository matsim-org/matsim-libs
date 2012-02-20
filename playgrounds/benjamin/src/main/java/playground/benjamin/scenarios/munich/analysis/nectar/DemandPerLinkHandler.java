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
package playground.benjamin.scenarios.munich.analysis.nectar;


/**
 * @author benjamin after fhuelsmann
 *
 */

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;

public class DemandPerLinkHandler implements LinkLeaveEventHandler {
	private static final Logger logger = Logger.getLogger(DemandPerLinkHandler.class);

	Map<Double, Map<Id, Integer>> time2LinkIdAndDemand = new HashMap<Double, Map<Id, Integer>>();
	private final int noOfTimeBins;
	private final double timeBinSize;

	public DemandPerLinkHandler(double simulationEndTime, int noOfTimeBins) {
		this.noOfTimeBins = noOfTimeBins;
		this.timeBinSize = simulationEndTime / noOfTimeBins;
	}

	public void reset(final int iteration) {
		this.time2LinkIdAndDemand.clear();
		logger.info("Resetting travel demand aggregation to " + this.time2LinkIdAndDemand);
	}

	public void handleEvent(final LinkLeaveEvent event) {
		Id linkId = event.getLinkId();
		double linkLeaveTime = event.getTime();
		double endOfTimeInterval;
		
		for(int i = 0; i < noOfTimeBins; i++){
			if(linkLeaveTime > i * timeBinSize && linkLeaveTime <= (i + 1) * timeBinSize){
				endOfTimeInterval = (i + 1) * timeBinSize;
				Map<Id, Integer> linkId2Counts = new HashMap<Id, Integer>();

				if(time2LinkIdAndDemand.get(endOfTimeInterval) == null){
					linkId2Counts.put(linkId, 1);
				} else {
					linkId2Counts = time2LinkIdAndDemand.get(endOfTimeInterval);
					if(linkId2Counts.get(linkId) == null){
						linkId2Counts.put(linkId, 1);
					} else {
						int countsSoFar = linkId2Counts.get(linkId);
						int newValue = countsSoFar + 1;
						linkId2Counts.put(linkId, newValue);
					}
				}
				time2LinkIdAndDemand.put(endOfTimeInterval, linkId2Counts);
			}
		}
	}

	public Map<Double, Map<Id, Integer>> getDemandPerLinkAndTimeInterval() {
		return this.time2LinkIdAndDemand;
	}

}
