/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxi.schedule.reconstruct;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.taxi.passenger.TaxiRequest;

/**
 * @author michalm
 */
class TaxiRequestWithModifiableToLink extends TaxiRequest {
	public TaxiRequestWithModifiableToLink(Id<Request> id, Id<Person> passengerId, String mode, Link fromLink,
			double time) {
		super(id, passengerId, mode, fromLink, null, time, time);
	}

	private Link toLink;

	@Override
	public Link getToLink() {
		return toLink;
	}

	void setToLink(Link toLink) {
		this.toLink = toLink;
	}
}
