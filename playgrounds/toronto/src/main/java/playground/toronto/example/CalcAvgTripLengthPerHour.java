/* *********************************************************************** *
 * project: org.matsim.*
 * CalcTripLengthPerHour.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.toronto.example;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.network.NetworkLayer;

/**
 * Calculates the average length of all trips started in a hour.
 *
 * @author mrieser
 */
public class CalcAvgTripLengthPerHour implements AgentDepartureEventHandler, AgentArrivalEventHandler, LinkEnterEventHandler {

	private final static int NUM_OF_HOURS = 30;

	private double[] travelDistanceSum = new double[NUM_OF_HOURS];
	private int[] travelDistanceCnt = new int[NUM_OF_HOURS];

	private Map<Id, Double> travelStartPerAgent = new HashMap<Id, Double>(1000);
	private Map<Id, Double> travelDistancePerAgent = new HashMap<Id, Double>(1000);

	private final NetworkLayer network;

	public CalcAvgTripLengthPerHour(final NetworkLayer network) {
		this.network = network;
	}

	public void handleEvent(final AgentDepartureEvent event) {
		this.travelStartPerAgent.put(event.getPersonId(), event.getTime());
		this.travelDistancePerAgent.put(event.getPersonId(), 0.0);
	}

	public void handleEvent(final AgentArrivalEvent event) {
		Double distance = this.travelDistancePerAgent.remove(event.getPersonId());

		if (distance > 0.0) {
			Double startTime = this.travelStartPerAgent.get(event.getPersonId());
			int hour = startTime.intValue() / 3600;
			this.travelDistanceSum[hour] += distance;
			this.travelDistanceCnt[hour]++;
		}
	}

	public void handleEvent(final LinkEnterEvent event) {
		Double distance = this.travelDistancePerAgent.get(event.getPersonId());
		Link link = this.network.getLinks().get(event.getLinkId());
		distance = distance + link.getLength();
		this.travelDistancePerAgent.put(event.getPersonId(), distance);
	}

	public void reset(final int iteration) {
		this.travelDistanceSum = new double[NUM_OF_HOURS];
		this.travelDistanceCnt = new int[NUM_OF_HOURS];
		this.travelDistancePerAgent.clear();
		this.travelStartPerAgent.clear();
	}

	/**
	 * @param hour the hour of the day, starting at 0
	 * @return average trip length of all trips starting in the specified hour, -1 if no trips have started in that hour
	 */
	public double getAvgTripLength(final int hour) {
		int count = this.travelDistanceCnt[hour];
		if (count == 0) {
			return -1;
		}
		// else
		return this.travelDistanceSum[hour] / count;
	}

}
