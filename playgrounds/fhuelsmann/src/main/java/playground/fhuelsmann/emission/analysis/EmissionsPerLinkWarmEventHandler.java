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
package playground.fhuelsmann.emission.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import playground.benjamin.events.emissions.WarmEmissionEvent;
import playground.benjamin.events.emissions.WarmEmissionEventHandler;
import playground.benjamin.events.emissions.WarmPollutant;


/**
 * @author friederike & benjamin
 *
 */

public class EmissionsPerLinkWarmEventHandler implements WarmEmissionEventHandler {
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
				Map<Id, Map<WarmPollutant, Double>> warmEmissionsTotal = new HashMap<Id, Map<WarmPollutant, Double>>();;
				
				if(time2warmEmissionsTotal.get(endOfTimeInterval) != null){
					warmEmissionsTotal = time2warmEmissionsTotal.get(endOfTimeInterval);

					if(warmEmissionsTotal.get(linkId) != null){
						Map<WarmPollutant, Double> warmEmissionsSoFar = warmEmissionsTotal.get(linkId);
						for(Entry<WarmPollutant, Double> entry : warmEmissionsOfEvent.entrySet()){
							WarmPollutant pollutant = entry.getKey();
							Double eventValue = entry.getValue();

							Double previousValue = warmEmissionsSoFar.get(pollutant);
							Double newValue = previousValue + eventValue;
							warmEmissionsSoFar.put(pollutant, newValue);
						}
						warmEmissionsTotal.put(linkId, warmEmissionsSoFar);
					} else {
						warmEmissionsTotal.put(linkId, warmEmissionsOfEvent);
					}
				} else {
					warmEmissionsTotal.put(linkId, warmEmissionsOfEvent);
				}
				time2warmEmissionsTotal.put(endOfTimeInterval, warmEmissionsTotal);
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
				Id linkId = entry1.getKey();
				Map<WarmPollutant, Double> pollutant2Values = entry1.getValue();
				Map<String, Double> pollutantString2Values = new HashMap<String, Double>();

				for (Entry<WarmPollutant, Double> entry2: pollutant2Values.entrySet()){
					String pollutant = entry2.getKey().toString();
					Double value = entry2.getValue();
					pollutantString2Values.put(pollutant, value);
				}
				linkId2warmEmissionsAsString.put(linkId, pollutantString2Values);
			}
			time2warmEmissionsTotal.put(endOfTimeInterval, linkId2warmEmissionsAsString);
		}
		return time2warmEmissionsTotal;
	}

	@Override
	public void reset(int iteration) {
		this.time2warmEmissionsTotal.clear();
		logger.info("Resetting warm emission aggregation to " + this.time2warmEmissionsTotal);
	}

	public Map<Id, Map<Integer, Map<WarmPollutant, Double>>> getWarmEmissionsTotal() {
		// TODO Auto-generated method stub
		return null;
	}


/*	Map<Id, Map<Integer, Map<WarmPollutant, Double>>> warmEmissionsTotal = new HashMap<Id, Map<Integer, Map<WarmPollutant, Double>>>();

	public EmissionsPerLinkWarmEventHandler() {
	}

	public void handleEvent(WarmEmissionEvent event) {
		Id linkId= event.getLinkId();
		Map<WarmPollutant, Double> warmEmissionsOfEvent = event.getWarmEmissions();
		double time = event.getTime();
		double timeClass = time / 3600;
		int timeClassrounded = (int) timeClass+1;

		if(!warmEmissionsTotal.containsKey(linkId)){
			
			Map<Integer, Map<WarmPollutant, Double>> timeClass2Pollutant2Emissions = new TreeMap<Integer, Map<WarmPollutant, Double>>();
			timeClass2Pollutant2Emissions.put(timeClassrounded,warmEmissionsOfEvent);
			warmEmissionsTotal.put(linkId, timeClass2Pollutant2Emissions);
			if(linkId.toString().equals("576273431-592536888"))
				System.out.println("linkId "+linkId+ " timeClass2Pollutant2Emissions "+timeClass2Pollutant2Emissions);
			}

		
		else{	
			if(warmEmissionsTotal.get(linkId).containsKey(timeClassrounded)){
				
				Map<Integer, Map<WarmPollutant, Double>> newValue = new TreeMap<Integer, Map<WarmPollutant, Double>>();
				
				Map<WarmPollutant, Double> warmEmissionsSoFar = newValue.get(linkId);
				for(Entry<WarmPollutant, Double> entry : warmEmissionsOfEvent.entrySet()){
					WarmPollutant pollutant = entry.getKey();
					Double eventValue = entry.getValue();
					Double previousValue = warmEmissionsOfEvent.get(pollutant);
					Double sumValue = previousValue + eventValue;
					warmEmissionsSoFar.put(pollutant,sumValue);
					newValue.put(timeClassrounded,newValue.get(warmEmissionsSoFar));
					warmEmissionsTotal.put(linkId, newValue);
					if(linkId.toString().equals("576273431-592536888"))
						System.out.println("linkId "+linkId+ " newValue "+newValue);
				}
			}
					
			else{
					Map<Integer, Map<WarmPollutant, Double>> newValue = new TreeMap<Integer, Map<WarmPollutant, Double>>();
					
					Map<WarmPollutant, Double> warmEmissionsSoFar = newValue.get(linkId);
					for(Entry<WarmPollutant, Double> entry : warmEmissionsOfEvent.entrySet()){
						WarmPollutant pollutant = entry.getKey();
						Double eventValue = entry.getValue();
						warmEmissionsSoFar.put(pollutant,eventValue);
						newValue.put(timeClassrounded,warmEmissionsSoFar);
						warmEmissionsTotal.put(linkId, newValue);
						if(linkId.toString().equals("576273431-592536888"))
							System.out.println("linkId "+linkId+ " newValue "+newValue);
					}		
				}
		}
	}


	public Map<Id, Map<Integer, Map<WarmPollutant, Double>>> getWarmEmissionsTotal() {
		return warmEmissionsTotal;
	}

	public void setWarmEmissionsTotal(
			Map<Id, Map<Integer, Map<WarmPollutant, Double>>> warmEmissionsTotal) {
		this.warmEmissionsTotal = warmEmissionsTotal;
	}

	

	public void reset(int iteration) {
		// TODO Auto-generated method stub
	}*/
}
