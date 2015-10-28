/* *********************************************************************** *
 * project: org.matsim.*
 * MoneyEventHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.population.Person;

/**
 * 
 * @author ikaddoura , lkroeger
 *
 */
public class BasicPersonTripAnalysisHandler implements PersonMoneyEventHandler, TransitDriverStartsEventHandler , ActivityEndEventHandler ,
PersonDepartureEventHandler , PersonArrivalEventHandler , LinkEnterEventHandler, PersonEntersVehicleEventHandler ,
PersonLeavesVehicleEventHandler , PersonStuckEventHandler {
	
	private final static Logger log = Logger.getLogger(BasicPersonTripAnalysisHandler.class);
	
	private final Scenario scenario;
	
	// temporary information
	private final Map<Id<Person>,Integer> personId2currentTripNumber = new HashMap<>();
	private final Map<Id<Person>,Double> personId2distanceEnterValue = new HashMap<Id<Person>,Double>();
	private final Map<Id<Person>,Double> driverId2totalDistance = new HashMap<>();
	
	// information to be stored
	private final Set<Id<Person>> ptDrivers = new HashSet<Id<Person>>();
	private Map<Id<Person>,Map<Integer,String>> personId2tripNumber2legMode = new HashMap<>();
	private Map<Id<Person>,Map<Integer,Double>> personId2tripNumber2departureTime = new HashMap<>();
	private Map<Id<Person>,Map<Integer,Double>> personId2tripNumber2arrivalTime = new HashMap<>();
	private Map<Id<Person>,Map<Integer,Double>> personId2tripNumber2travelTime = new HashMap<>();
	private Map<Id<Person>,Map<Integer,Double>> personId2tripNumber2tripDistance = new HashMap<>();
	private Map<Id<Person>,Map<Integer,Double>> personId2tripNumber2payment = new HashMap<>();
	private Map<Id<Person>,Map<Integer,Boolean>> personId2tripNumber2stuckAbort = new HashMap<>();
	
	private Map<Id<Person>, Double> personId2totalpayments = new HashMap <Id<Person>, Double>();
	
	private double totalPayments = 0.;

	public BasicPersonTripAnalysisHandler(Scenario scenario) {
		this.scenario = scenario;
	}

	@Override
	public void reset(int iteration) {
		personId2currentTripNumber.clear();
		personId2tripNumber2departureTime.clear();
		personId2tripNumber2arrivalTime.clear();
		personId2tripNumber2tripDistance.clear();
		personId2tripNumber2travelTime.clear();
		personId2tripNumber2payment.clear();
		personId2tripNumber2stuckAbort.clear();
		personId2tripNumber2legMode.clear();
		driverId2totalDistance.clear();
		personId2totalpayments.clear();
		personId2distanceEnterValue.clear();
		ptDrivers.clear();
	}
	
	@Override
	public void handleEvent(PersonMoneyEvent event) {	
		
		// trip
		
		totalPayments = totalPayments + ( -1. * event.getAmount() );
		
		int tripNumber = this.personId2currentTripNumber.get(event.getPersonId());
				
		double paymentBefore = personId2tripNumber2payment.get(event.getPersonId()).get(tripNumber);
		double updatedPayment = paymentBefore + (-1. * event.getAmount());
		Map<Integer,Double> tripNumber2payment = personId2tripNumber2payment.get(event.getPersonId());
		tripNumber2payment.put(tripNumber, updatedPayment);
		personId2tripNumber2payment.put(event.getPersonId(), tripNumber2payment);
		
		// person
		
		if (this.personId2totalpayments.get(event.getPersonId()) == null) {
			this.personId2totalpayments.put(event.getPersonId(), event.getAmount() * (-1.));
		} else {
			double amountSoFar = this.personId2totalpayments.get(event.getPersonId());
			double amountNew = amountSoFar + ( event.getAmount() * (-1.) );
			this.personId2totalpayments.put(event.getPersonId(), amountNew);
		}
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		
		double linkLength = this.scenario.getNetwork().getLinks().get(event.getLinkId()).getLength();
		
		if (ptDrivers.contains(event.getVehicleId())) {
			
			if (driverId2totalDistance.containsKey(event.getVehicleId())) {
				driverId2totalDistance.put(Id.createPersonId(event.getVehicleId()), driverId2totalDistance.get(Id.createPersonId(event.getVehicleId())) + linkLength);
			} else {
				driverId2totalDistance.put(Id.createPersonId(event.getVehicleId()), linkLength);
			}
			
		} else {
			// updating the trip Length
			int tripNumber = personId2currentTripNumber.get(event.getVehicleId());
			double distanceBefore = personId2tripNumber2tripDistance.get(event.getVehicleId()).get(tripNumber);
			double updatedDistance = distanceBefore + linkLength;
			Map<Integer,Double> tripNumber2tripDistance = personId2tripNumber2tripDistance.get(event.getVehicleId());
			tripNumber2tripDistance.put(tripNumber, updatedDistance);
			personId2tripNumber2tripDistance.put(Id.createPersonId(event.getVehicleId()), tripNumber2tripDistance);
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		
		if(event.getActType().toString().equals("pt interaction")){
			// pseudo activities are excluded
			
		} else {
			// a "real" activity is ended
			
			if (personId2currentTripNumber.containsKey(event.getPersonId())) {
				// the following trip is at least the person's second trip
				personId2currentTripNumber.put(event.getPersonId(), personId2currentTripNumber.get(event.getPersonId()) + 1);
				
				Map<Integer,Double> tripNumber2departureTime = personId2tripNumber2departureTime.get(event.getPersonId());
				tripNumber2departureTime.put(personId2currentTripNumber.get(event.getPersonId()), event.getTime());
				personId2tripNumber2departureTime.put(event.getPersonId(), tripNumber2departureTime);
				
				Map<Integer,Double> tripNumber2tripDistance = personId2tripNumber2tripDistance.get(event.getPersonId());
				tripNumber2tripDistance.put(personId2currentTripNumber.get(event.getPersonId()), 0.0);
				personId2tripNumber2tripDistance.put(event.getPersonId(), tripNumber2tripDistance);
					
				Map<Integer,Double> tripNumber2amount = personId2tripNumber2payment.get(event.getPersonId());
				tripNumber2amount.put(personId2currentTripNumber.get(event.getPersonId()), 0.0);
				personId2tripNumber2payment.put(event.getPersonId(), tripNumber2amount);
		
			} else {
				// the following trip is the person's first trip
				personId2currentTripNumber.put(event.getPersonId(), 1);
				
				Map<Integer,Double> tripNumber2departureTime = new HashMap<Integer, Double>();
				tripNumber2departureTime.put(1, event.getTime());
				personId2tripNumber2departureTime.put(event.getPersonId(), tripNumber2departureTime);
				
				Map<Integer,Double> tripNumber2tripDistance = new HashMap<Integer, Double>();
				tripNumber2tripDistance.put(1, 0.0);
				personId2tripNumber2tripDistance.put(event.getPersonId(), tripNumber2tripDistance);
				
				Map<Integer,Double> tripNumber2amount = new HashMap<Integer, Double>();
				tripNumber2amount.put(1, 0.0);
				personId2tripNumber2payment.put(event.getPersonId(), tripNumber2amount);
			}
		}	
	}
	
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (ptDrivers.contains(event.getPersonId())){
			// pt drivers are not considered
			
		} else {
			
			if (personId2tripNumber2legMode.containsKey(event.getPersonId())) {
				// at least the person's second trip
				int tripNumber = personId2currentTripNumber.get(event.getPersonId());
				Map<Integer,String> tripNumber2legMode = personId2tripNumber2legMode.get(event.getPersonId());
				if (tripNumber2legMode.containsKey(tripNumber)){
					if (!tripNumber2legMode.get(tripNumber).toString().equals("pt")){
						throw new RuntimeException("A leg mode has already been listed.");
					}
				} else {
					String legMode = event.getLegMode();
					if((event.getLegMode().toString().equals(TransportMode.transit_walk))){
						legMode = "pt";
					}
					tripNumber2legMode.put(personId2currentTripNumber.get(event.getPersonId()), legMode);
					personId2tripNumber2legMode.put(event.getPersonId(), tripNumber2legMode);
				}
				
			} else {
				// the person's first trip
				Map<Integer,String> tripNumber2legMode = new HashMap<Integer,String>();
				String legMode = event.getLegMode();
				if((event.getLegMode().toString().equals(TransportMode.transit_walk))){
					legMode = "pt";
				}
				tripNumber2legMode.put(1, legMode);
				personId2tripNumber2legMode.put(event.getPersonId(), tripNumber2legMode);
			}
		}
	}
	
	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		if (ptDrivers.contains(event.getDriverId())) {
			// already listed
		} else {
			ptDrivers.add(event.getDriverId());
			driverId2totalDistance.put(Id.createPersonId(event.getVehicleId()),0.0);
		}
	}
	
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if (ptDrivers.contains(event.getPersonId())){
			// ptDrivers are not considered
		
		} else {
			
			int tripNumber = personId2currentTripNumber.get(event.getPersonId());
			Map<Integer,String> tripNumber2legMode = personId2tripNumber2legMode.get(event.getPersonId());
			
			if ((tripNumber2legMode.get(tripNumber)).equals(TransportMode.car)) {
				// car drivers not considered here
				
			} else {
				double distanceTravelled = (driverId2totalDistance.get(event.getVehicleId()) - personId2distanceEnterValue.get(event.getPersonId())); 
				
				Map<Integer,Double> tripNumber2distance = personId2tripNumber2tripDistance.get(event.getPersonId());
				tripNumber2distance.put(tripNumber, tripNumber2distance.get(tripNumber) + distanceTravelled);
				
				personId2distanceEnterValue.remove(event.getPersonId());
			}
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		
		if (ptDrivers.contains(event.getPersonId())) {
			// ptDrivers are not considered
		
		} else {
			int tripNumber = personId2currentTripNumber.get(event.getPersonId());
			Map<Integer,String> tripNumber2legMode = personId2tripNumber2legMode.get(event.getPersonId());
			
			if ((tripNumber2legMode.get(tripNumber)).equals(TransportMode.car)){
			// car drivers not considered here
			
			} else {
				personId2distanceEnterValue.put(event.getPersonId(), driverId2totalDistance.get(event.getVehicleId()));
			}
		}
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
			
		int currentTripNumber = this.personId2currentTripNumber.get(event.getPersonId());
		
		// travel time
		
		Map<Integer, Double> tripNumber2travelTime;
		if (this.personId2tripNumber2travelTime.containsKey(event.getPersonId())) {
			tripNumber2travelTime = this.personId2tripNumber2travelTime.get(event.getPersonId());

		} else {
			tripNumber2travelTime = new HashMap<Integer, Double>();
		}
		
		tripNumber2travelTime.put(currentTripNumber, event.getTime() - this.personId2tripNumber2departureTime.get(event.getPersonId()).get(currentTripNumber));
		this.personId2tripNumber2travelTime.put(event.getPersonId(), tripNumber2travelTime);
		
		 // arrival time
		
		Map<Integer, Double> tripNumber2arrivalTime;
		if (this.personId2tripNumber2arrivalTime.containsKey(event.getPersonId())) {
			tripNumber2arrivalTime = this.personId2tripNumber2arrivalTime.get(event.getPersonId());

		} else {
			tripNumber2arrivalTime = new HashMap<Integer, Double>();
		}
		
		tripNumber2arrivalTime.put(currentTripNumber, event.getTime());
		personId2tripNumber2arrivalTime.put(event.getPersonId(), tripNumber2arrivalTime);
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		
		log.info("### Person is stucking. ###");
		log.info(event.toString());
		
		if (this.scenario.getConfig().qsim().isRemoveStuckVehicles() || event.getTime() == 30 * 3600.) { // scenario end time
			
			log.warn("A person is stucking... This may affect the travel time calculation.");
			
			int currentTripNumber = this.personId2currentTripNumber.get(event.getPersonId());

			// travel time

			Map<Integer, Double> tripNumber2travelTime;
			if (this.personId2tripNumber2travelTime.containsKey(event.getPersonId())) {
				tripNumber2travelTime = this.personId2tripNumber2travelTime.get(event.getPersonId());

			} else {
				tripNumber2travelTime = new HashMap<Integer, Double>();
			}
									
			double traveltime = 0.;
			if (event.getTime() == 30 * 3600.) {
				log.warn("The stuck event is thrown at the end of the simulation. Computing the travel time for this trip as follows: simulation end time - trip departure time");
				traveltime = event.getTime() - this.personId2tripNumber2departureTime.get(event.getPersonId()).get(currentTripNumber);
			} else {
				log.warn("Stucking vehicle will be removed and teleported to the destination activity. The travel time cannot be interpreted.");
				traveltime = Double.POSITIVE_INFINITY;
			}
			log.warn("The travel time is set to " + traveltime);
			
			tripNumber2travelTime.put(currentTripNumber, traveltime);
			this.personId2tripNumber2travelTime.put(event.getPersonId(), tripNumber2travelTime);
			
			// stuck and abort 
			
			Map<Integer, Boolean> tripNr2StuckAbort;
			if (this.personId2tripNumber2stuckAbort.containsKey(event.getPersonId())) {
				tripNr2StuckAbort = this.personId2tripNumber2stuckAbort.get(event.getPersonId());
			} else {
				tripNr2StuckAbort = new HashMap<Integer, Boolean>();
			}
			
			tripNr2StuckAbort.put(currentTripNumber, true);
			this.personId2tripNumber2stuckAbort.put(event.getPersonId(), tripNr2StuckAbort);
					
		} else {
				// The agent should arrive and a travel time can be calculated.
		}
	}
	
	public Map<Id<Person>, Map<Integer, Boolean>> getPersonId2tripNumber2stuckAbort() {
		return personId2tripNumber2stuckAbort;
	}

	public Map<Id<Person>, Integer> getPersonId2currentTripNumber() {
		return personId2currentTripNumber;
	}

	public Map<Id<Person>, Double> getPersonId2distanceEnterValue() {
		return personId2distanceEnterValue;
	}

	public Map<Id<Person>, Map<Integer, String>> getPersonId2tripNumber2legMode() {
		return personId2tripNumber2legMode;
	}

	public Map<Id<Person>, Map<Integer, Double>> getPersonId2tripNumber2departureTime() {
		return personId2tripNumber2departureTime;
	}

	public Map<Id<Person>, Map<Integer, Double>> getPersonId2tripNumber2arrivalTime() {
		return personId2tripNumber2arrivalTime;
	}

	public Map<Id<Person>, Map<Integer, Double>> getPersonId2tripNumber2travelTime() {
		return personId2tripNumber2travelTime;
	}

	public Map<Id<Person>, Map<Integer, Double>> getPersonId2tripNumber2tripDistance() {
		return personId2tripNumber2tripDistance;
	}

	int n = 0;
	public Map<Id<Person>, Map<Integer, Double>> getPersonId2tripNumber2payment() {
		
		if (n == 0) {
			log.warn("No guarantee that monetary payments are ascribed to the right trip (money events, i.e. tolls, may be charged after the person has started the next trip).");
			log.warn("Additional warnings of this type are suppressed.");
		}
		n++;

		return personId2tripNumber2payment;
	}

	public double getTotalPayments() {
		return totalPayments;
	}

	public Scenario getScenario() {
		return scenario;
	}

}
