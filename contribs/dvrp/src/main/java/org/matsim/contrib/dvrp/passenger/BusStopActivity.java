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

import java.util.Set;

import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dynagent.AbstractDynActivity;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;

/**
 * Multiple passenger dropoff and pickup activity
 * 
 * @author michalm
 */
public class BusStopActivity extends AbstractDynActivity implements PassengerPickupActivity {
	private final PassengerEngine passengerEngine;
	private final DynAgent driver;
	private final Set<? extends PassengerRequest> dropoffRequests;
	private final Set<? extends PassengerRequest> pickupRequests;

	private int passengersAboard;
	private double endTime = END_ACTIVITY_LATER;
	private double departureTime;

	public BusStopActivity(PassengerEngine passengerEngine, DynAgent driver, StayTask task,
			Set<? extends PassengerRequest> dropoffRequests, Set<? extends PassengerRequest> pickupRequests,
			String activityType) {
		super(activityType);

		this.passengerEngine = passengerEngine;
		this.driver = driver;
		this.dropoffRequests = dropoffRequests;
		this.pickupRequests = pickupRequests;
		this.departureTime = task.getEndTime();

		double now = task.getBeginTime();
		dropoffPassengers(now);
	}

	// TODO probably we should simulate it more accurately (passenger by passenger, not all at once...)
	private void dropoffPassengers(double now) {
		for (PassengerRequest request : dropoffRequests) {
			passengerEngine.dropOffPassenger(driver, request, now);
		}
	}

	@Override
	public double getEndTime() {
		return endTime;
	}

	@Override
	public void doSimStep(double now) {
		if (now < departureTime) {
			return;
		}

		if (now == departureTime) {
			// picking up is at the end of stay
			// TODO probably we should simulate it more accurately (passenger by passenger, not all at once...)
			for (PassengerRequest request : pickupRequests) {
				if (passengerEngine.pickUpPassenger(this, driver, request, now)) {
					passengersAboard++;
				}
			}

			if (passengersAboard == pickupRequests.size()) {
				endTime = now;
			}
		}
	}

	@Override
	public void notifyPassengerIsReadyForDeparture(MobsimPassengerAgent passenger, double now) {
		if (now < departureTime) {
			return;// pick up only at the end of stop activity
		}

		PassengerRequest request = getRequestForPassenger(passenger);
		if (passengerEngine.pickUpPassenger(this, driver, request, now)) {
			passengersAboard++;
		} else {
			throw new IllegalStateException("The passenger is not on the link or not available for departure!");
		}
	}

	private PassengerRequest getRequestForPassenger(MobsimPassengerAgent passenger) {
		for (PassengerRequest request : pickupRequests) {
			if (passenger == request.getPassenger()) {
				return request;
			}
		}
		throw new IllegalArgumentException("I am waiting for different passengers!");
	}
}
