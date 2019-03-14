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
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

class AdvanceRequestStorage {
	private final Multimap<Id<Person>, PassengerRequest> advanceRequests = ArrayListMultimap.create();

	public void storeRequest(PassengerRequest request) {
		advanceRequests.put(request.getPassengerId(), request);
	}

	public void removeRequest(PassengerRequest request) {
		advanceRequests.remove(request.getPassengerId(), request);
	}

	public List<PassengerRequest> retrieveRequests(MobsimPassengerAgent passenger, Id<Link> fromLinkId,
			Id<Link> toLinkId) {
		Collection<PassengerRequest> allRequests = advanceRequests.get(passenger.getId());
		List<PassengerRequest> filteredRequests = advanceRequests.get(passenger.getId())
				.stream()
				.filter(r -> r.getFromLink().getId().equals(fromLinkId) && r.getToLink().getId().equals(toLinkId))
				.collect(Collectors.toList());
		allRequests.removeAll(filteredRequests);
		return filteredRequests;
	}
}
