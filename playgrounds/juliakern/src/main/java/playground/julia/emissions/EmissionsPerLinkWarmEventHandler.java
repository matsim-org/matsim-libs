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
package playground.julia.emissions;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import playground.vsp.emissions.events.WarmEmissionEvent;
import playground.vsp.emissions.events.WarmEmissionEventHandler;
import playground.vsp.emissions.types.WarmPollutant;
import playground.vsp.emissions.utils.EmissionUtils;

/**
 * @author benjamin, julia
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
		//TODO vermutlich schnell mit try und catch:
		// in den meisten faellen wird das event alle eintraege haben und schon 
		Double time = event.getTime();
		Id linkId = event.getLinkId();
		Map<WarmPollutant, Double> warmEmissionsOfEvent = event.getWarmEmissions();
		// do not use modulo for doubles
		int numberOfInterval = (int)Math.ceil(time/timeBinSize);
		if(numberOfInterval==0) numberOfInterval=1; //only happens if time = 0.0
		if(numberOfInterval>noOfTimeBins) numberOfInterval=noOfTimeBins; // this should not happen but might due to rounding errors when time = simulationEndTime
		double endOfTimeInterval = numberOfInterval * timeBinSize;

		Map<Id, Map<WarmPollutant, Double>> warmEmissionsTotal = new HashMap<Id, Map<WarmPollutant, Double>>();
		Map<Id, Double> countTotal = new HashMap<Id, Double>();

		if (endOfTimeInterval < this.noOfTimeBins * this.timeBinSize+1) {
			
			// make sure all fields are initilized / set values to zero
			// get previous emissions of this time interval
			if(this.time2warmEmissionsTotal != null && this.time2warmEmissionsTotal.containsKey(endOfTimeInterval)){
				warmEmissionsTotal.putAll(this.time2warmEmissionsTotal.get(endOfTimeInterval));
			}else{ // no emissions at this time so far - initialize as zero
				Map<WarmPollutant, Double> map = new HashMap<WarmPollutant, Double>();
				for(WarmPollutant wp: WarmPollutant.values()){
					map.put(wp, 0.0);
				}
				warmEmissionsTotal.put(linkId, map);
			}
			
			// get previous counts or set them to zero 
			if(this.time2linkIdLeaveCount != null && this.time2linkIdLeaveCount.containsKey(endOfTimeInterval)){
				countTotal.putAll(this.time2linkIdLeaveCount.get(endOfTimeInterval));	
			}else{ // no counts so far - initialize as zero
				countTotal.put(linkId, 0.0);
			}
			
			// this event
			if(warmEmissionsOfEvent == null){
				warmEmissionsOfEvent = new HashMap<WarmPollutant, Double>();
				for(WarmPollutant wp : WarmPollutant.values()){
					warmEmissionsOfEvent.put(wp, 0.0);
				}
			}
			// check whether all pollutants exist
			for(WarmPollutant wp: WarmPollutant.values()){
				if(warmEmissionsOfEvent.containsKey(wp)==false){
					warmEmissionsOfEvent.put(wp, 0.0);
				}
			}
			
			// check wheter an old entry for this link exists - if not create on with zero as values
			if(warmEmissionsTotal.containsKey(linkId)==false){
				Map<WarmPollutant, Double> map = new HashMap<WarmPollutant, Double>();
				for(WarmPollutant wp : WarmPollutant.values()){
					map.put(wp, 0.0);
				}
				warmEmissionsTotal.put(linkId, map);
			}
			
			Map<WarmPollutant, Double> warmEmissionsSoFar = warmEmissionsTotal.get(linkId);
			// make sure an entry for each pollutant exists
			for(WarmPollutant wp : WarmPollutant.values()){ //funktioniert, falls der wp gar nicht enthalten ist, aber auch, wenn der wert 'null' ist
				// schnellere abfrage aber laengerer code? via contains(null) und keySet.size()
				if(warmEmissionsSoFar.get(wp)==null){
					warmEmissionsSoFar.put(wp, 0.0);
				}
			}
						
			// sum up emission values
			for(WarmPollutant wp : WarmPollutant.values()){
				warmEmissionsSoFar.put(wp, warmEmissionsSoFar.get(wp)+warmEmissionsOfEvent.get(wp)); //this is ok without check... values set to zero above if they didnt exist before
			}
			warmEmissionsTotal.put(linkId, warmEmissionsSoFar);
			
			// make sure an entry for the counts exists
			if (countTotal.get(linkId)==null){
				countTotal.put(linkId, 0.0);
			}
			countTotal.put(linkId, countTotal.get(linkId)+1);
		
			this.time2warmEmissionsTotal.put(endOfTimeInterval, warmEmissionsTotal);
			this.time2linkIdLeaveCount.put(endOfTimeInterval, countTotal);
		}
		// do nothing if time of event is later than the simulation end
	}

	public Map<Double, Map<Id, Double>> getTime2linkIdLeaveCount() {
		return this.time2linkIdLeaveCount;
	}

	public Map<Double, Map<Id, Map<WarmPollutant, Double>>> getWarmEmissionsPerLinkAndTimeInterval() {
		return time2warmEmissionsTotal;
	}
}