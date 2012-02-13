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
package playground.benjamin.scenarios.munich.analysis.cupum;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import playground.benjamin.emissions.events.WarmEmissionEvent;
import playground.benjamin.emissions.events.WarmEmissionEventHandler;
import playground.benjamin.emissions.types.WarmPollutant;
import playground.benjamin.scenarios.munich.analysis.filter.PersonFilter;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;

/**
 * @author benjamin
 *
 */
public class EmissionsPerGroupWarmEventHandler implements WarmEmissionEventHandler {
	private static final Logger logger = Logger.getLogger(EmissionsPerGroupWarmEventHandler.class);

	Map<UserGroup, Map<WarmPollutant, Double>> group2Emissions = new HashMap<UserGroup, Map<WarmPollutant, Double>>();
	PersonFilter filter = new PersonFilter();

	@Override
	public void handleEvent(WarmEmissionEvent event) {
		Id personId = event.getVehicleId();
		Map<WarmPollutant, Double> warmEmissionsOfEvent = event.getWarmEmissions();

		for(UserGroup userGroup : UserGroup.values()){
			if(!filter.isPersonIdFromUserGroup(personId, userGroup)){
				// person is not from this user group
			} else {
				if(group2Emissions.get(userGroup) != null){
					Map<WarmPollutant, Double> warmEmissionsSoFar = group2Emissions.get(userGroup);
					for(Entry<WarmPollutant, Double> entry : warmEmissionsOfEvent.entrySet()){
						WarmPollutant pollutant = entry.getKey();
						Double eventValue = entry.getValue();

						Double previousValue = warmEmissionsSoFar.get(pollutant);
						Double newValue = previousValue + eventValue;
						warmEmissionsSoFar.put(pollutant, newValue);
					}
					group2Emissions.put(userGroup, warmEmissionsSoFar);
				} else {
					group2Emissions.put(userGroup, warmEmissionsOfEvent);
				}
			}
		}
	}

	public Map<UserGroup, Map<String, Double>> getWarmEmissionsPerGroup() {
		Map<UserGroup, Map<String, Double>> group2warmEmissionsAsString = new HashMap<UserGroup, Map<String, Double>>();

		for (Entry<UserGroup, Map<WarmPollutant, Double>> entry1: this.group2Emissions.entrySet()){
			UserGroup group = entry1.getKey();
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
		this.group2Emissions.clear();
	}
}
