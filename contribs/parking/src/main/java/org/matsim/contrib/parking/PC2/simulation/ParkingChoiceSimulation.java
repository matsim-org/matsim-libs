/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.parking.PC2.simulation;

import java.util.HashMap;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.IntegerValueHashMap;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.ActivityImpl;

public class ParkingChoiceSimulation implements PersonDepartureEventHandler, ActivityStartEventHandler, PersonArrivalEventHandler, ActivityEndEventHandler {

	private ParkingInfrastructureManager parkingInfrastructureManager;
	private Controler controler;
	IntegerValueHashMap<Id> currentPlanElementIndex;
	HashMap<Id, ParkingOperationRequestAttributes> parkingOperationRequestAttributes;

	public ParkingChoiceSimulation(Controler controler, ParkingInfrastructureManager parkingInfrastructureManager){
		this.controler = controler;
		this.parkingInfrastructureManager = parkingInfrastructureManager;
	}
	
	@Override
	public void reset(int iteration) {
		currentPlanElementIndex=new IntegerValueHashMap<Id>();
		parkingOperationRequestAttributes=new HashMap<Id, ParkingOperationRequestAttributes>();
	}
	

	@Override
	public void handleEvent(ActivityStartEvent event) {
		
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equalsIgnoreCase(TransportMode.car)){
			ParkingOperationRequestAttributes parkingAttributes = parkingOperationRequestAttributes.get(event.getPersonId());
			parkingAttributes.parkingDurationInSeconds=GeneralLib.getIntervalDuration(parkingAttributes.arrivalTime, event.getTime());
			
			parkingInfrastructureManager.personCarDepartureEvent(parkingAttributes);
			
			
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		currentPlanElementIndex.increment(event.getPersonId());
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		Id personId = event.getPersonId();
		if (event.getLegMode().equalsIgnoreCase(TransportMode.car)){
			ParkingOperationRequestAttributes parkingAttributes =new ParkingOperationRequestAttributes();
			Link link = controler.getNetwork().getLinks().get(event.getLinkId());
			ActivityImpl activity = getNextActivity(personId);
			// TODO: fix this. 
			parkingAttributes.destCoordinate=link.getCoord();
			parkingAttributes.arrivalTime=event.getTime();
			parkingAttributes.parkingDurationInSeconds=GeneralLib.getIntervalDuration(event.getTime(), activity.getEndTime());
			parkingAttributes.personId=personId;
			parkingAttributes.facilityId=activity.getFacilityId();
			parkingAttributes.actType=activity.getType();
			
			parkingInfrastructureManager.parkVehicle(parkingAttributes);
		}
		
		
		
		currentPlanElementIndex.increment(personId);
	}
	
	private ActivityImpl getNextActivity(Id personId){
		Person person = controler.getPopulation().getPersons().get(personId);
		List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
		for (int i=currentPlanElementIndex.get(personId);i<planElements.size();i++){
			if (planElements.get(i) instanceof ActivityImpl){
				return (ActivityImpl) planElements.get(i);
			}
		}
		return null;
	}
	
}
