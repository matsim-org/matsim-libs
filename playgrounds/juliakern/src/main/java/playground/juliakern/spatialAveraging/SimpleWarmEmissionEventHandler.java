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

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.contrib.emissions.types.WarmPollutant;


public class SimpleWarmEmissionEventHandler implements WarmEmissionEventHandler {

	Map<Id, Map<WarmPollutant, Double>> personId2warmEmissions;
	
	public SimpleWarmEmissionEventHandler(){
		this.personId2warmEmissions = new HashMap<Id, Map<WarmPollutant,Double>>();
	}

	@Override
	public void reset(int iteration) {
		personId2warmEmissions = new HashMap<Id, Map<WarmPollutant,Double>>();

	}

	@Override
	public void handleEvent(WarmEmissionEvent event) {
		Id personId = event.getVehicleId();
		if(!personId2warmEmissions.containsKey(personId)){
			Map<WarmPollutant, Double> warmEmissions = event.getWarmEmissions();
			personId2warmEmissions.put(personId, warmEmissions);
		}else{
			Map<WarmPollutant, Double> warmEmissions = event.getWarmEmissions();
			Map<WarmPollutant, Double> oldEmissions = personId2warmEmissions.get(personId);
			for(WarmPollutant wp: warmEmissions.keySet()){
				if(oldEmissions.containsKey(wp)){
					oldEmissions.put(wp, oldEmissions.get(wp)+warmEmissions.get(wp));
				}else{
					oldEmissions.put(wp, warmEmissions.get(wp));
				}
			}
		}
	}
	
	public Map<Id,Map<WarmPollutant, Double>> getPersonId2warmEmissions(){
		return personId2warmEmissions;
	}

	public Map<Id, Double> getPersonId2pollutant(WarmPollutant wp) {
		Map<Id, Double> personId2warmPollutant = new HashMap<Id, Double>();
		for(Id personId: personId2warmEmissions.keySet()){
			personId2warmPollutant.put(personId, personId2warmEmissions.get(personId).get(wp));
		}
		return personId2warmPollutant;
	}

}
