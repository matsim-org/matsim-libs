/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package tutorial.programming.example21tutorialTUBclass.class2016.events;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

public class CarTravelDistanceEvaluator implements LinkEnterEventHandler, PersonDepartureEventHandler,
		PersonArrivalEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {

	private Network network;
	private Map<Id<Person>,Double> travelledDistance = new HashMap<>(); 
	private Map<Id<Vehicle>,Id<Person>> vehicles2Persons = new HashMap<>();
	
	private int[] distanceDistribution = new int[30];
	
	public CarTravelDistanceEvaluator(Network network) {
		this.network = network;
	}
	
	@Override
	public void reset(int iteration) {
		distanceDistribution = new int[30];
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (travelledDistance.containsKey(event.getPersonId())){
			vehicles2Persons.put(event.getVehicleId(), event.getPersonId());
		}
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (this.travelledDistance.containsKey(event.getPersonId())){
			double distance = this.travelledDistance.get(event.getPersonId());
			this.travelledDistance.remove(event.getPersonId()); //could be one step instead
			int distanceInKm = (int) (distance/1000);
			if (distanceInKm>29) {
				distanceInKm = 29;
			//everything above 29km goes into the last bin	
			}
			this.distanceDistribution[distanceInKm]++;
			
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals("car")){
			if (!event.getPersonId().toString().startsWith("pt_")){
				travelledDistance.put(event.getPersonId(), 0.0);
			}
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (this.vehicles2Persons.containsKey(event.getVehicleId())){
		Id<Person> personId = this.vehicles2Persons.get(event.getVehicleId());
		double distanceSoFarTravelled = this.travelledDistance.get(personId);
		double length = this.network.getLinks().get(event.getLinkId()).getLength();
		double newDistanceTravelled = distanceSoFarTravelled+length;
		this.travelledDistance.put(personId, newDistanceTravelled);
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if (this.vehicles2Persons.containsKey(event.getVehicleId())){
			this.vehicles2Persons.remove(event.getVehicleId());
		}
	}
	
	public int[] getDistanceDistribution() {
		return distanceDistribution;
	}

}
