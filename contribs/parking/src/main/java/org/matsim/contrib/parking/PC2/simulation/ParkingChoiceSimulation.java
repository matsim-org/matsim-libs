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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.parking.PC2.infrastructure.PC2Parking;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.contrib.parking.lib.obj.IntegerValueHashMap;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationUtils;

public final class ParkingChoiceSimulation implements PersonDepartureEventHandler, PersonArrivalEventHandler, 
ActivityEndEventHandler {

	private ParkingInfrastructureManager parkingInfrastructureManager;
	private Controler controler;
	private IntegerValueHashMap<Id<Person>> currentPlanElementIndex;
	private HashMap<Id<Person>, ParkingOperationRequestAttributes> parkingOperationRequestAttributes;
	private DoubleValueHashMap<Id<Person>> firstDepartureTimeOfDay;

	public ParkingChoiceSimulation(Controler controler, ParkingInfrastructureManager parkingInfrastructureManager){
		this.controler = controler;
		this.parkingInfrastructureManager = parkingInfrastructureManager;
	}

	@Override
	public void reset(int iteration) {
		DebugLib.emptyFunctionForSettingBreakPoint();
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

	private boolean isFirstCarDepartureOfDay(Id<Person> personId) {
		Person person = controler.getScenario().getPopulation().getPersons().get(personId);
		List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
		for (int i=currentPlanElementIndex.get(personId)-1;i>=0;i--){
			if (planElements.get(i) instanceof Leg){
				Leg leg= (Leg) planElements.get(i);

				if (leg.getMode().equalsIgnoreCase(TransportMode.car)){
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
		Id<Person> personId = event.getPersonId();
		if (event.getLegMode().equalsIgnoreCase(TransportMode.car)){
			ParkingOperationRequestAttributes parkingAttributes =new ParkingOperationRequestAttributes();
			Link link = controler.getScenario().getNetwork().getLinks().get(event.getLinkId());
			Activity nextActivity = getNextActivity(personId);

			parkingAttributes.destCoordinate=link.getCoord();
			parkingAttributes.arrivalTime=event.getTime();
			parkingAttributes.personId=personId;
			parkingAttributes.facilityId=nextActivity.getFacilityId();
			parkingAttributes.actType=nextActivity.getType();

			if (isLastCarLegOfDay(personId)){
				parkingAttributes.parkingDurationInSeconds=GeneralLib.getIntervalDuration(event.getTime(), firstDepartureTimeOfDay.get(personId));

			} else {
				Activity activityBeforeNextCarLeg = getActivityBeforeNextCarLeg(personId);
				parkingAttributes.parkingDurationInSeconds=GeneralLib.getIntervalDuration(event.getTime(), activityBeforeNextCarLeg.getEndTime());
			}

			parkingAttributes.legIndex=currentPlanElementIndex.get(personId);

			PC2Parking parking = parkingInfrastructureManager.parkVehicle(parkingAttributes);
			// to me this looks like first the agent arrives at his/her activity.  And then the negative parking score is added after the
			// fact, however without consuming time.  I.e. there is no physics.  kai, jul'15

			if (isLastCarLegOfDay(personId)){
				parkingInfrastructureManager.scoreParkingOperation(parkingAttributes,parking);
			}

			parkingOperationRequestAttributes.put(personId, parkingAttributes);

		}

		currentPlanElementIndex.increment(personId);
	}

	// TODO: operation could be made faster through caching.
	private boolean isLastCarLegOfDay(Id<Person> personId){
		Person person = controler.getScenario().getPopulation().getPersons().get(personId);
		List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
		for (int i=currentPlanElementIndex.get(personId)+1;i<planElements.size();i++){
			if (planElements.get(i) instanceof Leg){
				Leg Leg= (Leg) planElements.get(i);

				if (Leg.getMode().equalsIgnoreCase(TransportMode.car)){
					return false;
				}

			}
		}
		return true;
	}

	private Activity getActivityBeforeNextCarLeg(Id<Person> personId){
		Person person = controler.getScenario().getPopulation().getPersons().get(personId);
		List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
		int indexOfNextCarLeg=-1;
		for (int i=currentPlanElementIndex.get(personId)+1;i<planElements.size();i++){
			if (planElements.get(i) instanceof Leg){
				Leg Leg= (Leg) planElements.get(i);

				if (Leg.getMode().equalsIgnoreCase(TransportMode.car)){
					indexOfNextCarLeg=i;
					break;
				}

			}
		}

		for (int i=indexOfNextCarLeg-1;i>=0;i--){
			if (planElements.get(i) instanceof Activity){
				return (Activity) planElements.get(i);
			}
		}

		return null;
	}

	private Activity getNextActivity(Id<Person> personId){
		Person person = controler.getScenario().getPopulation().getPersons().get(personId);
		List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
		for (int i=currentPlanElementIndex.get(personId);i<planElements.size();i++){
			if (planElements.get(i) instanceof Activity){
				return (Activity) planElements.get(i);
			}
		}
		return null;
	}

	public void prepareForNewIteration() {
		currentPlanElementIndex=new IntegerValueHashMap<>();
		parkingOperationRequestAttributes=new HashMap<>();
		firstDepartureTimeOfDay=new DoubleValueHashMap<>();

		for (Person person: controler.getScenario().getPopulation().getPersons().values()){
			if (PopulationUtils.hasCarLeg(person.getSelectedPlan())){
				DebugLib.traceAgent(person.getId());
				ParkingOperationRequestAttributes parkingAttributes = new ParkingOperationRequestAttributes();

				Activity firstActivityOfDayBeforeDepartingWithCar = PopulationUtils.getFirstActivityOfDayBeforeDepartingWithCar(person.getSelectedPlan());
				Activity firstActivityAfterLastCarLegOfDay = PopulationUtils.getFirstActivityAfterLastCarLegOfDay(person.getSelectedPlan());

				parkingAttributes.destCoordinate=firstActivityAfterLastCarLegOfDay.getCoord();
				//parkingAttributes.arrivalTime=firstActivityAfterLastCarLegOfDay.getStartTime();
				parkingAttributes.arrivalTime=0;

				parkingAttributes.personId=person.getId();
				parkingAttributes.facilityId=firstActivityAfterLastCarLegOfDay.getFacilityId();
				parkingAttributes.actType=firstActivityAfterLastCarLegOfDay.getType();
				parkingAttributes.parkingDurationInSeconds=GeneralLib.getIntervalDuration(firstActivityAfterLastCarLegOfDay.getStartTime(), 
						firstActivityOfDayBeforeDepartingWithCar.getEndTime());

				parkingAttributes.legIndex=0;

				parkingInfrastructureManager.parkVehicle(parkingAttributes);
			}
		}
	}

}
