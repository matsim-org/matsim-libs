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

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.optimizer.Request;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

class AdvanceRequestStorage {
	private final Multimap<Id<Person>, PassengerRequest> advanceRequests = ArrayListMultimap.create();

	void storeRequest(PassengerRequest request) {
		advanceRequests.put(request.getPassengerId(), request);
	}

	//XXX it should be enough to provide only requestId (consider replacing Multimap with Table)
	boolean removeRequest(Id<Person> passengerId, Id<Request> requestId) {
		return advanceRequests.get(passengerId).removeIf(req -> req.getId().equals(requestId));
	}

	List<PassengerRequest> retrieveRequests(Id<Person> passengerId, Id<Link> fromLinkId, Id<Link> toLinkId) {
		Collection<PassengerRequest> allRequests = advanceRequests.get(passengerId);
		List<PassengerRequest> filteredRequests = advanceRequests.get(passengerId)
				.stream()
				.filter(r -> r.getFromLink().getId().equals(fromLinkId) && r.getToLink().getId().equals(toLinkId))
				.collect(Collectors.toList());
		allRequests.removeAll(filteredRequests);
		return filteredRequests;
	}
}
