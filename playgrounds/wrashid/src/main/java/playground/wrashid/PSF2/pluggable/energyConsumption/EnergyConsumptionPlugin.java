/* *********************************************************************** *
 * project: org.matsim.*
 * EnergyConsumptionPlugin.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.wrashid.PSF2.pluggable.energyConsumption;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.contrib.parking.lib.obj.LinkedListValueHashMap;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;

import playground.wrashid.PSF2.vehicle.vehicleFleet.Vehicle;

/**
 * This class assigns one energy consumption value to each trip of a vehicle.
 * 
 * note: this module is compatible with both the quesim and jdeqsim model. the maxAllowedSpeedInNetworkInKmPerHour parameter 
 * to the energyConsumptionModel filters out, if the first or last link have been driven by the vehicle.
 * 
 * @author wrashid
 *
 */
public class EnergyConsumptionPlugin implements LinkEnterEventHandler, LinkLeaveEventHandler, 
VehicleEntersTrafficEventHandler, PersonArrivalEventHandler, VehicleLeavesTrafficEventHandler {
	
	Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler() ;
	
	private EnergyConsumptionModel energyConsumptionModel;
	
	//Id: person
	HashMap<Id<Person>, Double> timeOfEnteringOrWaitingToEnterCurrentLink;
	
	//Id: person
	DoubleValueHashMap<Id<Person>> energyConsumptionOfCurrentLeg;

	//Id: person
	LinkedListValueHashMap<Id<Person>,Double> energyConsumptionOfLegs;
	
	//Id: person
	DoubleValueHashMap<Id<Person>> tripLengthOfCurrentLegInMeters;

	//Id: person
	LinkedListValueHashMap<Id<Person>,Double> tripLengthOfLegsInMeters;
	
	// agent Id, linkId
	HashMap<Id<Person>,Id<Link>> lastLinkEntered;
	
	
	public LinkedListValueHashMap<Id<Person>, Double> getEnergyConsumptionOfLegs() {
		return energyConsumptionOfLegs;
	}
	
	public LinkedListValueHashMap<Id<Person>, Double> getTripLengthOfLegsInMeters() {
		return tripLengthOfLegsInMeters;
	}

	private LinkedListValueHashMap<Id<Vehicle>, Vehicle> vehicles;

	private Network network;

	

	public EnergyConsumptionPlugin(EnergyConsumptionModel energyConsumptionModel, LinkedListValueHashMap<Id<Vehicle>, Vehicle> vehicles, Network network) {
		this.energyConsumptionModel=energyConsumptionModel;
		this.vehicles=vehicles;
		this.network=network;
		
		reset(0);
	}
	
	@Override
	public void reset(int iteration) {
		timeOfEnteringOrWaitingToEnterCurrentLink=new HashMap<>();
		energyConsumptionOfCurrentLeg=new DoubleValueHashMap<>();
		tripLengthOfCurrentLegInMeters=new DoubleValueHashMap<>();
		
		energyConsumptionOfLegs=new LinkedListValueHashMap<>();
		tripLengthOfLegsInMeters=new LinkedListValueHashMap<>();
		
		lastLinkEntered = new HashMap<>();
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id<Person> driverId = delegate.getDriverOfVehicle( event.getVehicleId() ) ;
		
		logLinkEnteranceTime(driverId, event.getTime());
		
		lastLinkEntered.put(driverId, event.getLinkId());
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		this.delegate.handleEvent(event);
		logLinkEnteranceTime(event.getPersonId(), event.getTime());
	}
	
	private void logLinkEnteranceTime(Id<Person> personId, double timeOfEnteranceOfLinkOrWaitingToEnterLink){
		timeOfEnteringOrWaitingToEnterCurrentLink.put(personId, timeOfEnteranceOfLinkOrWaitingToEnterLink);
	}

	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id<Person> driverId = delegate.getDriverOfVehicle( event.getVehicleId() ) ;
		updateEnergyConsumptionOfLeg(driverId,event.getTime(),event.getLinkId());
	}
	
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (isValidArrivalEventWithCar(event.getPersonId(),event.getLinkId())){
			updateEnergyConsumptionOfLeg(event.getPersonId(),event.getTime(),event.getLinkId());
			
			handleLegCompletion(event.getPersonId());
			
			resetLastLinkEntered(event.getPersonId());
		}
		
	}

	private void updateEnergyConsumptionOfLeg(Id<Person> personId,double linkLeaveTime, Id<Link> linkId){
		Double linkEnteranceTime=timeOfEnteringOrWaitingToEnterCurrentLink.get(personId);
		
		if (linkEnteranceTime==null){
			DebugLib.stopSystemAndReportInconsistency();
		}
		
		double timeSpendOnLink= GeneralLib.getIntervalDuration(linkEnteranceTime, linkLeaveTime);
		Link link = network.getLinks().get(linkId);
		
		Vehicle vehicle;
		if (vehicles.containsKey(Id.create(personId, Vehicle.class))){
			vehicle=vehicles.getValue(Id.create(personId, Vehicle.class));
		} else {
			vehicle=vehicles.getValue(Id.create(Vehicle.getPlaceholderForUnmappedPersonIds(), Vehicle.class));
		}
		
		Double energyConsumptionOnLink=energyConsumptionModel.getEnergyConsumptionForLinkInJoule(vehicle, timeSpendOnLink, link);
		
		energyConsumptionOfCurrentLeg.incrementBy(personId, energyConsumptionOnLink);
		tripLengthOfCurrentLegInMeters.incrementBy(personId, link.getLength());
		
		resetLinkEnteranceTime(personId);
	}
	
	private void resetLinkEnteranceTime(Id<Person> personId){
		timeOfEnteringOrWaitingToEnterCurrentLink.remove(personId);
	}
	
	private void handleLegCompletion(Id<Person> personId) {
		energyConsumptionOfLegs.put(personId, energyConsumptionOfCurrentLeg.get(personId));
		energyConsumptionOfCurrentLeg.put(personId, 0.0);
		
		tripLengthOfLegsInMeters.put(personId, tripLengthOfCurrentLegInMeters.get(personId));
		tripLengthOfCurrentLegInMeters.put(personId, 0.0);
		
	}
	
	private void resetLastLinkEntered(Id<Person> personId){
		lastLinkEntered.put(personId, null);
	}
	
	private boolean isValidArrivalEventWithCar(Id<Person> personId, Id<Link> linkId){
		return lastLinkEntered.containsKey(personId) && lastLinkEntered.get(personId)!=null && lastLinkEntered.get(personId).equals(linkId);
	}
	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		this.delegate.handleEvent(event);
	}
}
