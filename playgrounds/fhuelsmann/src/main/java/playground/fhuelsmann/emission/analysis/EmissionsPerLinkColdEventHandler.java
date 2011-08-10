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
package playground.fhuelsmann.emission.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;

import playground.benjamin.events.emissions.ColdEmissionEvent;
import playground.benjamin.events.emissions.ColdEmissionEventHandler;
import playground.benjamin.events.emissions.ColdPollutant;

/**
 * @author benjamin, friederike
 *
 */

public class EmissionsPerLinkColdEventHandler implements ColdEmissionEventHandler{

	Map<Id, Map<ColdPollutant, Double>> coldEmissionsTotal = new HashMap<Id, Map<ColdPollutant, Double>>();

	public void handleEvent(ColdEmissionEvent event) {
		Id linkId = event.getLinkId();
		Map<ColdPollutant, Double> coldEmissionsOfEvent = event.getColdEmissions();

		if(!coldEmissionsTotal.containsKey(linkId)){
			coldEmissionsTotal.put(linkId, coldEmissionsOfEvent);
		}
		else{
			Map<ColdPollutant, Double> coldEmissionsSoFar = coldEmissionsTotal.get(linkId);
			for(Entry<ColdPollutant, Double> entry : coldEmissionsOfEvent.entrySet()){
				ColdPollutant pollutant = entry.getKey();
				Double eventValue = entry.getValue();

				if(!coldEmissionsSoFar.containsKey(pollutant)){
					coldEmissionsSoFar.put(pollutant, eventValue);
					coldEmissionsTotal.put(linkId, coldEmissionsSoFar);
				}
				else{
					Double previousValue = coldEmissionsSoFar.get(pollutant);
					Double newValue = previousValue + eventValue;
					coldEmissionsSoFar.put(pollutant, newValue);
					coldEmissionsTotal.put(linkId, coldEmissionsSoFar);
				}
			}
		}
	}

	public Map<Id, Map<String, Double>> getColdEmissionsPerLink() {
		Map<Id, Map<String, Double>> linkId2coldEmissionsAsString = new HashMap<Id, Map<String, Double>>();

		for (Entry<Id, Map<ColdPollutant, Double>> entry1: this.coldEmissionsTotal.entrySet()){
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
		return linkId2coldEmissionsAsString;
	}

	public void reset(int iteration) {
		// TODO Auto-generated method stub
	}
}
