/* *********************************************************************** *
 *                                                                         *
 * project: org.matsim.*
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
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;

public class SinglePassengerPickupActivity extends FirstLastSimStepDynActivity implements PassengerPickupActivity {
	private final PassengerEngine passengerEngine;
	private final DynAgent driver;
	private final PassengerRequest request;
	private final double expectedEndTime;

	private boolean passengerAboard = false;

	public SinglePassengerPickupActivity(PassengerEngine passengerEngine, DynAgent driver, StayTask pickupTask,
			PassengerRequest request, String activityType) {
		super(activityType);

		this.passengerEngine = passengerEngine;
		this.driver = driver;
		this.request = request;
		this.expectedEndTime = pickupTask.getEndTime();
	}

	@Override
	protected boolean isLastStep(double now) {
		return passengerAboard && now >= expectedEndTime;
	}

	@Override
	protected void beforeFirstStep(double now) {
		passengerAboard = passengerEngine.pickUpPassenger(this, driver, request, now);
	}

	@Override
	public void notifyPassengerIsReadyForDeparture(MobsimPassengerAgent passenger, double now) {
		if (passenger.getId().equals(request.getPassengerId())) {
			throw new IllegalArgumentException("I am waiting for a different passenger!");
		}

		passengerAboard = passengerEngine.pickUpPassenger(this, driver, request, now);
		if (!passengerAboard) {
			throw new IllegalStateException("The passenger is not on the link or not available for departure!");
		}
	}
}
