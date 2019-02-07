/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.passenger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.drt.data.DrtRequest;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.PlanAgent;

/**
 * @author michalm
 */
public class DrtRequestCreator implements PassengerRequestCreator {
	private final String mode;
	private final EventsManager eventsManager;
	private final MobsimTimer timer;

	public DrtRequestCreator(String mode, EventsManager eventsManager, MobsimTimer timer) {
		this.mode = mode;
		this.eventsManager = eventsManager;
		this.timer = timer;
	}

	@Override
	public DrtRequest createRequest(Id<Request> id, MobsimPassengerAgent passenger, Link fromLink, Link toLink,
			double departureTime, double submissionTime) {
		// yyyy remove parameter MobsimPassengerAgent and get necessary info from somewhere else.
		// (Also in reality, such information is not pushed into the person, but stored somewhere on the provider side.)
		// kai, gregor, jan'19

		//XXX this will not work if pre-booking is allowed in DRT
		Leg leg = (Leg)((PlanAgent)passenger).getCurrentPlanElement();
		DrtRoute drtRoute = (DrtRoute)leg.getRoute();
		double latestDepartureTime = departureTime + drtRoute.getMaxWaitTime();
		double latestArrivalTime = departureTime + drtRoute.getTravelTime();

		eventsManager.processEvent(
				new DrtRequestSubmittedEvent(timer.getTimeOfDay(), mode, id, passenger.getId(), fromLink.getId(),
						toLink.getId(), drtRoute.getDirectRideTime(), drtRoute.getDistance()));

		return new DrtRequest(id, passenger.getId(), mode, fromLink, toLink, departureTime, latestDepartureTime,
				latestArrivalTime, submissionTime);
	}
}
