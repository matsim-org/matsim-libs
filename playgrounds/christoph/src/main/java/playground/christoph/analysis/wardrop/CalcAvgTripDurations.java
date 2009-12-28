/* *********************************************************************** *
 * project: org.matsim.*
 * CalcAvgTripDurations.java
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

package playground.christoph.analysis.wardrop;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;


/**
 * Calculates the average trip durations per hour
 *
 * @author cdobler
 */
public class CalcAvgTripDurations implements AgentDepartureEventHandler, AgentArrivalEventHandler {

	private final static int NUM_OF_HOURS = 24;

	/*
	 * stores the last known departure time per agent
	 */
	private final Map<Id, Double> agentDepartures = new TreeMap<Id, Double>();

	private double[] travelTimeSum = new double[NUM_OF_HOURS];
	private int[] travelTimeCnt = new int[NUM_OF_HOURS];

	public void handleEvent(final AgentDepartureEvent event) {
		this.agentDepartures.put(event.getPersonId(), event.getTime());
	}

	public void handleEvent(final AgentArrivalEvent event) {
		double departureTime = this.agentDepartures.get(event.getPersonId());
		double travelTime = event.getTime() - departureTime;
		int hour = (int) (departureTime / 3600);
		this.travelTimeSum[hour] += travelTime;
		this.travelTimeCnt[hour]++;
	}

	public void reset(final int iteration) {
		this.agentDepartures.clear();
		this.travelTimeSum = new double[NUM_OF_HOURS];
		this.travelTimeCnt = new int[NUM_OF_HOURS];
	}

	/**
	 * @param hour the hour of the day, starting at 0
	 * @return average trip duration of all trips starting in the specified hour, -1 if no trips have started in that hour
	 */
	public double getAvgTripDuration(final int hour) {
		int count = this.travelTimeCnt[hour];
		if (count == 0) {
			return -1;
		}
		// else
		return this.travelTimeSum[hour] / count;
	}

}
