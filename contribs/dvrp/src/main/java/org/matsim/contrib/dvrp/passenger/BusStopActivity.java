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

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.contrib.dynagent.FirstLastSimStepDynActivity;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;

/**
 * Multiple passenger dropoff and pickup activity
 *
 * @author michalm
 */
public class BusStopActivity extends FirstLastSimStepDynActivity implements PassengerPickupActivity {
	private final PassengerEngine passengerEngine;
	private final DynAgent driver;
	private final Map<Id<Request>, ? extends PassengerRequest> dropoffRequests;
	private final Map<Id<Request>, ? extends PassengerRequest> pickupRequests;
	private final double expectedEndTime;

	private int passengersPickedUp = 0;

	public BusStopActivity(PassengerEngine passengerEngine, DynAgent driver, StayTask task,
			Map<Id<Request>, ? extends PassengerRequest> dropoffRequests,
			Map<Id<Request>, ? extends PassengerRequest> pickupRequests, String activityType) {
		super(activityType);
		this.passengerEngine = passengerEngine;
		this.driver = driver;
		this.dropoffRequests = dropoffRequests;
		this.pickupRequests = pickupRequests;
		this.expectedEndTime = task.getEndTime();
	}

	@Override
	protected boolean isLastStep(double now) {
		return passengersPickedUp == pickupRequests.size() && now >= expectedEndTime;
	}

	@Override
	protected void beforeFirstStep(double now) {
		// TODO probably we should simulate it more accurately (passenger by passenger, not all at once...)
		for (PassengerRequest request : dropoffRequests.values()) {
			passengerEngine.dropOffPassenger(driver, request, now);
		}
	}

	@Override
	protected void simStep(double now) {
		if (now == expectedEndTime) {
			for (PassengerRequest request : pickupRequests.values()) {
				if (passengerEngine.pickUpPassenger(this, driver, request, now)) {
					passengersPickedUp++;
				}
			}
		}
	}

	@Override
	public void notifyPassengerIsReadyForDeparture(MobsimPassengerAgent passenger, double now) {
		if (now < expectedEndTime) {
			return;// pick up only at the end of stop activity
		}

		PassengerRequest request = getRequestForPassenger(passenger.getId());
		if (passengerEngine.pickUpPassenger(this, driver, request, now)) {
			passengersPickedUp++;
		} else {
			throw new IllegalStateException("The passenger is not on the link or not available for departure!");
		}
	}

	private PassengerRequest getRequestForPassenger(Id<Person> passengerId) {
		return pickupRequests.values().stream()
				.filter(r -> passengerId.equals(r.getPassengerId()))
				.findAny()
				.orElseThrow(() -> new IllegalArgumentException("I am waiting for different passengers!"));
	}
}
