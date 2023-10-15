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

import org.matsim.api.core.v01.Identifiable;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.contrib.dynagent.FirstLastSimStepDynActivity;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SinglePassengerPickupActivity extends FirstLastSimStepDynActivity implements PassengerPickupActivity {
	private final PassengerHandler passengerHandler;
	private final DynAgent driver;
	private final PassengerRequest request;
	private final double expectedEndTime;

	private boolean passengersAboard = false;

	public SinglePassengerPickupActivity(PassengerHandler passengerHandler, DynAgent driver, StayTask pickupTask,
			PassengerRequest request, String activityType) {
		super(activityType);

		this.passengerHandler = passengerHandler;
		this.driver = driver;
		this.request = request;
		this.expectedEndTime = pickupTask.getEndTime();
	}

	@Override
	protected boolean isLastStep(double now) {
		return passengersAboard && now >= expectedEndTime;
	}

	@Override
	protected void beforeFirstStep(double now) {
		passengersAboard = passengerHandler.tryPickUpPassengers(this, driver, request.getId(), now);
	}

	@Override
	public void notifyPassengersAreReadyForDeparture(List<MobsimPassengerAgent> passengers, double now) {
		if (request.getPassengerIds().containsAll(passengers.stream().map(Identifiable::getId).toList())) {
			throw new IllegalArgumentException("I am waiting for a different passenger!");
		}

		passengersAboard = passengerHandler.tryPickUpPassengers(this, driver, request.getId(), now);
		if (!passengersAboard) {
			throw new IllegalStateException("The passenger is not on the link or not available for departure!");
		}
	}
}
