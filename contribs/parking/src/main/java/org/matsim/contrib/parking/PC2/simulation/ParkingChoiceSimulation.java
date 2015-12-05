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
import org.matsim.api.core.v01.Scenario;
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
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PopulationUtils;

public final class ParkingChoiceSimulation
		implements PersonDepartureEventHandler, PersonArrivalEventHandler, ActivityEndEventHandler {

	
	
	private ParkingInfrastructureManager parkingInfrastructureManager;
	private Scenario scenario;
	private IntegerValueHashMap<Id<Person>> currentPlanElementIndex;
	private HashMap<Id<Person>, ParkingOperationRequestAttributes> parkingOperationRequestAttributes;
	private DoubleValueHashMap<Id<Person>> firstDepartureTimeOfDay;

	public ParkingChoiceSimulation(Scenario scenario, ParkingInfrastructureManager parkingInfrastructureManager) {
		this.scenario = scenario;
		this.parkingInfrastructureManager = parkingInfrastructureManager;
	}

	@Override
	public void reset(int iteration) {
		DebugLib.emptyFunctionForSettingBreakPoint();
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		currentPlanElementIndex.increment(event.getPersonId());
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equalsIgnoreCase(TransportMode.car) && !event.getPersonId().toString().contains("pt") && isNotTransitAgent(event.getPersonId())) {
			if (!firstDepartureTimeOfDay.containsKey(event.getPersonId())) {
				firstDepartureTimeOfDay.put(event.getPersonId(), event.getTime());
				// (I think that this is to remember the wrap-around activity.
				// kai, jul'15)
			}

			if (isFirstCarDepartureOfDay(event.getPersonId())) {
				DebugLib.emptyFunctionForSettingBreakPoint();
				ParkingOperationRequestAttributes parkingAttributes = new ParkingOperationRequestAttributes();
				parkingAttributes.personId = event.getPersonId();
				// this is a trick to get the correct departure time
				parkingAttributes.arrivalTime = 0;
				parkingAttributes.parkingDurationInSeconds = GeneralLib.getIntervalDuration(0, event.getTime());
				parkingInfrastructureManager.personCarDepartureEvent(parkingAttributes);
			} else {
				ParkingOperationRequestAttributes parkingAttributes = parkingOperationRequestAttributes
						.get(event.getPersonId());
				parkingAttributes.parkingDurationInSeconds = GeneralLib
						.getIntervalDuration(parkingAttributes.arrivalTime, event.getTime());
				if (parkingAttributes.parkingDurationInSeconds == 24 * 3600) {
					// (yyyy no idea what this is and why. kai, jul'15)

					parkingAttributes.parkingDurationInSeconds = 1; // not zero,
																	// because
																	// this
																	// might
																	// lead to
																	// NaN
				}

				PC2Parking parking = parkingInfrastructureManager.personCarDepartureEvent(parkingAttributes);
				parkingInfrastructureManager.scoreParkingOperation(parkingAttributes, parking);
			}

		}
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		Id<Person> personId = event.getPersonId();
		if (event.getLegMode().equalsIgnoreCase(TransportMode.car)  && !event.getPersonId().toString().contains("pt") && isNotTransitAgent(event.getPersonId())) {
			ParkingOperationRequestAttributes parkingAttributes = new ParkingOperationRequestAttributes();
			Link link = scenario.getNetwork().getLinks().get(event.getLinkId());
			Activity nextActivity = getNextActivity(personId);

			parkingAttributes.destCoordinate = link.getCoord();
			parkingAttributes.arrivalTime = event.getTime();
			parkingAttributes.personId = personId;
			parkingAttributes.facilityId = nextActivity.getFacilityId();
			parkingAttributes.actType = nextActivity.getType();

			if (isLastCarLegOfDay(personId)) {
				parkingAttributes.parkingDurationInSeconds = GeneralLib.getIntervalDuration(event.getTime(),
						firstDepartureTimeOfDay.get(personId));
			} else {
				Activity activityBeforeNextCarLeg = getActivityBeforeNextCarLeg(personId);
				
				double endTime=activityBeforeNextCarLeg.getEndTime();
				double parkingDuration=0;
				
				if (endTime==Double.NEGATIVE_INFINITY || endTime==Double.POSITIVE_INFINITY){
					// try to estimate parking duration
					
					Person person = scenario.getPopulation().getPersons().get(personId);
					List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
					
					for (int i = currentPlanElementIndex.get(personId); i < planElements.size(); i++) {
						if (planElements.get(i) instanceof Activity) {
							parkingDuration+= ((ActivityImpl) planElements.get(i)).getMaximumDuration();
						}
						
						if (planElements.get(i) == activityBeforeNextCarLeg) {
							endTime=event.getTime()+parkingDuration;
							break;
						}
					}
				}
				
				parkingAttributes.parkingDurationInSeconds = GeneralLib.getIntervalDuration(event.getTime(),
						endTime);
			}

			parkingAttributes.legIndex = currentPlanElementIndex.get(personId);

			PC2Parking parking = parkingInfrastructureManager.parkVehicle(parkingAttributes);
			// to me this looks like first the agent arrives at his/her
			// activity. And then the negative parking score is added after the
			// fact, however without consuming time. I.e. there is no physics.
			// kai, jul'15

			if (isLastCarLegOfDay(personId)) {
				parkingInfrastructureManager.scoreParkingOperation(parkingAttributes, parking);
			}

			parkingOperationRequestAttributes.put(personId, parkingAttributes);

		}

		currentPlanElementIndex.increment(personId);
	}
	
	private boolean isNotTransitAgent(Id<Person> persondId) {
		return (Integer.parseInt(persondId.toString())< 1000000000);
	}

	public void prepareForNewIteration() {
		currentPlanElementIndex = new IntegerValueHashMap<>();
		parkingOperationRequestAttributes = new HashMap<>();
		firstDepartureTimeOfDay = new DoubleValueHashMap<>();

		for (Person person : scenario.getPopulation().getPersons().values()) {
			if (PopulationUtils.hasCarLeg(person.getSelectedPlan()) && isNotTransitAgent(person.getId())) {
				DebugLib.traceAgent(person.getId());
				ParkingOperationRequestAttributes parkingAttributes = new ParkingOperationRequestAttributes();

				Activity firstActivityOfDayBeforeDepartingWithCar = PopulationUtils
						.getFirstActivityOfDayBeforeDepartingWithCar(person.getSelectedPlan());
				Activity firstActivityAfterLastCarLegOfDay = PopulationUtils
						.getFirstActivityAfterLastCarLegOfDay(person.getSelectedPlan());

				parkingAttributes.destCoordinate = firstActivityAfterLastCarLegOfDay.getCoord();
				// parkingAttributes.arrivalTime=firstActivityAfterLastCarLegOfDay.getStartTime();
				parkingAttributes.arrivalTime = 0;

				parkingAttributes.personId = person.getId();
				parkingAttributes.facilityId = firstActivityAfterLastCarLegOfDay.getFacilityId();
				parkingAttributes.actType = firstActivityAfterLastCarLegOfDay.getType();

				double startTime = firstActivityAfterLastCarLegOfDay.getStartTime();
				if (startTime == Double.NEGATIVE_INFINITY || startTime == Double.POSITIVE_INFINITY) {
					parkingAttributes.parkingDurationInSeconds = GeneralLib.getIntervalDuration(0,
							firstActivityOfDayBeforeDepartingWithCar.getEndTime());
				} else {
					parkingAttributes.parkingDurationInSeconds = GeneralLib.getIntervalDuration(startTime,
							firstActivityOfDayBeforeDepartingWithCar.getEndTime());
				}
				parkingAttributes.legIndex = 0;

				parkingInfrastructureManager.parkVehicle(parkingAttributes);
			}
		}
	}

	// === only private helper functions below this line ===

	private boolean isFirstCarDepartureOfDay(Id<Person> personId) {
		Person person = scenario.getPopulation().getPersons().get(personId);
		
		if (person==null){
			DebugLib.stopSystemAndReportInconsistency();
		}
		
		List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
		for (int i = currentPlanElementIndex.get(personId) - 1; i >= 0; i--) {
			if (planElements.get(i) instanceof Leg) {
				Leg leg = (Leg) planElements.get(i);

				if (leg.getMode().equalsIgnoreCase(TransportMode.car)) {
					return false;
				}

			}
		}
		return true;
	}

	// TODO: operation could be made faster through caching.
	private boolean isLastCarLegOfDay(Id<Person> personId) {
		Person person = scenario.getPopulation().getPersons().get(personId);
		List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
		for (int i = currentPlanElementIndex.get(personId) + 1; i < planElements.size(); i++) {
			if (planElements.get(i) instanceof Leg) {
				Leg Leg = (Leg) planElements.get(i);

				if (Leg.getMode().equalsIgnoreCase(TransportMode.car)) {
					return false;
				}

			}
		}
		return true;
	}

	private Activity getActivityBeforeNextCarLeg(Id<Person> personId) {
		Person person = scenario.getPopulation().getPersons().get(personId);
		List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
		int indexOfNextCarLeg = -1;
		for (int i = currentPlanElementIndex.get(personId) + 1; i < planElements.size(); i++) {
			if (planElements.get(i) instanceof Leg) {
				Leg Leg = (Leg) planElements.get(i);

				if (Leg.getMode().equalsIgnoreCase(TransportMode.car)) {
					indexOfNextCarLeg = i;
					break;
				}

			}
		}

		for (int i = indexOfNextCarLeg - 1; i >= 0; i--) {
			if (planElements.get(i) instanceof Activity) {
				return (Activity) planElements.get(i);
			}
		}

		return null;
	}

	private Activity getNextActivity(Id<Person> personId) {
		Person person = scenario.getPopulation().getPersons().get(personId);
		List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
		for (int i = currentPlanElementIndex.get(personId); i < planElements.size(); i++) {
			if (planElements.get(i) instanceof Activity) {
				return (Activity) planElements.get(i);
			}
		}
		return null;
	}
}
