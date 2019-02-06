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

package org.matsim.contrib.taxi.schedule.reconstruct;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.fleet.FleetImpl;
import org.matsim.contrib.taxi.passenger.TaxiRequest;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.vehicles.Vehicle;

public class ScheduleReconstructor {
	private final FleetImpl fleet = new FleetImpl();
	final Map<Id<Request>, TaxiRequest> taxiRequests = new LinkedHashMap<>();
	final Map<Id<Link>, ? extends Link> links;

	final Map<Id<Person>, ScheduleBuilder> scheduleBuilders = new LinkedHashMap<>();
	private boolean schedulesValidatedAndVehiclesAddedToFleet = false;

	private final DriveRecorder driveRecorder;
	private final StayRecorder stayRecorder;
	private final RequestRecorder requestRecorder;

	public ScheduleReconstructor(Network network, EventsManager eventsManager, String mode) {
		links = network.getLinks();

		driveRecorder = new DriveRecorder(this);
		eventsManager.addHandler(driveRecorder);

		stayRecorder = new StayRecorder(this);
		eventsManager.addHandler(stayRecorder);

		requestRecorder = new RequestRecorder(this, mode);
		eventsManager.addHandler(requestRecorder);
	}

	Id<Person> getDriver(Id<Vehicle> vehicleId) {
		return Id.createPersonId(vehicleId);
	}

	ScheduleBuilder getBuilder(Id<Person> personId) {
		return scheduleBuilders.get(personId);
	}

	private void validateSchedulesAndAddVehiclesToFleet() {
		if (driveRecorder.hasOngoingDrives()
				|| stayRecorder.hasOngoingStays()
				|| requestRecorder.hasAwaitingRequests()) {
			throw new IllegalStateException();
		}

		scheduleBuilders.values().stream().map(ScheduleBuilder::getVehicle).forEach(fleet::addVehicle);
	}

	public Fleet getFleet() {
		if (!schedulesValidatedAndVehiclesAddedToFleet) {
			validateSchedulesAndAddVehiclesToFleet();
		}

		return fleet;
	}

	public static Fleet reconstructFromFile(Network network, String eventsFile, String mode) {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		ScheduleReconstructor reconstructor = new ScheduleReconstructor(network, eventsManager, mode);
		new MatsimEventsReader(eventsManager).readFile(eventsFile);
		return reconstructor.getFleet();
	}
}
