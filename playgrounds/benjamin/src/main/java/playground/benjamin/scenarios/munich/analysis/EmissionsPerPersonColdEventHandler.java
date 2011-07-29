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
package playground.benjamin.scenarios.munich.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;

import playground.benjamin.events.ColdEmissionEvent;
import playground.benjamin.events.ColdEmissionEventHandler;

/**
 * @author benjamin
 *
 */
public class EmissionsPerPersonColdEventHandler implements ColdEmissionEventHandler {

	Map<Id, Map<String, Double>> coldEmissionsTotal = new HashMap<Id, Map<String, Double>>();

	public void handleEvent(ColdEmissionEvent event) {
		Id vehicleId = event.getVehicleId();
		Map<String, Double> coldEmissionsOfEvent = event.getColdEmissions();

		if(!coldEmissionsTotal.containsKey(vehicleId)){
			coldEmissionsTotal.put(vehicleId, coldEmissionsOfEvent);
		}
		else{
			Map<String, Double> coldEmissionsSoFar = coldEmissionsTotal.get(vehicleId);
			for(Entry<String, Double> entry : coldEmissionsOfEvent.entrySet()){
				String pollutant = entry.getKey();
				Double eventValue = entry.getValue();

				if(!coldEmissionsSoFar.containsKey(pollutant)){
					coldEmissionsSoFar.put(pollutant, eventValue);
					coldEmissionsTotal.put(vehicleId, coldEmissionsSoFar);
				}
				else{
					Double previousValue = coldEmissionsSoFar.get(pollutant);
					Double newValue = previousValue + eventValue;
					coldEmissionsSoFar.put(pollutant, newValue);
					coldEmissionsTotal.put(vehicleId, coldEmissionsSoFar);
				}
			}
		}
	}

	public Map<Id, Map<String, Double>> getColdEmissionsPerPerson() {
		return coldEmissionsTotal;
	}

	public void reset(int iteration) {
		// TODO Auto-generated method stub
	}
}
