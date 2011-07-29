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

import playground.benjamin.events.WarmEmissionEvent;
import playground.benjamin.events.WarmEmissionEventHandler;

/**
 * @author benjamin
 *
 */
public class EmissionsPerPersonHotEventHandler implements WarmEmissionEventHandler {

	Map<Id, Map<String, Double>> hotEmissionsTotal = new HashMap<Id, Map<String, Double>>();

	public EmissionsPerPersonHotEventHandler() {
	}

	public void handleEvent(WarmEmissionEvent event) {
		Id vehicleId = event.getVehicleId();
		Map<String, Double> hotEmissionsOfEvent = event.getHotEmissions();

		if(!hotEmissionsTotal.containsKey(vehicleId)){
			hotEmissionsTotal.put(vehicleId, hotEmissionsOfEvent);
		}
		else{
			Map<String, Double> hotEmissionsSoFar = hotEmissionsTotal.get(vehicleId);
			for(Entry<String, Double> entry : hotEmissionsOfEvent.entrySet()){
				String pollutant = entry.getKey();
				Double eventValue = entry.getValue();
				
				if(!hotEmissionsSoFar.containsKey(pollutant)){
					hotEmissionsSoFar.put(pollutant, eventValue);
					hotEmissionsTotal.put(vehicleId, hotEmissionsSoFar);
				}
				else{
					Double previousValue = hotEmissionsSoFar.get(pollutant);
					Double newValue = previousValue + eventValue;
					hotEmissionsSoFar.put(pollutant, newValue);
					hotEmissionsTotal.put(vehicleId, hotEmissionsSoFar);
				}
			}
		}
	}

	public Map<Id, Map<String, Double>> getHotEmissionsPerPerson() {
		return hotEmissionsTotal;
	}

	public void reset(int iteration) {
		// TODO Auto-generated method stub
	}
}
