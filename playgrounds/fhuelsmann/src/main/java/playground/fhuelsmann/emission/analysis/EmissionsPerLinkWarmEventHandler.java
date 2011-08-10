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
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;

import playground.benjamin.events.emissions.WarmEmissionEvent;
import playground.benjamin.events.emissions.WarmEmissionEventHandler;
import playground.benjamin.events.emissions.WarmPollutant;

/**
 * @author benjamin, friederike
 *
 */

public class EmissionsPerLinkWarmEventHandler implements WarmEmissionEventHandler {

	Map<Id, Map<WarmPollutant, Double>> warmEmissionsTotal = new HashMap<Id, Map<WarmPollutant, Double>>();

	public EmissionsPerLinkWarmEventHandler() {
	}

	public void handleEvent(WarmEmissionEvent event) {
		Id linkId= event.getLinkId();
		Map<WarmPollutant, Double> warmEmissionsOfEvent = event.getWarmEmissions();

		if(!warmEmissionsTotal.containsKey(linkId)){
			warmEmissionsTotal.put(linkId, warmEmissionsOfEvent);
			}
			
		else{		
			Map<WarmPollutant, Double> warmEmissionsSoFar = warmEmissionsTotal.get(linkId);
			for(Entry<WarmPollutant, Double> entry : warmEmissionsOfEvent.entrySet()){
				WarmPollutant pollutant = entry.getKey();
				Double eventValue = entry.getValue();
				if(linkId.toString().equals("10038"))
					System.out.println("linkId "+linkId+ "pollutant "+ pollutant+ " eventValue "+eventValue);
				if(!warmEmissionsSoFar.containsKey(pollutant)){
					warmEmissionsSoFar.put(pollutant, eventValue);
					warmEmissionsTotal.put(linkId, warmEmissionsSoFar);
					if(linkId.toString().equals("10038"))
					System.out.println("linkId "+linkId+ " pollutant "+ pollutant+" eventValue "+eventValue);
				}
				else{
			
				Double previousValue = warmEmissionsSoFar.get(pollutant);
				Double newValue = previousValue + eventValue;
				warmEmissionsSoFar.put(pollutant, newValue);
				warmEmissionsTotal.put(linkId, warmEmissionsSoFar);
					if(linkId.toString().equals("10038"))
					System.out.println("linkId "+linkId+ "pollutant "+ pollutant+ " previousValue "+previousValue+" eventValue "+eventValue
							+" newValue "+newValue);
				}
			}
		}
	}


	public Map<Id, Map<String, Double>> getWarmEmissionsPerLink() {
		Map<Id, Map<String, Double>> linkId2warmEmissionsAsString = new HashMap<Id, Map<String, Double>>();

		for (Entry<Id, Map<WarmPollutant, Double>> entry1: this.warmEmissionsTotal.entrySet()){
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
		return linkId2warmEmissionsAsString;
	}

	public void reset(int iteration) {
		// TODO Auto-generated method stub
	}
}
