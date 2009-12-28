/* *********************************************************************** *
 * project: org.matsim.*
 * CalcVehicleKilometerTraveled.java
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

import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.network.NetworkLayer;

/**
 * Calculates the distance all vehicles travel per hour
 *
 * @author mrieser
 */
public class CalcVehicleKilometerTraveled implements LinkEnterEventHandler {

	private final static int NUM_OF_HOURS = 30;

	private double[] travelDistanceSum = new double[NUM_OF_HOURS];

	private final NetworkLayer network;
	
	public CalcVehicleKilometerTraveled(final NetworkLayer network) {
		this.network = network;
	}
	
	public void handleEvent(final LinkEnterEvent event) {
		int hour = (int) event.getTime() / 3600;
		this.travelDistanceSum[hour] += this.network.getLinks().get(event.getLinkId()).getLength(); // this assumes link.length is specified in meters!
	}

	public void reset(final int iteration) {
		this.travelDistanceSum = new double[NUM_OF_HOURS];
	}

	/**
	 * @param hour the hour of the day, starting at 0
	 * @return distance traveled by all vehicles in the specified hours, in kilometers
	 */
	public double getAvgTripDuration(final int hour) {
		return this.travelDistanceSum[hour] / 1000.0;
	}

}
