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
import org.matsim.core.population.PlanImpl;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.events.AgentArrivalEventImpl;
import org.matsim.core.events.AgentDepartureEventImpl;
import org.matsim.core.events.handler.AgentArrivalEventHandler;
import org.matsim.core.events.handler.AgentDepartureEventHandler;

import playground.anhorni.locationchoice.preprocess.helper.Utils;

public class CalcLegTimes implements AgentDepartureEventHandler, AgentArrivalEventHandler {

	private Population population = null;
	private final TreeMap<Id, Double> agentDepartures = new TreeMap<Id, Double>();
	private final TreeMap<String, Double> sumTripDurationsByModeAndActType = new TreeMap<String, Double>();
	private final TreeMap<String, Integer> sumTripsByModeAndActType = new TreeMap<String, Integer>();
	private boolean wayThere = false;

	public CalcLegTimes(Population population, boolean wayThere) {
		super();
		this.population = population;
		this.wayThere = wayThere;
	}

	public void handleEvent(AgentDepartureEventImpl event) {
		this.agentDepartures.put(event.getPersonId(), event.getTime());
	}

	public void reset(int iteration) {
		this.sumTripDurationsByModeAndActType.clear();
		this.sumTripsByModeAndActType.clear();
	}

	public void handleEvent(AgentArrivalEventImpl event) {
		Double depTime = this.agentDepartures.remove(event.getPersonId());
		Person agent = this.population.getPersons().get(event.getPersonId());
			
		if (depTime != null && agent != null) {
			double travTime = event.getTime() - depTime;

			PlanImpl plan = event.getPerson().getSelectedPlan();
			String actType;
			
			if (this.wayThere) {
				actType = plan.getNextActivity(event.getLeg()).getType();
			}
			else {
				actType = Utils.getActType(plan, plan.getNextActivity(event.getLeg()));
			}
			
			// leisure_xxx
			if (actType.startsWith("leisure")) actType = "leisure";
			// work_sector3, ...
			if (actType.startsWith("work")) actType = "work";
			
			if (actType.startsWith("education")) actType = "education";
			
			String key = event.getLeg().getMode().toString() + "_" + actType;
			
			if (actType.equals("shop_grocery") || actType.equals("shop_nongrocery")) {
				String shopKey = event.getLeg().getMode().toString() + "_shop";
				
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
			this.sumTripsByModeAndActType.put(event.getLeg().getMode().toString() + "_" + actType, oldSumTrips + 1);
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
