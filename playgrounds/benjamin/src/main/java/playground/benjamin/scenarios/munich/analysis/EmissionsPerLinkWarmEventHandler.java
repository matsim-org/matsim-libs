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
package playground.benjamin.scenarios.munich.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import playground.benjamin.events.emissions.WarmEmissionEvent;
import playground.benjamin.events.emissions.WarmEmissionEventHandler;
import playground.benjamin.events.emissions.WarmPollutant;

/**
 * @author benjamin
 *
 */
public class EmissionsPerLinkWarmEventHandler implements WarmEmissionEventHandler{
	private static final Logger logger = Logger.getLogger(EmissionsPerLinkWarmEventHandler.class);

	Map<Double, Map<Id, Map<WarmPollutant, Double>>> time2warmEmissionsTotal = new HashMap<Double, Map<Id, Map<WarmPollutant, Double>>>();

	private final int noOfTimeBins;
	private final double timeBinSize;

	public EmissionsPerLinkWarmEventHandler(double simulationEndTime, int noOfTimeBins){
		this.noOfTimeBins = noOfTimeBins;
		this.timeBinSize = simulationEndTime / noOfTimeBins;
	}

	@Override
	public void handleEvent(WarmEmissionEvent event) {
		Double time = event.getTime();
		Id linkId = event.getLinkId();
		Map<WarmPollutant, Double> warmEmissionsOfEvent = event.getWarmEmissions();
		double endOfTimeInterval = 0.0;

		for(int i = 0; i < noOfTimeBins; i++){
			if(time > i * timeBinSize && time <= (i + 1) * timeBinSize){
				endOfTimeInterval = (i + 1) * timeBinSize;

				if(time2warmEmissionsTotal.get(endOfTimeInterval) != null){
					Map<Id, Map<WarmPollutant, Double>> warmEmissionsTotal = time2warmEmissionsTotal.get(endOfTimeInterval);

					if(warmEmissionsTotal.get(linkId) != null){
						Map<WarmPollutant, Double> warmEmissionsSoFar = warmEmissionsTotal.get(linkId);
						for(Entry<WarmPollutant, Double> entry : warmEmissionsOfEvent.entrySet()){
							WarmPollutant pollutant = entry.getKey();
							Double eventValue = entry.getValue();

							if(warmEmissionsSoFar.get(pollutant) != null){
								Double previousValue = warmEmissionsSoFar.get(pollutant);
								Double newValue = previousValue + eventValue;
								warmEmissionsSoFar.put(pollutant, newValue);
								warmEmissionsTotal.put(linkId, warmEmissionsSoFar);
							} else {
								warmEmissionsSoFar.put(pollutant, eventValue);
								warmEmissionsTotal.put(linkId, warmEmissionsSoFar);

							}
						}
					} else {
						warmEmissionsTotal.put(linkId, warmEmissionsOfEvent);
					}
					time2warmEmissionsTotal.put(endOfTimeInterval, warmEmissionsTotal);
				} else {
					Map<Id, Map<WarmPollutant, Double>> warmEmissionsTotal = new HashMap<Id, Map<WarmPollutant, Double>>();
					warmEmissionsTotal.put(linkId, warmEmissionsOfEvent);
					time2warmEmissionsTotal.put(endOfTimeInterval, warmEmissionsTotal);
				}
			}
		}
	}

	public Map<Double, Map<Id, Map<String, Double>>> getWarmEmissionsPerLinkAndTimeInterval() {
		Map<Double, Map<Id, Map<String, Double>>> time2warmEmissionsTotal = new HashMap<Double, Map<Id, Map<String, Double>>>();

		for(Entry<Double, Map<Id, Map<WarmPollutant, Double>>> entry0 : this.time2warmEmissionsTotal.entrySet()){
			Double endOfTimeInterval = entry0.getKey();
			Map<Id, Map<WarmPollutant, Double>> linkId2warmEmissions = entry0.getValue();
			Map<Id, Map<String, Double>> linkId2warmEmissionsAsString = new HashMap<Id, Map<String, Double>>();
			
			for (Entry<Id, Map<WarmPollutant, Double>> entry1: linkId2warmEmissions.entrySet()){
				Id personId = entry1.getKey();
				Map<WarmPollutant, Double> pollutant2Values = entry1.getValue();
				Map<String, Double> pollutantString2Values = new HashMap<String, Double>();
				
				for (Entry<WarmPollutant, Double> entry2: pollutant2Values.entrySet()){
					String pollutant = entry2.getKey().toString();
					Double value = entry2.getValue();
					pollutantString2Values.put(pollutant, value);
				}
				linkId2warmEmissionsAsString.put(personId, pollutantString2Values);
			}
			time2warmEmissionsTotal.put(endOfTimeInterval, linkId2warmEmissionsAsString);
		}
		return time2warmEmissionsTotal;
	}

	@Override
	public void reset(int iteration) {
		this.time2warmEmissionsTotal.clear();
		logger.info("Resetting warm emission handler to " + this.time2warmEmissionsTotal);
	}
}
