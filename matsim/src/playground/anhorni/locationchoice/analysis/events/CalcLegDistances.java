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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.utils.geometry.CoordImpl;

public class CalcLegDistances implements AgentDepartureEventHandler, AgentArrivalEventHandler {

	private Population population = null;
	private final TreeMap<Id, Double> agentDepartures = new TreeMap<Id, Double>();
	private final TreeMap<String, Double> sumTripDistancesByModeAndActType = new TreeMap<String, Double>();
	private final TreeMap<String, Integer> sumTripsByModeAndActType = new TreeMap<String, Integer>();
	private final TreeMap<Id, Integer> agentArrivalCounts = new TreeMap<Id, Integer>();


	public CalcLegDistances(Population population) {
		super();
		this.population = population;
	}

	public void handleEvent(AgentDepartureEvent event) {
		this.agentDepartures.put(event.getPersonId(), event.getTime());
	}

	public void reset(int iteration) {
		this.sumTripDistancesByModeAndActType.clear();
		this.sumTripsByModeAndActType.clear();
	}

	private void increaseAgentArrivalCount(Id agentId) {
		Integer count = this.agentArrivalCounts.get(agentId);
		if (count == null) {
			this.agentArrivalCounts.put(agentId, Integer.valueOf(1));
		} else {
			this.agentArrivalCounts.put(agentId, Integer.valueOf(1 + count.intValue()));
		}
	}
	
	private Activity getAgentsNextActivity(Plan plan) {
		int count = this.agentArrivalCounts.get(plan.getPerson().getId()).intValue();
		// assuming strict order Activity, Leg, Activity, Leg, Activity
		// count is the number of the current Leg, starting at 1
		return (Activity) plan.getPlanElements().get(count*2);
	}

	private Activity getAgentsPreviousActivity(Plan plan) {
		int count = this.agentArrivalCounts.get(plan.getPerson().getId()).intValue();
		// assuming strict order Activity, Leg, Activity, Leg, Activity
		// count is the number of the current Leg, starting at 1
		return (Activity) plan.getPlanElements().get(count*2 - 2);
	}
	
	public void handleEvent(AgentArrivalEvent event) {
		increaseAgentArrivalCount(event.getPersonId());
		Double depTime = this.agentDepartures.remove(event.getPersonId());
		Person agent = this.population.getPersons().get(event.getPersonId());
			
		if (depTime != null && agent != null) {
			Plan plan = agent.getSelectedPlan();
			Activity nextAct = getAgentsNextActivity(plan);
			double travDistance = ((CoordImpl)getAgentsPreviousActivity(plan).getCoord()).calcDistance(
					nextAct.getCoord());
			
			String actType;

			actType = nextAct.getType();
			
			// leisure_xxx
			if (actType.startsWith("leisure")) actType = "leisure";
			// work_sector3, ...
			if (actType.startsWith("work")) actType = "work";
			
			if (actType.startsWith("education")) actType = "education";
			
			String key = event.getLegMode().toString() + "_" + actType;
			
			if (actType.equals("shop_grocery") || actType.equals("shop_nongrocery")) {
				String shopKey = event.getLegMode().toString() + "_shop";
				
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
			this.sumTripsByModeAndActType.put(event.getLegMode().toString() + "_" + actType, oldSumTrips + 1);
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
