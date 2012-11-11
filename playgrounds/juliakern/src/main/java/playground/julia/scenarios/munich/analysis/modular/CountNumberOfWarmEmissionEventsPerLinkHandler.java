/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionsPerLinkWarmEventHandler.java
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
package playground.julia.scenarios.munich.analysis.modular;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import playground.vsp.emissions.events.WarmEmissionEvent;
import playground.vsp.emissions.events.WarmEmissionEventHandler;
import playground.vsp.emissions.utils.EmissionUtils;

/**
 * @author benjamin, julia
 *
 */
public class CountNumberOfWarmEmissionEventsPerLinkHandler implements WarmEmissionEventHandler{
	private static final Logger logger = Logger.getLogger(CountNumberOfWarmEmissionEventsPerLinkHandler.class);

	
	Map<Double, Map<Id, Integer>> time2linkIdLeaveCount = new HashMap<Double, Map<Id,Integer>>();
	
	final int noOfTimeBins;
	final double timeBinSize;
	EmissionUtils emissionUtils;

	public CountNumberOfWarmEmissionEventsPerLinkHandler(double simulationEndTime, int noOfTimeBins){
		this.noOfTimeBins = noOfTimeBins;
		this.timeBinSize = simulationEndTime / noOfTimeBins;
		this.emissionUtils = new EmissionUtils();
	}

	@Override
	public void reset(int iteration) {
		this.time2linkIdLeaveCount.clear();
		logger.info("Resetting linkLeve counter to " + this.time2linkIdLeaveCount);
	}

	/*
	 * count the number of warm emission events on each link an each time interval
	 * (non-Javadoc)
	 * @see playground.vsp.emissions.events.WarmEmissionEventHandler#handleEvent(playground.vsp.emissions.events.WarmEmissionEvent)
	 */
	@Override
	public void handleEvent(WarmEmissionEvent event) {
		Double time = event.getTime();
		Id linkId = event.getLinkId();
		double endOfTimeInterval = (int)Math.ceil(time/this.timeBinSize) * this.timeBinSize;

					try {
						Map<Id, Integer> countTotal = new HashMap<Id, Integer>();
						countTotal = this.time2linkIdLeaveCount.get(endOfTimeInterval);
						if (countTotal.get(linkId) != null) {
							countTotal.put(linkId, countTotal.get(linkId) + 1);
						} else {
							countTotal.put(linkId, 1);
						}
						this.time2linkIdLeaveCount.put(endOfTimeInterval, countTotal);
						
					} catch (Exception e) { 
						//outside time interval
						if (!(0.0<=endOfTimeInterval && endOfTimeInterval<=this.noOfTimeBins*this.timeBinSize)){ 
							logger.warn("Event on link "+linkId+" is at "+time+ "which is outside the time intervall of [0.0,"+noOfTimeBins*timeBinSize+
							"] and will be ignored.");
						} else {
							//actually the above 'outside time interval' exception is a null pointer -> differentiate if other exceptions will be handled
							logger.warn("This is an uncaught exception that should be handled.");
						}
						
					}
							
	}

	public Map<Double, Map<Id, Integer>> getTime2linkIdLeaveCount() {
		return this.time2linkIdLeaveCount;
	}
	
	public Integer getValueOfTime2LinkIdLeaveCount(Double time, Id linkId){
		if(this.time2linkIdLeaveCount.get(time).get(linkId)!=null){
			return this.time2linkIdLeaveCount.get(time).get(linkId);
		}
		return 0; //this does also return zero if no value is set! no difference to a zero-value (which should not appear)
	}
}