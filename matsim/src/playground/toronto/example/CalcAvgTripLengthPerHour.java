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

import org.matsim.events.AgentDepartureEvent;
import org.matsim.events.handler.AgentDepartureEventHandler;

/**
 * Calculates the average trip length per hour
 *
 * @author mrieser
 */
public class CalcAvgTripLengthPerHour implements AgentDepartureEventHandler {

	private final static int NUM_OF_HOURS = 30;

	private double[] travelDistanceSum = new double[NUM_OF_HOURS];
	private int[] travelDistanceCnt = new int[NUM_OF_HOURS];

	public void handleEvent(final AgentDepartureEvent event) {
		// NOTE: This does not (yet?) include the distance traveled on links where Acts are.
		int hour = (int) event.time / 3600;
		double distance = event.leg.getRoute().getDist();
		if (!Double.isNaN(distance)) {
			this.travelDistanceSum[hour] += distance;
		}
	}

	public void reset(final int iteration) {
		this.travelDistanceSum = new double[NUM_OF_HOURS];
		this.travelDistanceCnt = new int[NUM_OF_HOURS];
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
