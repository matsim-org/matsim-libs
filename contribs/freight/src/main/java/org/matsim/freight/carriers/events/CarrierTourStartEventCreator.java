/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
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

package org.matsim.freight.carriers.events;

import java.util.TreeMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierConstants;
import org.matsim.freight.carriers.ScheduledTour;
import org.matsim.vehicles.Vehicle;

/*package-private*/  final class CarrierTourStartEventCreator implements CarrierEventCreator {

	final TreeMap<Id<Person>, ActivityEndEvent> endEventMap = new TreeMap<>();
	final TreeMap<Id<Person>, PersonEntersVehicleEvent> personEntersVehicleEventMap = new TreeMap<>();


	@Override
	public Event createEvent(Event event, Carrier carrier, Activity activity, ScheduledTour scheduledTour, int activityCounter, Id<Vehicle> vehicleId) {
		//temporarily store some information, because ActivityEndEvent does not have any information about the person's vehicle.
		if((event instanceof ActivityEndEvent endEvent) && CarrierConstants.START.equals(endEvent.getActType()) ) {
			final Id<Person> personId = endEvent.getPersonId();
			endEventMap.put(personId, endEvent);

			//it is unclear in which order the events arrive in the events stream, when they have the same time step ->Check if TourStartsEvent should be thrown now.
			if (personEntersVehicleEventMap.containsKey(personId) && endEventMap.containsKey(personId)) {
				return createFreightTourStartsEvent(personId, carrier, scheduledTour);
			}
		}

		if (event instanceof PersonEntersVehicleEvent personEntersVehicleEvent) { //now we have the persons vehicle
			final Id<Person> personId = personEntersVehicleEvent.getPersonId();
			personEntersVehicleEventMap.put(personId, personEntersVehicleEvent);

			//it is unclear in which order the events arrive in the events stream, when they have the same time step ->Check if TourStartsEvent should be thrown now.
			if (personEntersVehicleEventMap.containsKey(personId) && endEventMap.containsKey(personId)) {
				return createFreightTourStartsEvent(personId, carrier, scheduledTour);
			}
		}
		return null;
	}


	/**
	 * Creating the FreightTourStartsEvent
	 *
	 * @param personId id of the driver (person)
	 * @param carrier the carrier
	 * @param scheduledTour the scheduledTour
	 * @return CarrierTourStartEvent
	 */
	private CarrierTourStartEvent createFreightTourStartsEvent(Id<Person> personId, Carrier carrier, ScheduledTour scheduledTour) {
		assert endEventMap.containsKey(personId);
		final var endEvent = endEventMap.get(personId);

		assert personEntersVehicleEventMap.containsKey(personId);
		final var entersVehicleEvent = personEntersVehicleEventMap.get(personId);

		//See if the events are corresponding by check if they are thrown at the same time
		if (endEvent.getTime() == entersVehicleEvent.getTime()) {
			//remove them from the maps, so we have a clean state for the next iteration
			endEventMap.remove(personId);
			personEntersVehicleEventMap.remove(personId);
			// TODO: If we have the tourId, we do not need to store the link here, kmt sep 22
			return new CarrierTourStartEvent(endEvent.getTime(), carrier.getId(), scheduledTour.getTour().getStartLinkId(), entersVehicleEvent.getVehicleId(), scheduledTour.getTour().getId());
		}
		return null;
	}

}
