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
import org.matsim.contrib.dynagent.*;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;

public class SinglePassengerPickupActivity extends AbstractDynActivity implements PassengerPickupActivity {
	private final PassengerEngine passengerEngine;
	private final DynAgent driver;
	private final PassengerRequest request;
	private final double pickupDuration;

	private boolean passengerAboard = false;
	private double endTime;

	public SinglePassengerPickupActivity(PassengerEngine passengerEngine, DynAgent driver, StayTask pickupTask,
			PassengerRequest request, double pickupDuration, String activityType) {
		super(activityType);

		this.passengerEngine = passengerEngine;
		this.driver = driver;
		this.request = request;
		this.pickupDuration = pickupDuration;

		double now = pickupTask.getBeginTime();

		passengerAboard = passengerEngine.pickUpPassenger(this, driver, request, now);

		if (passengerAboard) {
			endTime = now + pickupDuration;
		} else {
			setEndTimeIfWaitingForPassenger(now);
		}
	}

	@Override
	public double getEndTime() {
		return endTime;
	}

	@Override
	public void doSimStep(double now) {
		if (!passengerAboard) {
			setEndTimeIfWaitingForPassenger(now);// TODO use DynActivityEngine.END_ACTIVITY_LATER instead?
		}
	}

	private void setEndTimeIfWaitingForPassenger(double now) {
		// try to predict the passenger's arrival time
		endTime = Math.max(now, request.getEarliestStartTime()) + pickupDuration;

		if (endTime == now) {// happens only if pickupDuration == 0
			endTime += 1; // to prevent the driver departing now (before picking up the passenger)
		}
	}

	@Override
	public void notifyPassengerIsReadyForDeparture(MobsimPassengerAgent passenger, double now) {
		if (passenger != request.getPassenger()) {
			throw new IllegalArgumentException("I am waiting for a different passenger!");
		}

		passengerAboard = passengerEngine.pickUpPassenger(this, driver, request, now);

		if (!passengerAboard) {
			throw new IllegalStateException("The passenger is not on the link or not available for departure!");
		}

		endTime = now + pickupDuration;
	}
}
