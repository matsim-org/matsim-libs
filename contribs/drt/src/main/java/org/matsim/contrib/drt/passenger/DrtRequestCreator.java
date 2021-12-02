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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.utils.misc.OptionalTime;

/**
 * @author michalm
 */
public class DrtRequestCreator implements PassengerRequestCreator {
	private static final Logger log = Logger.getLogger(DrtRequestCreator.class);
	private final String mode;
	private final EventsManager eventsManager;
	private final TimeConstraintCalculator constraintCalculator;

	public DrtRequestCreator(String mode, EventsManager eventsManager, TimeConstraintCalculator constraintCalculator) {
		this.mode = mode;
		this.eventsManager = eventsManager;
		this.constraintCalculator = constraintCalculator;
	}

	@Override
	public DrtRequest createRequest(Id<Request> id, Id<Person> passengerId, Route route, Link fromLink, Link toLink,
			double departureTime, double submissionTime) {
		DrtRoute drtRoute = (DrtRoute)route;

		TimeConstraints constraints = constraintCalculator.calculateConstraints(id, passengerId, drtRoute, fromLink,
				toLink, departureTime, submissionTime);

		double latestDepartureTime = constraints.getLatestDepartureTime()
				.orElse(departureTime + drtRoute.getMaxWaitTime());
		double latestArrivalTime = constraints.getLatestArrivalTime()
				.orElse(departureTime + drtRoute.getTravelTime().seconds());

		eventsManager.processEvent(
				new DrtRequestSubmittedEvent(submissionTime, mode, id, passengerId, fromLink.getId(), toLink.getId(),
						drtRoute.getDirectRideTime(), drtRoute.getDistance()));

		DrtRequest request = DrtRequest.newBuilder()
				.id(id)
				.passengerId(passengerId)
				.mode(mode)
				.fromLink(fromLink)
				.toLink(toLink)
				.earliestStartTime(departureTime)
				.latestStartTime(latestDepartureTime)
				.latestArrivalTime(latestArrivalTime)
				.submissionTime(submissionTime)
				.build();

		log.debug(route);
		log.debug(request);
		return request;
	}
	

	public interface TimeConstraintCalculator {
		TimeConstraints calculateConstraints(Id<Request> id, Id<Person> passengerId, DrtRoute route, Link fromLink,
				Link toLink, double departureTime, double submissionTime);
	}
	
	public static final TimeConstraintCalculator DEFAULT_TIME_CONSTRAINT_CALCULATOR = (id, passengerId, route, fromLink, toLink,
			departureTime, submissionTime) -> new TimeConstraints(OptionalTime.undefined(), OptionalTime.undefined());

	public static class TimeConstraints {
		private final OptionalTime latestDepartureTime;
		private final OptionalTime latestArrivalTime;

		public TimeConstraints(OptionalTime latestDepartureTime, OptionalTime latestArrivalTime) {
			this.latestDepartureTime = latestDepartureTime;
			this.latestArrivalTime = latestArrivalTime;
		}

		public OptionalTime getLatestDepartureTime() {
			return latestDepartureTime;
		}

		public OptionalTime getLatestArrivalTime() {
			return latestArrivalTime;
		}
	}
}
