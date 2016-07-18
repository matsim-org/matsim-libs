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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

import playground.agarwalamit.utils.ListUtils;

/**
 * @author amit
 */

public class PassingEventsUpdator implements LinkEnterEventHandler, LinkLeaveEventHandler, PersonDepartureEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {
	
	private final Map<Id<Person>, Double> personId2TrackEnterTime;
	
	/**
	 * Along with personId2TrackEnterTime, this is also required, to identify the agents which enter at the same time 
	 * but leave one after other. Without this, it could also return that a bike is passing another bike.
	 */
	private final Map<Id<Person>, Double> personId2LinkEnterTime; 
	private final Map<Id<Person>, String> personId2LegMode;

	private final List<Double> bikesPassedByEachCarPerKm;
	
	private final List<Double> carsPerKm;
	private final Collection<String> seepModes;

	private final static Id<Link> TRACKING_START_LINK = Id.createLinkId(0);
	private final static Id<Link> TRACKING_END_LINK = Id.createLinkId(InputsForFDTestSetUp.SUBDIVISION_FACTOR*3-1);
	private boolean isFirstBikeLeavingTrack = false;

	private final Map<Id<Vehicle>, Id<Person>> driverAgents = new HashMap<>();
	
	public PassingEventsUpdator(final Collection<String> seepModes) {
		this.seepModes = seepModes;
		this.personId2TrackEnterTime = new HashMap<>();
		this.personId2LinkEnterTime = new HashMap<>();
		this.personId2LegMode = new HashMap<>();
		this.bikesPassedByEachCarPerKm = new ArrayList<Double>();
		this.carsPerKm = new ArrayList<Double>();
	}
	public PassingEventsUpdator() {
		this(new ArrayList<>()); //no seepage
	}
	
	@Override
	public void reset(int iteration) {
		this.personId2TrackEnterTime.clear();
		this.personId2LegMode.clear();
		this.bikesPassedByEachCarPerKm.clear();
		this.carsPerKm.clear();
		this.driverAgents.clear();
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id<Person> personId = this.driverAgents.get(event.getVehicleId());
		
		this.personId2LinkEnterTime.put( personId, event.getTime() );
		
		if(event.getLinkId().equals(TRACKING_START_LINK)){
			this.personId2TrackEnterTime.put(personId, event.getTime());
		}
	}

	@Override 
	public void handleEvent(LinkLeaveEvent event){
		Id<Person> personId = this.driverAgents.get(event.getVehicleId());

		if (event.getLinkId().equals(TRACKING_END_LINK)){
			// startsAveraging when first bike leaves test track
			if(this.personId2LegMode.get(personId).equals(TransportMode.bike) && !this.isFirstBikeLeavingTrack) this.isFirstBikeLeavingTrack = true;
			
			if( isFirstBikeLeavingTrack ) {
				double numberOfBicyclesOvertaken = 0;
				if( this.seepModes.contains( this.personId2LegMode.get(personId)) ){
//						this.personId2LegMode.get(personId).equals(TransportMode.bike)) {
					numberOfBicyclesOvertaken = - getNumberOfOvertakenVehicles(personId); // this means bike is overtaking cars (seepage)
				} else if ( this.personId2LegMode.get(personId).equals(TransportMode.car)){ 
					numberOfBicyclesOvertaken = getNumberOfOvertakenVehicles(personId); // this means car is overtaking bikes
				}
				
				double noOfBikesPerCarPerKm = numberOfBicyclesOvertaken *1000/(InputsForFDTestSetUp.LINK_LENGTH*3);
				this.bikesPassedByEachCarPerKm.add(noOfBikesPerCarPerKm);

				double noOfCars = getCars();
				double noOfCarsPerkm = noOfCars*1000/(InputsForFDTestSetUp.LINK_LENGTH*3);
				this.carsPerKm.add(noOfCarsPerkm);
				
			}
			this.personId2TrackEnterTime.remove(personId);
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


	private double getNumberOfOvertakenVehicles(Id<Person> leavingPersonId) {
		double overtakenBicycles =0;
		/* Simply, on a race track, enter time at start of track and leave time at end of track are recoreded, 
		 * Thus, if an agent is leaving, and leaving agent's enter time is more than n (for e.g. 5) vehicles, then
		 * total number of overtaken bikes are n (5). 
		 */
		for(Id<Person> personId:this.personId2TrackEnterTime.keySet()){
			if(this.personId2TrackEnterTime.get(leavingPersonId) > this.personId2TrackEnterTime.get(personId)){
				// check if overtaking is occured between same vehicle type
				if (personId2LegMode.get(personId).equals(this.personId2LegMode.get(leavingPersonId))) {
					// if so, check their enter time on the current link ( could be same or "leavingPersonId" may have entered one sec before "personId"
					if( this.personId2LinkEnterTime.get(leavingPersonId) > this.personId2LinkEnterTime.get(personId)  ){ // excluding same enter time and minor changes due to rouding errors.
						throw new RuntimeException(
								"Overtaking should not happen between same vehicle types. Persons "+ personId+"("+this.personId2LegMode.get(personId)+")"+", "+leavingPersonId
								+"("+this.personId2LegMode.get(leavingPersonId)+")"+" have entered the track at "+ this.personId2TrackEnterTime.get(personId)+ ", "
								+ this.personId2TrackEnterTime.get(leavingPersonId)+" respectively. However, the later agent is leaving first. These persons entered on the current link at "
								+ this.personId2LinkEnterTime.get(personId) + ", "+ this.personId2LinkEnterTime.get(leavingPersonId) +" respectively." );
					} else {
						// do nothing.
					}
				} else {
					overtakenBicycles++;	
				}
			} else {
				// do nothing; no overtaking occured.
			}
		}
		return overtakenBicycles;
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		this.personId2LegMode.put(event.getPersonId(), event.getLegMode());
	}
	
	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		driverAgents.put(event.getVehicleId(), event.getPersonId());
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		driverAgents.remove(event.getVehicleId());
	}

	public double getAvgBikesPassingRate(){
		return ListUtils.doubleMean(this.bikesPassedByEachCarPerKm);
	}

	public double getNoOfCarsPerKm(){
		return ListUtils.doubleMean(this.carsPerKm);
	}
}