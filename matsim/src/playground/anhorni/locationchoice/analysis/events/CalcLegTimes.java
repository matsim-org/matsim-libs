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

import playground.anhorni.locationchoice.preprocess.helper.Utils;

public class CalcLegTimes implements AgentDepartureEventHandler, AgentArrivalEventHandler {

	private Population population = null;
	private final TreeMap<Id, Double> agentDepartures = new TreeMap<Id, Double>();
	private final TreeMap<String, Double> sumTripDurationsByModeAndActType = new TreeMap<String, Double>();
	private final TreeMap<String, Integer> sumTripsByModeAndActType = new TreeMap<String, Integer>();
	private final TreeMap<Id, Integer> agentArrivalCounts = new TreeMap<Id, Integer>();
	private boolean wayThere = false;

	public CalcLegTimes(Population population, boolean wayThere) {
		super();
		this.population = population;
		this.wayThere = wayThere;
	}

	public void handleEvent(AgentDepartureEvent event) {
		this.agentDepartures.put(event.getPersonId(), event.getTime());
	}

	public void reset(int iteration) {
		this.sumTripDurationsByModeAndActType.clear();
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
	
	public void handleEvent(AgentArrivalEvent event) {
		increaseAgentArrivalCount(event.getPersonId());
		Double depTime = this.agentDepartures.remove(event.getPersonId());
		Person agent = this.population.getPersons().get(event.getPersonId());
			
		if (depTime != null && agent != null) {
			double travTime = event.getTime() - depTime;

			Plan plan = agent.getSelectedPlan();
			String actType;
			
			if (this.wayThere) {
				actType = getAgentsNextActivity(plan).getType();//((PlanImpl) plan).getNextActivity(event.getLeg()).getType();
			}
			else {
				actType = Utils.getActType(plan, getAgentsNextActivity(plan));
			}
			
			// leisure_xxx
			if (actType.startsWith("leisure")) actType = "leisure";
			// work_sector3, ...
			if (actType.startsWith("work")) actType = "work";
			
			if (actType.startsWith("education")) actType = "education";
			
			String key = event.getLegMode().toString() + "_" + actType;
			
			if (actType.equals("shop_grocery") || actType.equals("shop_nongrocery")) {
				String shopKey = event.getLegMode().toString() + "_shop";
				
				Double oldSumTripDurationShop = this.sumTripDurationsByModeAndActType.get(shopKey);
				if (oldSumTripDurationShop == null) oldSumTripDurationShop = 0.0;
				this.sumTripDurationsByModeAndActType.put(shopKey, oldSumTripDurationShop + travTime);
				
				Integer oldSumTripsShop = this.sumTripsByModeAndActType.get(shopKey);
				if (oldSumTripsShop == null) oldSumTripsShop = 0;
				this.sumTripsByModeAndActType.put(shopKey, oldSumTripsShop + 1);
			}
			
			Double oldSumTripDuration = this.sumTripDurationsByModeAndActType.get(key);
			if (oldSumTripDuration == null) oldSumTripDuration = 0.0;
			this.sumTripDurationsByModeAndActType.put(key, oldSumTripDuration + travTime);
			
			Integer oldSumTrips = this.sumTripsByModeAndActType.get(key);
			if (oldSumTrips == null) oldSumTrips = 0;
			this.sumTripsByModeAndActType.put(event.getLegMode().toString() + "_" + actType, oldSumTrips + 1);
		}
	}
	
	public TreeMap<String, Double> getAverageTripDurationsByModeAndActType() {
		TreeMap<String, Double> averageTripDurations = new TreeMap<String, Double>();
		for (String key : this.sumTripDurationsByModeAndActType.keySet()) {			
			// TODO: div by zero for small scenarios possible
			double avg = this.sumTripDurationsByModeAndActType.get(key) / this.sumTripsByModeAndActType.get(key);
			averageTripDurations.put(key, avg);
		}
		return averageTripDurations;
	}

	public double getAverageOverallTripDuration() {		
		double overallTripDuration = 0.0; 
		int overallNumTrips = 0;
		
		for (String key : this.sumTripDurationsByModeAndActType.keySet()) {
			overallTripDuration += this.sumTripDurationsByModeAndActType.get(key);
			overallNumTrips += this.sumTripsByModeAndActType.get(key);
		}
		return (overallTripDuration / overallNumTrips);
	}	
}
