/* *********************************************************************** *
 /* *********************************************************************** *
 * project: org.matsim.*
 * FhEmissions.java
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
 *                                                                         
 * *********************************************************************** */
package playground.fhuelsmann.emission;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;

import playground.benjamin.events.emissions.ColdEmissionEventImpl;
import playground.benjamin.events.emissions.ColdPollutant;
import playground.fhuelsmann.emission.objects.HbefaColdEmissionFactor;
import playground.fhuelsmann.emission.objects.HbefaColdEmissionTableCreator;

public class ColdEmissionAnalysisModule {
	private static final Logger logger = Logger.getLogger(ColdEmissionAnalysisModule.class);

	public void calculateColdEmissions(Id linkId, Id personId,
			double startEngineTime, double parkingDuration,
			double accumulatedDistance, HbefaColdEmissionTableCreator hbefaColdTable,
			EventsManager emissionEventsManager) {

		// TODO: CO2 not directly available for cold emissions; thus it could be
		// calculated through fc as follows:
		// get("FC")*0.865 - get("CO")*0.429 - get("HC")*0.866)/0.273;

		//two categories for distance: 0: 0-1km; 1: 1-2km; the data doesn't provide further distance categories
		//for the average cold start emission factors; the largest part of the cold start emissions is 
		//emitted during the first few kilometers
		int distance_km = -1;
		if ((accumulatedDistance / 1000) < 1.0) {
			distance_km = 0;
		} else {
			distance_km = 1;
		}

		//for parking duration there are 13 categories in hours: 0-1; 1-2,......,11-12; 12- max
		int parkingDuration_h = (int) (parkingDuration / 3600);
		if (parkingDuration_h >= 12){
			parkingDuration_h = 12;
		}
		ColdPollutant coldPollutant = null;
		Double generatedEmissions = null;
		Map<ColdPollutant, Double> coldEmissions = new HashMap<ColdPollutant, Double>();

		for (Entry<ColdPollutant, Map<Integer, Map<Integer, HbefaColdEmissionFactor>>> entry :	hbefaColdTable.getHbefaColdTable().entrySet()){
			Map<Integer, Map<Integer, HbefaColdEmissionFactor>> value = entry.getValue();
			double coldEf  = value.get(distance_km).get(parkingDuration_h).getColdEF();

			coldPollutant = entry.getKey();
			generatedEmissions = coldEf ;
			coldEmissions.put(coldPollutant, generatedEmissions);
		}
		Event coldEmissionEvent = new ColdEmissionEventImpl(startEngineTime, linkId, personId, coldEmissions);
		emissionEventsManager.processEvent(coldEmissionEvent);
	}
}