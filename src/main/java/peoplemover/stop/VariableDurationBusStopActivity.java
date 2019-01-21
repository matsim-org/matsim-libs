/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package peoplemover.stop;

import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.passenger.PassengerPickupActivity;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.contrib.dynagent.FirstLastSimStepDynActivity;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;

/**
 * Multiple passenger dropoff and pickup activity
 *
 * @author michalm
 */
public class VariableDurationBusStopActivity extends FirstLastSimStepDynActivity implements PassengerPickupActivity {
	private final PassengerEngine passengerEngine;
	private final DynAgent driver;
	private final Set<? extends PassengerRequest> dropoffRequests;
	private final Set<? extends PassengerRequest> pickupRequests;
	private final double expectedEndTime;

	private int passengersPickedUp = 0;

	public VariableDurationBusStopActivity(PassengerEngine passengerEngine, DynAgent driver, DrtStopTask task,
			double duration, String activityType) {
		super(activityType);

		this.passengerEngine = passengerEngine;
		this.driver = driver;
		this.dropoffRequests = task.getDropoffRequests();
		this.pickupRequests = task.getPickupRequests();

		double now = task.getBeginTime();
		this.expectedEndTime = now + duration;//from calculator, may be different than task.getEndTime();
	}

	@Override
	protected boolean isLastStep(double now) {
		return passengersPickedUp == pickupRequests.size() && now >= expectedEndTime;
	}

	@Override
	protected void beforeFirstStep(double now) {
		// TODO probably we should simulate it more accurately (passenger by passenger, not all at once...)
		for (PassengerRequest request : dropoffRequests) {
			passengerEngine.dropOffPassenger(driver, request, now);
		}
	}

	@Override
	protected void simStep(double now) {
		if (now == expectedEndTime) {
			for (PassengerRequest request : pickupRequests) {
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
		return pickupRequests.stream()
				.filter(r -> passengerId.equals(r.getPassengerId()))
				.findAny()
				.orElseThrow(() -> new IllegalArgumentException("I am waiting for different passengers!"));
	}
}
