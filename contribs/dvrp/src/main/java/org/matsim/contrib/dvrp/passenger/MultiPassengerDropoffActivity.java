/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.passenger;

import java.util.Set;

import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.contrib.dynagent.FirstLastSimStepDynActivity;

public class MultiPassengerDropoffActivity extends FirstLastSimStepDynActivity {
	private final PassengerEngine passengerEngine;
	private final DynAgent driver;
	private final Set<? extends PassengerRequest> requests;

	private final double departureTime;

	public MultiPassengerDropoffActivity(PassengerEngine passengerEngine, DynAgent driver, StayTask dropoffTask,
			Set<? extends PassengerRequest> requests, String activityType) {
		super(activityType);

		this.passengerEngine = passengerEngine;
		this.driver = driver;
		this.requests = requests;

		departureTime = dropoffTask.getEndTime();
	}

	@Override
	protected boolean isLastStep(double now) {
		return now >= departureTime;
	}

	@Override
	protected void afterLastStep(double now) {
		// dropoff at the end of stop activity
		for (PassengerRequest request : requests) {
			passengerEngine.dropOffPassenger(driver, request, now);
		}
	}
}
