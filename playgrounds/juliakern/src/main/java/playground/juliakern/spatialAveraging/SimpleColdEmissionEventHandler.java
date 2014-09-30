/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.juliakern.spatialAveraging;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.types.ColdPollutant;

import java.util.HashMap;
import java.util.Map;


public class SimpleColdEmissionEventHandler implements ColdEmissionEventHandler {

	Map<Id, Map<ColdPollutant, Double>> personId2coldEmissions;

	public SimpleColdEmissionEventHandler(){
		this.personId2coldEmissions = new HashMap<Id, Map<ColdPollutant,Double>>();
	}
	@Override
	public void reset(int iteration) {
		personId2coldEmissions = new HashMap<Id, Map<ColdPollutant, Double>>();

	}

	@Override
	public void handleEvent(ColdEmissionEvent event) {
		Id personId = event.getVehicleId();
		if (!personId2coldEmissions.containsKey(personId)) {
			Map<ColdPollutant, Double> coldEmissions = event.getColdEmissions();
			personId2coldEmissions.put(personId, coldEmissions);
		} else {
			Map<ColdPollutant, Double> coldEmissions = event.getColdEmissions();
			Map<ColdPollutant, Double> oldEmissions = personId2coldEmissions
					.get(personId);
			for (ColdPollutant wp : coldEmissions.keySet()) {
				if (oldEmissions.containsKey(wp)) {
					oldEmissions.put(wp,
							oldEmissions.get(wp) + coldEmissions.get(wp));
				} else {
					oldEmissions.put(wp, coldEmissions.get(wp));
				}
			}
		}
	}

	public Map<Id, Map<ColdPollutant, Double>> getPersonId2coldEmissions() {
		return personId2coldEmissions;
	}

	public Map<Id, Double> getPersonId2pollutant(ColdPollutant cp) {
			Map<Id, Double> personId2coldPollutant = new HashMap<Id, Double>();
			for(Id personId: personId2coldEmissions.keySet()){
				personId2coldPollutant.put(personId, personId2coldEmissions.get(personId).get(cp));
			}
			return personId2coldPollutant;
		}
	}


