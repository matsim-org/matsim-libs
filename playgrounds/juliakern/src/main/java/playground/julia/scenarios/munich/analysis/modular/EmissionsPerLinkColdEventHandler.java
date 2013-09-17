/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionsPerLinkColdEventHandler.java
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

import playground.vsp.emissions.events.ColdEmissionEvent;
import playground.vsp.emissions.events.ColdEmissionEventHandler;
import playground.vsp.emissions.types.ColdPollutant;
import playground.vsp.emissions.utils.EmissionUtils;

/**
 * @author benjamin, julia
 *
 */
public class EmissionsPerLinkColdEventHandler implements ColdEmissionEventHandler{
	private static final Logger logger = Logger.getLogger(EmissionsPerLinkColdEventHandler.class);

	Map<Double, Map<Id, Map<ColdPollutant, Double>>> time2coldEmissionsTotal = new HashMap<Double, Map<Id, Map<ColdPollutant, Double>>>();

	final int noOfTimeBins;
	final double timeBinSize;
	EmissionUtils emissionUtils;

	public EmissionsPerLinkColdEventHandler(double simulationEndTime, int noOfTimeBins) {
		this.noOfTimeBins = noOfTimeBins;
		this.timeBinSize = simulationEndTime / noOfTimeBins;
		this.emissionUtils = new EmissionUtils();
	}

	@Override
	public void reset(int iteration) {
		this.time2coldEmissionsTotal.clear();
		logger.info("Resetting cold emission aggregation to " + this.time2coldEmissionsTotal);
	}

	@Override
	public void handleEvent(ColdEmissionEvent event) {
		Double time = event.getTime();
		Id linkId = event.getLinkId();
		Map<ColdPollutant, Double> coldEmissionsOfEvent = event.getColdEmissions(); //TODO catch nullpointer? im moment bei warmen auch nciht
		double 	endOfTimeInterval = (int)Math.ceil(time/this.timeBinSize) * this.timeBinSize;

		try {
			if(this.time2coldEmissionsTotal.get(endOfTimeInterval).get(linkId)!=null){
				for(Entry<ColdPollutant,Double> entry : coldEmissionsOfEvent.entrySet()){
					Double oldValue = time2coldEmissionsTotal.get(endOfTimeInterval).get(linkId).get(entry.getKey());
					this.time2coldEmissionsTotal.get(endOfTimeInterval).get(linkId).put(entry.getKey(), entry.getValue()+oldValue);
				}
			} else {
				if(this.time2coldEmissionsTotal.get(endOfTimeInterval)!=null){
					this.time2coldEmissionsTotal.get(endOfTimeInterval).put(linkId, coldEmissionsOfEvent);
				} else {
					Map<Id, Map<ColdPollutant, Double>> coldEmissionsTotal = new HashMap<Id, Map<ColdPollutant, Double>>();
					coldEmissionsTotal.put(linkId, coldEmissionsOfEvent);
					this.time2coldEmissionsTotal.put(endOfTimeInterval, coldEmissionsTotal);
				}
			}
			
		} catch (Exception e) {
			//outside time interval
			if (!(0.0<=endOfTimeInterval && endOfTimeInterval<=this.noOfTimeBins*this.timeBinSize)){ 
				logger.warn("Event on link "+linkId+" is at "+time+ "which is outside the time intervall of [0.0,"+noOfTimeBins*timeBinSize+
				"] and will be ignored.");
			} else {
				//actually the above 'outside time interval' exception is a null pointer -> differentiate when other exceptions will be handled
				logger.warn("This is an uncaught exception that should be handled.");
			}
		}
	}

	public Map<Double, Map<Id, Map<ColdPollutant, Double>>> getColdEmissionsPerLinkAndTimeInterval() {
		return time2coldEmissionsTotal;
	}
	
	public Map<ColdPollutant, Double> getListEmissionValues(Double time, Id linkId){
		if(time2coldEmissionsTotal.get(time).get(linkId)!=null)return time2coldEmissionsTotal.get(time).get(linkId);
		return null; 
	}
	
	public Double getEmissionValue(Double time, Id linkId, ColdPollutant wp){
		if(time2coldEmissionsTotal.get(time).get(linkId).get(wp)!=null)return time2coldEmissionsTotal.get(time).get(linkId).get(wp);
		return .0;
	}

}
