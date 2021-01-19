/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C) 2021 by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.contrib.freight.controler;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

import java.util.TreeSet;

/**
 * This will create lists of the
 *  1) Agents Ids
 *  2) Vehicle Ids
 *  found in the PersonEntersVehicleEvents.
 */
class DriverAndVehicleIdFromEventsHandlerForTest implements PersonEntersVehicleEventHandler {

	private TreeSet<Id<Person>> setOfDriverIds = new TreeSet<>();
	private TreeSet<Id<Vehicle>> setOfVehicleIds = new TreeSet<>();


	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		setOfDriverIds.add(event.getPersonId());
		setOfVehicleIds.add(event.getVehicleId());
	}

	public TreeSet<Id<Person>> getSetOfDriverIds() {
		return setOfDriverIds;
	}

	public TreeSet<Id<Vehicle>> getSetOfVehicleIds() {
		return setOfVehicleIds;
	}

}
