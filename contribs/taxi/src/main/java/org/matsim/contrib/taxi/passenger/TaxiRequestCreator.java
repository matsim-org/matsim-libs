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
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;

import com.google.inject.Inject;

/**
 * @author michalm
 */
public class TaxiRequestCreator implements PassengerRequestCreator {
	@Inject(optional = true)
	private SubmittedTaxiRequestsCollector requestsCollector;

	@Override
	public TaxiRequest createRequest(Id<Request> id, MobsimPassengerAgent passenger, Link fromLink, Link toLink,
			double departureTime, double submissionTime) {
		TaxiRequest request = new TaxiRequest(id, passenger, fromLink, toLink, departureTime, submissionTime);
		if (requestsCollector != null) {
			requestsCollector.addRequest(request);
		}
		return request;
	}
}
