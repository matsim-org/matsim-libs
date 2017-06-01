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
package org.matsim.contrib.analysis.vsp.traveltimes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class CarTripsExtractor implements PersonDepartureEventHandler, PersonArrivalEventHandler {

	
	private final Set<Id<Person>> personsWithPlan;
	private final Network network;
	private final Map<Id<Person>,Double> lastDepartureTime = new HashMap<>();
	private final Map<Id<Person>,Coord> lastDepartureLocation = new HashMap<>();
	
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
				CarTrip trip = new CarTrip(event.getPersonId(), departureTime, event.getTime(), departureCoord, arrivalCoord);
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
}
	
