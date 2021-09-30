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

package org.matsim.contrib.taxi.passenger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.contrib.dvrp.passenger.PassengerRequestSubmittedEvent;
import org.matsim.core.api.experimental.events.EventsManager;

/**
 * @author michalm
 */
public class TaxiRequestCreator implements PassengerRequestCreator {
	private final String mode;
	private final EventsManager eventsManager;

	public TaxiRequestCreator(String mode, EventsManager eventsManager) {
		this.mode = mode;
		this.eventsManager = eventsManager;
	}

	@Override
	public TaxiRequest createRequest(Id<Request> id, Id<Person> passengerId, Route route, Link fromLink, Link toLink,
			double departureTime, double submissionTime) {
		eventsManager.processEvent(
				new PassengerRequestSubmittedEvent(submissionTime, mode, id, passengerId, fromLink.getId(),
						toLink.getId()));

		return new TaxiRequest(id, passengerId, mode, fromLink, toLink, departureTime, submissionTime);
	}
}
