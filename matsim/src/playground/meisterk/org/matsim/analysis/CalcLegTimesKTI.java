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

package playground.meisterk.org.matsim.analysis;

import java.util.TreeMap;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Population;
import org.matsim.core.events.AgentArrivalEvent;
import org.matsim.core.events.AgentDepartureEvent;
import org.matsim.core.events.handler.AgentArrivalEventHandler;
import org.matsim.core.events.handler.AgentDepartureEventHandler;

/**
 * Calculates average trip durations by mode.
 * 
 * @author meisterk
 *
 */
public class CalcLegTimesKTI implements AgentDepartureEventHandler, AgentArrivalEventHandler {

	private Population population = null;
	private final TreeMap<Id, Double> agentDepartures = new TreeMap<Id, Double>();
	private final TreeMap<TransportMode, Double> sumTripDurationsByMode = new TreeMap<TransportMode, Double>();
	private final TreeMap<TransportMode, Integer> sumTripsByMode = new TreeMap<TransportMode, Integer>();

	public CalcLegTimesKTI(Population population) {
		super();
		this.population = population;
	}

	public void handleEvent(AgentDepartureEvent event) {
		this.agentDepartures.put(event.getPersonId(), event.getTime());
	}

	public void reset(int iteration) {
		this.sumTripDurationsByMode.clear();
		this.sumTripsByMode.clear();
	}

	public void handleEvent(AgentArrivalEvent event) {
		Double depTime = this.agentDepartures.remove(event.getPersonId());
		Person agent = this.population.getPersons().get(event.getPersonId());
		if (depTime != null && agent != null) {
			double travTime = event.getTime() - depTime;

			Double oldSumTripDuration = this.sumTripDurationsByMode.get(event.getLeg().getMode());
			if (oldSumTripDuration == null) oldSumTripDuration = 0.0;
			this.sumTripDurationsByMode.put(event.getLeg().getMode(), oldSumTripDuration + travTime);
			
			Integer oldSumTripsByMode = this.sumTripsByMode.get(event.getLeg().getMode());
			if (oldSumTripsByMode == null) oldSumTripsByMode = 0;
			this.sumTripsByMode.put(event.getLeg().getMode(), oldSumTripsByMode + 1);
		}
	}

	public TreeMap<TransportMode, Double> getAverageTripDurationsByMode() {
		TreeMap<TransportMode, Double> averageTripDurations = new TreeMap<TransportMode, Double>();
		for (TransportMode mode : this.sumTripDurationsByMode.keySet()) {
			averageTripDurations.put(mode, this.sumTripDurationsByMode.get(mode) / this.sumTripsByMode.get(mode));
		}
		return averageTripDurations;
	}

	public double getAverageOverallTripDuration() {
		
		double overallTripDuration = 0.0; 
		int overallNumTrips = 0;
		
		for (TransportMode mode : this.sumTripDurationsByMode.keySet()) {
			overallTripDuration += this.sumTripDurationsByMode.get(mode);
			overallNumTrips += this.sumTripsByMode.get(mode);
		}
		
		return (overallTripDuration / overallNumTrips);
	}
	
}
