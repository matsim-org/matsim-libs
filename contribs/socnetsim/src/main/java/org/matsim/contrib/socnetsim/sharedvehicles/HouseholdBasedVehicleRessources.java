/* *********************************************************************** *
 * project: org.matsim.*
 * HouseholdBasedVehicleRessources.java
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
package org.matsim.contrib.socnetsim.sharedvehicles;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.vehicles.Vehicle;

/**
 * @author thibautd
 */
public class HouseholdBasedVehicleRessources implements VehicleRessources {
	private final Households households;
	private final Map<Id, Id> agent2hh = new LinkedHashMap<Id, Id>();

	public HouseholdBasedVehicleRessources(
			final Households hhs) {
		this.households = hhs;

		for (Household hh : hhs.getHouseholds().values()) {
			final Id hhId = hh.getId();
			for (Id agentId : hh.getMemberIds()) {
				agent2hh.put( agentId , hhId );
			}
		}
	}

	@Override
	public Set<Id<Vehicle>> identifyVehiclesUsableForAgent(final Id<Person> person) {
		// we can't simply remember an agent->hh mapping at construction,
		// as the households container is mutable. It would not make any sense
		// to modify it, but one is never too prudent.
		final Id<Household> hhId = agent2hh.get( person );
		if ( hhId == null ) throw new RuntimeException( "no household known for "+person );

		final Household hh = households.getHouseholds().get( hhId );
		if ( hh == null ) throw new RuntimeException( "household "+hhId+" vanished!" );
		if ( !hh.getMemberIds().contains( person ) ) throw new RuntimeException( "household "+hhId+" does not contain "+person );

		return new HashSet<Id<Vehicle>>( hh.getVehicleIds() );
	}
}

