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

import java.nio.channels.IllegalSelectorException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.FleetImpl;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class ScheduleReconstructor {
	final FleetImpl fleet = new FleetImpl();
	final Map<Id<Request>, TaxiRequest> taxiRequests = new LinkedHashMap<>();
	final Map<Id<Link>, ? extends Link> links;

	final Map<Id<Person>, ScheduleBuilder> scheduleBuilders = new LinkedHashMap<>();
	private boolean schedulesValidated = false;

	private final DriveRecorder driveRecorder;
	private final StayRecorder stayRecorder;
	private final RequestRecorder requestRecorder;

	@Inject
	public ScheduleReconstructor(@Named(DvrpRoutingNetworkProvider.DVRP_ROUTING) Network network,
			EventsManager eventsManager) {
		links = network.getLinks();

		driveRecorder = new DriveRecorder(this);
		eventsManager.addHandler(driveRecorder);

		stayRecorder = new StayRecorder(this);
		eventsManager.addHandler(stayRecorder);

		requestRecorder = new RequestRecorder(this, TransportMode.taxi);
		eventsManager.addHandler(requestRecorder);
	}

	Id<Person> getDriver(Id<Vehicle> vehicleId) {
		return Id.createPersonId(vehicleId);
	}

	ScheduleBuilder getBuilder(Id<Person> personId) {
		return scheduleBuilders.get(personId);
	}

	private void validateSchedules() {
		if (driveRecorder.hasOngoingDrives() || stayRecorder.hasOngoingStays()
				|| requestRecorder.hasAwaitingRequests()) {
			throw new IllegalStateException();
		}

		for (ScheduleBuilder sb : scheduleBuilders.values()) {
			if (!sb.isScheduleBuilt()) {
				throw new IllegalSelectorException();
			}
		}
	}

	public Fleet getFleet() {
		if (!schedulesValidated) {
			validateSchedules();
		}

		return fleet;
	}

	public static Fleet reconstructFromFile(Network network, String eventsFile) {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		ScheduleReconstructor reconstructor = new ScheduleReconstructor(network, eventsManager);
		new MatsimEventsReader(eventsManager).readFile(eventsFile);
		return reconstructor.getFleet();
	}
}
