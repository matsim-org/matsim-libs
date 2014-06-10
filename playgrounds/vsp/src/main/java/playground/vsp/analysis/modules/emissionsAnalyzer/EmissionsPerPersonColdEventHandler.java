/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionsPerPersonColdEventHandler.java
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
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.types.ColdPollutant;


/**
 * @author benjamin
 *
 */
public class EmissionsPerPersonColdEventHandler implements ColdEmissionEventHandler {
	private static final Logger logger = Logger.getLogger(EmissionsPerPersonColdEventHandler.class);

	Map<Id, Map<ColdPollutant, Double>> coldEmissionsTotal = new HashMap<Id, Map<ColdPollutant, Double>>();

	public void handleEvent(ColdEmissionEvent event) {
		Id vehicleId = event.getVehicleId();
		Map<ColdPollutant, Double> coldEmissionsOfEvent = event.getColdEmissions();

		if(coldEmissionsTotal.get(vehicleId) != null){
			Map<ColdPollutant, Double> coldEmissionsSoFar = coldEmissionsTotal.get(vehicleId);
			for(Entry<ColdPollutant, Double> entry : coldEmissionsOfEvent.entrySet()){
				ColdPollutant pollutant = entry.getKey();
				Double eventValue = entry.getValue();

				Double previousValue = coldEmissionsSoFar.get(pollutant);
				Double newValue = previousValue + eventValue;
				coldEmissionsSoFar.put(pollutant, newValue);
			}
			coldEmissionsTotal.put(vehicleId, coldEmissionsSoFar);
		} else {
			coldEmissionsTotal.put(vehicleId, coldEmissionsOfEvent);
		}
	}

	public Map<Id, Map<ColdPollutant, Double>> getColdEmissionsPerPerson() {
		return coldEmissionsTotal;
	}

	public void reset(int iteration) {
		coldEmissionsTotal.clear();
	}
}
