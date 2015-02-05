/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.mixedTraffic.FDTestSetUp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

/**
 * @author amit
 */

public class PassingEventsUpdator implements LinkEnterEventHandler, LinkLeaveEventHandler, PersonDepartureEventHandler {

	public PassingEventsUpdator() {
		this.personId2LinkEnterTime = new HashMap<>();
		this.personId2LegMode = new HashMap<>();
		this.bikesPassedByEachCar = new ArrayList<Double>();
	}

	private Map<Id<Person>, Double> personId2LinkEnterTime;
	private Map<Id<Person>, String> personId2LegMode;

	private List<Double> bikesPassedByEachCar;

	private final static Id<Link> trackStartLink = Id.createLinkId(0);
	private final static Id<Link> trackEndLink = Id.createLinkId(InputsForFDTestSetUp.SUBDIVISION_FACTOR*3-1);
	private boolean firstBikeLeavingTrack = false;
//	private boolean lastCarLeavingTrack = false;

	@Override
	public void reset(int iteration) {
		this.personId2LinkEnterTime.clear();
		this.personId2LegMode.clear();
		this.bikesPassedByEachCar.clear();
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id<Person> personId = Id.createPersonId(event.getVehicleId());
		if(event.getLinkId().equals(trackStartLink)){
			this.personId2LinkEnterTime.put(personId, event.getTime());
		}
	}

	@Override 
	public void handleEvent(LinkLeaveEvent event){
		Id<Person> personId = Id.createPersonId(event.getVehicleId());

		if (event.getLinkId().equals(trackStartLink)){

			if(this.personId2LegMode.get(personId).equals(TransportMode.bike) && !firstBikeLeavingTrack) firstBikeLeavingTrack = true;

			if(firstBikeLeavingTrack && !this.personId2LegMode.get(personId).equals(TransportMode.bike)) {
				double numberOfBicyclesOvertaken = getNumberOfBicycleOvertaken(personId);
				this.bikesPassedByEachCar.add(numberOfBicyclesOvertaken);
			}
			
			this.personId2LinkEnterTime.remove(personId);
		}
	}

	private double getCars(){
		double cars =0;
		for (Id<Person> personId : this.personId2LegMode.keySet()){
			if(this.personId2LegMode.get(personId).equals(TransportMode.car)) cars++;
		}
		return cars;
	}


	private double getNumberOfBicycleOvertaken(Id<Person> leavingPersonId) {
		double overtakenBicycles =0;
		/* Simply, on a race track, enter time at start of track and leave time at end of track are recoreded, 
		 * Thus, if an agent is leaving, and leaving agent's enter time is more than 5 (for e.g.) vehicles, then
		 * total number of overtaken bikes are 5 
		 */
		for(Id<Person> personId:this.personId2LinkEnterTime.keySet()){
			if(this.personId2LinkEnterTime.get(leavingPersonId) > this.personId2LinkEnterTime.get(personId)){
				overtakenBicycles++;
			}
		}
		return overtakenBicycles;
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		this.personId2LegMode.put(event.getPersonId(), event.getLegMode());
	}

	public double getAvgBikesPassingRate(){
		double avg=0;
		for(double d:this.bikesPassedByEachCar){
			avg += d;
		}
		return avg/this.bikesPassedByEachCar.size();
	}

	public double getTotalBikesPassed(){
		double noOfCarsOnTrack = getCars();
		return noOfCarsOnTrack*getAvgBikesPassingRate();
	}

}
