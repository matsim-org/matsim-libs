/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.fhuelsmann.emission.analysis.mobility;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Population;

public class CalcAvgTripDurations implements PersonDepartureEventHandler, PersonArrivalEventHandler {

	//TO DO: getRelevantPopulation between ZoneId x and all other zones, 
	//calculate the aggregated travel time for all people traveling between zone x and all other zones 
	//and divide it by the number of people traveling between zone x and all other zones 

	/**
	 * stores the last known departure time per agent
	 */
	private final Map<String, Double> agentDepartures = new TreeMap<String, Double>();
	

	private double travelTimeSum;
	private int travelTimeCnt;

	public void handleEvent(final PersonDepartureEvent event) {
		this.agentDepartures.put(event.getPersonId().toString(), event.getTime());
	}

	public void handleEvent(final PersonArrivalEvent event) {
		double departureTime = this.agentDepartures.get(event.getPersonId().toString());
		double travelTime = event.getTime() - departureTime;
		this.travelTimeSum+= travelTime;
		this.travelTimeCnt++;
		
	}

	public void reset(final int iteration) {
		this.agentDepartures.clear();
	
	}

	/**
	 * @return average trip duration from zone x to all other zones
	 */
	public Map<Id, Double> getAvgTripDuration(Population population) {
		Map<Id, Double> avgTripduration = new TreeMap<Id, Double>();
		Id zoneId = null;
		
		int count = this.travelTimeCnt;
		if (count == 0) {
			avgTripduration.put(zoneId, 0.0);
			return avgTripduration;
			
		}
		// else
		System.out.println("+++++++++++++++++++++averageTravelTime"+this.travelTimeSum/count);
		
		avgTripduration.put(zoneId, travelTimeSum/travelTimeCnt);
		return avgTripduration;
		
	}
}