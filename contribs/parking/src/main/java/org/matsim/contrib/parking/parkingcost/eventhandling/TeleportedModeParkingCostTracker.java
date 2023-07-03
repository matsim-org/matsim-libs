/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2022 by the members listed in the COPYING,        *
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

package org.matsim.contrib.parking.parkingcost.eventhandling;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.parkingcost.config.ParkingCostConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;

import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author mrieser, jfbischoff (SBB)
 */
public class TeleportedModeParkingCostTracker implements PersonArrivalEventHandler, ActivityEndEventHandler {

	private final static Logger log = LogManager.getLogger(MainModeParkingCostVehicleTracker.class);

	private final String parkingCostAttributeName;
	private final Map<Id<Person>, Double> arrivalsPerPerson = new HashMap<>();
	private final String trackedMode;
	private final Set<String> untrackedActivities;
	private final String purpose;
	@Inject
	EventsManager events;
	@Inject
	Network network;
	private boolean badAttributeTypeWarningShown = false;

	public TeleportedModeParkingCostTracker(String mode, ParkingCostConfigGroup parkingCostConfigGroup) {
		this.untrackedActivities = parkingCostConfigGroup.getActivityTypesWithoutParkingCost();
		this.parkingCostAttributeName = parkingCostConfigGroup.linkAttributePrefix + mode;
		this.trackedMode = mode;
		this.purpose = mode + " parking cost";

	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (trackedMode.equals(event.getLegMode())) {
			this.arrivalsPerPerson.put(event.getPersonId(), event.getTime());
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getActType().endsWith("interaction")) {
			// do nothing in the case of a stageActivity
			return;
		}

		Double arrivalTime = this.arrivalsPerPerson.remove(event.getPersonId());
		if (arrivalTime == null ||
				(this.untrackedActivities.stream().anyMatch(s -> event.getActType().contains(s)))) {
			return;
		}

		Link link = network.getLinks().get(event.getLinkId());
		Object value = link.getAttributes().getAttribute(this.parkingCostAttributeName);
		if (value == null) {
			return;
		}
		if (value instanceof Number n) {
			double parkDuration = event.getTime() - arrivalTime;
			double hourlyParkingCost = n.doubleValue();
			double parkingCost = hourlyParkingCost * (parkDuration / 3600.0);
			this.events.processEvent(new PersonMoneyEvent(event.getTime(), event.getPersonId(), -parkingCost, purpose, null, link.getId().toString()));
		} else if (!this.badAttributeTypeWarningShown) {
			log.error("Ride-ParkingCost attribute must be of type Double or Integer, but is of type " + value.getClass() + ". This message is only given once.");
			this.badAttributeTypeWarningShown = true;
		}
	}

	@Override
	public void reset(int iteration) {
		this.arrivalsPerPerson.clear();
	}
}
