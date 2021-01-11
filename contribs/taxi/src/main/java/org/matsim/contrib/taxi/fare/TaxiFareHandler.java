/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.taxi.fare;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

/**
 * @author jbischoff
 * A simple implementation for taxi fares.
 * Note that these fares are scored in excess to anything set in the modeparams in the config file.
 */
public class TaxiFareHandler
		implements LinkEnterEventHandler, PersonEntersVehicleEventHandler, PersonDepartureEventHandler,
		PersonArrivalEventHandler {

	@Inject
	private EventsManager events;
	@Inject
	private Network network;

	private final double distanceFare_Meter;
	private final double baseFare;
	private final double minFarePerTrip;
	private final double timeFare_hour;
	private final double dailyFee;

	private final Map<Id<Vehicle>, MutableDouble> currentRideDistance = new HashMap<>();
	private final Map<Id<Person>, Id<Vehicle>> currentVehicle = new HashMap<>();
	private final Set<Id<Person>> waitingPax = new HashSet<>();
	private final Map<Id<Person>, Double> vehicleEnterTime = new HashMap<>();
	private final Set<Id<Person>> dailyFeeCharged = new HashSet<>();
	private final String mode;

	public TaxiFareHandler(String mode, TaxiFareParams taxiFareParams) {
		this.distanceFare_Meter = taxiFareParams.getDistanceFare_m();
		this.baseFare = taxiFareParams.getBasefare();
		this.minFarePerTrip = taxiFareParams.getMinFarePerTrip();
		this.dailyFee = taxiFareParams.getDailySubscriptionFee();
		this.timeFare_hour = taxiFareParams.getTimeFare_h();
		this.mode = mode;
	}

	TaxiFareHandler(String mode, TaxiFareParams taxiFareParams, Network network, EventsManager events) {
		this(mode, taxiFareParams);
		this.network = network;
		this.events = events;

	}

	@Override
	public void reset(int iteration) {
		waitingPax.clear();
		currentVehicle.clear();
		currentRideDistance.clear();
		dailyFeeCharged.clear();
		vehicleEnterTime.clear();
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (currentVehicle.containsKey(event.getPersonId())) {
			Id<Vehicle> vid = currentVehicle.remove(event.getPersonId());
			double distance = currentRideDistance.remove(vid).doubleValue();
			double rideTime = (event.getTime() - vehicleEnterTime.remove(event.getPersonId())) / 3600.0;
			double fare = baseFare + distance * distanceFare_Meter + rideTime * timeFare_hour;
			if (fare < minFarePerTrip) {
				fare = minFarePerTrip;
			}
			events.processEvent(new PersonMoneyEvent(event.getTime(), event.getPersonId(), -fare, "taxiFare", mode));
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals(mode)) {
			waitingPax.add(event.getPersonId());
			if (!dailyFeeCharged.contains(event.getPersonId())) {
				dailyFeeCharged.add(event.getPersonId());
				events.processEvent(
						new PersonMoneyEvent(event.getTime(), event.getPersonId(), -dailyFee, "taxiFare", mode));
			}
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (waitingPax.contains(event.getPersonId())) {
			currentVehicle.put(event.getPersonId(), event.getVehicleId());
			currentRideDistance.put(event.getVehicleId(), new MutableDouble(0.0));
			waitingPax.remove(event.getPersonId());
			vehicleEnterTime.put(event.getPersonId(), event.getTime());
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (currentRideDistance.containsKey(event.getVehicleId())) {
			double length = network.getLinks().get(event.getLinkId()).getLength();
			currentRideDistance.get(event.getVehicleId()).add(length);
		}
	}
}
