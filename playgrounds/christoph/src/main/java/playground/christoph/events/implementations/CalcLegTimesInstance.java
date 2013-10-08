/* *********************************************************************** *
 * project: org.matsim.*
 * CalcLegTimes.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.events.implementations;

import java.util.concurrent.ConcurrentMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;

import playground.christoph.events.EventHandlerInstance;

/**
 * @author mrieser
 * @author cdobler
 *
 * Calculates the distribution of legs-durations, e.g. how many legs took at
 * most 5 minutes, how many between 5 and 10 minutes, and so on.
 * Also calculates the average trip duration.
 * Trips ended because of vehicles being stuck are not counted.
 */
public class CalcLegTimesInstance implements EventHandlerInstance, PersonDepartureEventHandler, 
		PersonArrivalEventHandler, ActivityEndEventHandler, ActivityStartEventHandler {

	private final ConcurrentMap<Id, Double> agentDepartures;
	private final ConcurrentMap<Id, Double> agentArrivals;
	private final ConcurrentMap<String, int[]> legStats;
	private final ConcurrentMap<Id, String> previousActivityTypes;
	private double sumTripDurations = 0;
	private int sumTrips = 0;

	public CalcLegTimesInstance(ConcurrentMap<Id, Double> agentDepartures, ConcurrentMap<Id, Double> agentArrivals,
			ConcurrentMap<String, int[]> legStats, ConcurrentMap<Id, String> previousActivityTypes) {
		this.agentArrivals = agentArrivals;
		this.agentDepartures = agentDepartures;
		this.legStats = legStats;
		this.previousActivityTypes = previousActivityTypes;
	}
	
	@Override
	public void handleEvent(ActivityEndEvent event) {
		this.previousActivityTypes.put(event.getPersonId(), event.getActType());
	}
	
	@Override
	public void handleEvent(final PersonDepartureEvent event) {
		this.agentDepartures.put(event.getPersonId(), event.getTime());
	}

	@Override
	public void handleEvent(final PersonArrivalEvent event) {
		this.agentArrivals.put(event.getPersonId(), event.getTime());
	}


	@Override
	public void handleEvent(ActivityStartEvent event) {
		Double depTime = this.agentDepartures.remove(event.getPersonId());
		Double arrTime = this.agentArrivals.remove(event.getPersonId());
		if (depTime != null) {
			double travTime = arrTime - depTime;
			String fromActType = previousActivityTypes.remove(event.getPersonId());
			String toActType = event.getActType();
			String legType = fromActType + "---" + toActType;
			int[] stats = this.legStats.get(legType);
			if (stats == null) {
				stats = new int[CalcLegTimes.MAXINDEX+1];
				for (int i = 0; i <= CalcLegTimes.MAXINDEX; i++) {
					stats[i] = 0;
				}
				this.legStats.put(legType, stats);
			}
			stats[getTimeslotIndex(travTime)]++;

			this.sumTripDurations += travTime;
			this.sumTrips++;
		}
	}

	/*package*/ double getSumTripDurations() {
		return this.sumTripDurations;
	}
	
	/*package*/ int getSumTrips() {
		return this.sumTrips;
	}
		
	@Override
	public void reset(final int iteration) {
		this.sumTripDurations = 0;
		this.sumTrips = 0;
	}

	private int getTimeslotIndex(final double time_s) {
		int idx = (int)(time_s / CalcLegTimes.SLOT_SIZE);
		if (idx > CalcLegTimes.MAXINDEX) idx = CalcLegTimes.MAXINDEX;
		return idx;
	}

	@Override
	public void synchronize(double time) {
		// nothing to do here
	}
}