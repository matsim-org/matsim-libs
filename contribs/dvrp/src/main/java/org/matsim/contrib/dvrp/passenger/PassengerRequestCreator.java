/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.dvrp.optimizer.Request;

import java.util.List;

/**
 * @author michalm
 */
public interface PassengerRequestCreator {
	/**
	 * Thread safety: This method can be called concurrently from multiple QSim worker threads.
	 * Prefer stateless implementation, otherwise provide other ways to achieve thread-safety.
	 *
	 * @param id             request ID
	 * @param passengerIds   list of unique passenger IDs
	 * @param route          planned route (the required route type depends on the optimizer)
	 * @param fromLink       start location
	 * @param toLink         end location
	 * @param departureTime  requested time of departure
	 * @param submissionTime time at which request was submitted
	 * @return
	 */
	PassengerRequest createRequest(Id<Request> id, List<Id<Person>> passengerIds, Route route, Link fromLink, Link toLink,
								   double departureTime, double submissionTime);
}
