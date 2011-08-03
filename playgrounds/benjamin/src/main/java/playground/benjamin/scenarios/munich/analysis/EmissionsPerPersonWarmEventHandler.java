/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionsPerPersonEventHandler.java
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

import org.matsim.api.core.v01.Id;

import playground.benjamin.events.emissions.WarmEmissionEvent;
import playground.benjamin.events.emissions.WarmEmissionEventHandler;
import playground.benjamin.events.emissions.WarmPollutant;

/**
 * @author benjamin
 *
 */
public class EmissionsPerPersonWarmEventHandler implements WarmEmissionEventHandler {

	Map<Id, Map<WarmPollutant, Double>> warmEmissionsTotal = new HashMap<Id, Map<WarmPollutant, Double>>();

	public EmissionsPerPersonWarmEventHandler() {
	}

	public void handleEvent(WarmEmissionEvent event) {
		Id vehicleId = event.getVehicleId();
		Map<WarmPollutant, Double> warmEmissionsOfEvent = event.getWarmEmissions();

		if(!warmEmissionsTotal.containsKey(vehicleId)){
			warmEmissionsTotal.put(vehicleId, warmEmissionsOfEvent);
		}
		else{
			Map<WarmPollutant, Double> warmEmissionsSoFar = warmEmissionsTotal.get(vehicleId);
			for(Entry<WarmPollutant, Double> entry : warmEmissionsOfEvent.entrySet()){
				WarmPollutant pollutant = entry.getKey();
				Double eventValue = entry.getValue();
				
				if(!warmEmissionsSoFar.containsKey(pollutant)){
					warmEmissionsSoFar.put(pollutant, eventValue);
					warmEmissionsTotal.put(vehicleId, warmEmissionsSoFar);
				}
				else{
					Double previousValue = warmEmissionsSoFar.get(pollutant);
					Double newValue = previousValue + eventValue;
					warmEmissionsSoFar.put(pollutant, newValue);
					warmEmissionsTotal.put(vehicleId, warmEmissionsSoFar);
				}
			}
		}
	}

	public Map<Id, Map<String, Double>> getWarmEmissionsPerPerson() {
		Map<Id, Map<String, Double>> personId2warmEmissionsAsString = new HashMap<Id, Map<String, Double>>();

		for (Entry<Id, Map<WarmPollutant, Double>> entry1: this.warmEmissionsTotal.entrySet()){
			Id personId = entry1.getKey();
			Map<WarmPollutant, Double> pollutant2Values = entry1.getValue();
			Map<String, Double> pollutantString2Values = new HashMap<String, Double>();
			for (Entry<WarmPollutant, Double> entry2: pollutant2Values.entrySet()){
				String pollutant = entry2.getKey().toString();
				Double value = entry2.getValue();
				pollutantString2Values.put(pollutant, value);
			}
			personId2warmEmissionsAsString.put(personId, pollutantString2Values);
		}
		return personId2warmEmissionsAsString;
	}

	public void reset(int iteration) {
		// TODO Auto-generated method stub
	}
}
