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
package playground.benjamin.scenarios.munich.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import playground.benjamin.events.emissions.ColdEmissionEvent;
import playground.benjamin.events.emissions.ColdEmissionEventHandler;
import playground.benjamin.events.emissions.ColdPollutant;

/**
 * @author benjamin
 *
 */
public class EmissionsPerLinkColdEventHandler implements ColdEmissionEventHandler{
	private static final Logger logger = Logger.getLogger(EmissionsPerLinkColdEventHandler.class);
	
	Map<Double, Map<Id, Map<ColdPollutant, Double>>> time2coldEmissionsTotal = new HashMap<Double, Map<Id, Map<ColdPollutant, Double>>>();

	private final int noOfTimeBins;
	private final double timeBinSize;

	public EmissionsPerLinkColdEventHandler(double simulationEndTime, int noOfTimeBins) {
		this.noOfTimeBins = noOfTimeBins;
		this.timeBinSize = simulationEndTime / noOfTimeBins;
	}

	@Override
	public void handleEvent(ColdEmissionEvent event) {
		Double time = event.getTime();
		Id linkId = event.getLinkId();
		Map<ColdPollutant, Double> coldEmissionsOfEvent = event.getColdEmissions();
		double endOfTimeInterval = 0.0;

		for(int i = 0; i < noOfTimeBins; i++){
			if(time > i * timeBinSize && time <= (i + 1) * timeBinSize){
				endOfTimeInterval = (i + 1) * timeBinSize;
				Map<Id, Map<ColdPollutant, Double>> coldEmissionsTotal = new HashMap<Id, Map<ColdPollutant, Double>>();
				
				if(time2coldEmissionsTotal.get(endOfTimeInterval) != null){
					coldEmissionsTotal = time2coldEmissionsTotal.get(endOfTimeInterval);

					if(coldEmissionsTotal.get(linkId) != null){
						Map<ColdPollutant, Double> coldEmissionsSoFar = coldEmissionsTotal.get(linkId);
						for(Entry<ColdPollutant, Double> entry : coldEmissionsOfEvent.entrySet()){
							ColdPollutant pollutant = entry.getKey();
							Double eventValue = entry.getValue();

							if(coldEmissionsSoFar.get(pollutant) != null){
								Double previousValue = coldEmissionsSoFar.get(pollutant);
								Double newValue = previousValue + eventValue;
								coldEmissionsSoFar.put(pollutant, newValue);
								coldEmissionsTotal.put(linkId, coldEmissionsSoFar);
							} else {
								coldEmissionsSoFar.put(pollutant, eventValue);
								coldEmissionsTotal.put(linkId, coldEmissionsSoFar);
							}
						}
					} else {
						coldEmissionsTotal.put(linkId, coldEmissionsOfEvent);
					}
				} else {
					coldEmissionsTotal.put(linkId, coldEmissionsOfEvent);
				}
				time2coldEmissionsTotal.put(endOfTimeInterval, coldEmissionsTotal);
			}
		}
	}

	public Map<Double, Map<Id, Map<String, Double>>> getColdEmissionsPerLinkAndTimeInterval() {
		Map<Double, Map<Id, Map<String, Double>>> time2coldEmissionsTotal = new HashMap<Double, Map<Id, Map<String, Double>>>();

		for(Entry<Double, Map<Id, Map<ColdPollutant, Double>>> entry0 : this.time2coldEmissionsTotal.entrySet()){
			Double endOfTimeInterval = entry0.getKey();
			Map<Id, Map<ColdPollutant, Double>> linkId2coldEmissions = entry0.getValue();
			Map<Id, Map<String, Double>> linkId2coldEmissionsAsString = new HashMap<Id, Map<String, Double>>();
			
			for (Entry<Id, Map<ColdPollutant, Double>> entry1: linkId2coldEmissions.entrySet()){
				Id linkId = entry1.getKey();
				Map<ColdPollutant, Double> pollutant2Values = entry1.getValue();
				Map<String, Double> pollutantString2Values = new HashMap<String, Double>();
				
				for (Entry<ColdPollutant, Double> entry2: pollutant2Values.entrySet()){
					String pollutant = entry2.getKey().toString();
					Double value = entry2.getValue();
					pollutantString2Values.put(pollutant, value);
				}
				linkId2coldEmissionsAsString.put(linkId, pollutantString2Values);
			}
			time2coldEmissionsTotal.put(endOfTimeInterval, linkId2coldEmissionsAsString);
		}
		return time2coldEmissionsTotal;
	}

	@Override
	public void reset(int iteration) {
		this.time2coldEmissionsTotal.clear();
		logger.info("Resetting cold emission handler to " + this.time2coldEmissionsTotal);
	}

}
