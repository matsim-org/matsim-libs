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

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;

import playground.benjamin.events.ColdEmissionEventImpl;
import playground.fhuelsmann.emission.objects.ColdPollutant;
import playground.fhuelsmann.emission.objects.HbefaColdEmissionTable;
import playground.fhuelsmann.emission.objects.HbefaColdObject;

public class ColdEmissionAnalysisModule {

	public void calculateColdEmissions(Id linkId, Id personId,
			double startEngineTime, double parkingDuration,
			double accumulatedDistance, HbefaColdEmissionTable hbefaColdTable,
			EventsManager emissionEventsManager) {

		// TODO: CO2 not directly available for cold emissions; thus it could be
		// calculated through fc as follows:
		// get("FC")*0.865 - get("CO")*0.429 - get("HC")*0.866)/0.273;

		// TODO: Why is distance classified as follows?!?
		int distance_km = -1;
		if ((accumulatedDistance / 1000) < 1.0) {
			distance_km = 0;
		} else {
			distance_km = 1;
		}

		int parkingDuration_h = (int) (parkingDuration / 3600);
		if (parkingDuration_h > 12)
			parkingDuration_h = 12;

		// TODO: What is this?
		int nightTime = 12;
		int initDis = 1;

		ColdPollutant coldPollutant = null;
		Double generatedEmissions = null;
		Map<ColdPollutant, Double> coldEmissions = new HashMap<ColdPollutant, Double>();
		
		for (Entry<ColdPollutant, Map<Integer, Map<Integer, HbefaColdObject>>> entry :	hbefaColdTable.getHbefaColdTable().entrySet()){
			Map<Integer, Map<Integer, HbefaColdObject>> value = entry.getValue();
			double coldEfOtherAct = value.get(distance_km).get(parkingDuration_h).getColdEF();
			double coldEfNight = 0.0;
			if (!personId.toString().contains("gv_")){ // HDV emissions; TODO: better filter?
				coldEfNight = value.get(initDis).get(nightTime).getColdEF();
			}
			coldPollutant = entry.getKey();
			generatedEmissions = coldEfNight + coldEfOtherAct;
			coldEmissions.put(coldPollutant, generatedEmissions);
		}
		Map<String, Double> coldEmissionStrings = new HashMap<String, Double>();
		for (Entry<ColdPollutant, Double> entry : coldEmissions.entrySet()) {
			coldEmissionStrings.put(entry.getKey().getText(), entry.getValue());
		}
		Event coldEmissionEvent = new ColdEmissionEventImpl(startEngineTime, linkId, personId, coldEmissionStrings);
		emissionEventsManager.processEvent(coldEmissionEvent);
	}
}
