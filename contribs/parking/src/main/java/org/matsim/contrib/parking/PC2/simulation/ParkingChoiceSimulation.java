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
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.parking.PC2.infrastructure.PC2Parking;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.contrib.parking.lib.obj.IntegerValueHashMap;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;

import java.util.HashMap;
import java.util.List;

public class ParkingChoiceSimulation implements PersonDepartureEventHandler, ActivityStartEventHandler, PersonArrivalEventHandler, ActivityEndEventHandler, BeforeMobsimListener {

	private ParkingInfrastructureManager parkingInfrastructureManager;
	private Controler controler;
	IntegerValueHashMap<Id> currentPlanElementIndex;
	HashMap<Id, ParkingOperationRequestAttributes> parkingOperationRequestAttributes;
	DoubleValueHashMap<Id> firstDepartureTimeOfDay;

	public ParkingChoiceSimulation(Controler controler, ParkingInfrastructureManager parkingInfrastructureManager){
		this.controler = controler;
		this.parkingInfrastructureManager = parkingInfrastructureManager;
	}
	
	@Override
	public void reset(int iteration) {
		DebugLib.emptyFunctionForSettingBreakPoint();
	}
	
	public ActivityImpl getFirstActivityAfterLastCarLegOfDay(Plan plan){
		List<PlanElement> planElements = plan.getPlanElements();
		int indexOfLastCarLegOfDay=-1;
		for (int i=planElements.size()-1;i>=0;i--){
			if (planElements.get(i) instanceof LegImpl){
				LegImpl Leg= (LegImpl) planElements.get(i);
				
				if (Leg.getMode().equalsIgnoreCase(TransportMode.car)){
					indexOfLastCarLegOfDay=i;
					break;
				}
				
			}
		}
		
		for (int i=indexOfLastCarLegOfDay+1;i<planElements.size();i++){
			if (planElements.get(i) instanceof ActivityImpl){
				return (ActivityImpl) planElements.get(i);
			}
		}
		return null;
	}
	
	public ActivityImpl getFirstActivityOfDayBeforeDepartingWithCar(Plan plan){
		List<PlanElement> planElements = plan.getPlanElements();
		int indexOfFirstCarLegOfDay=-1;
		for (int i=0;i<planElements.size();i++){
			if (planElements.get(i) instanceof LegImpl){
				LegImpl Leg= (LegImpl) planElements.get(i);
				
				if (Leg.getMode().equalsIgnoreCase(TransportMode.car)){
					indexOfFirstCarLegOfDay=i;
					break;
				}
				
			}
		}
		for (int i=indexOfFirstCarLegOfDay-1;i>=0;i--){
			if (planElements.get(i) instanceof ActivityImpl){
				return (ActivityImpl) planElements.get(i);
			}
		}
		return null;
	}
	
	public boolean hasCarLeg(Plan plan){
		List<PlanElement> planElements = plan.getPlanElements();
		for (int i=0;i<planElements.size();i++){
			if (planElements.get(i) instanceof LegImpl){
				LegImpl Leg= (LegImpl) planElements.get(i);
				
				if (Leg.getMode().equalsIgnoreCase(TransportMode.car)){
					return true;
				}
				
			}
		}
		return false;
	}
	

