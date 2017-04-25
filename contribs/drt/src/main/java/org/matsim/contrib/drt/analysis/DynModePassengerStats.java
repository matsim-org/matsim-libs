/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.contrib.drt.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

/**
 * @author jbischoff
 *
 */
public class DynModePassengerStats implements PersonEntersVehicleEventHandler, PersonDepartureEventHandler,
		PersonArrivalEventHandler, LinkEnterEventHandler, ActivityEndEventHandler {

	final private Map<Id<Person>, Double> departureTimes = new HashMap<>();
	final private Map<Id<Person>, Id<Link>> departureLinks = new HashMap<>();
	final private List<DynModeTrip> drtTrips = new ArrayList<>();
	final private Map<Id<Vehicle>, Map<Id<Person>, MutableDouble>> inVehicleDistance = new HashMap<>();
	final private Map<Id<Vehicle>, double[]> vehicleDistances = new HashMap<>();
	final private Map<Id<Person>, DynModeTrip> currentTrips = new HashMap<>();
	final String mode;

	final private Network network;
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.core.events.handler.EventHandler#reset(int)
	 */

	/**
	 * 
	 */
	@Inject
	public DynModePassengerStats(Network network, EventsManager events, Config config) {
		this.mode = ((DvrpConfigGroup)config.getModules().get(DvrpConfigGroup.GROUP_NAME)).getMode();
		this.network = network;
		events.addHandler(this);
	}

	public DynModePassengerStats(Network network, String mode) {
		this.mode = mode;
		this.network = network;
	}

	@Override
	public void reset(int iteration) {
		this.drtTrips.clear();
		departureTimes.clear();
		departureLinks.clear();
		inVehicleDistance.clear();
		currentTrips.clear();
		vehicleDistances.clear();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.api.core.v01.events.handler.ActivityEndEventHandler#handleEvent(org.matsim.api.core.v01.events.
	 * ActivityEndEvent)
	 */
	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getActType().equals(VrpAgentLogic.BEFORE_SCHEDULE_ACTIVITY_TYPE)) {
			Id<Vehicle> vid = Id.createVehicleId(event.getPersonId().toString());
			this.inVehicleDistance.put(vid, new HashMap<Id<Person>, MutableDouble>());
			this.vehicleDistances.put(vid, new double[3]);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.api.core.v01.events.handler.LinkEnterEventHandler#handleEvent(org.matsim.api.core.v01.events.
	 * LinkEnterEvent)
	 */
	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (inVehicleDistance.containsKey(event.getVehicleId())) {
			double distance = network.getLinks().get(event.getLinkId()).getLength();
			for (MutableDouble d : inVehicleDistance.get(event.getVehicleId()).values()) {
				d.add(distance);
			}
			this.vehicleDistances.get(event.getVehicleId())[0] += distance; // overall distance drive
			this.vehicleDistances.get(event.getVehicleId())[1] += distance
					* inVehicleDistance.get(event.getVehicleId()).size(); // overall revenue distance
			if (inVehicleDistance.get(event.getVehicleId()).size() > 0) {
				this.vehicleDistances.get(event.getVehicleId())[2] += distance; // overall occupied distance
			}
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler#handleEvent(org.matsim.api.core.v01.events.
	 * PersonArrivalEvent)
	 */
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (event.getLegMode().equals(mode)) {
			DynModeTrip trip = currentTrips.remove(event.getPersonId());
			if (trip != null) {
				double distance = inVehicleDistance.get(trip.getVehicle()).remove(event.getPersonId()).doubleValue();
				trip.setTravelDistance(distance);
				trip.setArrivalTime(event.getTime());
				trip.setToLink(event.getLinkId());
				Coord toCoord = this.network.getLinks().get(event.getLinkId()).getCoord();
				trip.setToCoord(toCoord);
				trip.setInVehicleTravelTime(event.getTime() - trip.getDepartureTime() - trip.getWaitTime());
			} else {
				throw new NullPointerException("Arrival without departure?");
			}
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler#handleEvent(org.matsim.api.core.v01.events.
	 * PersonDepartureEvent)
	 */
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals(mode)) {
			this.departureTimes.put(event.getPersonId(), event.getTime());
			this.departureLinks.put(event.getPersonId(), event.getLinkId());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler#handleEvent(org.matsim.api.core.v01.events
	 * .PersonEntersVehicleEvent)
	 */
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (this.departureTimes.containsKey(event.getPersonId())) {
			double departureTime = this.departureTimes.remove(event.getPersonId());
			double waitTime = event.getTime() - departureTime;
			Id<Link> departureLink = this.departureLinks.remove(event.getPersonId());
			Coord departureCoord = this.network.getLinks().get(departureLink).getCoord();
			DynModeTrip trip = new DynModeTrip(departureTime, event.getPersonId(), event.getVehicleId(), departureLink,
					departureCoord, waitTime);
			this.drtTrips.add(trip);
			this.currentTrips.put(event.getPersonId(), trip);
			this.inVehicleDistance.get(event.getVehicleId()).put(event.getPersonId(), new MutableDouble());
		}
	}

	/**
	 * @return the drtTrips
	 */
	public List<DynModeTrip> getDrtTrips() {
		return drtTrips;
	}

	/**
	 * @return the vehicleDistances
	 */
	public Map<Id<Vehicle>, double[]> getVehicleDistances() {
		return vehicleDistances;
	}
}
