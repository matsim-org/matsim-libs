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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.core.api.experimental.events.EventsManager;

import java.util.List;

/**
 * @author michalm
 */
public class DrtRequestCreator implements PassengerRequestCreator {
	private static final Logger log = LogManager.getLogger(DrtRequestCreator.class);
	private final String mode;
	private final EventsManager eventsManager;

	public DrtRequestCreator(String mode, EventsManager eventsManager) {
		this.mode = mode;
		this.eventsManager = eventsManager;
	}

	@Override
	public DrtRequest createRequest(Id<Request> id, List<Id<Person>> passengerIds, Route route, Link fromLink, Link toLink,
									double departureTime, double submissionTime) {
		DrtRoute drtRoute = (DrtRoute)route;
		double latestDepartureTime = departureTime + drtRoute.getMaxWaitTime();
		double latestArrivalTime = departureTime + drtRoute.getTravelTime().seconds();

		eventsManager.processEvent(
				new DrtRequestSubmittedEvent(submissionTime, mode, id, passengerIds, fromLink.getId(), toLink.getId(),
						drtRoute.getDirectRideTime(), drtRoute.getDistance(), departureTime, latestDepartureTime, latestArrivalTime));

		DrtRequest request = DrtRequest.newBuilder()
				.id(id)
				.passengerIds(passengerIds)
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
}