	@Override
	public void handleEvent(ActivityStartEvent event) {
		
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equalsIgnoreCase(TransportMode.car)){
			if (!firstDepartureTimeOfDay.containsKey(event.getPersonId())){
				firstDepartureTimeOfDay.put(event.getPersonId(),event.getTime());
			}
			
			
			if (isFirstCarDepartureOfDay(event.getPersonId())){
				DebugLib.emptyFunctionForSettingBreakPoint();
				ParkingOperationRequestAttributes parkingAttributes = new ParkingOperationRequestAttributes();
				parkingAttributes.personId=event.getPersonId();
				// this is a trick to get the correct departure time
				parkingAttributes.arrivalTime=0;
				parkingAttributes.parkingDurationInSeconds=GeneralLib.getIntervalDuration(0, event.getTime());
				parkingInfrastructureManager.personCarDepartureEvent(parkingAttributes);
			} else {
				ParkingOperationRequestAttributes parkingAttributes = parkingOperationRequestAttributes.get(event.getPersonId());
				parkingAttributes.parkingDurationInSeconds=GeneralLib.getIntervalDuration(parkingAttributes.arrivalTime, event.getTime());
				if (parkingAttributes.parkingDurationInSeconds==24*3600){
					parkingAttributes.parkingDurationInSeconds=1; // not zero, because this might lead to NaN
				}
				
				PC2Parking parking = parkingInfrastructureManager.personCarDepartureEvent(parkingAttributes);
				parkingInfrastructureManager.scoreParkingOperation(parkingAttributes,parking);
			}
			
			
		}
	}

	private boolean isFirstCarDepartureOfDay(Id personId) {
        Person person = controler.getScenario().getPopulation().getPersons().get(personId);
		List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
		for (int i=currentPlanElementIndex.get(personId)-1;i>=0;i--){
			if (planElements.get(i) instanceof LegImpl){
				LegImpl Leg= (LegImpl) planElements.get(i);
				
				if (Leg.getMode().equalsIgnoreCase(TransportMode.car)){
					return false;
				}
				
			}
		}
		return true;
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
            Link link = controler.getScenario().getNetwork().getLinks().get(event.getLinkId());
			ActivityImpl nextActivity = getNextActivity(personId);
			
			parkingAttributes.destCoordinate=link.getCoord();
			parkingAttributes.arrivalTime=event.getTime();
			parkingAttributes.personId=personId;
			parkingAttributes.facilityId=nextActivity.getFacilityId();
			parkingAttributes.actType=nextActivity.getType();
			
			if (isLastCarLegOfDay(personId)){
				parkingAttributes.parkingDurationInSeconds=GeneralLib.getIntervalDuration(event.getTime(), firstDepartureTimeOfDay.get(personId));
				
			} else {
				ActivityImpl activityBeforeNextCarLeg = getActivityBeforeNextCarLeg(personId);
				parkingAttributes.parkingDurationInSeconds=GeneralLib.getIntervalDuration(event.getTime(), activityBeforeNextCarLeg.getEndTime());
			}
			
			parkingAttributes.legIndex=currentPlanElementIndex.get(personId);
			
			PC2Parking parking = parkingInfrastructureManager.parkVehicle(parkingAttributes);
			
			if (isLastCarLegOfDay(personId)){
				parkingInfrastructureManager.scoreParkingOperation(parkingAttributes,parking);
			}
			
			parkingOperationRequestAttributes.put(personId, parkingAttributes);
			
		}
		
		
		
		currentPlanElementIndex.increment(personId);
	}
	
	// TODO: operation could be made faster through caching.
	private boolean isLastCarLegOfDay(Id personId){
        Person person = controler.getScenario().getPopulation().getPersons().get(personId);
		List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
		for (int i=currentPlanElementIndex.get(personId)+1;i<planElements.size();i++){
			if (planElements.get(i) instanceof LegImpl){
				LegImpl Leg= (LegImpl) planElements.get(i);
				
				if (Leg.getMode().equalsIgnoreCase(TransportMode.car)){
					return false;
				}
				
			}
		}
		return true;
	}
	
	private ActivityImpl getActivityBeforeNextCarLeg(Id personId){
        Person person = controler.getScenario().getPopulation().getPersons().get(personId);
		List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
		int indexOfNextCarLeg=-1;
		for (int i=currentPlanElementIndex.get(personId)+1;i<planElements.size();i++){
			if (planElements.get(i) instanceof LegImpl){
				LegImpl Leg= (LegImpl) planElements.get(i);
				
				if (Leg.getMode().equalsIgnoreCase(TransportMode.car)){
					indexOfNextCarLeg=i;
					break;
				}
				
			}
		}
		
		for (int i=indexOfNextCarLeg-1;i>=0;i--){
			if (planElements.get(i) instanceof ActivityImpl){
				return (ActivityImpl) planElements.get(i);
			}
		}
		
		return null;
	}
	
	private ActivityImpl getNextActivity(Id personId){
        Person person = controler.getScenario().getPopulation().getPersons().get(personId);
		List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
		for (int i=currentPlanElementIndex.get(personId);i<planElements.size();i++){
			if (planElements.get(i) instanceof ActivityImpl){
				return (ActivityImpl) planElements.get(i);
			}
		}
		return null;
	}

	public void prepareForNewIteration() {
		currentPlanElementIndex=new IntegerValueHashMap<Id>();
		parkingOperationRequestAttributes=new HashMap<Id, ParkingOperationRequestAttributes>();
		firstDepartureTimeOfDay=new DoubleValueHashMap<Id>();

        for (Person person: controler.getScenario().getPopulation().getPersons().values()){
			if (hasCarLeg(person.getSelectedPlan())){
				DebugLib.traceAgent(person.getId());
				ParkingOperationRequestAttributes parkingAttributes = new ParkingOperationRequestAttributes();
				
				ActivityImpl firstActivityOfDayBeforeDepartingWithCar = getFirstActivityOfDayBeforeDepartingWithCar(person.getSelectedPlan());
				ActivityImpl firstActivityAfterLastCarLegOfDay = getFirstActivityAfterLastCarLegOfDay(person.getSelectedPlan());
				
				parkingAttributes.destCoordinate=firstActivityAfterLastCarLegOfDay.getCoord();
				//parkingAttributes.arrivalTime=firstActivityAfterLastCarLegOfDay.getStartTime();
				parkingAttributes.arrivalTime=0;
				
				parkingAttributes.personId=person.getId();
				parkingAttributes.facilityId=firstActivityAfterLastCarLegOfDay.getFacilityId();
				parkingAttributes.actType=firstActivityAfterLastCarLegOfDay.getType();
				parkingAttributes.parkingDurationInSeconds=GeneralLib.getIntervalDuration(firstActivityAfterLastCarLegOfDay.getStartTime(), firstActivityOfDayBeforeDepartingWithCar.getEndTime());
				
				parkingAttributes.legIndex=0;
				
				parkingInfrastructureManager.parkVehicle(parkingAttributes);
			}
		}
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		DebugLib.emptyFunctionForSettingBreakPoint();
	}
	
}
