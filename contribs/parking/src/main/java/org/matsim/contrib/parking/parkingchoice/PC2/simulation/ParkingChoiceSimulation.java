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

package org.matsim.contrib.parking.parkingchoice.PC2.simulation;

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
import org.matsim.contrib.parking.parkingchoice.PC2.infrastructure.PC2Parking;
import org.matsim.contrib.parking.parkingchoice.lib.GeneralLib;
import org.matsim.contrib.parking.parkingchoice.lib.obj.DoubleValueHashMap;
import org.matsim.contrib.parking.parkingchoice.lib.obj.IntegerValueHashMap;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.misc.OptionalTime;

public final class ParkingChoiceSimulation
		implements PersonDepartureEventHandler, PersonArrivalEventHandler, ActivityEndEventHandler {

	private final ParkingInfrastructure parkingInfrastructureManager;
	private final Scenario scenario;
	private IntegerValueHashMap<Id<Person>> currentPlanElementIndices;
	private HashMap<Id<Person>, ParkingOperationRequestAttributes> parkingOperationRequestAttributes;
	private DoubleValueHashMap<Id<Person>> firstDepartureTimeOfDay;

	public ParkingChoiceSimulation(Scenario scenario, ParkingInfrastructure parkingInfrastructureManager) {
		this.scenario = scenario;
		this.parkingInfrastructureManager = parkingInfrastructureManager;
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		currentPlanElementIndices.increment(event.getPersonId() );
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {

		if (event.getLegMode().equalsIgnoreCase(TransportMode.car) && !event.getPersonId().toString().contains("pt") && isNotTransitAgent(event.getPersonId())) {
			// (exclude some cases (in a brittle way, i.e. based on IDs))

			if (!firstDepartureTimeOfDay.containsKey(event.getPersonId())) {
				firstDepartureTimeOfDay.put(event.getPersonId(), event.getTime());
				// (I think that this is to remember the wrap-around activity. kai, jul'15)
			}

			if (isFirstCarDepartureOfDay(event.getPersonId())) {
				// for the first departure of the day, we do not know when the parking actually started.  The scoring is, in
				// consequence, not done here, but done when _starting_ the _last_ parking of the day (see there).
				// (yy what happens if the agent arrives by car but departs by some other mode, or the other way round?  Can happen in particular if there is an additional home stop during the day.)

				ParkingOperationRequestAttributes parkingAttributes = new ParkingOperationRequestAttributes();
				parkingAttributes.personId = event.getPersonId();

				// for the time being, we memorize a parking record with arrival time zero.   This is corrected later, at the last arrival of the day.
				parkingAttributes.arrivalTime = 0;
				parkingAttributes.parkingDurationInSeconds = GeneralLib.getIntervalDuration(0, event.getTime());
				parkingInfrastructureManager.personCarDepartureEvent(parkingAttributes);
			} else {
				// parking has just ended:

				// finalize the corresponding record:
				ParkingOperationRequestAttributes parkingAttributes = parkingOperationRequestAttributes.get(event.getPersonId());
				parkingAttributes.parkingDurationInSeconds = GeneralLib.getIntervalDuration(parkingAttributes.arrivalTime, event.getTime());

				// hedge against a special case:
				if (parkingAttributes.parkingDurationInSeconds == 24 * 3600) {
					// (yyyy no idea what this is and why. kai, jul'15)
					// (Presumably, the code is such that it cannot handle a parking duration of zero.  Presumably, a parking
					// duration of zero cannot happen, and therefore this is ok.  However, if someone parks for exactly 24 hours,
					// then this is at some point mapped back to zero, and then it may happen. kai, feb'24)

					parkingAttributes.parkingDurationInSeconds = 1; // not zero, because this might lead to NaN
				}

				// score the parking:
				final PC2Parking parking = parkingInfrastructureManager.personCarDepartureEvent( parkingAttributes );
				parkingInfrastructureManager.scoreParkingOperation(parkingAttributes, parking );
			}

		}
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		Id<Person> personId = event.getPersonId();
		if (event.getLegMode().equalsIgnoreCase(TransportMode.car)  && !event.getPersonId().toString().contains("pt") && isNotTransitAgent(event.getPersonId())) {
			// (exclude some cases (in a brittle way, i.e. based on IDs))

			// Generate most of the parking record (departure time will be added later):
			ParkingOperationRequestAttributes parkingAttributes = new ParkingOperationRequestAttributes();
			{
				Link link = scenario.getNetwork().getLinks().get( event.getLinkId() );
				Activity nextActivity = getNextActivity( personId );

				parkingAttributes.destCoordinate = link.getCoord();
				parkingAttributes.arrivalTime = event.getTime();
				parkingAttributes.personId = personId;
				parkingAttributes.facilityId = nextActivity.getFacilityId();
				parkingAttributes.actType = nextActivity.getType();
			}

			if (isLastCarLegOfDay(personId)) {
				// if this is the last arrival of the day, the parking record is already there, since it was generated at the first
				// departure.  However, the duration is not correct since at that time the arrival time was not known.
				// (yy It looks to me that the arrivalTime remains at 0.  why?)
				parkingAttributes.parkingDurationInSeconds = GeneralLib.getIntervalDuration(event.getTime(), firstDepartureTimeOfDay.get(personId));

				// scoring of this special case is done further down, see there

			} else {
				Activity activityBeforeNextCarLeg = getActivityBeforeNextCarLeg(personId);

				double endTime= activityBeforeNextCarLeg.getEndTime().seconds();
				double parkingDuration=0;

				if (endTime==Double.NEGATIVE_INFINITY || endTime==Double.POSITIVE_INFINITY){
					// (in general, we take the (parking) end time from above.  Sometimes, this end time does not have a useful value, in which case the current

					// try to estimate parking duration:

					Person person = scenario.getPopulation().getPersons().get(personId);
					List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();

					for ( int ii = currentPlanElementIndices.get(personId ) ; ii < planElements.size(); ii++) {
						if (planElements.get(ii) instanceof Activity) {
							parkingDuration+= ((Activity)planElements.get(ii)).getMaximumDuration().seconds();
						}

						if (planElements.get(ii) == activityBeforeNextCarLeg) {
							endTime=event.getTime()+parkingDuration;
							break;
						}
					}
				}

				parkingAttributes.parkingDurationInSeconds = GeneralLib.getIntervalDuration(event.getTime(), endTime);
				// (This is the _estimated_ parking duration, since we are at arrival, and in this special case we did not have the
				// corresponding activity end time.  This is needed to define the "best" parking
				// location ... cf. short-term/long-term parking at airports. Could rename the attributed into "expected...", but we
				// have seen at other places in the code that such attributes may change their interpretation based on context so will
				// not do this here.)
			}

			parkingAttributes.legIndex = currentPlanElementIndices.get(personId );

			final PC2Parking parking = parkingInfrastructureManager.parkVehicle(parkingAttributes);
			// to me this looks like first the agent arrives at his/her activity. And then the negative parking score is added after the
			// fact, however without consuming time. I.e. there is no physics. kai, jul'15

			if (isLastCarLegOfDay(personId)) {
				parkingInfrastructureManager.scoreParkingOperation(parkingAttributes, parking);
				// (The last arrival of the day is scored here, since there is no corresponding departure event.)
			}

			parkingOperationRequestAttributes.put(personId, parkingAttributes);

		}

		currentPlanElementIndices.increment(personId );
	}

	public void notifyBeforeMobsim() {
		currentPlanElementIndices = new IntegerValueHashMap<>();
		parkingOperationRequestAttributes = new HashMap<>();
		firstDepartureTimeOfDay = new DoubleValueHashMap<>();

		for (Person person : scenario.getPopulation().getPersons().values()) {
			if (PopulationUtils.hasCarLeg(person.getSelectedPlan()) && isNotTransitAgent(person.getId())) {
				ParkingOperationRequestAttributes parkingAttributes = new ParkingOperationRequestAttributes();

				Activity firstActivityOfDayBeforeDepartingWithCar = PopulationUtils.getFirstActivityOfDayBeforeDepartingWithCar(person.getSelectedPlan());
				Activity firstActivityAfterLastCarLegOfDay = PopulationUtils.getFirstActivityAfterLastCarLegOfDay(person.getSelectedPlan());

				parkingAttributes.destCoordinate = firstActivityAfterLastCarLegOfDay.getCoord();
				// parkingAttributes.arrivalTime=firstActivityAfterLastCarLegOfDay.getStartTime();
				parkingAttributes.arrivalTime = 0;

				parkingAttributes.personId = person.getId();
				parkingAttributes.facilityId = firstActivityAfterLastCarLegOfDay.getFacilityId();
				parkingAttributes.actType = firstActivityAfterLastCarLegOfDay.getType();

				OptionalTime startTime = firstActivityAfterLastCarLegOfDay.getStartTime();
				if (startTime.isUndefined() || startTime.seconds() == Double.POSITIVE_INFINITY) {
					parkingAttributes.parkingDurationInSeconds = GeneralLib.getIntervalDuration(0,
							firstActivityOfDayBeforeDepartingWithCar.getEndTime().seconds());
				} else {
					parkingAttributes.parkingDurationInSeconds = GeneralLib.getIntervalDuration(startTime.seconds(),
							firstActivityOfDayBeforeDepartingWithCar.getEndTime().seconds());
				}
				parkingAttributes.legIndex = 0;

				parkingInfrastructureManager.parkVehicle(parkingAttributes);
			}
		}
	}

	// === only private helper functions below this line ===

	private boolean isNotTransitAgent(Id<Person> persondId) {
		return (Integer.parseInt(persondId.toString())< 1000000000);
	}

	private boolean isFirstCarDepartureOfDay(Id<Person> personId) {
		Person person = scenario.getPopulation().getPersons().get(personId);

		if (person==null){
			throw new Error("system is in inconsistent state");
		}

		List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
		for ( int i = currentPlanElementIndices.get(personId ) - 1 ; i >= 0; i--) {
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
		for ( int i = currentPlanElementIndices.get(personId ) + 1 ; i < planElements.size(); i++) {
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
		for ( int i = currentPlanElementIndices.get(personId ) + 1 ; i < planElements.size(); i++) {
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
		for ( int i = currentPlanElementIndices.get(personId ) ; i < planElements.size(); i++) {
			if (planElements.get(i) instanceof Activity) {
				return (Activity) planElements.get(i);
			}
		}
		return null;
	}
}
