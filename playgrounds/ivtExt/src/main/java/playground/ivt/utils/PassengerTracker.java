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
package playground.ivt.utils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.vehicles.Vehicle;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Simple helper class to keep track of who is in which vehicle
 * @author thibautd
 */
public class PassengerTracker implements
			PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler,
			VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {
	private final Map<Id<Vehicle>, Set<Id<Person>>> passengers = new HashMap<>();
	private final Map<Id<Vehicle>, Id<Person>> drivers = new HashMap<>();

	@Override
	public void handleEvent( final PersonEntersVehicleEvent event ) {
		MapUtils.getSet(
				event.getVehicleId(),
				passengers ).add(
						event.getPersonId() );
	}

	@Override
	public void handleEvent( final PersonLeavesVehicleEvent event ) {
		MapUtils.getSet(
				event.getVehicleId(),
				passengers ).remove(
				event.getPersonId() );
	}

	@Override
	public void reset( int iteration ) {
		passengers.clear();
		drivers.clear();
	}

	@Override
	public void handleEvent( VehicleEntersTrafficEvent event ) {
		drivers.put( event.getVehicleId(), event.getPersonId() );

		MapUtils.getSet(
				event.getVehicleId(),
				passengers ).remove(
				event.getPersonId() );
	}

	@Override
	public void handleEvent( VehicleLeavesTrafficEvent event ) {
		drivers.remove( event.getVehicleId() );
	}

	public Set<Id<Person>> getPassengers( final Id<Vehicle> v ) {
		final Set<Id<Person>> p = passengers.get( v );
		return p != null ? p : Collections.<Id<Person>>emptySet();
	}

	public Id<Person> getDriver( final Id<Vehicle> v ) {
		return drivers.get( v );
	}
}
