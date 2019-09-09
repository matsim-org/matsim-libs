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
package playground.vsp.congestion.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.population.Person;

import playground.vsp.congestion.events.CongestionEvent;
import playground.vsp.congestion.handlers.CongestionEventHandler;

/**
 * 
 * @author ikaddoura , lkroeger
 *
 */
public class CongestionAnalysisEventHandler implements PersonMoneyEventHandler, TransitDriverStartsEventHandler , ActivityEndEventHandler , PersonDepartureEventHandler , LinkEnterEventHandler, PersonEntersVehicleEventHandler , PersonLeavesVehicleEventHandler , CongestionEventHandler {
	private final static Logger log = Logger.getLogger(CongestionAnalysisEventHandler.class);
	private final double vtts_car;
	
	// This analysis uses either money events or congestion events.
	private final boolean useMoneyEvents;
	
	private Map<Id<Person>,Integer> personId2actualTripNumber = new HashMap<Id<Person>, Integer>();
	private Map<Id<Person>,Map<Integer,String>> personId2tripNumber2legMode = new HashMap<Id<Person>,Map<Integer,String>>();
	
	private Map<Id<Person>,Map<Integer,Double>> personId2tripNumber2departureTime = new HashMap<Id<Person>, Map<Integer,Double>>();
	private Map<Id<Person>,Map<Integer,Double>> personId2tripNumber2tripDistance = new HashMap<Id<Person>, Map<Integer,Double>>();
	private Map<Id<Person>,Map<Integer,Double>> personId2tripNumber2amount = new HashMap<Id<Person>, Map<Integer,Double>>();
	private Map<Id<Person>,Double> driverId2totalDistance = new HashMap<Id<Person>,Double>();
	
	private Map<Id<Person>, Double> causingAgentId2amountSum = new HashMap <Id<Person>, Double>();
	private Map<Id<Person>, Double> affectedAgentId2amountSum = new HashMap <Id<Person>, Double>();
	private Set<Id<Person>> persons = new HashSet<Id<Person>>();
	
	// for pt-distance calculation
	private Map<Id<Person>,Double> personId2distanceEnterValue = new HashMap<Id<Person>,Double>();
	
	private List<Id<Person>> ptDrivers = new ArrayList<Id<Person>>();
	private Scenario scenario;
	
	private double distance = 500.; // TODO: set dynamically!
	private double maxDistance = 40 * distance; // TODO: set dynamically!
	private double timeBinSize = 900.0; // TODO: set dynamically!
	
	public CongestionAnalysisEventHandler(Scenario scenario, boolean useMoneyEvents) {
		this.scenario = scenario;
		this.useMoneyEvents = useMoneyEvents;
		log.info("UseMoneyEvents : " + useMoneyEvents);
		if (useMoneyEvents) {
			log.warn("Money events may be thrown later than the congestion events... May result in a wrong interpretation of the results. Better use directly the congestion events for analysis.");
		}
		this.vtts_car = (this.scenario.getConfig().planCalcScore().getModes().get(TransportMode.car).getMarginalUtilityOfTraveling() - this.scenario.getConfig().planCalcScore().getPerforming_utils_hr()) / this.scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();
		log.info("Anlayzing the congestion events during the simulation. Assuming the following VTTS (equal for all agents): " + vtts_car);
	}

	@Override
	public void reset(int iteration) {
		personId2actualTripNumber.clear();
		personId2tripNumber2departureTime.clear();
		personId2tripNumber2tripDistance.clear();
		personId2tripNumber2amount.clear();
		personId2tripNumber2legMode.clear();
		driverId2totalDistance.clear();
		personId2distanceEnterValue.clear();
		ptDrivers.clear(); // not really necessary
		causingAgentId2amountSum.clear();
		affectedAgentId2amountSum.clear();
		this.persons.clear();
	}

