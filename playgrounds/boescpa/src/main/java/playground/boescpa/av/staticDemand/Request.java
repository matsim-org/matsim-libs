/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.boescpa.av.staticDemand;

import playground.boescpa.lib.tools.tripReader.Trip;

/**
 * WHAT IS IT FOR?
 * WHAT DOES IT?
 *
 * @author boescpa
 */
public class Request {

	private final Trip trip;
	private boolean ifMet = false;
	private double assignmentTime = -1;
	private double responseTime = -1;

	public Request(Trip trip) {
		this.trip = trip;
	}

	public void setIfMet(boolean ifMet) {
		this.ifMet = ifMet;
	}

	public void setAssignmentTime(double assignmentTime) {
		this.assignmentTime = assignmentTime;
	}

	public void setResponseTime(double responseTime) {
		this.responseTime = responseTime;
	}

	public static String getStatsDescr() {
		return "met"
				+ Stats.delimiter + "timeOfRequest"
				+ Stats.delimiter + "assignmentTime"
				+ Stats.delimiter + "responseTime"
				+ Stats.delimiter + "requestDuration"
				+ Stats.delimiter + "requestDistance";
	}

	public String getStats() {
		return ifMet
				+ Stats.delimiter + trip.startTime
				+ Stats.delimiter + assignmentTime
				+ Stats.delimiter + responseTime
				+ Stats.delimiter + (trip.endTime - trip.startTime)
				+ Stats.delimiter + trip.distance;
	}
}
