/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2022 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.passenger;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerHandler;
import org.matsim.contrib.dvrp.passenger.PassengerPickupActivity;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.contrib.dynagent.FirstLastSimStepDynActivity;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;

/**
 * Multiple passenger dropoff and pickup activity
 *
 * @author michalm
 */
public class DrtStopActivity extends FirstLastSimStepDynActivity implements PassengerPickupActivity {
	private final PassengerHandler passengerHandler;
	private final DynAgent driver;
	private final Map<Id<Request>, ? extends AcceptedDrtRequest> dropoffRequests;
	private final Map<Id<Request>, ? extends AcceptedDrtRequest> pickupRequests;
	private final double expectedEndTime;

	private int passengersPickedUp = 0;

	public DrtStopActivity(PassengerHandler passengerHandler, DynAgent driver, StayTask task,
			Map<Id<Request>, ? extends AcceptedDrtRequest> dropoffRequests,
			Map<Id<Request>, ? extends AcceptedDrtRequest> pickupRequests, String activityType) {
		super(activityType);
		this.passengerHandler = passengerHandler;
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
		for (var request : dropoffRequests.values()) {
			passengerHandler.dropOffPassenger(driver, request.getId(), now);
		}
	}

	@Override
	protected void simStep(double now) {
		if (now == expectedEndTime) {
			for (var request : pickupRequests.values()) {
				if (passengerHandler.tryPickUpPassenger(this, driver, request.getId(), now)) {
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

		var request = getRequestForPassenger(passenger.getId());
		if (passengerHandler.tryPickUpPassenger(this, driver, request.getId(), now)) {
			passengersPickedUp++;
		} else {
			throw new IllegalStateException("The passenger is not on the link or not available for departure!");
		}
	}

	private AcceptedDrtRequest getRequestForPassenger(Id<Person> passengerId) {
		return pickupRequests.values()
				.stream()
				.filter(r -> passengerId.equals(r.getPassengerId()))
				.findAny()
				.orElseThrow(() -> new IllegalArgumentException("I am waiting for different passengers!"));
	}
}
