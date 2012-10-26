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
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import playground.benjamin.scenarios.munich.analysis.EmissionUtils;
import playground.vsp.emissions.events.WarmEmissionEvent;
import playground.vsp.emissions.events.WarmEmissionEventHandler;
import playground.vsp.emissions.types.WarmPollutant;

/**
 * @author benjamin
 * 
 */
public class EmissionsPerLinkWarmEventHandlerV2 implements
		WarmEmissionEventHandler {
	private static final Logger logger = Logger.getLogger(EmissionsPerLinkWarmEventHandlerV2.class);

	Map<Double, Map<Id, Map<WarmPollutant, Double>>> time2warmEmissionsTotal = new HashMap<Double, Map<Id, Map<WarmPollutant, Double>>>();
	
	final int noOfTimeBins;
	final double timeBinSize;
	EmissionUtils emissionUtils;

	public EmissionsPerLinkWarmEventHandlerV2(double simulationEndTime,
			int noOfTimeBins) {
		this.noOfTimeBins = noOfTimeBins;
		this.timeBinSize = simulationEndTime / noOfTimeBins;
		this.emissionUtils = new EmissionUtils();
	}

	@Override
	public void reset(int iteration) {
		this.time2warmEmissionsTotal.clear();
		logger.info("Resetting warm emission aggregation to "+ this.time2warmEmissionsTotal);
		
	}

	@Override
	public void handleEvent(WarmEmissionEvent event) {
		Double time = event.getTime();
		Id linkId = event.getLinkId();
		Map<WarmPollutant, Double> warmEmissionsOfEvent = event.getWarmEmissions(); //TODO macht das einen unterschied, ob ich hier eine kopie erstelle? sollte nicht nullpointer?
		double endOfTimeInterval = (int) Math.ceil(time / this.timeBinSize)* this.timeBinSize;

		try {
			Map<Id, Map<WarmPollutant, Double>> warmEmissionsTotal = new HashMap<Id, Map<WarmPollutant, Double>>();

			//probably most frequent case
			if (this.time2warmEmissionsTotal.get(endOfTimeInterval).get(linkId) != null) { 
				warmEmissionsTotal = this.time2warmEmissionsTotal.get(endOfTimeInterval);
				Map<WarmPollutant, Double> warmEmissionsSoFar = warmEmissionsTotal.get(linkId); //TODO different to using a copy? shouldnt but recheck!
				
				for (Entry<WarmPollutant, Double> entry : warmEmissionsOfEvent.entrySet()) {
					Double oldValue = warmEmissionsSoFar.get(entry.getKey());
					this.time2warmEmissionsTotal.get(endOfTimeInterval).get(linkId).put(entry.getKey(), entry.getValue()+oldValue);
					}
			}
			else {
				//map for recent time slot exists but inner map for specific link does not
				if(this.time2warmEmissionsTotal.get(endOfTimeInterval)!= null){
					this.time2warmEmissionsTotal.get(time).put(linkId, warmEmissionsOfEvent);
				}
				else{
					warmEmissionsTotal.put(linkId, warmEmissionsOfEvent);
					this.time2warmEmissionsTotal.put(endOfTimeInterval, warmEmissionsTotal);
				}
			}
			
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
	//TODO compare here or at test unit?

	public Map<Double, Map<Id, Map<WarmPollutant, Double>>> getWarmEmissionsPerLinkAndTimeInterval() {
		return time2warmEmissionsTotal;
	}
	
	public Map<WarmPollutant, Double> getListEmissionValues(Double time, Id linkId){
		if(time2warmEmissionsTotal.get(time).get(linkId)!=null)return time2warmEmissionsTotal.get(time).get(linkId);
		return null; //TODO return sth else? empty list?
	}
	
	public Double getWarmEmissionValue(Double time, Id linkId, WarmPollutant wp){
		if(time2warmEmissionsTotal.get(time).get(linkId).get(wp)!=null)return time2warmEmissionsTotal.get(time).get(linkId).get(wp);
		return .0;
	}
}