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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.contrib.emissions.types.WarmPollutant;


/**
 * @author benjamin
 *
 */
public class EmissionsPerPersonWarmEventHandler implements WarmEmissionEventHandler {
	private static final Logger logger = Logger.getLogger(EmissionsPerPersonWarmEventHandler.class);

	Map<Id, Map<WarmPollutant, Double>> warmEmissionsTotal = new HashMap<Id, Map<WarmPollutant, Double>>();

	public EmissionsPerPersonWarmEventHandler() {
	}

	public void handleEvent(WarmEmissionEvent event) {
		Id vehicleId = event.getVehicleId();
		Map<WarmPollutant, Double> warmEmissionsOfEvent = event.getWarmEmissions();

		if(warmEmissionsTotal.get(vehicleId) != null){
			Map<WarmPollutant, Double> warmEmissionsSoFar = warmEmissionsTotal.get(vehicleId);
			for(Entry<WarmPollutant, Double> entry : warmEmissionsOfEvent.entrySet()){
				WarmPollutant pollutant = entry.getKey();
				Double eventValue = entry.getValue();

				Double previousValue = warmEmissionsSoFar.get(pollutant);
				Double newValue = previousValue + eventValue;
				warmEmissionsSoFar.put(pollutant, newValue);
			}
			warmEmissionsTotal.put(vehicleId, warmEmissionsSoFar);
		} else {
			warmEmissionsTotal.put(vehicleId, warmEmissionsOfEvent);
		}
	}

	public Map<Id, Map<WarmPollutant, Double>> getWarmEmissionsPerPerson() {
		return warmEmissionsTotal;
	}

	public void reset(int iteration) {
		warmEmissionsTotal.clear();
	}
}
