/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionsPerGroupWarmEventHandler.java
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
import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;

/**
 * @author benjamin
 *
 */
public class EmissionsPerGroupWarmEventHandler implements WarmEmissionEventHandler {
	private static final Logger logger = Logger.getLogger(EmissionsPerGroupWarmEventHandler.class);
	
	Map<String, Map<WarmPollutant, Double>> group2Emissions = new HashMap<String, Map<WarmPollutant, Double>>();
	PersonFilter filter = new PersonFilter();
	
	@Override
	public void handleEvent(WarmEmissionEvent event) {
		Id personId = event.getVehicleId();
		Map<WarmPollutant, Double> warmEmissionsOfEvent = event.getWarmEmissions();

		String group = null;
		if(filter.isPersonFromMID(personId)) group = "MID";
		else if(filter.isPersonOutCommuter(personId)) group = "outCommuter";
		else if(filter.isPersonInnCommuter(personId)) group = "innCommuter";
		else if(filter.isPersonFreight(personId)) group = "freight";
		else logger.warn("person " + personId + " cannot be put in one of the defined groups!");
		
		if(group2Emissions.get(group) != null){
			Map<WarmPollutant, Double> warmEmissionsSoFar = group2Emissions.get(group);
			for(Entry<WarmPollutant, Double> entry : warmEmissionsOfEvent.entrySet()){
				WarmPollutant pollutant = entry.getKey();
				Double eventValue = entry.getValue();

				Double previousValue = warmEmissionsSoFar.get(pollutant);
				Double newValue = previousValue + eventValue;
				warmEmissionsSoFar.put(pollutant, newValue);
			}
			group2Emissions.put(group, warmEmissionsSoFar);
		} else {
			group2Emissions.put(group, warmEmissionsOfEvent);
		}
	}
	
	public Map<String, Map<String, Double>> getWarmEmissionsPerGroup() {
		Map<String, Map<String, Double>> group2warmEmissionsAsString = new HashMap<String, Map<String, Double>>();

		for (Entry<String, Map<WarmPollutant, Double>> entry1: this.group2Emissions.entrySet()){
			String group = entry1.getKey();
			Map<WarmPollutant, Double> pollutant2Values = entry1.getValue();
			Map<String, Double> pollutantString2Values = new HashMap<String, Double>();
			for (Entry<WarmPollutant, Double> entry2: pollutant2Values.entrySet()){
				String pollutant = entry2.getKey().toString();
				Double value = entry2.getValue();
				pollutantString2Values.put(pollutant, value);
			}
			group2warmEmissionsAsString.put(group, pollutantString2Values);
		}
		return group2warmEmissionsAsString;
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

}
