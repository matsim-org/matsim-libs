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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.contrib.dynagent.FirstLastSimStepDynActivity;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;

public class MultiPassengerPickupActivity extends FirstLastSimStepDynActivity implements PassengerPickupActivity {
	private final PassengerHandler passengerHandler;
	private final DynAgent driver;
	private final Map<Id<Request>, ? extends PassengerRequest> requests;
	private final double expectedEndTime;

	private int requestsPickedUp = 0;

	public MultiPassengerPickupActivity(PassengerHandler passengerHandler, DynAgent driver, StayTask pickupTask,
			Map<Id<Request>, ? extends PassengerRequest> requests, String activityType) {
		super(activityType);

		this.passengerHandler = passengerHandler;
		this.driver = driver;
		this.requests = requests;
		this.expectedEndTime = pickupTask.getEndTime();
	}

	@Override
	protected boolean isLastStep(double now) {
		return requestsPickedUp == requests.size() && now >= expectedEndTime;
	}

	@Override
	protected void beforeFirstStep(double now) {
		for (PassengerRequest request : requests.values()) {
			if (passengerHandler.tryPickUpPassengers(this, driver, request.getId(), now)) {
				requestsPickedUp++;
			}
		}
	}

	@Override
	public void notifyPassengersAreReadyForDeparture(List<MobsimPassengerAgent> passengers, double now) {
		PassengerRequest request = getRequestForPassenger(passengers.stream().map(Identifiable::getId).toList());
		if (passengerHandler.tryPickUpPassengers(this, driver, request.getId(), now)) {
			requestsPickedUp++;
		} else {
			throw new IllegalStateException("The passenger is not on the link or not available for departure!");
		}
	}

	private PassengerRequest getRequestForPassenger(List<Id<Person>> passengerIds) {
		return requests.values()
				.stream()
				.filter(r -> r.getPassengerIds().size() == passengerIds.size() && r.getPassengerIds().containsAll(passengerIds))
				.findAny()
				.orElseThrow(() -> new IllegalArgumentException("I am waiting for different passengers!"));
	}
}
