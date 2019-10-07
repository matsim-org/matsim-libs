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
package playground.vsp.analysis.modules.emissionsAnalyzer;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.contrib.emissions.types.WarmPollutant;


/**
 * @author benjamin
 *
 */
public class EmissionsPerPersonWarmEventHandler implements WarmEmissionEventHandler {
	Map<Id<Person>, Map<String, Double>> warmEmissionsTotal = new HashMap<>();

	public EmissionsPerPersonWarmEventHandler() {
	}

	@Override
	public void handleEvent(WarmEmissionEvent event) {
		// TODO person id statt vehicleid??? woher?
		Id<Person> personId = Id.create(event.getVehicleId(), Person.class);
		Map<String, Double> warmEmissionsOfEvent = event.getWarmEmissions();

		if(warmEmissionsTotal.get(personId) != null){
			Map<String, Double> warmEmissionsSoFar = warmEmissionsTotal.get(personId);
			for(Entry<String, Double> entry : warmEmissionsOfEvent.entrySet()){
				String pollutant = entry.getKey();
				Double eventValue = entry.getValue();

				Double previousValue = warmEmissionsSoFar.get(pollutant);
				Double newValue = previousValue + eventValue;
				warmEmissionsSoFar.put(pollutant, newValue);
			}
			warmEmissionsTotal.put(personId, warmEmissionsSoFar);
		} else {
			warmEmissionsTotal.put(personId, warmEmissionsOfEvent);
		}
	}

	public Map<Id<Person>, Map<String, Double>> getWarmEmissionsPerPerson() {
		return warmEmissionsTotal;
	}

	@Override
	public void reset(int iteration) {
		warmEmissionsTotal.clear();
	}
}
