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

/**
 * 
 */
package playground.jbischoff.pt.taxi;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.mutable.MutableDouble;
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
 * @author  jbischoff
 *
 */
/**
 *
 */

public class DistanceBasedTaxiFare implements LinkEnterEventHandler, PersonEntersVehicleEventHandler,
		PersonDepartureEventHandler, PersonArrivalEventHandler {
	
	
	@Inject
	EventsManager events;
	@Inject
	Network network;
	
	private double distanceFare_Meter = 0.25 / 1000.0;
	private double baseFare = 0;
	
	Map<Id<Vehicle>,MutableDouble> currentRideDistance = new HashMap<>();
	Map<Id<Person>,Id<Vehicle>> currentVehicle = new HashMap<>();
	Set<Id<Person>> waitingPax = new HashSet<>();
	

	/* (non-Javadoc)
	 * @see org.matsim.core.events.handler.EventHandler#reset(int)
	 */
	@Override
	public void reset(int iteration) {
		waitingPax.clear();
		currentVehicle.clear();
		currentRideDistance.clear();
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler#handleEvent(org.matsim.api.core.v01.events.PersonArrivalEvent)
	 */
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (currentVehicle.containsKey(event.getPersonId())){
			Id<Vehicle> vid = currentVehicle.remove(event.getPersonId());
			double distance = currentRideDistance.remove(vid).doubleValue();
			double fare = -(baseFare + distance*distanceFare_Meter);
			events.processEvent(new PersonMoneyEvent(event.getTime(), event.getPersonId(), fare));
		}
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler#handleEvent(org.matsim.api.core.v01.events.PersonDepartureEvent)
	 */
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals("taxi")){
			waitingPax.add(event.getPersonId());
		}
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler#handleEvent(org.matsim.api.core.v01.events.PersonEntersVehicleEvent)
	 */
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (waitingPax.contains(event.getPersonId())){
			currentVehicle.put(event.getPersonId(), event.getVehicleId());
			currentRideDistance.put(event.getVehicleId(), new MutableDouble(0.0));
			waitingPax.remove(event.getPersonId());
		}
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.LinkEnterEventHandler#handleEvent(org.matsim.api.core.v01.events.LinkEnterEvent)
	 */
	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (currentRideDistance.containsKey(event.getVehicleId())){
			double length = network.getLinks().get(event.getLinkId()).getLength();
			currentRideDistance.get(event.getVehicleId()).add(length);
		}
	}

}
