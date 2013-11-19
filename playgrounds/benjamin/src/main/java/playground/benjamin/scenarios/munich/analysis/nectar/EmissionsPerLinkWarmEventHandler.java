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
package playground.benjamin.scenarios.munich.analysis.nectar;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import playground.vsp.emissions.events.WarmEmissionEvent;
import playground.vsp.emissions.events.WarmEmissionEventHandler;
import playground.vsp.emissions.types.WarmPollutant;
import playground.vsp.emissions.utils.EmissionUtils;

/**
 * @author benjamin
 *
 */
public class EmissionsPerLinkWarmEventHandler implements WarmEmissionEventHandler{
	private static final Logger logger = Logger.getLogger(EmissionsPerLinkWarmEventHandler.class);

	Map<Double, Map<Id, Map<WarmPollutant, Double>>> time2warmEmissionsTotal = new HashMap<Double, Map<Id, Map<WarmPollutant, Double>>>();
	Map<Double, Map<Id, Double>> time2linkIdLeaveCount = new HashMap<Double, Map<Id,Double>>();
	
	final int noOfTimeBins;
	final double timeBinSize;
	EmissionUtils emissionUtils;

	public EmissionsPerLinkWarmEventHandler(double simulationEndTime, int noOfTimeBins){
		this.noOfTimeBins = noOfTimeBins;
		this.timeBinSize = simulationEndTime / noOfTimeBins;
		this.emissionUtils = new EmissionUtils();
	}

	@Override
	public void reset(int iteration) {
		this.time2warmEmissionsTotal.clear();
		logger.info("Resetting warm emission aggregation to " + this.time2warmEmissionsTotal);
		this.time2linkIdLeaveCount.clear();
		logger.info("Resetting linkLeave counter to " + this.time2linkIdLeaveCount);
	}

	
	@Override
	public void handleEvent(WarmEmissionEvent event) {
		Double time = event.getTime(); 
		if(time ==0.0) time = this.timeBinSize;
		Id linkId = event.getLinkId();
		Map<WarmPollutant, Double> warmEmissionsOfEvent = event.getWarmEmissions();
		double endOfTimeInterval = 0.0;
		
		if(warmEmissionsOfEvent==null){
			warmEmissionsOfEvent = new HashMap<WarmPollutant, Double>();
			for(WarmPollutant wp: WarmPollutant.values()){
				warmEmissionsOfEvent.put(wp, 0.0);
			}
		}else{
			for(WarmPollutant wp: WarmPollutant.values()){
				if(warmEmissionsOfEvent.get(wp)==null){
					warmEmissionsOfEvent.put(wp, 0.0);
				}
			}
		}
		
		endOfTimeInterval = Math.ceil(time/timeBinSize)*timeBinSize;
		if(endOfTimeInterval<=0.0)endOfTimeInterval=timeBinSize;
		

				Map<Id, Map<WarmPollutant, Double>> warmEmissionsTotal = new HashMap<Id, Map<WarmPollutant, Double>>();;
				Map<Id, Double> countTotal = new HashMap<Id, Double>();
				
				if(this.time2warmEmissionsTotal.get(endOfTimeInterval) != null){
					warmEmissionsTotal = this.time2warmEmissionsTotal.get(endOfTimeInterval);
					countTotal = this.time2linkIdLeaveCount.get(endOfTimeInterval);
					
					
					if(warmEmissionsTotal.get(linkId) != null){
						Map<WarmPollutant, Double> warmEmissionsSoFar = warmEmissionsTotal.get(linkId);
						
						for(WarmPollutant wp : WarmPollutant.values()){
							warmEmissionsSoFar.put(wp, warmEmissionsOfEvent.get(wp)+warmEmissionsSoFar.get(wp));
						}
						
						double countsSoFar = countTotal.get(linkId);
						double newValue = countsSoFar + 1.;
						countTotal.put(linkId, newValue);
					} else {
						warmEmissionsTotal.put(linkId, warmEmissionsOfEvent);
						countTotal.put(linkId, 1.);
					}
				} else {
					warmEmissionsTotal.put(linkId, warmEmissionsOfEvent);
					countTotal.put(linkId, 1.);
				}
				this.time2warmEmissionsTotal.put(endOfTimeInterval, warmEmissionsTotal);
				this.time2linkIdLeaveCount.put(endOfTimeInterval, countTotal);
		

	}

	public Map<Double, Map<Id, Double>> getTime2linkIdLeaveCount() {
		return this.time2linkIdLeaveCount;
	}

	public Map<Double, Map<Id, Map<WarmPollutant, Double>>> getWarmEmissionsPerLinkAndTimeInterval() {
		return time2warmEmissionsTotal;
	}
}