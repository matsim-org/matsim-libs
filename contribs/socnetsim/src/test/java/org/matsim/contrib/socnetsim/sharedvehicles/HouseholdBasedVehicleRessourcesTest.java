/* *********************************************************************** *
 * project: org.matsim.*
 * HouseholdBasedVehicleRessourcesTest.java
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.households.Household;
import org.matsim.households.HouseholdImpl;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsImpl;
import org.matsim.vehicles.Vehicle;

/**
 * @author thibautd
 */
public class HouseholdBasedVehicleRessourcesTest {

	@Test
	void testVehiclesIdsAreCorrect() throws Exception {
		final Households households = createHouseholds();
		final VehicleRessources testee = new HouseholdBasedVehicleRessources( households );

		for (Household household : households.getHouseholds().values()) {
			final Set<Id<Vehicle>> expected = new HashSet<Id<Vehicle>>( household.getVehicleIds() );
			for (Id<Person> person : household.getMemberIds()) {
				final Set<Id<Vehicle>> actual = testee.identifyVehiclesUsableForAgent( person );
				assertEquals(
						expected,
						actual,
						"unexpected vehicles for agent "+person);
			}
		}
	}

	private static Households createHouseholds() {
		final HouseholdsImpl hhs = new HouseholdsImpl();

		int c = 0;
		int v = 0;
		// NO INTERFACE BASED WAY TO ADD MEMBERS ???????
		HouseholdImpl hh = (HouseholdImpl) hhs.getFactory().createHousehold( Id.create( "small" , Household.class) );
		hh.setMemberIds( Arrays.<Id<Person>>asList( Id.create( c++ , Person.class ) ) );
		hh.setVehicleIds( Collections.<Id<Vehicle>>emptyList() );
		hhs.addHousehold( hh );

		hh = (HouseholdImpl) hhs.getFactory().createHousehold( Id.create( "big" , Household.class) );
		hh.setMemberIds( Arrays.<Id<Person>>asList(
					Id.create( c++ , Person.class ),
					Id.create( c++ , Person.class ),
					Id.create( c++ , Person.class ),
					Id.create( c++ , Person.class ),
					Id.create( c++ , Person.class ),
					Id.create( c++ , Person.class ),
					Id.create( c++ , Person.class ),
					Id.create( c++ , Person.class )) );
		hh.setVehicleIds( Arrays.<Id<Vehicle>>asList( Id.create( v++ , Vehicle.class ) ) );
		hhs.addHousehold( hh );

		hh = (HouseholdImpl) hhs.getFactory().createHousehold( Id.create( "lots of vehicles" , Household.class) );
		hh.setMemberIds( Arrays.<Id<Person>>asList(
					Id.create( c++ , Person.class ),
					Id.create( c++ , Person.class ),
					Id.create( c++ , Person.class ),
					Id.create( c++ , Person.class ),
					Id.create( c++ , Person.class ),
					Id.create( c++ , Person.class ),
					Id.create( c++ , Person.class ),
					Id.create( c++ , Person.class )) );
		hh.setVehicleIds( Arrays.<Id<Vehicle>>asList(
					Id.create( v++ , Vehicle.class ),
					Id.create( v++ , Vehicle.class ),
					Id.create( v++ , Vehicle.class ),
					Id.create( v++ , Vehicle.class ),
					Id.create( v++ , Vehicle.class ),
					Id.create( v++ , Vehicle.class ),
					Id.create( v++ , Vehicle.class ),
					Id.create( v++ , Vehicle.class ),
					Id.create( v++ , Vehicle.class ),
					Id.create( v++ , Vehicle.class ),
					Id.create( v++ , Vehicle.class ),
					Id.create( v++ , Vehicle.class ),
					Id.create( v++ , Vehicle.class ),
					Id.create( v++ , Vehicle.class ),
					Id.create( v++ , Vehicle.class ),
					Id.create( v++ , Vehicle.class ),
					Id.create( v++ , Vehicle.class ),
					Id.create( v++ , Vehicle.class ),
					Id.create( v++ , Vehicle.class ),
					Id.create( v++ , Vehicle.class ),
					Id.create( v++ , Vehicle.class ),
					Id.create( v++ , Vehicle.class )) );
		hhs.addHousehold( hh );

		return hhs;
	}
}

