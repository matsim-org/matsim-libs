/* *********************************************************************** *
 * project: org.matsim.*
 * LinkLeaveCountsPerTimeInterval.java
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
package playground.benjamin.scenarios.munich.analysis.modular;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;

/**
 * @author benjamin
 *
 */
public class LinkLeaveCountsPerTimeInterval {
	private static final Logger logger = Logger.getLogger(LinkLeaveCountsPerTimeInterval.class);

	Map<Id, ArrayList<LinkLeaveEvent>> linkId2LinkLeaveEvent;
	Map<Double, Map<Id, Integer>> time2LinkIdAndCount = new HashMap<Double, Map<Id, Integer>>();

	private final int noOfTimeBins;
	private final double timeBinSize;

	public LinkLeaveCountsPerTimeInterval(double simulationEndTime,
										  int noOfTimeBins, Map<Id,
										  ArrayList<LinkLeaveEvent>> linkId2LinkLeaveEvent){
		this.noOfTimeBins = noOfTimeBins;
		this.timeBinSize = simulationEndTime / noOfTimeBins;
		this.linkId2LinkLeaveEvent = linkId2LinkLeaveEvent;
	}


	public void calculateTime2LinkIdAndCount(){
		for(Id linkId : linkId2LinkLeaveEvent.keySet()){
			ArrayList<LinkLeaveEvent> eventsList = linkId2LinkLeaveEvent.get(linkId);
			for(LinkLeaveEvent lle : eventsList){
				double linkLeaveTime = lle.getTime();
				double endOfTimeInterval;

				for(int i = 0; i < noOfTimeBins; i++){
					if(linkLeaveTime > i * timeBinSize && linkLeaveTime <= (i + 1) * timeBinSize){
						endOfTimeInterval = (i + 1) * timeBinSize;
						Map<Id, Integer> linkId2Counts;

						if(time2LinkIdAndCount.get(endOfTimeInterval) == null){
							linkId2Counts = new HashMap<Id, Integer>();
							linkId2Counts.put(linkId, 1);
						} else {
							linkId2Counts = time2LinkIdAndCount.get(endOfTimeInterval);
							if(linkId2Counts.get(linkId) == null){
								linkId2Counts.put(linkId, 1);
							} else {
								int countsSoFar = linkId2Counts.get(linkId);
								int newValue = countsSoFar + 1;
								linkId2Counts.put(linkId, newValue);
							}
						}
						time2LinkIdAndCount.put(endOfTimeInterval, linkId2Counts);
						continue;
					}
				}
			}
		}
	}

	public Map<Double, Map<Id, Integer>> getTime2LinkIdAndCount() {
		return this.time2LinkIdAndCount;
	}
}