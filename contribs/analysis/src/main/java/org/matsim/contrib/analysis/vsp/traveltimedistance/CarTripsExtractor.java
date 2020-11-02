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
package org.matsim.contrib.analysis.vsp.traveltimedistance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class CarTripsExtractor implements PersonDepartureEventHandler, PersonArrivalEventHandler, PersonEntersVehicleEventHandler, LinkEnterEventHandler {

	
	private final Set<Id<Person>> personsWithPlan;
	private final Network network;
	private final Map<Id<Person>,Double> lastDepartureTime = new HashMap<>();
	private final Map<Id<Person>,Coord> lastDepartureLocation = new HashMap<>();
	private final Map<Id<Person>,Id<Vehicle>> vehicle2pax  = new HashMap<>();;
	private final Map<Id<Vehicle>,Double> distanceTraveled = new HashMap<>();
	private final List<CarTrip> trips = new ArrayList<>();
	
	
	public CarTripsExtractor(Set<Id<Person>> personsWithPlan, Network network) {
		this.personsWithPlan = personsWithPlan;
		this.network = network;
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.events.handler.EventHandler#reset(int)
	 */
	@Override
	public void reset(int iteration) {
		lastDepartureLocation.clear();
		lastDepartureTime.clear();
		vehicle2pax.clear();
		distanceTraveled.clear();
		trips.clear();
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler#handleEvent(org.matsim.api.core.v01.events.PersonArrivalEvent)
	 */
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (event.getLegMode().equals(TransportMode.car)){
			if (this.personsWithPlan.contains(event.getPersonId()))
			{
				Coord arrivalCoord = network.getLinks().get(event.getLinkId()).getCoord();
				Coord departureCoord = this.lastDepartureLocation.remove(event.getPersonId());
				double departureTime = this.lastDepartureTime.remove(event.getPersonId());
				Id<Vehicle> vehicleId = vehicle2pax.remove(event.getPersonId());
				double distance = this.distanceTraveled.remove(vehicleId);
				CarTrip trip = new CarTrip(event.getPersonId(), departureTime, event.getTime(), distance, departureCoord, arrivalCoord);
				trip.setActualTravelTime(event.getTime()-departureTime);
				this.trips.add(trip);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler#handleEvent(org.matsim.api.core.v01.events.PersonDepartureEvent)
	 */
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals(TransportMode.car)){
			if (this.personsWithPlan.contains(event.getPersonId()))
			{
				Coord departureCoord = network.getLinks().get(event.getLinkId()).getCoord();
				this.lastDepartureLocation.put(event.getPersonId(), departureCoord );
				this.lastDepartureTime.put(event.getPersonId(), event.getTime());

			}
		}
	}
	/**
	 * @return the trips
	 */
	public List<CarTrip> getTrips() {
		return trips;
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.LinkEnterEventHandler#handleEvent(org.matsim.api.core.v01.events.LinkEnterEvent)
	 */
	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (distanceTraveled.containsKey(event.getVehicleId())){
			double length =  network.getLinks().get(event.getLinkId()).getLength();
			double distance = distanceTraveled.get(event.getVehicleId()) + length;
			distanceTraveled.put(event.getVehicleId(), distance);
		}
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler#handleEvent(org.matsim.api.core.v01.events.PersonEntersVehicleEvent)
	 */
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (this.lastDepartureTime.containsKey(event.getPersonId())){
			this.vehicle2pax.put(event.getPersonId(), event.getVehicleId());
			this.distanceTraveled.put(event.getVehicleId(), 0.0);
		}
	}
}
	