	@Override
	public void handleEvent(CongestionEvent event) {
		
		if (useMoneyEvents == false){
			
			// trip-based analysis
			double amount = event.getDelay() / 3600 * this.vtts_car;
			double emergenceTime = event.getEmergenceTime();
			int tripNumber = 0;
			double maxDepTime = 0.;
			Map<Integer,Double> tripNumber2departureTime = personId2tripNumber2departureTime.get(event.getCausingAgentId());
			
			for(int tripNr : tripNumber2departureTime.keySet()) {
				if(emergenceTime >= tripNumber2departureTime.get(tripNr)) {
					if (tripNumber2departureTime.get(tripNr) >= maxDepTime) {
						tripNumber = tripNr;
					}
				}
			}
				
			double amountBefore = personId2tripNumber2amount.get(event.getCausingAgentId()).get(tripNumber);
			double updatedAmount = amountBefore + amount;
			Map<Integer,Double> tripNumber2amount = personId2tripNumber2amount.get(event.getCausingAgentId());
			tripNumber2amount.put(tripNumber, updatedAmount);
			personId2tripNumber2amount.put(event.getCausingAgentId(), tripNumber2amount);
			
			// person-based analysis - causing agent
			if (this.causingAgentId2amountSum.get(event.getCausingAgentId()) == null) {
				this.causingAgentId2amountSum.put(event.getCausingAgentId(), amount);
			} else {
				double amountSoFar = this.causingAgentId2amountSum.get(event.getCausingAgentId());
				double amountNew = amountSoFar + amount;
				this.causingAgentId2amountSum.put(event.getCausingAgentId(), amountNew);
			}
			
			// person-based analysis - affected agent
			if (this.affectedAgentId2amountSum.get(event.getAffectedAgentId()) == null) {
				this.affectedAgentId2amountSum.put(event.getAffectedAgentId(), amount);
			} else {
				double amountSoFar = this.affectedAgentId2amountSum.get(event.getAffectedAgentId());
				double amountNew = amountSoFar + amount;
				this.affectedAgentId2amountSum.put(event.getAffectedAgentId(), amountNew);
			}
		}
	}
	
	@Override
	public void handleEvent(PersonMoneyEvent event) {
				
		if (useMoneyEvents == true) {
			
			// trip-based analysis
			double amount = event.getAmount();
			double eventTime = event.getTime();
			int tripNumber = 0;
			double maxDepTime = 0.;
			Map<Integer,Double> tripNumber2departureTime = personId2tripNumber2departureTime.get(event.getPersonId());
			
			for(int tripNr : tripNumber2departureTime.keySet()) {
				if(eventTime >= tripNumber2departureTime.get(tripNr)) {
					if (tripNumber2departureTime.get(tripNr) >= maxDepTime) {
						tripNumber = tripNr;
					}
				}
			}
				
			double amountBefore = personId2tripNumber2amount.get(event.getPersonId()).get(tripNumber);
			double updatedAmount = amountBefore + amount;
			Map<Integer,Double> tripNumber2amount = personId2tripNumber2amount.get(event.getPersonId());
			tripNumber2amount.put(tripNumber, updatedAmount);
			personId2tripNumber2amount.put(event.getPersonId(), tripNumber2amount);
			
			// person-based analysis - causing agent
			if (this.causingAgentId2amountSum.get(event.getPersonId()) == null) {
				this.causingAgentId2amountSum.put(event.getPersonId(), amount);
			} else {
				double amountSoFar = this.causingAgentId2amountSum.get(event.getPersonId());
				double amountNew = amountSoFar + amount;
				this.causingAgentId2amountSum.put(event.getPersonId(), amountNew);
			}
			
		}
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		double linkLength = this.scenario.getNetwork().getLinks().get(event.getLinkId()).getLength();
		if(ptDrivers.contains(event.getVehicleId())){
			if(driverId2totalDistance.containsKey(event.getVehicleId())){
				driverId2totalDistance.put(Id.createPersonId(event.getVehicleId()),driverId2totalDistance.get(Id.createPersonId(event.getVehicleId())) + linkLength);
			} else {
				driverId2totalDistance.put(Id.createPersonId(event.getVehicleId()),linkLength);
			}
		}else{
			// updating the trip Length
			int tripNumber = personId2actualTripNumber.get(event.getVehicleId());
			double distanceBefore = personId2tripNumber2tripDistance.get(event.getVehicleId()).get(tripNumber);
			double updatedDistance = distanceBefore + linkLength;
			Map<Integer,Double> tripNumber2tripDistance = personId2tripNumber2tripDistance.get(event.getVehicleId());
			tripNumber2tripDistance.put(tripNumber, updatedDistance);
			personId2tripNumber2tripDistance.put(Id.createPersonId(event.getVehicleId()), tripNumber2tripDistance);
		}
	}
	
//	// ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	
	public Map<Id<Person>,List<Double>> getPersonId2listOfDepartureTimes(String mode) {
		Map<Id<Person>,List<Double>> personId2listOfDepartureTimes = new HashMap<Id<Person>, List<Double>>();
		for(Id<Person> personId: personId2tripNumber2departureTime.keySet()){
			List<Double> times = new ArrayList<Double>();
			for(int i : personId2tripNumber2departureTime.get(personId).keySet()){
				if(personId2tripNumber2legMode.get(personId).get(i).toString().equals(mode)){
					double time = personId2tripNumber2departureTime.get(personId).get(i);
					times.add(time);
				}else{
				}
			}
			personId2listOfDepartureTimes.put(personId, times);
		}
		return personId2listOfDepartureTimes;
	}
	
