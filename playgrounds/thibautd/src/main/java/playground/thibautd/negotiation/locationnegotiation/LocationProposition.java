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
package playground.thibautd.negotiation.locationnegotiation;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.ActivityFacility;
import playground.thibautd.negotiation.framework.Proposition;

import java.util.Collection;

/**
 * @author thibautd
 */
public class LocationProposition implements Proposition {
	private final Id<Person> proposer;
	private final Collection<Id<Person>> proposed;

	private final ActivityFacility facility;

	public LocationProposition( final Id<Person> proposer,
			final Collection<Id<Person>> proposed,
			final ActivityFacility facility ) {
		this.proposer = proposer;
		this.proposed = proposed;
		this.facility = facility;
	}

	@Override
	public Id<Person> getProposerId() {
		return proposer;
	}

	@Override
	public Collection<Id<Person>> getProposedIds() {
		return proposed;
	}

	public ActivityFacility getFacility() {
		return facility;
	}
}
