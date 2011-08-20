/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionsPerGroupColdEventHandler.java
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
import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;

/**
 * @author benjamin
 *
 */
public class EmissionsPerGroupColdEventHandler implements ColdEmissionEventHandler {
	private static final Logger logger = Logger.getLogger(EmissionsPerGroupColdEventHandler.class);
	
	Map<String, Map<ColdPollutant, Double>> group2Emissions = new HashMap<String, Map<ColdPollutant, Double>>();
	PersonFilter filter = new PersonFilter();
	
	@Override
	public void handleEvent(ColdEmissionEvent event) {
		Id personId = event.getVehicleId();
		Map<ColdPollutant, Double> coldEmissionsOfEvent = event.getColdEmissions();

		String group = null;
		if(filter.isPersonFromMID(personId)) group = "MID";
		else if(filter.isPersonOutCommuter(personId)) group = "outCommuter";
		else if(filter.isPersonInnCommuter(personId)) group = "innCommuter";
		else if(filter.isPersonFreight(personId)) group = "freight";
		else logger.warn("person " + personId + " cannot be put in one of the defined groups!");
		
		if(group2Emissions.get(group) != null){
			Map<ColdPollutant, Double> coldEmissionsSoFar = group2Emissions.get(group);
			for(Entry<ColdPollutant, Double> entry : coldEmissionsOfEvent.entrySet()){
				ColdPollutant pollutant = entry.getKey();
				Double eventValue = entry.getValue();

				Double previousValue = coldEmissionsSoFar.get(pollutant);
				Double newValue = previousValue + eventValue;
				coldEmissionsSoFar.put(pollutant, newValue);
			}
			group2Emissions.put(group, coldEmissionsSoFar);
		} else {
			group2Emissions.put(group, coldEmissionsOfEvent);
		}
	}
	
	public Map<String, Map<String, Double>> getColdEmissionsPerGroup() {
		Map<String, Map<String, Double>> group2coldEmissionsAsString = new HashMap<String, Map<String, Double>>();

		for (Entry<String, Map<ColdPollutant, Double>> entry1: this.group2Emissions.entrySet()){
			String group = entry1.getKey();
			Map<ColdPollutant, Double> pollutant2Values = entry1.getValue();
			Map<String, Double> pollutantString2Values = new HashMap<String, Double>();
			for (Entry<ColdPollutant, Double> entry2: pollutant2Values.entrySet()){
				String pollutant = entry2.getKey().toString();
				Double value = entry2.getValue();
				pollutantString2Values.put(pollutant, value);
			}
			group2coldEmissionsAsString.put(group, pollutantString2Values);
		}
		return group2coldEmissionsAsString;
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

}
