/* *********************************************************************** *
 * project: org.matsim.*
 * CalcLegTimesKTI.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.anhorni.locationchoice.analysis.events;

import java.util.TreeMap;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.events.AgentArrivalEventImpl;
import org.matsim.core.events.AgentDepartureEventImpl;
import org.matsim.core.events.handler.AgentArrivalEventHandler;
import org.matsim.core.events.handler.AgentDepartureEventHandler;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.geometry.CoordImpl;

public class CalcLegDistances implements AgentDepartureEventHandler, AgentArrivalEventHandler {

	private Population population = null;
	private final TreeMap<Id, Double> agentDepartures = new TreeMap<Id, Double>();
	private final TreeMap<String, Double> sumTripDistancesByModeAndActType = new TreeMap<String, Double>();
	private final TreeMap<String, Integer> sumTripsByModeAndActType = new TreeMap<String, Integer>();

	public CalcLegDistances(Population population) {
		super();
		this.population = population;
	}

	public void handleEvent(AgentDepartureEventImpl event) {
		this.agentDepartures.put(event.getPersonId(), event.getTime());
	}

	public void reset(int iteration) {
		this.sumTripDistancesByModeAndActType.clear();
		this.sumTripsByModeAndActType.clear();
	}

	public void handleEvent(AgentArrivalEventImpl event) {
		Double depTime = this.agentDepartures.remove(event.getPersonId());
		Person agent = this.population.getPersons().get(event.getPersonId());
			
		if (depTime != null && agent != null) {
			Plan plan = event.getPerson().getSelectedPlan();
			double travDistance = ((CoordImpl)((PlanImpl) plan).getPreviousActivity(event.getLeg()).getCoord()).calcDistance(
					((PlanImpl) plan).getNextActivity(event.getLeg()).getCoord());
			
			String actType;

			actType = ((PlanImpl) plan).getNextActivity(event.getLeg()).getType();
			
			// leisure_xxx
			if (actType.startsWith("leisure")) actType = "leisure";
			// work_sector3, ...
			if (actType.startsWith("work")) actType = "work";
			
			if (actType.startsWith("education")) actType = "education";
			
			String key = event.getLeg().getMode().toString() + "_" + actType;
			
			if (actType.equals("shop_grocery") || actType.equals("shop_nongrocery")) {
				String shopKey = event.getLeg().getMode().toString() + "_shop";
				
				Double oldSumTripDistancesShop = this.sumTripDistancesByModeAndActType.get(shopKey);
				if (oldSumTripDistancesShop == null) oldSumTripDistancesShop = 0.0;
				this.sumTripDistancesByModeAndActType.put(shopKey, oldSumTripDistancesShop + travDistance);
				
				Integer oldSumTripsShop = this.sumTripsByModeAndActType.get(shopKey);
				if (oldSumTripsShop == null) oldSumTripsShop = 0;
				this.sumTripsByModeAndActType.put(shopKey, oldSumTripsShop + 1);
			}
			
			Double oldSumTripDistances = this.sumTripDistancesByModeAndActType.get(key);
			if (oldSumTripDistances == null) oldSumTripDistances = 0.0;
			this.sumTripDistancesByModeAndActType.put(key, oldSumTripDistances + travDistance);
			
			Integer oldSumTrips = this.sumTripsByModeAndActType.get(key);
			if (oldSumTrips == null) oldSumTrips = 0;
			this.sumTripsByModeAndActType.put(event.getLeg().getMode().toString() + "_" + actType, oldSumTrips + 1);
		}
	}
	
	public TreeMap<String, Double> getAverageTripDistancesByModeAndActType() {
		TreeMap<String, Double> averageTripDistances = new TreeMap<String, Double>();
		for (String key : this.sumTripDistancesByModeAndActType.keySet()) {			
			// TODO: div by zero for small scenarios possible
			double avg = this.sumTripDistancesByModeAndActType.get(key) / this.sumTripsByModeAndActType.get(key);
			averageTripDistances.put(key, avg);
		}
		return averageTripDistances;
	}	
}
