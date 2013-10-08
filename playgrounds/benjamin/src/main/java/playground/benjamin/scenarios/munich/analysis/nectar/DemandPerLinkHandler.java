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
 * @author benjamin
 *
 */

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;

public class DemandPerLinkHandler implements LinkLeaveEventHandler {
	private static final Logger logger = Logger.getLogger(DemandPerLinkHandler.class);

	Map<Double, Map<Id, Double>> time2LinkIdAndDemand = new HashMap<Double, Map<Id, Double>>();
	private final int noOfTimeBins;
	private final double timeBinSize;

	public DemandPerLinkHandler(double simulationEndTime, int noOfTimeBins) {
		this.noOfTimeBins = noOfTimeBins;
		this.timeBinSize = simulationEndTime / noOfTimeBins;
	}

	@Override
	public void reset(final int iteration) {
		this.time2LinkIdAndDemand.clear();
		logger.info("Resetting travel demand aggregation per link to " + this.time2LinkIdAndDemand);
	}

	@Override
	public void handleEvent(final LinkLeaveEvent event) {
		Id linkId = event.getLinkId();
		double linkLeaveTime = event.getTime();
		double endOfTimeInterval;
		
		for(int i = 0; i < noOfTimeBins; i++){
			if(linkLeaveTime > i * timeBinSize && linkLeaveTime <= (i + 1) * timeBinSize){
				endOfTimeInterval = (i + 1) * timeBinSize;
				Map<Id, Double> linkId2Counts = new HashMap<Id, Double>();
				double count = 1.;
				
				if(time2LinkIdAndDemand.get(endOfTimeInterval) == null){
					linkId2Counts.put(linkId, count);
				} else {
					linkId2Counts = time2LinkIdAndDemand.get(endOfTimeInterval);
					if(linkId2Counts.get(linkId) == null){
						linkId2Counts.put(linkId, count);
					} else {
						double countsSoFar = linkId2Counts.get(linkId);
						double countsAfter = countsSoFar + count;
						linkId2Counts.put(linkId, countsAfter);
					}
				}
				time2LinkIdAndDemand.put(endOfTimeInterval, linkId2Counts);
			}
		}
	}

	public Map<Double, Map<Id, Double>> getDemandPerLinkAndTimeInterval() {
		return this.time2LinkIdAndDemand;
	}

}
