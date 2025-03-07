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

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.dvrp.load.DvrpLoad;
import org.matsim.contrib.dvrp.load.DvrpLoadType;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.core.api.experimental.events.EventsManager;

import com.google.common.base.Preconditions;

/**
 * @author michalm
 */
public class DrtRequestCreator implements PassengerRequestCreator {
	private static final Logger log = LogManager.getLogger(DrtRequestCreator.class);
	private final String mode;
	private final EventsManager eventsManager;
	private final DvrpLoadType dvrpLoadType;
	private final DvrpLoad emptyLoad;

	public DrtRequestCreator(String mode, EventsManager eventsManager, DvrpLoadType dvrpLoadType) {
		this.mode = mode;
		this.eventsManager = eventsManager;
		this.dvrpLoadType = dvrpLoadType;
		this.emptyLoad = dvrpLoadType.getEmptyLoad();
	}

	@Override
	public DrtRequest createRequest(Id<Request> id, List<Id<Person>> passengerIds, List<Route> routes, Link fromLink, Link toLink,
									double departureTime, double submissionTime) {
		double latestDepartureTime = Double.POSITIVE_INFINITY;
		double latestArrivalTime = Double.POSITIVE_INFINITY;
		double maxRideDuration = Double.POSITIVE_INFINITY;
		DvrpLoad load = emptyLoad;

		Preconditions.checkArgument(!passengerIds.isEmpty());
		for (Route route : routes) {
			DrtRoute drtRoute = (DrtRoute)route;
			latestDepartureTime = Math.min(latestDepartureTime, departureTime + drtRoute.getMaxWaitTime());
			latestArrivalTime = Math.min(latestArrivalTime, departureTime + drtRoute.getTravelTime().seconds());
			maxRideDuration = Math.min(drtRoute.getMaxRideTime(), maxRideDuration);
			load = load.add(drtRoute.getLoad(dvrpLoadType));
		}

		// get one representative route, we assume that distance and direct ride time are equivalent
		DrtRoute drtRoute = (DrtRoute) routes.get(0);
		String serializedLoad = this.dvrpLoadType.serialize(load);

		eventsManager.processEvent(
				new DrtRequestSubmittedEvent(submissionTime, mode, id, passengerIds, fromLink.getId(), toLink.getId(),
						drtRoute.getDirectRideTime(), drtRoute.getDistance(), departureTime, latestDepartureTime, latestArrivalTime, maxRideDuration, load, serializedLoad));

		DrtRequest request = DrtRequest.newBuilder()
				.id(id)
				.passengerIds(passengerIds)
				.mode(mode)
				.fromLink(fromLink)
				.toLink(toLink)
				.earliestStartTime(departureTime)
				.latestStartTime(latestDepartureTime)
				.latestArrivalTime(latestArrivalTime)
				.maxRideDuration(maxRideDuration)
				.submissionTime(submissionTime)
				.load(load)
				.build();

		log.debug(drtRoute);
		log.debug(request);
		return request;
	}
}
