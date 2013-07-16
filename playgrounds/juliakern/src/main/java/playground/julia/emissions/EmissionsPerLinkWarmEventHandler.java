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
		System.out.println("number of time bins " + noOfTimeBins);
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
		Id linkId = event.getLinkId();
		Map<WarmPollutant, Double> warmEmissionsOfEvent = event.getWarmEmissions();
		// do not cannot use modulo for doubles
		int numberOfInterval = (int)Math.ceil(time/timeBinSize);
		if(numberOfInterval==0) numberOfInterval=1; //only happens if time = 0.0
		if(numberOfInterval>noOfTimeBins) numberOfInterval=noOfTimeBins; // this should not happen but might due to rounding errors when time = simulationEndTime
		double endOfTimeInterval = numberOfInterval * timeBinSize;

		Map<Id, Map<WarmPollutant, Double>> warmEmissionsTotal = new HashMap<Id, Map<WarmPollutant, Double>>();
		Map<Id, Double> countTotal = new HashMap<Id, Double>();

		if (endOfTimeInterval < this.noOfTimeBins * this.timeBinSize+1) {
			
			// make sure all fields are initilized / set values to zero
			// TODO not needed.... check local maps instead?
			if (this.time2warmEmissionsTotal.containsKey(endOfTimeInterval)==false){
				Map<Id, Map<WarmPollutant, Double>> map = new HashMap<Id, Map<WarmPollutant,Double>>();
				Map<WarmPollutant, Double> warmpollutant2zero = new HashMap<WarmPollutant, Double>();
				for(WarmPollutant wp: WarmPollutant.values()){
					warmpollutant2zero.put(wp, 0.0);
				}
				map.put(linkId, warmpollutant2zero);
				this.time2warmEmissionsTotal.put(endOfTimeInterval, map);
			}
			if (this.time2linkIdLeaveCount.containsKey(endOfTimeInterval)==false){
				Map<Id, Double> map = new HashMap<Id, Double>();
				map.put(linkId, 0.0);
				this.time2linkIdLeaveCount.put(endOfTimeInterval, map);
			}
			
			// time2warmEmissionsTotal contains at least that entry
			warmEmissionsTotal.putAll(this.time2warmEmissionsTotal.get(endOfTimeInterval));
			// time2linkIdLeaveCount contains at least that entry
			countTotal.putAll(this.time2linkIdLeaveCount.get(endOfTimeInterval));	
			
			if (this.time2warmEmissionsTotal.get(endOfTimeInterval) == null) {
				Map<Id, Map<WarmPollutant, Double>> map = new HashMap<Id, Map<WarmPollutant, Double>>();
				this.time2warmEmissionsTotal.put(endOfTimeInterval, map );
			}
			if (this.time2linkIdLeaveCount.get(endOfTimeInterval) == null) {
				Map<Id, Double> map = new HashMap<Id, Double>();
				this.time2linkIdLeaveCount.put(endOfTimeInterval, map);
			}
			
			if(warmEmissionsOfEvent == null){
				warmEmissionsOfEvent = new HashMap<WarmPollutant, Double>();
				for(WarmPollutant wp : WarmPollutant.values()){
					warmEmissionsOfEvent.put(wp, 0.0);
				}
			}
			
			if(warmEmissionsTotal.containsKey(linkId)==false){
				Map<WarmPollutant, Double> map = new HashMap<WarmPollutant, Double>();
				for(WarmPollutant wp : WarmPollutant.values()){
					map.put(wp, 0.0);
				}
				warmEmissionsTotal.put(linkId, map);
			}
			
			Map<WarmPollutant, Double> warmEmissionsSoFar = warmEmissionsTotal.get(linkId);
			Map<WarmPollutant, Double> newWarmEmissionsSoFar = new HashMap<WarmPollutant, Double>();
			
			// make sure all maps are initialized / set values to zero
			newWarmEmissionsSoFar.putAll(warmEmissionsSoFar); // siehe ***
			for(WarmPollutant wp : WarmPollutant.values()){ //funktioniert, falls der wp gar nicht enthalten ist, aber auch, wenn der wert 'null' ist
				// schnellere abfrage aber laengerer code? via contains(null) und keySet.size()
				if(newWarmEmissionsSoFar.get(wp)==null){
					newWarmEmissionsSoFar.put(wp, 0.0);
				}
			}
						


			if (countTotal.get(linkId)==null){
				countTotal.put(linkId, 0.0);
			}

			for(Entry<WarmPollutant, Double> entry : warmEmissionsOfEvent.entrySet()){
					WarmPollutant pollutant = entry.getKey();
					Double eventValue;
					if (entry.getValue()!=null) {
						eventValue = entry.getValue();
					}else{
						eventValue = 0.0;
					}
					Double previousValue = warmEmissionsSoFar.get(pollutant); // *** warmEmissionsSoFar koennte Eintraege haben, die 
									// das aktuelle event nicht hat. die wuerden dann verloren gehen
					Double newValue = previousValue + eventValue;
					newWarmEmissionsSoFar.put(pollutant, newValue);
				}
			
			warmEmissionsTotal.put(linkId, newWarmEmissionsSoFar);
			double countsSoFar = countTotal.get(linkId);
			double newValue = countsSoFar + 1.;
			countTotal.put(linkId, newValue);
		
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