	public Map<Id<Person>,List<Double>> getPersonId2listOfDistances(String mode) {
		Map<Id<Person>,List<Double>> personId2listOfDistances = new HashMap<Id<Person>, List<Double>>();
		for(Id<Person> personId: personId2tripNumber2tripDistance.keySet()){
			List<Double> distances = new ArrayList<Double>();
			for(int i : personId2tripNumber2tripDistance.get(personId).keySet()){
				if(personId2tripNumber2legMode.get(personId).get(i).toString().equals(mode)){
					double distance = personId2tripNumber2tripDistance.get(personId).get(i);
					distances.add(distance);
				}else{
				}
			}
			personId2listOfDistances.put(personId, distances);
		}
		return personId2listOfDistances;
	}
	
	public Map<Id<Person>,List<Double>> getPersonId2listOfAmounts(String mode) {
		Map<Id<Person>,List<Double>> personId2listOfAmounts = new HashMap<Id<Person>, List<Double>>();
		for(Id<Person> personId: personId2tripNumber2amount.keySet()){
			List<Double> amounts = new ArrayList<Double>();
			for(int i : personId2tripNumber2amount.get(personId).keySet()){
				if(personId2tripNumber2legMode.get(personId).get(i).toString().equals(mode)){
					double amount = personId2tripNumber2amount.get(personId).get(i);
					amounts.add(amount);
				}else{
				}
			}
			personId2listOfAmounts.put(personId, amounts);
		}
		return personId2listOfAmounts;
	}
	
