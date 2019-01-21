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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.contrib.dynagent.FirstLastSimStepDynActivity;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;

public class MultiPassengerPickupActivity extends FirstLastSimStepDynActivity implements PassengerPickupActivity {
	private final PassengerEngine passengerEngine;
	private final DynAgent driver;
	private final Set<? extends PassengerRequest> requests;
	private final double expectedEndTime;

	private int passengersPickedUp;

	public MultiPassengerPickupActivity(PassengerEngine passengerEngine, DynAgent driver, StayTask pickupTask,
			Set<? extends PassengerRequest> requests, String activityType) {
		super(activityType);

		this.passengerEngine = passengerEngine;
		this.driver = driver;
		this.requests = requests;
		this.expectedEndTime = pickupTask.getEndTime();
	}

	@Override
	protected boolean isLastStep(double now) {
		return passengersPickedUp == requests.size() && now >= expectedEndTime;
	}

	@Override
	protected void beforeFirstStep(double now) {
		for (PassengerRequest request : requests) {
			if (passengerEngine.pickUpPassenger(this, driver, request, now)) {
				passengersPickedUp++;
			}
		}
	}

	@Override
	public void notifyPassengerIsReadyForDeparture(MobsimPassengerAgent passenger, double now) {
		PassengerRequest request = getRequestForPassenger(passenger.getId());
		if (passengerEngine.pickUpPassenger(this, driver, request, now)) {
			passengersPickedUp++;
		} else {
			throw new IllegalStateException("The passenger is not on the link or not available for departure!");
		}
	}

	private PassengerRequest getRequestForPassenger(Id<Person> passengerId) {
		return requests.stream()
				.filter(r -> passengerId.equals(r.getPassengerId()))
				.findAny()
				.orElseThrow(() -> new IllegalArgumentException("I am waiting for different passengers!"));
	}
}
