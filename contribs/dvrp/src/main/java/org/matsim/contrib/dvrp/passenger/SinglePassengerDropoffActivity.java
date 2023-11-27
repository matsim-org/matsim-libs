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

import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.contrib.dynagent.FirstLastSimStepDynActivity;

public class SinglePassengerDropoffActivity extends FirstLastSimStepDynActivity {
	private final PassengerHandler passengerHandler;
	private final DynAgent driver;
	private final PassengerRequest request;

	private final double departureTime;

	public SinglePassengerDropoffActivity(PassengerHandler passengerHandler, DynAgent driver, StayTask dropoffTask,
			PassengerRequest request, String activityType) {
		super(activityType);

		this.passengerHandler = passengerHandler;
		this.driver = driver;
		this.request = request;

		departureTime = dropoffTask.getEndTime();
	}

	@Override
	protected boolean isLastStep(double now) {
		return now >= departureTime;
	}

	@Override
	protected void afterLastStep(double now) {
		passengerHandler.dropOffPassengers(driver, request.getId(), now);
	}
}