	public Map<Double, Double> getAvgAmountPerTripDepartureTime(String mode) {
		Map<Double, Double> tripDepTime2avgFare = new HashMap<Double, Double>();

		Map<Double, List<Double>> tripDepTime2fares = new HashMap<Double, List<Double>>();
		double startTime = this.timeBinSize;
		double periodLength = this.timeBinSize;
		double endTime = 30. * 3600;
		
		for (double time = startTime; time <= endTime; time = time + periodLength){
			List<Double> fares = new ArrayList<Double>();
			tripDepTime2fares.put(time, fares);
		}
		
		Map<Integer, double[]> counter2allDepartureTimesAndAmounts = new HashMap<Integer, double[]>();
		int i = 0;
		
		for(Id<Person> personId : personId2tripNumber2departureTime.keySet()){
			for(int tripNumber : personId2tripNumber2departureTime.get(personId).keySet()){
				if(personId2tripNumber2legMode.get(personId).get(tripNumber).toString().equals(mode)){
					double departureTime = personId2tripNumber2departureTime.get(personId).get(tripNumber);
					double belongingAmount = personId2tripNumber2amount.get(personId).get(tripNumber);
					double[] departureTimeAndAmount = new double[2];
					departureTimeAndAmount[0] = departureTime;
					departureTimeAndAmount[1] = belongingAmount;				
					counter2allDepartureTimesAndAmounts.put(i, departureTimeAndAmount);
					i++;
				}else{
				}
			}
		}
		
		for (Double time : tripDepTime2fares.keySet()){
			for (int counter : counter2allDepartureTimesAndAmounts.keySet()){
				if (counter2allDepartureTimesAndAmounts.get(counter)[0] < time && counter2allDepartureTimesAndAmounts.get(counter)[0] >= (time - periodLength)) {
					if (tripDepTime2fares.containsKey(time)){
						tripDepTime2fares.get(time).add(counter2allDepartureTimesAndAmounts.get(counter)[1]);
					}
				}
			}
		}
		
		for (Double time : tripDepTime2fares.keySet()){
			double amountSum = 0.;
			double counter = 0.;
			for (Double amount : tripDepTime2fares.get(time)){
				if (amount == null){
					
				} else {
					amountSum = amountSum + amount;
					counter++;
				}
			}
			
			double avgFare = 0.;
			if (counter!=0.){
				avgFare = (-1) * amountSum / counter;
			}
			tripDepTime2avgFare.put(time, avgFare);
		}
		return tripDepTime2avgFare;
	}
	
