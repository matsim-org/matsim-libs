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

import java.util.Set;

import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.vrpagent.VrpActivity;
import org.matsim.contrib.dynagent.DynAgent;

public class MultiPassengerDropoffActivity extends VrpActivity {
	private final PassengerEngine passengerEngine;
	private final DynAgent driver;
	private final Set<? extends PassengerRequest> requests;

	public MultiPassengerDropoffActivity(PassengerEngine passengerEngine, DynAgent driver, StayTask dropoffTask,
			Set<? extends PassengerRequest> requests, String activityType) {
		super(activityType, dropoffTask);

		this.passengerEngine = passengerEngine;
		this.driver = driver;
		this.requests = requests;
	}

	@Override
	public void finalizeAction(double now) {
		for (PassengerRequest request : requests) {
			passengerEngine.dropOffPassenger(driver, request, now);
		}
	}
}
