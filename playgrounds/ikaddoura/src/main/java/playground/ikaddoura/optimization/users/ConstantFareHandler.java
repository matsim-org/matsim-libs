/* *********************************************************************** *
 * project: org.matsim.*
 * MoneyThrowEventHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.ikaddoura.optimization.users;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.vehicles.Vehicle;

/**
 * @author Ihab
 *
 */
public class ConstantFareHandler implements PersonEntersVehicleEventHandler, TransitDriverStartsEventHandler {

	private final EventsManager events;
	private final double fare;
	private final List<Id<Person>> ptDriverIDs = new ArrayList<>();
	private final List<Id<Vehicle>> ptVehicleIDs = new ArrayList<>();

	public ConstantFareHandler(EventsManager events, double fare) {
		this.events = events;
		this.fare = fare;
	}
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		Id<Person> personId = event.getPersonId();
		Id<Vehicle> vehId = event.getVehicleId();
		if (!ptDriverIDs.contains(personId) && ptVehicleIDs.contains(vehId)){
			double fareForTrip = calculateFare(event);
			if (fareForTrip == 0.){
				// not processing zero money events
			} else {
				PersonMoneyEvent moneyEvent = new PersonMoneyEvent(event.getTime(), event.getPersonId(), fareForTrip);
				this.events.processEvent(moneyEvent);
			}
		}
	}

	// this method needs to be extended when differentiated fares apply.
	private double calculateFare(PersonEntersVehicleEvent event) {
		return this.fare;
	}

	@Override
	public void reset(int iteration) {
		this.ptDriverIDs.clear();
		this.ptVehicleIDs.clear();
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		
		if (!this.ptDriverIDs.contains(event.getDriverId())){
			this.ptDriverIDs.add(event.getDriverId());
		}
		
		if (!this.ptVehicleIDs.contains(event.getVehicleId())){
			this.ptVehicleIDs.add(event.getVehicleId());
		}
	}
}