	public Map<Double, Double> getAvgAmountPerTripDistance(String mode) {
		Map<Double, Double> tripDistance2avgAmount = new HashMap<Double, Double>();

		Map<Double, List<Double>> tripDistance2amount = new HashMap<Double, List<Double>>();
		double startDistance = this.distance;
		double groupsize = this.distance;
		double endDistance = this.maxDistance;
		
		for (double distance = startDistance; distance <= endDistance; distance = distance + groupsize){
			List<Double> amounts = new ArrayList<Double>();
			tripDistance2amount.put(distance, amounts);
		}
		
		Map<Integer, double[]> counter2allDistancesAndAmounts = new HashMap<Integer, double[]>();
		int i = 0;
		
		for(Id<Person> personId : personId2tripNumber2tripDistance.keySet()){
			for(int tripNumber : personId2tripNumber2tripDistance.get(personId).keySet()){
				if(personId2tripNumber2legMode.get(personId).get(tripNumber).toString().equals(mode)){
					double tripDistance = personId2tripNumber2tripDistance.get(personId).get(tripNumber);
					double belongingAmount = personId2tripNumber2amount.get(personId).get(tripNumber);
					double[] tripDistanceAndAmount = new double[2];
					tripDistanceAndAmount[0] = tripDistance;
					tripDistanceAndAmount[1] = belongingAmount;				
					counter2allDistancesAndAmounts.put(i, tripDistanceAndAmount);
					i++;
				}
			}
		}
		
		for (Double dist : tripDistance2amount.keySet()){
			for (int counter : counter2allDistancesAndAmounts.keySet()){
				if (counter2allDistancesAndAmounts.get(counter)[0] < dist && counter2allDistancesAndAmounts.get(counter)[0] >= (dist - groupsize)) {
					if (tripDistance2amount.containsKey(dist)){
						tripDistance2amount.get(dist).add(counter2allDistancesAndAmounts.get(counter)[1]);
					}
				}
			}
		}
		
		for (Double dist : tripDistance2amount.keySet()){
			double amountSum = 0.;
			double counter = 0.;
			for (Double amount : tripDistance2amount.get(dist)){
				if (amount == null){
					
				} else {
					amountSum = amountSum + amount;
					counter++;
				}
			}
			
			double avgAmount = 0.;
			if (counter!=0.){
				avgAmount = (-1) * amountSum / counter;
			}
			tripDistance2avgAmount.put(dist, avgAmount);
		}
		return tripDistance2avgAmount;
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		
		if (this.persons.contains(event.getPersonId())){
			// do nothing
		} else {
			this.persons.add(event.getPersonId());
		}
		
		// A transit driver should not practice any activity,
		// otherwise the code has to be adapted here.
		if(ptDrivers.contains(event.getPersonId())){
			throw new RuntimeException("ActivityEndEvent by a transit-driver! The code has to be adapted.");
		}
		if(event.getActType().toString().equals("pt interaction")){
			// pt_interactions are not considered
		} else {
			if(personId2actualTripNumber.containsKey(event.getPersonId())){
				// The trip which starts immediately is at least the second trip of the person
				personId2actualTripNumber.put(event.getPersonId(), personId2actualTripNumber.get(event.getPersonId())+1);
				Map<Integer,Double> tripNumber2departureTime = personId2tripNumber2departureTime.get(event.getPersonId());
				tripNumber2departureTime.put(personId2actualTripNumber.get(event.getPersonId()), event.getTime());
				personId2tripNumber2departureTime.put(event.getPersonId(), tripNumber2departureTime);
				Map<Integer,Double> tripNumber2tripDistance = personId2tripNumber2tripDistance.get(event.getPersonId());
				tripNumber2tripDistance.put(personId2actualTripNumber.get(event.getPersonId()), 0.0);
				personId2tripNumber2tripDistance.put(event.getPersonId(), tripNumber2tripDistance);
					
				Map<Integer,Double> tripNumber2amount = personId2tripNumber2amount.get(event.getPersonId());
				tripNumber2amount.put(personId2actualTripNumber.get(event.getPersonId()), 0.0);
				personId2tripNumber2amount.put(event.getPersonId(), tripNumber2amount);
		
			} else {
				// The trip which starts immediately is the first trip of the person
				personId2actualTripNumber.put(event.getPersonId(), 1);
				Map<Integer,Double> tripNumber2departureTime = new HashMap<Integer, Double>();
				tripNumber2departureTime.put(1, event.getTime());
				personId2tripNumber2departureTime.put(event.getPersonId(), tripNumber2departureTime);
				Map<Integer,Double> tripNumber2tripDistance = new HashMap<Integer, Double>();
				tripNumber2tripDistance.put(1, 0.0);
				personId2tripNumber2tripDistance.put(event.getPersonId(), tripNumber2tripDistance);
				
				Map<Integer,Double> tripNumber2amount = new HashMap<Integer, Double>();
				tripNumber2amount.put(1, 0.0);
				personId2tripNumber2amount.put(event.getPersonId(), tripNumber2amount);
			}
		}	
	}
	
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if(ptDrivers.contains(event.getPersonId())){
			// ptDrivers are not considered
		}else{
			// The leg mode has to be saved here.
			// The actual trip number has just been adapted before
			if(personId2tripNumber2legMode.containsKey(event.getPersonId())){
				// This is at least the second trip.
				int tripNumber = personId2actualTripNumber.get(event.getPersonId());
				Map<Integer,String> tripNumber2legMode = personId2tripNumber2legMode.get(event.getPersonId());
				if(tripNumber2legMode.containsKey(tripNumber)){
					// legMode already listed, possible for pt trips
					if(tripNumber2legMode.get(tripNumber).toString().equals("pt")){	
					} else{
						throw new RuntimeException("A leg mode has already been listed.");
					}
				} else {
					// the leg mode has to be saved.
					String legMode = event.getLegMode();
					if((event.getLegMode().toString().equals(TransportMode.transit_walk))){
						legMode = "pt";
					} else {
					}
					tripNumber2legMode.put(personId2actualTripNumber.get(event.getPersonId()), legMode);
					personId2tripNumber2legMode.put(event.getPersonId(), tripNumber2legMode);
				}
			} else {
				// This is the first trip of the person
				Map<Integer,String> tripNumber2legMode = new HashMap<Integer,String>();
				String legMode = event.getLegMode();
				if((event.getLegMode().toString().equals(TransportMode.transit_walk))){
					legMode = "pt";
				} else {
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
		if(ptDrivers.contains(event.getPersonId())){
			// ptDrivers are not considered
		} else {
			int tripNumber = personId2actualTripNumber.get(event.getPersonId());
			Map<Integer,String> tripNumber2legMode = personId2tripNumber2legMode.get(event.getPersonId());
			if((tripNumber2legMode.get(tripNumber)).equals(TransportMode.car)){
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
		if(ptDrivers.contains(event.getPersonId())){
			// ptDrivers are not considered
		} else {
			int tripNumber = personId2actualTripNumber.get(event.getPersonId());
			Map<Integer,String> tripNumber2legMode = personId2tripNumber2legMode.get(event.getPersonId());
			if((tripNumber2legMode.get(tripNumber)).equals(TransportMode.car)){
			// car drivers not considered here
			} else {
				personId2distanceEnterValue.put(event.getPersonId(), driverId2totalDistance.get(event.getVehicleId()));
			}
		}
	}

	public Map<Id<Person>, Double> getCausingAgentId2amountSum() {
		return causingAgentId2amountSum;
	}
	
	public Map<Id<Person>, Double> getAffectedAgentId2amountSum() {
		return affectedAgentId2amountSum;
	}

	public Map<Id<Person>, Double> getCausingAgentId2amountSumAllAgents() {
		Map<Id<Person>, Double> personId2amountSumAllAgents = new HashMap<Id<Person>, Double>();
		
		List<Id<Person>> personIds = new ArrayList<Id<Person>>();
		if (this.scenario.getPopulation().getPersons().isEmpty()) {
			log.warn("Scenario does not contain a Population. Using the person IDs from the events file for the person-based analysis (total: " + personIds.size() +").");
			personIds.addAll(this.persons);
		} else {
			log.info("Scenario contains a Population. Using the person IDs from the population for the person-based analysis.");
			personIds.addAll(this.scenario.getPopulation().getPersons().keySet());
		}
		
		for (Id<Person> id : personIds) {
			double amountSum = 0.;
			if (this.causingAgentId2amountSum.get(id) == null) {
				// no monetary payments
			} else {
				amountSum = -1.0 * this.causingAgentId2amountSum.get(id);
			}
			personId2amountSumAllAgents.put(id, amountSum);
		}
		return personId2amountSumAllAgents;
	}
	
	public Map<Id<Person>, Double> getAffectedAgentId2amountSumAllAgents() {
		Map<Id<Person>, Double> personId2amountSumAllAgents = new HashMap<Id<Person>, Double>();
		
		if (this.affectedAgentId2amountSum.isEmpty()) {
			log.warn("There is no info re the affected agents. This info is only avalable when analyzing congestion events and not the money events.");		
		
		} else {
			List<Id<Person>> personIds = new ArrayList<Id<Person>>();
			if (this.scenario.getPopulation().getPersons().isEmpty()) {
				log.warn("Scenario does not contain a Population. Using the person IDs from the events file for the person-based analysis (total: " + personIds.size() +").");
				personIds.addAll(this.persons);
			} else {
				log.info("Scenario contains a Population. Using the person IDs from the population for the person-based analysis.");
				personIds.addAll(this.scenario.getPopulation().getPersons().keySet());
			}
			
			for (Id<Person> id : personIds) {
				double amountSum = 0.;
				if (this.affectedAgentId2amountSum.get(id) == null) {
					// no monetary payments
				} else {
					amountSum = -1.0 * this.affectedAgentId2amountSum.get(id);
				}
				personId2amountSumAllAgents.put(id, amountSum);
			}
		}
		return personId2amountSumAllAgents;
	}

}
