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
	private final Map<Id<Person>, Double> personId2LinkEnterTime;
	private final Map<Id<Person>, String> personId2LegMode;

	private final List<Double> bikesPassedByEachCarPerKm;
	private final List<Double> bikesPassedByAllCarPerKm;
	private final List<Double> carsPerKm;

	private final static Id<Link> TRACKING_START_LINK = Id.createLinkId(0);
	private final static Id<Link> TRACKING_END_LINK = Id.createLinkId(InputsForFDTestSetUp.SUBDIVISION_FACTOR*3-1);
	private boolean isFirstBikeLeavingTrack = false;
	private Id<Person> firstCarId ;
	private double noOfCycles = 0;

	private final Map<Id<Vehicle>, Id<Person>> driverAgents = new HashMap<>();
	
	public PassingEventsUpdator() {
		this.personId2LinkEnterTime = new HashMap<>();
		this.personId2LegMode = new HashMap<>();
		this.bikesPassedByEachCarPerKm = new ArrayList<Double>();
		this.bikesPassedByAllCarPerKm = new ArrayList<Double>();
		this.carsPerKm = new ArrayList<Double>();
	}
	
	@Override
	public void reset(int iteration) {
		this.personId2LinkEnterTime.clear();
		this.personId2LegMode.clear();
		this.bikesPassedByEachCarPerKm.clear();
		this.bikesPassedByAllCarPerKm.clear();
		this.carsPerKm.clear();
		driverAgents.clear();
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id<Person> personId = this.driverAgents.get(event.getVehicleId());
		if(event.getLinkId().equals(TRACKING_START_LINK)){
			this.personId2LinkEnterTime.put(personId, event.getTime());
		}
	}

	private List<Double> tempAvgBikePassedPerCar  = new ArrayList<Double>();
	@Override 
	public void handleEvent(LinkLeaveEvent event){
		Id<Person> personId = this.driverAgents.get(event.getVehicleId());

		if (event.getLinkId().equals(TRACKING_END_LINK)){
			// startsAveraging when first bike leaves test track
			if(this.personId2LegMode.get(personId).equals(TransportMode.bike) && !isFirstBikeLeavingTrack) isFirstBikeLeavingTrack = true;
			
			//start counting cycles when first bike leaves test track
			if(!this.personId2LegMode.get(personId).equals(TransportMode.bike) && isFirstBikeLeavingTrack && noOfCycles==0) {
				firstCarId = personId;
			}

			if(isFirstBikeLeavingTrack && !this.personId2LegMode.get(personId).equals(TransportMode.bike)) {
				double numberOfBicyclesOvertaken = getNumberOfBicycleOvertaken(personId);
				double noOfBikesPerCarPerKm = numberOfBicyclesOvertaken *1000/(InputsForFDTestSetUp.LINK_LENGTH*3);
				this.bikesPassedByEachCarPerKm.add(noOfBikesPerCarPerKm);
				this.tempAvgBikePassedPerCar.add(noOfBikesPerCarPerKm);

				if(firstCarId.equals(personId)) {
					noOfCycles ++;
					double noOfPassedBikesByAllCars =0;
//					for (double d : this.bikesPassedByEachCarPerKm){
//						noOfPassedBikesByAllCars += d;
//					}
//					noOfPassedBikesByAllCars = noOfPassedBikesByAllCars/noOfCycles;
					for (double d : this.tempAvgBikePassedPerCar){
						noOfPassedBikesByAllCars += d;
					}
					double noOfPassedBikesByAllCarsPerKm = noOfPassedBikesByAllCars*1000/(InputsForFDTestSetUp.LINK_LENGTH*3);
					this.bikesPassedByAllCarPerKm.add(noOfPassedBikesByAllCarsPerKm);
					this.tempAvgBikePassedPerCar = new ArrayList<Double>();
				}
				
				double noOfCars = getCars();
				double noOfCarsPerkm = noOfCars*1000/(InputsForFDTestSetUp.LINK_LENGTH*3);
				this.carsPerKm.add(noOfCarsPerkm);
//				this.bikesPassedByAllCarPerKm.add(noOfBikesPerCarPerKm*noOfCarsPerkm);
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

	public double getTotalBikesPassedByAllCarsPerKm(){
//		for(double d:this.bikesPassedByEachCarPerKm){
//			sum += d;
//		}
//		return (sum/noOfCycles)*1000/(InputsForFDTestSetUp.LINK_LENGTH*3);
		return ListUtils.doubleMean(this.bikesPassedByAllCarPerKm);
	}
	
	public double getNoOfCarsPerKm(){
		return ListUtils.doubleMean(this.carsPerKm);
	}

}
